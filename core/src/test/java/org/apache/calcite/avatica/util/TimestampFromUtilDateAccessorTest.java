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
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import static org.apache.calcite.avatica.util.DateTimeUtils.MILLIS_PER_HOUR;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test conversions from SQL {@link java.util.Date} to JDBC types in
 * {@link AbstractCursor.TimestampFromUtilDateAccessor}.
 */
public class TimestampFromUtilDateAccessorTest {

  private static final Calendar UTC =
      Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);

  private Cursor.Accessor instance;
  private Calendar localCalendar;
  private Date value;

  /**
   * Setup test environment by creating a {@link AbstractCursor.TimestampFromUtilDateAccessor} that
   * reads from the instance variable {@code value}.
   */
  @Before public void before() {
    final AbstractCursor.Getter getter = new LocalGetter();
    localCalendar = Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT);
    instance = new AbstractCursor.TimestampFromUtilDateAccessor(getter, localCalendar);
  }

  /**
   * Test {@code getTimestamp()} returns the same value as the input timestamp for the local
   * calendar.
   */
  @Test public void testTimestamp() throws SQLException {
    value = new Timestamp(0L);
    assertThat(instance.getTimestamp(null), is(value));

    value = Timestamp.valueOf("1970-01-01 00:00:00");
    assertThat(instance.getTimestamp(UTC), is(value));

    value = Timestamp.valueOf("2014-09-30 15:28:27.356");
    assertThat(instance.getTimestamp(UTC), is(value));

    value = Timestamp.valueOf("1500-04-30 12:00:00.123");
    assertThat(instance.getTimestamp(UTC), is(value));
  }

  /**
   * Test {@code getTimestamp()} handles time zone conversions relative to the local calendar and
   * not UTC.
   */
  @Test public void testTimestampWithCalendar() throws SQLException {
    value = new Timestamp(0L);

    final TimeZone minusFiveZone = TimeZone.getTimeZone("GMT-5:00");
    final Calendar minusFiveCal = Calendar.getInstance(minusFiveZone, Locale.ROOT);
    assertThat(instance.getTimestamp(minusFiveCal).getTime(),
        is(5 * MILLIS_PER_HOUR));

    final TimeZone plusFiveZone = TimeZone.getTimeZone("GMT+5:00");
    final Calendar plusFiveCal = Calendar.getInstance(plusFiveZone, Locale.ROOT);
    assertThat(instance.getTimestamp(plusFiveCal).getTime(),
        is(-5 * MILLIS_PER_HOUR));
  }

  /**
   * Test {@code getDate()} returns the same value as the input timestamp for the local calendar.
   */
  @Test public void testDate() throws SQLException {
    value = new java.sql.Date(0L);
    assertThat(instance.getDate(null), is(value));

    value = java.sql.Date.valueOf("1970-01-01");
    assertThat(instance.getDate(UTC), is(value));

    value = java.sql.Date.valueOf("1500-04-30");
    assertThat(instance.getDate(UTC), is(value));
  }

  /**
   * Test {@code getDate()} handles time zone conversions relative to the local calendar and not
   * UTC.
   */
  @Test public void testDateWithCalendar() throws SQLException {
    value = new java.sql.Date(0L);

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
   * Test {@code getTime()} returns the same value as the input timestamp for the local calendar.
   */
  @Test public void testTime() throws SQLException {
    value = new Time(0L);
    assertThat(instance.getTime(null), is(value));

    value = Time.valueOf("00:00:00");
    assertThat(instance.getTime(UTC), is(value));

    value = Time.valueOf("23:59:59");
    assertThat(instance.getTime(UTC).toString(), is("23:59:59"));
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

    value = new Time(0L);
    assertThat(instance.getTime(Calendar.getInstance(east, Locale.ROOT)),
        is(Timestamp.valueOf("1969-12-31 23:00:00")));
    assertThat(instance.getTime(Calendar.getInstance(west, Locale.ROOT)),
        is(Timestamp.valueOf("1970-01-01 01:00:00")));
  }

  /**
   * Test {@code getString()} returns the same value as the input timestamp.
   */
  @Test public void testStringWithLocalTimeZone() throws SQLException {
    value = Timestamp.valueOf("1970-01-01 00:00:00");
    assertThat(instance.getString(), is("1970-01-01 00:00:00"));

    value = Timestamp.valueOf("2014-09-30 15:28:27.356");
    assertThat(instance.getString(), is("2014-09-30 15:28:27"));

    value = Timestamp.valueOf("1500-04-30 12:00:00.123");
    assertThat(instance.getString(), is("1500-04-30 12:00:00"));
  }

  /**
   * Test {@code getString()} shifts between the standard Gregorian calendar and the proleptic
   * Gregorian calendar.
   */
  @Test public void testStringWithGregorianShift() throws SQLException {
    value = Timestamp.valueOf("1582-10-04 00:00:00");
    assertThat(instance.getString(), is("1582-10-04 00:00:00"));
    value = Timestamp.valueOf("1582-10-05 00:00:00");
    assertThat(instance.getString(), is("1582-10-15 00:00:00"));
    value = Timestamp.valueOf("1582-10-15 00:00:00");
    assertThat(instance.getString(), is("1582-10-15 00:00:00"));
  }

  /**
   * Test {@code getString()} returns timestamps relative to the local calendar.
   */
  @Test public void testStringWithUtc() throws SQLException {
    localCalendar.setTimeZone(UTC.getTimeZone());

    value = new Timestamp(0L);
    assertThat(instance.getString(), is("1970-01-01 00:00:00"));

    value = new Timestamp(1412090907356L /* 2014-09-30 15:28:27.356 UTC */);
    assertThat(instance.getString(), is("2014-09-30 15:28:27"));

    value = new Timestamp(-14820580799877L /* 1500-04-30 12:00:00.123 UTC */);
    assertThat(instance.getString(), is("1500-04-30 12:00:00"));
  }

  /**
   * Test {@code getString()} supports date range 0001-01-01 to 9999-12-31 required by ANSI SQL.
   *
   * <p>This test only uses the UTC time zone because some time zones don't have a January 1st
   * 12:00am for every year.
   */
  @Test public void testStringWithAnsiDateRange() throws SQLException {
    localCalendar.setTimeZone(UTC.getTimeZone());

    final Calendar utcCal = (Calendar) UTC.clone();
    utcCal.set(1, Calendar.JANUARY, 1, 0, 0, 0);
    utcCal.set(Calendar.MILLISECOND, 0);

    for (int i = 2; i <= 9999; ++i) {
      utcCal.set(Calendar.YEAR, i);
      value = new Timestamp(utcCal.getTimeInMillis());
      assertThat(instance.getString(),
          is(String.format(Locale.ROOT, "%04d-01-01 00:00:00", i)));
    }
  }

  /**
   * Test {@code getLong()} returns the same value as the input timestamp.
   */
  @Test public void testLong() throws SQLException {
    value = new Timestamp(0L);
    assertThat(instance.getLong(), is((long) -localCalendar.getTimeZone().getOffset(0L)));

    value = Timestamp.valueOf("2014-09-30 15:28:27.356");
    assertThat(instance.getLong(),
        is(value.getTime() - localCalendar.getTimeZone().getOffset(value.getTime())));

    value = Timestamp.valueOf("1500-04-30 00:00:00");
    assertThat(instance.getLong(),
        is(value.getTime() - localCalendar.getTimeZone().getOffset(value.getTime())));
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
