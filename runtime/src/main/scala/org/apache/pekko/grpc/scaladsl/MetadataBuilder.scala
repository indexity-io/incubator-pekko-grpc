/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2018-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.grpc.scaladsl

import scala.collection.immutable
import org.apache.pekko
import pekko.annotation.{ ApiMayChange, DoNotInherit }
import pekko.http.scaladsl.model.HttpHeader
import pekko.util.ByteString
import pekko.grpc.internal.{ EntryMetadataImpl, HeaderMetadataImpl, MetadataImpl }

/**
 * This class provides an interface for constructing immutable Metadata instances.
 */
@DoNotInherit
@ApiMayChange
class MetadataBuilder {

  /**
   * The list of metadata entries, in reverse order of addition.
   */
  private var entries: List[(String, MetadataEntry)] = Nil

  /**
   * Adds a string entry. The key must not end in the "-bin" binary suffix.
   * @param key The entry key.
   * @param value The entry value.
   * @return The updated builder.
   */
  def addText(key: String, value: String): MetadataBuilder = {
    if (key.endsWith(MetadataImpl.BINARY_SUFFIX))
      throw new IllegalArgumentException(s"String header names must not end with '${MetadataImpl.BINARY_SUFFIX}'")
    addEntry(key, StringEntry(value))
  }

  /**
   * Adds a binary entry. The key must end in the "-bin" binary suffix.
   * @param key The entry key.
   * @param value The entry value.
   * @return The updated builder.
   */
  def addBinary(key: String, value: ByteString): MetadataBuilder = {
    if (!key.endsWith(MetadataImpl.BINARY_SUFFIX))
      throw new IllegalArgumentException(s"Binary header names must end with '${MetadataImpl.BINARY_SUFFIX}'")
    addEntry(key, BytesEntry(value))
  }

  /**
   * Builds the immutable metadata instance.
   * @return The instance.
   */
  def build(): Metadata = {
    // Reverse the entries list to put it back in order of addition.
    new EntryMetadataImpl(entries.reverse)
  }

  /**
   * Helper method that handles adding an entry to the collection.
   */
  private def addEntry(key: String, entry: MetadataEntry): MetadataBuilder = {
    entries = (key, entry) :: entries
    this
  }
}

object MetadataBuilder {

  /**
   * An empty Metadata instance, for use as a default.
   */
  val empty: Metadata = new EntryMetadataImpl(Nil)

  /**
   * Creates a Metadata instance from a sequence of HTTP headers.
   * @param headers The headers.
   * @return The new Metadata instance.
   */
  def fromHeaders(headers: immutable.Seq[HttpHeader]): Metadata =
    new HeaderMetadataImpl(headers)
}
