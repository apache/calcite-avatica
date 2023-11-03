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
public class TimestampWithLocalTimeZoneFromNumberAccessorTest {

  // UTC+5:30
  private static final TimeZone IST_ZONE = TimeZone.getTimeZone("Asia/Kolkata");

  // Shifting from the Julian to Gregorian calendar required skipping 10 days.
  private static final long GREGORIAN_SHIFT = 10 * DateTimeUtils.MILLIS_PER_DAY;

  // UTC: 2014-09-30 15:28:27.356
  private static final long DST_INSTANT = 1412090907356L;
  private static final String DST_STRING = "2014-09-30 15:28:27";
  private static final String DST_OFFSET_STRING = "2014-09-30 20:58:27";

  // UTC: 1500-04-30 12:00:00.123
  private static final long PRE_GREG_INSTANT = -14820580799877L;
  private static final String PRE_GREG_STRING = "1500-04-30 12:00:00";
  private static final String PRE_GREG_SHIFT_STRING = "1500-05-10 12:00:00";
  private static final String PRE_GREG_OFFSET_STRING = "1500-05-10 17:30:00";

  // These values are used to test timestamps around the Gregorian shift.
  // Unix timestamps use the proleptic Gregorian calendar (Gregorian applied retroactively).
  // JDBC uses the Julian calendar and skips 10 days in October 1582 to shift to the Gregorian.
  // UTC: 1582-10-04 00:00:00
  private static final long SHIFT_INSTANT_1 = -12219379200000L;
  // UTC: 1582-10-05 00:00:00
  private static final long SHIFT_INSTANT_2 = SHIFT_INSTANT_1 + DateTimeUtils.MILLIS_PER_DAY;
  // UTC: 1582-10-16 00:00:00
  private static final long SHIFT_INSTANT_3 = SHIFT_INSTANT_2 + DateTimeUtils.MILLIS_PER_DAY;
  // UTC: 1582-10-17 00:00:00
  private static final long SHIFT_INSTANT_4 = SHIFT_INSTANT_3 + DateTimeUtils.MILLIS_PER_DAY;

  private Cursor.Accessor instance;
  private Calendar localCalendar;
  private Object value;

  /**
   * Setup test environment by creating a {@link AbstractCursor.TimestampFromNumberAccessor} that
   * reads from the instance variable {@code value}.
   */
  @Before public void before() {
    final AbstractCursor.Getter getter = new LocalGetter();
    localCalendar = Calendar.getInstance(IST_ZONE, Locale.ROOT);
    instance = new AbstractCursor.TimestampFromNumberAccessor(getter, localCalendar, true);
  }

  /**
   * Test {@code getDate()} does no time zone conversion because
   * {@code TIMESTAMP WITH LOCAL TIME ZONE} represents a global instant in time.
   */
  @Test public void testDate() throws SQLException {
    value = 0L;
    assertThat(instance.getDate(localCalendar),
        is(new Date(0L)));

    value = PRE_GREG_INSTANT;
    assertThat(instance.getDate(localCalendar),
        is(new Date(PRE_GREG_INSTANT + GREGORIAN_SHIFT)));
  }

  /**
   * Test {@code getDate()} does no time zone conversion because
   * {@code TIMESTAMP WITH LOCAL TIME ZONE} represents a global instant in time.
   */
  @Test public void testDateWithCalendar() throws SQLException {
    value = 0L;

    final TimeZone minusFiveZone = TimeZone.getTimeZone("GMT-5:00");
    final Calendar minusFiveCal = Calendar.getInstance(minusFiveZone, Locale.ROOT);
    assertThat(instance.getDate(minusFiveCal).getTime(),
        is(0L));

    final TimeZone plusFiveZone = TimeZone.getTimeZone("GMT+5:00");
    final Calendar plusFiveCal = Calendar.getInstance(plusFiveZone, Locale.ROOT);
    assertThat(instance.getDate(plusFiveCal).getTime(),
        is(0L));
  }

  /**
   * Test no time zone conversion occurs if the given calendar is {@code null}.
   */
  @Test public void testDateWithNullCalendar() throws SQLException {
    value = 0;
    assertThat(instance.getDate(null), is(new Date(0L)));
  }

  /**
   * Test {@code getString()} adjusts the string representation based on the default time zone.
   */
  @Test public void testString() throws SQLException {
    value = 0;
    assertThat(instance.getString(), is("1970-01-01 05:30:00"));

    value = DST_INSTANT;
    assertThat(instance.getString(), is(DST_OFFSET_STRING));

    value = PRE_GREG_INSTANT;
    assertThat(instance.getString(), is(PRE_GREG_OFFSET_STRING));
  }

  /**
   * Test {@code getString()} shifts between the standard Gregorian calendar and the proleptic
   * Gregorian calendar.
   */
  @Test public void testStringWithGregorianShift() throws SQLException {
    for (int i = 4; i <= 15; ++i) {
      final String str = String.format(Locale.ROOT, "1582-10-%02d 00:00:00", i);
      final String offset = String.format(Locale.ROOT, "1582-10-%02d 05:30:00", i);
      value = DateTimeUtils.timestampStringToUnixDate(str);
      assertThat(instance.getString(), is(offset));
    }
  }

  /**
   * Test {@code getString()} returns timestamps relative to the local calendar.
   */
  @Test public void testStringWithUtc() throws SQLException {
    localCalendar.setTimeZone(TimeZone.getTimeZone("UTC"));

    value = 0L;
    assertThat(instance.getString(), is("1970-01-01 00:00:00"));

    value = DST_INSTANT;
    assertThat(instance.getString(), is(DST_STRING));

    value = PRE_GREG_INSTANT;
    assertThat(instance.getString(), is(PRE_GREG_SHIFT_STRING));
  }

  /**
   * Test {@code getString()} supports date range 0001-01-01 to 9999-12-31 required by ANSI SQL.
   *
   * <p>This test only uses the UTC time zone because some time zones don't have a January 1st
   * 12:00am for every year.
   */
  @Test public void testStringWithAnsiDateRange() throws SQLException {
    for (int i = 1; i < 1900; ++i) {
      assertString(
          String.format(Locale.ROOT, "%04d-01-01 00:00:00", i),
          String.format(Locale.ROOT, "%04d-01-01 05:30:00", i));
    }
    for (int i = 1900; i < 1906; ++i) {
      assertString(
          String.format(Locale.ROOT, "%04d-01-01 00:00:00", i),
          String.format(Locale.ROOT, "%04d-01-01 05:21:10", i));
    }
    for (int i = 1906; i < 1942; ++i) {
      assertString(
          String.format(Locale.ROOT, "%04d-01-01 00:00:00", i),
          String.format(Locale.ROOT, "%04d-01-01 05:30:00", i));
    }
    for (int i = 1942; i < 1946; ++i) {
      assertString(
          String.format(Locale.ROOT, "%04d-01-01 00:00:00", i),
          String.format(Locale.ROOT, "%04d-01-01 06:30:00", i));
    }
    for (int i = 1946; i < 10000; ++i) {
      assertString(
          String.format(Locale.ROOT, "%04d-01-01 00:00:00", i),
          String.format(Locale.ROOT, "%04d-01-01 05:30:00", i));
    }
  }

  private void assertString(String valueString, String expected) throws SQLException {
    value = DateTimeUtils.timestampStringToUnixDate(valueString);
    assertThat(instance.getString(), is(expected));
  }

  /**
   * Test {@code getTime()} returns the same value as the input timestamp for the local calendar.
   */
  @Test public void testTime() throws SQLException {
    value = 0L;
    assertThat(instance.getTime(localCalendar), is(new Time(0L)));

    value = DST_INSTANT;
    assertThat(instance.getTime(localCalendar), is(new Time(DST_INSTANT)));
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
        is(new Time(0L)));
    assertThat(instance.getTime(Calendar.getInstance(west, Locale.ROOT)),
        is(new Time(0L)));
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
        is(new Timestamp(0L)));

    value = DST_INSTANT;
    assertThat(instance.getTimestamp(localCalendar),
        is(new Timestamp(DST_INSTANT)));

    value = PRE_GREG_INSTANT;
    assertThat(instance.getTimestamp(localCalendar),
        is(new Timestamp(PRE_GREG_INSTANT + GREGORIAN_SHIFT)));
  }

  /**
   * Test {@code getTimestamp()} shifts between the standard Gregorian calendar and the proleptic
   * Gregorian calendar.
   */
  @Test public void testTimestampWithGregorianShift() throws SQLException {
    value = SHIFT_INSTANT_1;
    assertThat(instance.getTimestamp(localCalendar),
        is(new Timestamp(SHIFT_INSTANT_1 + GREGORIAN_SHIFT)));

    value = SHIFT_INSTANT_2;
    assertThat(instance.getTimestamp(localCalendar), is(new Timestamp(SHIFT_INSTANT_2)));

    value = SHIFT_INSTANT_3;
    assertThat(instance.getTimestamp(localCalendar), is(new Timestamp(SHIFT_INSTANT_3)));

    value = SHIFT_INSTANT_4;
    assertThat(instance.getTimestamp(localCalendar), is(new Timestamp(SHIFT_INSTANT_4)));
  }

  /**
   * Test {@code getTimestamp()} supports date range 0001-01-01 to 9999-12-31 required by ANSI SQL.
   */
  @Test public void testTimestampWithAnsiDateRange() throws SQLException {
    for (int i = 1; i < 1943; ++i) {
      assertTimestamp(i,  TimeZone.getDefault().getRawOffset());
    }
    for (int i = 1943; i < 1946; ++i) {
      assertTimestamp(i,  TimeZone.getDefault().getRawOffset() + DateTimeUtils.MILLIS_PER_HOUR);
    }
    for (int i = 1946; i < 1949; ++i) {
      assertTimestamp(i,  TimeZone.getDefault().getRawOffset());
    }
    for (int i = 1949; i < 1950; ++i) {
      assertTimestamp(i,  TimeZone.getDefault().getRawOffset() + DateTimeUtils.MILLIS_PER_HOUR);
    }
    for (int i = 1950; i < 10000; ++i) {
      assertTimestamp(i,  TimeZone.getDefault().getRawOffset());
    }
  }

  private void assertTimestamp(int year, long offset) throws SQLException {
    final String valueString = String.format(Locale.ROOT, "%04d-01-01 00:00:00.0", year);
    value = DateTimeUtils.timestampStringToUnixDate(valueString);
    assertThat(instance.getTimestamp(localCalendar),
        is(new Timestamp(Timestamp.valueOf(valueString).getTime() + offset)));
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
        is(new Timestamp(0L)));
    assertThat(instance.getTimestamp(Calendar.getInstance(west, Locale.ROOT)),
        is(new Timestamp(0L)));
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
