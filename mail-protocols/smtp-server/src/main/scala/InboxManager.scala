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
object InboxManager {
  var userToInbox: Map[EmailAddress, Inbox] = Map()

  def put(email: Email): Unit = {
    userToInbox.synchronized {
      email.to
        .map(x => if (userToInbox.contains(x)) userToInbox(x).put(email) else {
          userToInbox += (x -> Inbox.create())
          userToInbox(x).put(email)
        })
    }
  }

  def get(emailAddr: EmailAddress): Option[Inbox] = {
    userToInbox.synchronized {
      return userToInbox.get(emailAddr)
    }
  }
}
