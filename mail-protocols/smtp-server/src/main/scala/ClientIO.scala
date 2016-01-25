import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket

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
class ClientIO(val socket: Socket) {
  val out = new PrintWriter(socket.getOutputStream(), true)
  val in = new InputStreamReader(socket.getInputStream())
  val inBuffered = new BufferedReader(in)

  def send(msg: String): Unit = {
    out.println(msg)
  }

  def receive(): String = {
    val chars = new Array[Char](256)
    in.read(chars)
    return new String(chars.takeWhile(x => x != 0))
  }

  /**
   * Reads the input stream from client but ignores line feeds and carriage returns (\r\n)
   * @return message from client as String without CR or LF
   */
  def recIgnoreCRLF(): String = {
    return inBuffered.readLine()
  }
}
