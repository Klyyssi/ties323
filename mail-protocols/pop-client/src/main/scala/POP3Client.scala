import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{InetSocketAddress, Socket}


/*
 * Copyright (c) 2016 Markus Mulkahainen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 */

/**
 * Created by markus on 22.1.2016.
 */

class POP3Client(val server: String, val port: Int) {

  val socket = new Socket()
  socket.connect(new InetSocketAddress(server, port))
  val io = new ClientIO(socket)
  println(io.receive())

  def login(username: String, password: String): (Boolean, String) = {
    io.send(new POP3Messages.USER(username).msg())
    var msg = POP3Messages.parseReturnMessage(io.receive())
    msg match {
      case POP3Messages.ERR(data) =>
        return (false, data)
      case POP3Messages.DEFAULT(data) =>
        return (false, data)
      case _ =>
        //nothing
    }

    io.send(new POP3Messages.PASS(password).msg())
    msg = POP3Messages.parseReturnMessage(io.receive())
    msg match {
      case POP3Messages.ERR(data) =>
        return (false, data)
      case POP3Messages.DEFAULT(data) =>
        return (false, data)
      case _ =>
        //nothing
    }

    return (true, "Login ok")
  }

  def getMessages(): List[MailRef] = {
    io.send(new POP3Messages.LIST().msg())
    val res = POP3Messages.parseReturnMessage(io.receive())

    res match {
      case POP3Messages.ERR(data) =>
        return List(MailRef.default())
      case POP3Messages.OK(data) =>
        val n = Integer.parseInt(data.split(" ").head) //"n messages"
        return (1 to n)
          .map(_ => io.receive())
          .map(x => new MailRef(x.split(" ").head, x.split(" ").tail.head))
          .toList
    }
  }

  def getMail(mail: MailRef): String = {
    io.send(new POP3Messages.RETR(mail.id).msg())
    val res = POP3Messages.parseReturnMessage(io.receive())
    res match {
      case POP3Messages.ERR(data) =>
        return data
      case _ =>
        //nothing
    }

    return Stream
      .continually(io.receive())
      .takeWhile(x => !x.equals("."))
      .reduce((a,b) => new String(a + "\n" +b))
  }

  def quit(): Unit = {
    io.send(new POP3Messages.QUIT().msg())
  }

  class MailRef(val id: String, val size: String) { override def toString():String = { return id + " " +size}}
  object MailRef { def default(): MailRef = { return new MailRef("", "")}}

  class ClientIO(val socket: Socket) {
    val out = new PrintWriter(socket.getOutputStream(), true)
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream()))

    def send(msg: String): Unit = {
      out.println(msg)
    }

    def receive(): String = {
      return in.readLine()
    }
  }

  private object POP3Messages {
    abstract class Pop3Message { def msg():String }
    case class USER(val username: String) extends Pop3Message { def msg(): String = { return "USER " + username }}
    case class PASS(val password: String) extends Pop3Message { def msg(): String = { return "PASS " + password }}
    case class LIST() extends Pop3Message { def msg(): String = { return "LIST" }}
    case class QUIT() extends Pop3Message { def msg(): String = { return "QUIT" }}
    case class RETR(val id: String) extends Pop3Message { def msg(): String = { return "RETR " + id }} // extra for the assignment

    abstract class Pop3ReturnMessage
    case class OK(val data: String = "") extends Pop3ReturnMessage
    case class ERR(val data: String = "") extends Pop3ReturnMessage
    case class DEFAULT(val data: String = "") extends Pop3ReturnMessage

    class DataSeparator(val sep: String)

    private val strToMsgMap: Map[String, (DataSeparator, Function[String, Pop3ReturnMessage])] = Map(
      "+OK" -> (new DataSeparator(" "), (x: String) => new OK(x)),
      "-ERR" -> (new DataSeparator(" "), (x: String) => new ERR(x))
    )

    def parseReturnMessage(msg: String): Pop3ReturnMessage = {
      val sepAndFn = strToMsgMap
        .filterKeys(x => msg.startsWith(x))
        .values
        .headOption
        .getOrElse((new DataSeparator(""), (x: String) => new DEFAULT(x)))

      return sepAndFn._2(msg.substring(msg.indexOf(sepAndFn._1.sep) + sepAndFn._1.sep.length()))
    }
  }
}
