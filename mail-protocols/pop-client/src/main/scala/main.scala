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
 * Created by Markus Mulkahainen on 21.1.2016.
 */
object Main {
  def main(args: Array[String]) = {
    val client = new POP3Client("127.0.0.1", 110)
    val res = client.login("username", "password")
    if (!res._1) { println(res._2); System.exit(1) }
    val list = client.getMessages()
    list.map(x => println(x))
    client.quit()
  }
}
