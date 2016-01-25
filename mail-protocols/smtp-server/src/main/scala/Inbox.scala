import java.util.concurrent.atomic.AtomicInteger

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
  def getMailRefs(): List[MailRef]
  def getMail(mailRef: MailRef): Email
}

object Inbox {
  def create(): InboxImpl = {
    return new InboxImpl()
  }
}

class InboxImpl extends Inbox {

  var mails: Map[MailRef, Email] = Map()
  val idGen = new AtomicInteger(0)

  override def put(mail: Email): Unit = {
    mails += (new MailRef(nextId().toString(), mail.mail.getBytes().length.toString()) -> mail)
  }

  override def getMails(): List[Email] = {
    return mails.values.toList
  }

  override def getMailRefs(): List[MailRef] = {
    return mails.keys.toList
  }

  override def getMail(mailRef: MailRef): Email = {
    return mails(mailRef)
  }

  private def nextId(): Int = {
    return idGen.getAndIncrement()
  }
}
