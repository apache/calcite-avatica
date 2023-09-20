/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.avatica.remote.looker;

import org.apache.calcite.avatica.Meta.Frame;

/**
 * Wrapper for either a {@link Frame} or {@link Exception}. Allows for
 * {@link LookerRemoteMeta#stmtQueueMap} to hold complete Frames and present exceptions when
 * consumers are ready to encounter them.
 */
public class LookerFrameEnvelope {

  private final Frame frame;
  private final Exception exception;

  private LookerFrameEnvelope(/*@Nullable*/ Frame frame, /*@Nullable*/ Exception exception) {
    this.frame = frame;
    this.exception = exception;
  }

  public Frame getFrame() {
    return this.frame;
  }

  public Exception getException() {
    return this.exception;
  }

  /**
   * Constructs a LookerFrameEnvelope with a {@link Frame}.
   */
  public static LookerFrameEnvelope ok(long offset, boolean done, Iterable<Object> rows) {
    Frame frame = new Frame(offset, done, rows);
    return new LookerFrameEnvelope(frame, null);
  }

  /**
   * Constructs a LookerFrameEnvelope to hold an exception
   */
  public static LookerFrameEnvelope error(Exception e) {
    return new LookerFrameEnvelope(null, e);
  }

  /**
   * Whether this LookerFrameEnvelope holds an exception. If true, the envelope holds no {@link Frame}.
   */
  public boolean hasException() {
    return this.exception != null;
  }
}
