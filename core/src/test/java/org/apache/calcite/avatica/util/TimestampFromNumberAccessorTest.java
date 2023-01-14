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
package org.apache.calcite.avatica.util;

import org.apache.calcite.avatica.util.AbstractCursor.Getter;
import org.apache.calcite.avatica.util.AbstractCursor.TimestampFromNumberAccessor;

import org.junit.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/** Unit tests for {@link TimestampFromNumberAccessor} */
public class TimestampFromNumberAccessorTest {

  // An example of a calendar that observes DST.
  private static final Calendar LOS_ANGELES_CALENDAR =
      GregorianCalendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"), Locale.ROOT);

  @Test
  public void testNoOffset() throws SQLException {
    test(
        1673657135052L,  // UTC: 2023-01-14 00:45:23.052
        null,
        parseUtc("2023-01-14T00:45:35.052"));
  }

  @Test
  public void testNoOffsetNanoseconds() throws SQLException {
    test(
        new BigDecimal("1673657135.052637485"),  // UTC: 2023-01-14 00:45:23.052637485
        null,
        parseUtc("2023-01-14T00:45:35.052637485"));
  }

  @Test
  public void testNoOffsetDaylightSavings() throws SQLException {
    test(
        1689320723052L,  // UTC: 2023-07-14 07:45:23.052
        null,
        parseUtc("2023-07-14T07:45:23.052"));
  }

  @Test
  public void testNoOffsetDaylightSavingsNanoseconds() throws SQLException {
    test(
        new BigDecimal("1689320723.052637485"),  // UTC: 2023-07-14 07:45:23.052637485
        null,
        parseUtc("2023-07-14T07:45:23.052637485"));
  }

  @Test
  public void testWithOffset() throws SQLException {
    test(
        1673657135052L,  // UTC: 2023-01-14 00:45:23.052
        LOS_ANGELES_CALENDAR,
        parseUtc("2023-01-14T08:45:35.052"));
  }

  @Test
  public void testWithOffsetNanoseconds() throws SQLException {
    test(
        new BigDecimal("1673657135.052637485"),  // UTC: 2023-01-14 00:45:23.052637485
        LOS_ANGELES_CALENDAR,
        parseUtc("2023-01-14T08:45:35.052637485"));
  }

  @Test
  public void testWithOffsetDaylightSavings() throws SQLException {
    test(
        1689320723052L,  // UTC: 2023-07-14 07:45:23.052
        LOS_ANGELES_CALENDAR,
        parseUtc("2023-07-14T14:45:23.052"));
  }

  @Test
  public void testWithOffsetDaylightSavingsNanoseconds() throws SQLException {
    test(
        new BigDecimal("1689320723.052637485"),  // UTC: 2023-07-14 07:45:23.052637485
        LOS_ANGELES_CALENDAR,
        parseUtc("2023-07-14T14:45:23.052637485"));
  }

  private static void test(Number v, Calendar calendar, Timestamp expectedValue)
      throws SQLException {
    TimestampFromNumberAccessor accessor =
        new TimestampFromNumberAccessor(
            new Getter() {
              @Override
              public Object getObject() {
                return v;
              }

              @Override
              public boolean wasNull() {
                return v == null;
              }
            },
            calendar);

    assertEquals(expectedValue, accessor.getTimestamp(calendar));
    assertEquals(expectedValue, accessor.getObject());
  }

  private static Timestamp parseUtc(String utcTimestamp) {
    return Timestamp.from(LocalDateTime.parse(utcTimestamp).toInstant(ZoneOffset.UTC));
  }
}
