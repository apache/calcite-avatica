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

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test conversions from SQL TIMESTAMP as the number of milliseconds since 1970-01-01 00:00:00 to
 * JDBC types in {@link AbstractCursor.TimestampFromNumberAccessor}.
 */
public class TimestampFromNumberAccessorTest {

  private Cursor.Accessor instance;
  private Calendar localCalendar;
  private Object value;

  /**
   * Setup test environment by creating a {@link AbstractCursor.TimestampFromNumberAccessor} that
   * reads from the instance variable {@code value}.
   */
  @Before public void before() {
    final AbstractCursor.Getter getter = new LocalGetter();
    localCalendar = Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT);
    instance = new AbstractCursor.TimestampFromNumberAccessor(getter, localCalendar, false);
  }

  /**
   * Test {@code getDate()} returns the same value as the input timestamp for the local calendar.
   */
  @Test public void testDate() throws SQLException {
    value = 0L;
    assertThat(instance.getDate(localCalendar),
        is(Date.valueOf("1970-01-01")));

    value = DateTimeUtils.timestampStringToUnixDate("1500-04-30 12:00:00");
    assertThat(instance.getDate(localCalendar),
        is(Timestamp.valueOf("1500-04-30 12:00:00")));
  }

  /**
   * Test {@code getDate()} handles time zone conversions relative to the local calendar and not
   * UTC.
   */
  @Test public void testDateWithCalendar() throws SQLException {
    value = 0L;

    final TimeZone minusFiveZone = TimeZone.getTimeZone("GMT-5:00");
    final Calendar minusFiveCal = Calendar.getInstance(minusFiveZone, Locale.ROOT);
    assertThat(instance.getDate(minusFiveCal).getTime(),
        is(5 * DateTimeUtils.MILLIS_PER_HOUR));

    final TimeZone plusFiveZone = TimeZone.getTimeZone("GMT+5:00");
    final Calendar plusFiveCal = Calendar.getInstance(plusFiveZone, Locale.ROOT);
    assertThat(instance.getDate(plusFiveCal).getTime(),
        is(-5 * DateTimeUtils.MILLIS_PER_HOUR));
  }

  /**
   * Test no time zone conversion occurs if the given calendar is {@code null}.
   */
  @Test public void testDateWithNullCalendar() throws SQLException {
    value = 0;
    assertThat(instance.getDate(null), is(new Date(0L)));
  }

  /**
   * Test {@code getString()} returns the same value as the input timestamp.
   */
  @Test public void testString() throws SQLException {
    value = 0;
    assertThat(instance.getString(), is("1970-01-01 00:00:00"));

    value = DateTimeUtils.timestampStringToUnixDate("2014-09-30 15:28:27.356");
    assertThat(instance.getString(), is("2014-09-30 15:28:27"));

    value = DateTimeUtils.timestampStringToUnixDate("1500-04-30 12:00:00.123");
    assertThat(instance.getString(), is("1500-04-30 12:00:00"));
  }

  /**
   * Test {@code getString()} shifts between the standard Gregorian calendar and the proleptic
   * Gregorian calendar.
   */
  @Test public void testStringWithGregorianShift() throws SQLException {
    for (int i = 4; i <= 15; ++i) {
      final String str = String.format(Locale.ROOT, "1582-10-%02d 00:00:00", i);
      value = DateTimeUtils.timestampStringToUnixDate(str);
      assertThat(instance.getString(), is(str));
    }
  }

  /**
   * Test {@code getString()} returns timestamps relative to the local calendar.
   */
  @Test public void testStringWithUtc() throws SQLException {
    localCalendar.setTimeZone(TimeZone.getTimeZone("UTC"));

    value = 0L;
    assertThat(instance.getString(), is("1970-01-01 00:00:00"));

    value = 1412090907356L;  // 2014-09-30 15:28:27.356 UTC
    assertThat(instance.getString(), is("2014-09-30 15:28:27"));

    value = -14821444799877L;  // 1500-04-30 12:00:00.123
    assertThat(instance.getString(), is("1500-04-30 12:00:00"));
  }

  /**
   * Test {@code getString()} supports date range 0001-01-01 to 9999-12-31 required by ANSI SQL.
   *
   * <p>This test only uses the UTC time zone because some time zones don't have a January 1st
   * 12:00am for every year.
   */
  @Test public void testStringWithAnsiDateRange() throws SQLException {
    for (int i = 1; i <= 9999; ++i) {
      final String str = String.format(Locale.ROOT, "%04d-01-01 00:00:00", i);
      value = DateTimeUtils.timestampStringToUnixDate(str);
      assertThat(instance.getString(), is(str));
    }
  }

  /**
   * Test {@code getTime()} returns the same value as the input timestamp for the local calendar.
   */
  @Test public void testTime() throws SQLException {
    value = 0L;
    assertThat(instance.getTime(localCalendar).toString(), is("00:00:00"));

    value = DateTimeUtils.timestampStringToUnixDate("2014-09-30 15:28:27.356");
    assertThat(instance.getTime(localCalendar).toString(), is("15:28:27"));
  }

  /**
   * Test {@code getTime()} handles time zone conversions relative to the local calendar and not
   * UTC.
   */
  @Test public void testTimeWithCalendar() throws SQLException {
    final int offset = localCalendar.getTimeZone().getOffset(0);
    final TimeZone east = new SimpleTimeZone(
        offset + (int) DateTimeUtils.MILLIS_PER_HOUR,
        "EAST");
    final TimeZone west = new SimpleTimeZone(
        offset - (int) DateTimeUtils.MILLIS_PER_HOUR,
        "WEST");

    value = 0;
    assertThat(instance.getTime(Calendar.getInstance(east, Locale.ROOT)),
        is(Timestamp.valueOf("1969-12-31 23:00:00")));
    assertThat(instance.getTime(Calendar.getInstance(west, Locale.ROOT)),
        is(Timestamp.valueOf("1970-01-01 01:00:00")));
  }

  /**
   * Test no time zone conversion occurs if the given calendar is {@code null}.
   */
  @Test public void testTimeWithNullCalendar() throws SQLException {
    value = 0;
    assertThat(instance.getTime(null), is(new Time(0L)));
  }

  /**
   * Test {@code getTimestamp()} returns the same value as the input timestamp for the local
   * calendar.
   */
  @Test public void testTimestamp() throws SQLException {
    value = 0L;
    assertThat(instance.getTimestamp(localCalendar),
        is(Timestamp.valueOf("1970-01-01 00:00:00.0")));

    value = DateTimeUtils.timestampStringToUnixDate("2014-09-30 15:28:27.356");
    assertThat(instance.getTimestamp(localCalendar),
        is(Timestamp.valueOf("2014-09-30 15:28:27.356")));

    value = DateTimeUtils.timestampStringToUnixDate("1500-04-30 12:00:00");
    assertThat(instance.getTimestamp(localCalendar),
        is(Timestamp.valueOf("1500-04-30 12:00:00.0")));
  }

  /**
   * Test {@code getTimestamp()} shifts between the standard Gregorian calendar and the proleptic
   * Gregorian calendar.
   */
  @Test public void testTimestampWithGregorianShift() throws SQLException {
    value = DateTimeUtils.timestampStringToUnixDate("1582-10-04 00:00:00");
    assertThat(instance.getTimestamp(localCalendar),
        is(Timestamp.valueOf("1582-10-04 00:00:00.0")));

    value = DateTimeUtils.timestampStringToUnixDate("1582-10-05 00:00:00");
    assertThat(instance.getTimestamp(localCalendar),
        is(Timestamp.valueOf("1582-10-15 00:00:00.0")));

    value = DateTimeUtils.timestampStringToUnixDate("1582-10-15 00:00:00");
    assertThat(instance.getTimestamp(localCalendar),
        is(Timestamp.valueOf("1582-10-15 00:00:00.0")));
  }

  /**
   * Test {@code getTimestamp()} supports date range 0001-01-01 to 9999-12-31 required by ANSI SQL.
   */
  @Test public void testTimestampWithAnsiDateRange() throws SQLException {
    for (int i = 1; i <= 9999; ++i) {
      final String str = String.format(Locale.ROOT, "%04d-01-01 00:00:00.0", i);
      value = DateTimeUtils.timestampStringToUnixDate(str);
      assertThat(instance.getTimestamp(localCalendar),
          is(Timestamp.valueOf(str)));
    }
  }

  /**
   * Test {@code getTimestamp()} handles time zone conversions relative to the local calendar and
   * not UTC.
   */
  @Test public void testTimestampWithCalendar() throws SQLException {
    final int offset = localCalendar.getTimeZone().getOffset(0);
    final TimeZone east = new SimpleTimeZone(
        offset + (int) DateTimeUtils.MILLIS_PER_HOUR,
        "EAST");
    final TimeZone west = new SimpleTimeZone(
        offset - (int) DateTimeUtils.MILLIS_PER_HOUR,
        "WEST");

    value = 0;
    assertThat(instance.getTimestamp(Calendar.getInstance(east, Locale.ROOT)),
        is(Timestamp.valueOf("1969-12-31 23:00:00.0")));
    assertThat(instance.getTimestamp(Calendar.getInstance(west, Locale.ROOT)),
        is(Timestamp.valueOf("1970-01-01 01:00:00.0")));
  }

  /**
   * Test no time zone conversion occurs if the given calendar is {@code null}.
   */
  @Test public void testTimestampWithNullCalendar() throws SQLException {
    value = 0;
    assertThat(instance.getTimestamp(null).getTime(),
        is(0L));
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
