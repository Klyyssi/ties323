import java.net.{SocketException, Socket, ServerSocket}

import rx.lang.scala.subjects.PublishSubject

import scala.util.Try

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

/**
 * Created by markus on 25.1.2016.
 */
class Pop3Server(val port: Int) {

  val subject = PublishSubject[Socket]()
  val socket = Try(new ServerSocket(port))
    .getOrElse(throw new SocketException("Couldn't set up serversocket on port " +port))

  println("POP3 server waiting for requests on port " + port)

  subject.subscribe(client => new Thread(new Runnable() {
    def run() {
      val io = new ClientIO(client)
      io.send(new POP3Messages.OK("Welcome!").msg())

      var thisUser: Option[EmailAddress] = None
      var inbox: Option[Inbox] = None

      while(!client.isClosed()) {
        val res = POP3Messages.parseReturnMessage(io.recIgnoreCRLF())

        res match {
          case POP3Messages.USER(user) => // user exists if somebody has sent him email
            thisUser = Some(EmailAddress.fromString(user))
            inbox = InboxManager.get(thisUser.get)
            if (inbox.isDefined) {
              io.send(new POP3Messages.OK("send pass").msg())
            } else {
              io.send(new POP3Messages.ERR("never heard of mailbox name").msg())
            }
          case POP3Messages.PASS(password) =>
            io.send(new POP3Messages.OK("password ok").msg()) // we don't have database for users, and for sake of simplicity im not making one
          case POP3Messages.LIST() =>
            val mailRefs = inbox.get.getMailRefs()
            io.send(new POP3Messages.OK(mailRefs.size + " messages").msg())
            mailRefs.map(ref => io.send(ref.toString()))
            io.send(".")
          case POP3Messages.RETR(id) =>
            val mail = Try(inbox.get.getMail(inbox.get.getMailRefs().filter(x => x.id.equals(id)).head)).get
            io.send(new POP3Messages.OK(mail.mail.getBytes().length + " octets").msg())
            io.send("RCPT TO: " + mail.to
              .map(x => x.email).reduce((a,b) => a + "," + b) +
              "\r\nFROM: " + mail.from.email + "\r\n"
              + mail.mail)
          case POP3Messages.QUIT() =>
            io.send(new POP3Messages.OK("Bye").msg())
            client.close()
        }
      }
    }
  }).start())

  val t = new Thread(new Runnable() {
    def run(): Unit = {
      while (subject.hasObservers) {
        subject.onNext(socket.accept())
      }
      println("TERMINATED")
    }
  })
  t.start()

  private class POP3Protocol {
    var user: String = ""
  }

  private object POP3Messages {
    abstract class Pop3ReturnMessage
    case class USER(val username: String) extends Pop3ReturnMessage
    case class PASS(val password: String) extends Pop3ReturnMessage
    case class LIST() extends Pop3ReturnMessage
    case class QUIT() extends Pop3ReturnMessage
    case class RETR(val id: String) extends Pop3ReturnMessage // extra for the assignment
    case class DEFAULT(val data: String = "") extends Pop3ReturnMessage

    abstract class Pop3Message { def msg():String }
    case class OK(val data: String) extends Pop3Message { def msg():String = {return "+OK " + data}}
    case class ERR(val data: String) extends Pop3Message { def msg():String = { return "-ERR " + data }}

    class DataSeparator(val sep: String)

    private val strToMsgMap: Map[String, (DataSeparator, Function[String, Pop3ReturnMessage])] = Map(
      "USER" -> (new DataSeparator(" "), (x: String) => new USER(x)),
      "PASS" -> (new DataSeparator(" "), (x: String) => new PASS(x)),
      "LIST" -> (new DataSeparator(" "), (x: String) => new LIST()),
      "QUIT" -> (new DataSeparator(" "), (x: String) => new QUIT()),
      "RETR" -> (new DataSeparator(" "), (x: String) => new RETR(x))
    )

    def parseReturnMessage(msg: String): Pop3ReturnMessage = {
      val sepAndFn = strToMsgMap
        .filterKeys(x => msg.toUpperCase().startsWith(x))
        .values
        .headOption
        .getOrElse((new DataSeparator(""), (x: String) => new DEFAULT(x)))

      return sepAndFn._2(msg.substring(msg.indexOf(sepAndFn._1.sep) + sepAndFn._1.sep.length()))
    }
  }
}
