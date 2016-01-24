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
 * Created by markus on 24.1.2016.
 */
trait Inbox {
  def put(mail: Email)
  def getMails(): List[Email]
}

object Inbox {
  def create(): InboxImpl = {
    return new InboxImpl()
  }
}

class InboxImpl extends Inbox {

  val mails = List()

  override def put(mail: Email): Unit = {
    mails :+ mail
  }

  override def getMails(): List[Email] = {
    return mails
  }
}
