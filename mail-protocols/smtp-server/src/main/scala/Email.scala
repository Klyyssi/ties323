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

class EmailAddress(val email: String)

object EmailAddress {
  def fromString(email: String): EmailAddress = {
    return new EmailAddress(email.replaceAll("<","").replaceAll(">",""))
  }
}
