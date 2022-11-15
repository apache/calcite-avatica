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
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test conversions from SQL TIME as the number of milliseconds since 1970-01-01 00:00:00 to JDBC
 * types in {@link AbstractCursor.TimeFromNumberAccessor}.
 */
public class TimeFromNumberAccessorTest {

  private Cursor.Accessor instance;
  private Calendar localCalendar;
  private Object value;

  /**
   * Setup test environment by creating a {@link AbstractCursor.TimeFromNumberAccessor} that reads
   * from the instance variable {@code value}.
   */
  @Before public void before() {
    final AbstractCursor.Getter getter = new LocalGetter();
    localCalendar = Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT);
    instance = new AbstractCursor.TimeFromNumberAccessor(getter,
        localCalendar);
  }

  /**
   * Test {@code getString()} returns the same value as the input time.
   */
  @Test public void testString() throws SQLException {
    value = 0;
    assertThat(instance.getString(), is("00:00:00"));

    value = DateTimeUtils.MILLIS_PER_DAY - 1000;
    assertThat(instance.getString(), is("23:59:59"));
  }

  /**
   * Test {@code getTime()} returns the same value as the input time for the local calendar.
   */
  @Test public void testTime() throws SQLException {
    value = 0;
    assertThat(instance.getTime(localCalendar), is(Time.valueOf("00:00:00")));

    value = DateTimeUtils.MILLIS_PER_DAY - 1000;
    assertThat(instance.getTime(localCalendar), is(Time.valueOf("23:59:59")));
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
    assertThat(instance.getTime(null).getTime(),
        is(0L));
  }

  /**
   * Test {@code getTimestamp()} returns the same value as the input time.
   */
  @Test public void testTimestamp() throws SQLException {
    value = 0;
    assertThat(instance.getTimestamp(localCalendar),
        is(Timestamp.valueOf("1970-01-01 00:00:00.0")));

    value = DateTimeUtils.MILLIS_PER_DAY - 1000;
    assertThat(instance.getTimestamp(localCalendar),
        is(Timestamp.valueOf("1970-01-01 23:59:59.0")));
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
