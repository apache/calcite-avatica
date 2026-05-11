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
package org.apache.calcite.avatica;

import org.junit.Test;

import java.lang.reflect.Method;
import java.sql.*;
import java.time.Instant;

import static org.junit.Assert.*;

public class AvaticaSiteTest {
  @Test
  public void testToDateIsoAndJdbc() throws Exception {
    Method toDate = org.apache.calcite.avatica.AvaticaSite.class.getDeclaredMethod("toDate",
        Object.class);
    toDate.setAccessible(true);

    assertEquals(Date.valueOf("2025-04-01"), toDate.invoke(null, "2025-04-01"));
    assertEquals(Date.valueOf("2025-04-01"), toDate.invoke(null, "2025-04-01T00:00:00Z"));
    assertEquals(Date.valueOf("2025-04-01"), toDate.invoke(null, "2025-04-01T00:00:00+05:00"));
    assertEquals(Date.valueOf("2025-04-01"), toDate.invoke(null, "2025-04-01T00:00:00.000+05:00"));
    assertEquals(Date.valueOf("2025-04-01"), toDate.invoke(null, "2025-04-01T00:00:00.000Z"));
  }

  @Test
  public void testToTimeIsoAndJdbc() throws Exception {
    Method toTime = org.apache.calcite.avatica.AvaticaSite.class.getDeclaredMethod("toTime",
        Object.class);
    toTime.setAccessible(true);

    assertEquals(Time.valueOf("21:39:50"), toTime.invoke(null, "21:39:50"));
    assertEquals(Time.valueOf("21:39:50"), toTime.invoke(null, "21:39:50Z"));
    assertEquals(Time.valueOf("21:39:50"), toTime.invoke(null, "21:39:50+05:00"));
  }

  @Test
  public void testToTimestampIsoAndJdbc() throws Exception {
    Method toTimestamp = org.apache.calcite.avatica.AvaticaSite.class.getDeclaredMethod(
        "toTimestamp", Object.class);
    toTimestamp.setAccessible(true);

    assertEquals(
        Timestamp.valueOf("2025-08-14 15:53:00.0"),
        toTimestamp.invoke(null, "2025-08-14 15:53:00.0")
    );
    assertEquals(
        Timestamp.valueOf("2025-08-14 15:53:00.0"),
        toTimestamp.invoke(null, "2025-08-14T15:53:00")
    );
    assertEquals(
        Timestamp.from(Instant.parse("2025-08-14T15:53:00.000Z")),
        toTimestamp.invoke(null, "2025-08-14T15:53:00.000Z")
    );
  }
}
