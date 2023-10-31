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

import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test conversions from SQL TIME as the number of milliseconds since 1970-01-01 00:00:00 to JDBC
 * types in {@link AbstractCursor.TimeFromNumberAccessor}.
 */
public class TimeFromNumberAccessorTest {

  private static final Calendar UTC =
      Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);

  // UTC+5:30
  private static final TimeZone IST_ZONE = TimeZone.getTimeZone("Asia/Kolkata");

  private Cursor.Accessor instance;
  private Calendar localCalendar;
  private Integer value;

  /**
   * Setup test environment by creating a {@link AbstractCursor.TimeFromNumberAccessor} that reads
   * from the instance variable {@code value}.
   */
  @Before public void before() {
    final AbstractCursor.Getter getter = new LocalGetter();
    localCalendar = Calendar.getInstance(IST_ZONE, Locale.ROOT);
    instance = new AbstractCursor.TimeFromNumberAccessor(getter,
        localCalendar, false);
  }

  /**
   * Test {@code getTime()} and {@code getTimestamp()} return the same instant as the input time for
   * the connection default calendar.
   */
  @Test public void testTime() throws SQLException {
    value = 12345;

    assertThat(instance.getTime(null), is(new Time(value)));
    assertThat(instance.getTimestamp(null), is(new Timestamp(value)));
  }

  /**
   * Test {@code getTime()} and {@code getTimestamp()} handle time zone conversions relative to the
   * provided calendar.
   */
  @Test public void testTimeWithCalendar() throws SQLException {
    value = 0;

    final TimeZone minusFiveZone = TimeZone.getTimeZone("GMT-5:00");
    final Calendar minusFiveCal = Calendar.getInstance(minusFiveZone, Locale.ROOT);
    assertThat(
        instance.getTime(minusFiveCal),
        is(new Time(5 * DateTimeUtils.MILLIS_PER_HOUR)));
    assertThat(
        instance.getTimestamp(minusFiveCal),
        is(new Timestamp(5 * DateTimeUtils.MILLIS_PER_HOUR)));

    final TimeZone plusFiveZone = TimeZone.getTimeZone("GMT+5:00");
    final Calendar plusFiveCal = Calendar.getInstance(plusFiveZone, Locale.ROOT);
    assertThat(
        instance.getTime(plusFiveCal),
        is(new Time(-5 * DateTimeUtils.MILLIS_PER_HOUR)));
    assertThat(
        instance.getTimestamp(plusFiveCal),
        is(new Timestamp(-5 * DateTimeUtils.MILLIS_PER_HOUR)));
  }

  /**
   * Test {@code getString()} returns the clock representation in UTC when the connection default
   * calendar is UTC.
   */
  @Test public void testStringWithUtc() throws SQLException {
    localCalendar.setTimeZone(UTC.getTimeZone());
    helpTestGetString();
  }

  /**
   * Test {@code getString()} also returns the clock representation in UTC when the connection
   * default calendar is *not* UTC.
   */
  @Test public void testStringWithDefaultTimeZone() throws SQLException {
    helpTestGetString();
  }

  private void helpTestGetString() throws SQLException {
    value = 0;
    assertThat(instance.getString(), is("00:00:00"));

    value = (int) (DateTimeUtils.MILLIS_PER_DAY - 1000);
    assertThat(instance.getString(), is("23:59:59"));

    value = (int) (DateTimeUtils.MILLIS_PER_DAY + 1000);
    assertThat(instance.getString(), is("00:00:01"));

    value = -1000;
    assertThat(instance.getString(), is("23:59:59"));
  }

  /**
   * Test {@code getLong()} returns the same value as the input time.
   */
  @Test public void testLong() throws SQLException {
    value = 123456;
    assertThat(instance.getLong(), is(123456L));

    value = (int) (DateTimeUtils.MILLIS_PER_DAY + 1000L);
    assertThat(instance.getLong(), is(1000L));

    value = -1000;
    assertThat(instance.getLong(), is(DateTimeUtils.MILLIS_PER_DAY - 1000L));
  }

  /**
   * Returns the value from the test instance to the accessor.
   */
  private class LocalGetter implements AbstractCursor.Getter {
    @Override public Object getObject() {
      return value;
    }

    @Override public boolean wasNull() {
      return value == null;
    }
  }
}
