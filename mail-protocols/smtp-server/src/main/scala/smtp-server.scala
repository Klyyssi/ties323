/*
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
          if (protocol.mailFrom(Parser.toEmail(msg.value))) io.send(s"250 Ok") else io.send("500 Syntax error, command unrecognized")
        case protocol.RCPTTO =>
          if (protocol.rcptTo(Parser.toEmail(msg.value))) io.send(s"250 Ok") else io.send("500 Syntax error, command unrecognized")
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

  class EmailAddress(val email: String)

  object Parser {
    def toEmail(email: String): EmailAddress = {
      return new EmailAddress(email)
    }

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

    object EMAIL {
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

    def appendData(value: String): Tuple2[Boolean, DataStatus] = {
     if (status != DATA) { return (false, DataStatus.NOT_FINISHED) }
     EMAIL.data += value
     if (EMAIL.data.getBytes().endsWith(CRLF_DOT_CRLF)) {
       status = QUIT
       return (true, DataStatus.FINISHED)
     }
     return (true, DataStatus.NOT_FINISHED)
    }
  }
}
