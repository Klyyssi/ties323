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
 */

import java.io.{PrintWriter, InputStreamReader, BufferedReader}
import java.net.{Socket, ServerSocket}

import rx.lang.scala.subjects.PublishSubject

/**
 * Created by Markus Mulkahainen on 21.1.2016.
 */
class SmtpServer(val port: Int) {

  val subject = PublishSubject[Socket]()
  val serverSocket = new ServerSocket(port)

  subject.subscribe(client => new Thread(){
    println(client.getLocalAddress().toString() + ":" + client.getLocalPort() + " connected")
    val protocol = new SMTPProtocol()
    val io = new ClientIO(client)
    io.send(s"220 welcome")

    while (!client.isClosed) {
      val msg = protocol.parse(io receive)

      msg.messageType match {
        case protocol.HELO =>
          io.send(s"250 Hello ${msg.value}, I am glad to meet you")
        case protocol.MAILFROM =>
          if (protocol.mailFrom(EmailAddress.fromString(msg.value))) io.send(s"250 Ok") else io.send("500 Syntax error, command unrecognized")
        case protocol.RCPTTO =>
          if (protocol.rcptTo(EmailAddress.fromString(msg.value))) io.send(s"250 Ok") else io.send("500 Syntax error, command unrecognized")
        case protocol.DATA =>
          if (protocol.data(msg.value)) io.send(s"354 End data with <CR><LF>.<CR><LF>") else io.send("500 Syntax error, command unrecognized")
        case protocol.QUIT =>
          io.send(s"221 Bye")
          client.close()
        case protocol.DEFAULT =>
          val res = protocol.appendData(msg.value)
          if (!res._1) {
            io.send("500 Syntax error, command unrecognized")
          } else {
            if (res._2 == protocol.DataStatus.FINISHED) {
              io.send("250 Ok")
              InboxManager.put(res._3.get)
            }
          }
      }
    }
  }.start(),
  x => { //onError
    System.exit(1)
  })

  println("Waiting for incoming requests")

  while (true) {
    subject.onNext(serverSocket.accept())
  }

  class ClientIO(val socket: Socket) {
    val out = new PrintWriter(socket.getOutputStream(), true)
    val in = new InputStreamReader(socket.getInputStream())

    def send(msg: String): Unit = {
      out.println(msg)
    }

    def receive(): String = {
      val chars = new Array[Char](256)
      in.read(chars)
      return new String(chars.takeWhile(x => x != 0))
    }
  }

  class SMTPProtocol {
    sealed abstract class MessageType(val dataSeparator: String)
    case object HELO extends MessageType(" ")
    case object MAILFROM extends MessageType(":")
    case object RCPTTO extends MessageType(":")
    case object DATA extends MessageType("\n")
    case object QUIT extends MessageType(" ")
    case object DEFAULT extends MessageType("")

    object DataStatus extends Enumeration {
      type DataStatus = Value
      val FINISHED, NOT_FINISHED = Value
    }
    import DataStatus._

    val CRLF = Array[Byte](13.toByte, 10.toByte)
    val CRLF_DOT_CRLF = (CRLF :+ 46.toByte) ++ CRLF

    class SMTPMessage(val messageType: MessageType,val value: String = "")

    var status: MessageType = HELO

    private object EMAIL {
      var mailfrom: EmailAddress = _
      var mailTo: scala.collection.mutable.MutableList[EmailAddress] = scala.collection.mutable.MutableList()
      var data: String = ""
    }

    val stringToType: scala.collection.immutable.Map[String, MessageType] = Map(
      "HELO" -> HELO,
      "MAIL FROM:" -> MAILFROM,
      "RCPT TO:" -> RCPTTO,
      "DATA" -> DATA,
      "QUIT" -> QUIT
    )

    def parse(value: String): SMTPMessage = {
      val messageType = stringToType.filterKeys(x => value.startsWith(x)).values.headOption.getOrElse(DEFAULT)
      return new SMTPMessage(messageType, value.substring(value.indexOf(messageType.dataSeparator) + messageType.dataSeparator.length()))
    }

    def mailFrom(email: EmailAddress): Boolean = {
      if (status != HELO) { return false }
      EMAIL.mailfrom = email
      status = RCPTTO
      return true
    }

    def rcptTo(email: EmailAddress): Boolean = {
      if (status != RCPTTO) { return false }
      EMAIL.mailTo.+:(email)
      return true
    }

    def data(value: String): Boolean = {
      if (status != RCPTTO) { return false }
      status = DATA
      return true
    }

    def appendData(value: String): (Boolean, DataStatus, Option[Email]) = {
     if (status != DATA) { return (false, DataStatus.NOT_FINISHED, None) }
     EMAIL.data += value
     if (EMAIL.data.getBytes().endsWith(CRLF_DOT_CRLF)) {
       status = QUIT
       return (true, DataStatus.FINISHED, Some(new Email(EMAIL.mailfrom, EMAIL.mailTo.toList, EMAIL.data)))
     }
     return (true, DataStatus.NOT_FINISHED, None)
    }
  }
}
