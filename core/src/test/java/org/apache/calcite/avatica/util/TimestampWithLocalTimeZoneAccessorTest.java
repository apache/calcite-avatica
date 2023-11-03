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
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test conversions from SQL {@link Timestamp} to JDBC types in
 * {@link AbstractCursor.TimestampAccessor}.
 */
public class TimestampWithLocalTimeZoneAccessorTest {

  private static final Calendar UTC =
      Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);

  // UTC+5:30
  private static final TimeZone IST_ZONE = TimeZone.getTimeZone("Asia/Kolkata");

  // UTC: 2014-09-30 15:28:27.356
  private static final long DST_INSTANT = 1412090907356L;
  private static final String DST_STRING = "2014-09-30 15:28:27";

  // UTC: 1500-04-30 12:00:00.123
  private static final long PRE_GREG_INSTANT = -14820580799877L;
  private static final String PRE_GREG_STRING = "1500-04-30 12:00:00";

  // These values are used to test timestamps around the Gregorian shift.
  // Unix timestamps use the proleptic Gregorian calendar (Gregorian applied retroactively).
  // JDBC uses the Julian calendar and skips 10 days in October 1582 to shift to the Gregorian.
  // UTC: 1582-10-04 00:00:00
  private static final long SHIFT_INSTANT_1 = -12219379200000L;
  private static final String SHIFT_STRING_1 = "1582-10-04 00:00:00";
  // UTC: 1582-10-05 00:00:00
  private static final long SHIFT_INSTANT_2 = SHIFT_INSTANT_1 + DateTimeUtils.MILLIS_PER_DAY;
  private static final String SHIFT_STRING_2 = "1582-10-05 00:00:00";
  // UTC: 1582-10-16 00:00:00
  private static final long SHIFT_INSTANT_3 = SHIFT_INSTANT_2 + DateTimeUtils.MILLIS_PER_DAY;
  private static final String SHIFT_STRING_3 = "1582-10-16 00:00:00";
  // UTC: 1582-10-17 00:00:00
  private static final long SHIFT_INSTANT_4 = SHIFT_INSTANT_3 + DateTimeUtils.MILLIS_PER_DAY;
  private static final String SHIFT_STRING_4 = "1582-10-17 00:00:00";

  private Cursor.Accessor instance;
  private Calendar localCalendar;
  private Timestamp value;

  /**
   * Setup test environment by creating a {@link AbstractCursor.TimestampAccessor} that reads from
   * the instance variable {@code value}.
   */
  @Before public void before() {
    final AbstractCursor.Getter getter = new LocalGetter();
    localCalendar = Calendar.getInstance(IST_ZONE, Locale.ROOT);
    instance = new AbstractCursor.TimestampAccessor(getter, localCalendar, true);
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
        is(0L));

    final TimeZone plusFiveZone = TimeZone.getTimeZone("GMT+5:00");
    final Calendar plusFiveCal = Calendar.getInstance(plusFiveZone, Locale.ROOT);
    assertThat(instance.getTimestamp(plusFiveCal).getTime(),
        is(0L));
  }

  /**
   * Test {@code getDate()} returns the same value as the input timestamp for the local calendar.
   */
  @Test public void testDate() throws SQLException {
    value = new Timestamp(0L);
    assertThat(instance.getDate(null), is(new Date(0L)));

    value = Timestamp.valueOf("1970-01-01 00:00:00");
    assertThat(instance.getDate(UTC), is(Date.valueOf("1970-01-01")));

    value = Timestamp.valueOf("1500-04-30 00:00:00");
    assertThat(instance.getDate(UTC), is(Date.valueOf("1500-04-30")));
  }

  /**
   * Test {@code getDate()} handles time zone conversions relative to the local calendar and not
   * UTC.
   */
  @Test public void testDateWithCalendar() throws SQLException {
    value = new Timestamp(0L);

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
   * Test {@code getTime()} returns the same value as the input timestamp for the local calendar.
   */
  @Test public void testTime() throws SQLException {
    value = new Timestamp(0L);
    assertThat(instance.getTime(null), is(new Time(0L)));

    value = Timestamp.valueOf("1970-01-01 00:00:00");
    assertThat(instance.getTime(UTC), is(Time.valueOf("00:00:00")));

    value = Timestamp.valueOf("2014-09-30 15:28:27.356");
    assertThat(instance.getTime(UTC).toString(), is("15:28:27"));
  }

  /**
   * Test {@code getTime()} handles time zone conversions relative to the local calendar and not
   * UTC.
   */
  @Test public void testTimeWithCalendar() throws SQLException {
    value = new Timestamp(0L);

    final TimeZone minusFiveZone = TimeZone.getTimeZone("GMT-5:00");
    final Calendar minusFiveCal = Calendar.getInstance(minusFiveZone, Locale.ROOT);
    assertThat(instance.getTime(minusFiveCal).getTime(),
        is(0L));

    final TimeZone plusFiveZone = TimeZone.getTimeZone("GMT+5:00");
    final Calendar plusFiveCal = Calendar.getInstance(plusFiveZone, Locale.ROOT);
    assertThat(instance.getTime(plusFiveCal).getTime(),
        is(0L));
  }

  /**
   * Test {@code getString()} always returns the same string, regardless of the connection default
   * calendar.
   */
  @Test public void testString() throws SQLException {
    localCalendar.setTimeZone(UTC.getTimeZone());

    value = new Timestamp(0L);
    assertThat(instance.getString(), is("1970-01-01 00:00:00"));

    value = new Timestamp(DST_INSTANT);
    assertThat(instance.getString(), is(DST_STRING));

    value = new Timestamp(PRE_GREG_INSTANT);
    assertThat(instance.getString(), is(PRE_GREG_STRING));
  }

  /**
   * Test {@code getString()} shifts between the standard Gregorian calendar and the proleptic
   * Gregorian calendar.
   */
  @Test public void testStringWithGregorianShift() throws SQLException {
    localCalendar.setTimeZone(UTC.getTimeZone());

    value = new Timestamp(SHIFT_INSTANT_1);
    assertThat(instance.getString(), is(SHIFT_STRING_1));
    value = new Timestamp(SHIFT_INSTANT_2);
    assertThat(instance.getString(), is(SHIFT_STRING_2));
    value = new Timestamp(SHIFT_INSTANT_3);
    assertThat(instance.getString(), is(SHIFT_STRING_3));
    value = new Timestamp(SHIFT_INSTANT_4);
    assertThat(instance.getString(), is(SHIFT_STRING_4));
  }

  /**
   * Test {@code getString()} always returns the same string, regardless of the connection default
   * calendar.
   */
  @Test public void testStringWithUtc() throws SQLException {
    localCalendar.setTimeZone(UTC.getTimeZone());

    value = new Timestamp(0L);
    assertThat(instance.getString(), is("1970-01-01 00:00:00"));

    value = new Timestamp(DST_INSTANT);
    assertThat(instance.getString(), is(DST_STRING));

    value = new Timestamp(PRE_GREG_INSTANT);
    assertThat(instance.getString(), is(PRE_GREG_STRING));
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
    assertThat(instance.getLong(), is(0L));

    value = Timestamp.valueOf("2014-09-30 15:28:27.356");
    assertThat(instance.getLong(), is(value.getTime()));

    value = Timestamp.valueOf("1500-04-30 00:00:00");
    assertThat(instance.getLong(), is(value.getTime()));
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
