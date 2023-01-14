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
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test conversions from SQL {@link Time} to JDBC types in {@link AbstractCursor.TimeAccessor}.
 */
public class TimeWithLocalTimeZoneAccessorTest {

  private static final Calendar UTC =
      Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);

  // UTC+5:30
  private static final TimeZone IST_ZONE = TimeZone.getTimeZone("Asia/Kolkata");

  private Cursor.Accessor instance;
  private Calendar localCalendar;
  private Time value;

  /**
   * Setup test environment by creating a {@link AbstractCursor.TimeAccessor} that reads from the
   * instance variable {@code value}.
   */
  @Before public void before() {
    final AbstractCursor.Getter getter = new LocalGetter();
    localCalendar = Calendar.getInstance(IST_ZONE, Locale.ROOT);
    instance = new AbstractCursor.TimeAccessor(getter, localCalendar, true);
  }

  /**
   * Test {@code getTime()} does no time zone conversion because {@code TIME WITH LOCAL TIME ZONE}
   * represents a global instant in time.
   */
  @Test public void testTime() throws SQLException {
    value = new Time(0L);
    assertThat(instance.getTime(null), is(value));

    value = Time.valueOf("00:00:00");
    assertThat(instance.getTime(UTC), is(value));

    value = Time.valueOf("23:59:59");
    assertThat(instance.getTime(UTC), is(value));
  }

  /**
   * Test {@code getTime()} does no time zone conversion because {@code TIME WITH LOCAL TIME ZONE}
   * represents a global instant in time.
   */
  @Test public void testTimeWithCalendar() throws SQLException {
    value = new Time(0L);

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
   * Test {@code getString()} adjusts the string representation based on the default time zone.
   */
  @Test public void testStringWithDefaultTimeZone() throws SQLException {
    value = new Time(0);
    assertThat(instance.getString(), is("05:30:00"));

    value = new Time(DateTimeUtils.MILLIS_PER_DAY - 1000);
    assertThat(instance.getString(), is("05:29:59"));
  }

  /**
   * Test {@code getString()} adjusts the string representation based on an explicit time zone.
   */
  @Test public void testStringWithUtc() throws SQLException {
    localCalendar.setTimeZone(UTC.getTimeZone());

    value = new Time(0L);
    assertThat(instance.getString(), is("00:00:00"));

    value = new Time(DateTimeUtils.MILLIS_PER_DAY - 1000);
    assertThat(instance.getString(), is("23:59:59"));
  }

  /**
   * Test {@code getLong()} returns the same value as the input time.
   */
  @Test public void testLong() throws SQLException {
    value = new Time(0L);
    assertThat(instance.getLong(), is(0L));

    value = Time.valueOf("23:59:59");
    assertThat(instance.getLong(), is(value.getTime() % DateTimeUtils.MILLIS_PER_DAY));
    final Time longTime = new Time(instance.getLong());
    assertThat(longTime.toString(), is("23:59:59"));
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
