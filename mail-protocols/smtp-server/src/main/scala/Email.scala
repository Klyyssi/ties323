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
class Email(val from: EmailAddress, val to: List[EmailAddress], val mail: String)

class EmailAddress(val email: String) {
  override def equals(other: Any) = other.isInstanceOf[EmailAddress] && email == other.asInstanceOf[EmailAddress].email
}

object EmailAddress {

  val replaceMap = Map(
    "<" -> "",
    ">" -> "",
    "\r" -> "",
    "\n" -> ""
  )

  def fromString(email: String): EmailAddress = {
    return new EmailAddress(replaceMap.foldLeft(email)((a, b) => a.replaceAll(b._1, b._2)))
  }
}

class MailRef(val id: String, val size: String) {
  override def toString():String = {
    return id + " " + size
  }
}

object MailRef {
  def default(): MailRef = { return new MailRef("", "")}
}