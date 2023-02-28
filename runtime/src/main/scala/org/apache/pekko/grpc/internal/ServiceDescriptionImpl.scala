/*
 * Copyright (C) 2020-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.grpc.internal

import com.google.protobuf.Descriptors.FileDescriptor;

import org.apache.pekko
import pekko.annotation.InternalApi

import pekko.grpc.ServiceDescription

/**
 * INTERNAL API
 */
@InternalApi
class ServiceDescriptionImpl(val name: String, val descriptor: FileDescriptor) extends ServiceDescription
