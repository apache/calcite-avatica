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
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test conversions from SQL {@link java.sql.Date} to JDBC types in
 * {@link AbstractCursor.DateAccessor}.
 */
public class DateAccessorTest {

  private static final Calendar UTC =
      Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);

  private Cursor.Accessor instance;
  private Calendar localCalendar;
  private Date value;

  /**
   * Setup test environment by creating a {@link AbstractCursor.DateAccessor} that reads from the
   * instance variable {@code value}.
   */
  @Before public void before() {
    final AbstractCursor.Getter getter = new LocalGetter();
    localCalendar = Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT);
    instance = new AbstractCursor.DateAccessor(getter, localCalendar);
  }

  /**
   * Test {@code getDate()} returns the same value as the input date.
   */
  @Test public void testDate() throws SQLException {
    value = new Date(0L);
    assertThat(instance.getDate(null), is(value));

    value = Date.valueOf("1970-01-01");
    assertThat(instance.getDate(UTC), is(value));

    value = Date.valueOf("1500-04-30");
    assertThat(instance.getDate(UTC), is(value));
  }

  /**
   * Test {@code getDate()} handles time zone conversions relative to the local calendar and not
   * UTC.
   */
  @Test public void testDateWithCalendar() throws SQLException {
    value = new Date(0L);

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
      value = new Date(utcCal.getTimeInMillis());
      assertThat(instance.getString(),
          is(String.format(Locale.ROOT, "%04d-01-01", i)));
    }
  }

  /**
   * Test {@code getString()} returns the same value as the input date.
   */
  @Test public void testStringWithLocalTimeZone() throws SQLException {
    value = Date.valueOf("1970-01-01");
    assertThat(instance.getString(), is("1970-01-01"));

    value = Date.valueOf("1500-04-30");
    assertThat(instance.getString(), is("1500-04-30"));
  }

  /**
   * Test {@code getString()} returns dates relative to the local calendar.
   */
  @Test public void testStringWithUtc() throws SQLException {
    localCalendar.setTimeZone(UTC.getTimeZone());

    value = new Date(0L);
    assertThat(instance.getString(), is("1970-01-01"));

    value = new Date(-14820624000000L /* 1500-04-30 */);
    assertThat(instance.getString(), is("1500-04-30"));
  }

  /**
   * Test {@code getLong()} returns the same value as the input date.
   */
  @Test public void testLong() throws SQLException {
    value = new Date(0L);
    assertThat(instance.getLong(), is(0L));

    value = Date.valueOf("1500-04-30");
    final Date longDate = new Date(instance.getLong() * DateTimeUtils.MILLIS_PER_DAY);
    assertThat(longDate.toString(), is("1500-04-30"));
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
