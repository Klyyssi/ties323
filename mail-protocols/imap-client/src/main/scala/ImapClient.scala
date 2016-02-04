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

import java.io.{IOException, InputStreamReader, BufferedReader, PrintWriter}
import java.net.{InetSocketAddress, Socket}

import scala.util.Try

/**
 * Created by markus on 4.2.2016.
 */
@throws(classOf[IOException])
class ImapClient(val server: String, val port: Int) {

  val socket = new Socket()
  socket.connect(new InetSocketAddress(server, port))
  val io = new ClientIO(socket)
  var state: IMAPProtocol.IMAPState = IMAPProtocol.NOT_AUTHENTICATED



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

  private object IMAPProtocol {
    trait IMAPState
    case object NOT_AUTHENTICATED extends IMAPState
    case object AUTHENTICATED extends IMAPState
    case object SELECTED extends IMAPState
    case object LOGOUT extends IMAPState

    abstract class IMAPMessage { def msg():String }
    case class USER(val username: String) extends IMAPMessage { def msg(): String = { return "USER " + username }}
    case class PASS(val password: String) extends IMAPMessage { def msg(): String = { return "PASS " + password }}
    case class LIST() extends IMAPMessage { def msg(): String = { return "LIST" }}
    case class QUIT() extends IMAPMessage { def msg(): String = { return "QUIT" }}
    case class RETR(val id: String) extends IMAPMessage { def msg(): String = { return "RETR " + id }} // extra for the assignment

    abstract class IMAPReturnMessage
    case class OK(val data: String = "") extends IMAPReturnMessage
    case class NO(val data: String = "") extends IMAPReturnMessage
    case class BAD(val data: String = "") extends IMAPReturnMessage
    case class BYE(val data: String = "") extends IMAPReturnMessage
    case class DEFAULT(val data: String = "") extends IMAPReturnMessage

    class DataSeparator(val sep: String)

    private val strToMsgMap: Map[String, (DataSeparator, Function[String, IMAPReturnMessage])] = Map(
      ".{0,5} OK" -> (new DataSeparator(" "), (x: String) => new OK(x)),
      ".{0,5} BAD" -> (new DataSeparator(" "), (x: String) => new BAD(x)),
      ".{0,5} NO" -> (new DataSeparator(" "), (x: String) => new NO(x)),
      ".{0,5} BYE" -> (new DataSeparator(" "), (x: String) => new BYE(x))
    )

    def parseReturnMessage(msg: String): IMAPReturnMessage = {
      val sepAndFn = strToMsgMap
        .filterKeys(x => msg.startsWith(x))
        .values
        .headOption
        .getOrElse((new DataSeparator(""), (x: String) => new DEFAULT(x)))

      return sepAndFn._2(msg.substring(msg.indexOf(sepAndFn._1.sep) + sepAndFn._1.sep.length()))
    }
  }
}
