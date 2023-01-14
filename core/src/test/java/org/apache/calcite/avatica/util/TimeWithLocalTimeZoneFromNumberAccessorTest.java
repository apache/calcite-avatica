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
public class TimeWithLocalTimeZoneFromNumberAccessorTest {

  // UTC+5:30
  private static final TimeZone IST_ZONE = TimeZone.getTimeZone("Asia/Kolkata");

  private Cursor.Accessor instance;
  private Calendar localCalendar;
  private Object value;

  /**
   * Setup test environment by creating a {@link AbstractCursor.TimeFromNumberAccessor} that reads
   * from the instance variable {@code value}.
   */
  @Before public void before() {
    final AbstractCursor.Getter getter = new LocalGetter();
    localCalendar = Calendar.getInstance(IST_ZONE, Locale.ROOT);
    instance = new AbstractCursor.TimeFromNumberAccessor(getter,
        localCalendar, true);
  }

  /**
   * Test {@code getString()} adjusts the string representation based on the default time zone.
   */
  @Test public void testString() throws SQLException {
    value = 0;
    assertThat(instance.getString(), is("05:30:00"));

    value = DateTimeUtils.MILLIS_PER_DAY - 1000;
    assertThat(instance.getString(), is("05:29:59"));
  }

  /**
   * Test {@code getTime()} does no time zone conversion because {@code TIME WITH LOCAL TIME ZONE}
   * represents a global instant in time.
   */
  @Test public void testTime() throws SQLException {
    value = 0;
    assertThat(instance.getTime(localCalendar), is(new Time(0L)));

    value = DateTimeUtils.MILLIS_PER_DAY - 1000;
    assertThat(instance.getTime(localCalendar), is(new Time(DateTimeUtils.MILLIS_PER_DAY - 1000)));
  }

  /**
   * Test {@code getTime()} does no time zone conversion because {@code TIME WITH LOCAL TIME ZONE}
   * represents a global instant in time.
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
    assertThat(instance.getTime(null).getTime(),
        is(0L));
  }

  /**
   * Test {@code getTimestamp()} does no time zone conversion because
   * {@code TIME WITH LOCAL TIME ZONE} represents a global instant in time.
   */
  @Test public void testTimestamp() throws SQLException {
    value = 0;
    assertThat(instance.getTimestamp(localCalendar),
        is(new Timestamp(0L)));

    value = DateTimeUtils.MILLIS_PER_DAY - 1000;
    assertThat(instance.getTimestamp(localCalendar),
        is(new Timestamp(DateTimeUtils.MILLIS_PER_DAY - 1000)));
  }

  /**
   * Test {@code getTimestamp()} does no time zone conversion because
   * {@code TIME WITH LOCAL TIME ZONE} represents a global instant in time.
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
