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

import org.apache.calcite.avatica.ColumnMetaData;

import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * <p>Test cases for
 * <a href="https://issues.apache.org/jira/browse/CALCITE-7494">[CALCITE-7494]
 * Avatica conversion to string of TIMESTAMP WITH TIME ZONE does not include
 * time zone</a>.
 */
public class TimestampTzFromNumberAccessorTest {
  // UTC: 2014-09-30 15:28:27.356
  private static final long DST_INSTANT = 1412090907356L;

  private Cursor.Accessor instance;
  private Calendar localCalendar;
  private Object value;

  /**
   * Setup test environment by creating a
   * {@link AbstractCursor.TimestampTzFromNumberAccessor} that reads from the
   * instance variable {@code value}.
   */
  @Before public void before() {
    final AbstractCursor.Getter getter = new LocalGetter();
    localCalendar = Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT);
    instance =
        new AbstractCursor.TimestampTzFromNumberAccessor(getter, localCalendar, 0);
  }

  /**
   * Test {@code getString()} includes the time zone.
   */
  @Test public void testString() throws SQLException {
    value = 0L;
    assertThat(instance.getString(), is("1970-01-01 00:00:00 UTC"));

    value = DST_INSTANT;
    assertThat(instance.getString(), is("2014-09-30 15:28:27 UTC"));
  }

  /**
   * Test {@code getString()} keeps the fractional seconds allowed by the
   * column precision; fractional seconds beyond the precision are truncated,
   * not rounded.
   */
  @Test public void testStringWithPrecision() throws SQLException {
    value = DST_INSTANT;
    assertThat(stringWithPrecision(3), is("2014-09-30 15:28:27.356 UTC"));
    assertThat(stringWithPrecision(2), is("2014-09-30 15:28:27.35 UTC"));
    assertThat(stringWithPrecision(1), is("2014-09-30 15:28:27.3 UTC"));
    assertThat(stringWithPrecision(0), is("2014-09-30 15:28:27 UTC"));
  }

  /** Renders {@code value} via an accessor with the given column precision. */
  private String stringWithPrecision(int precision) throws SQLException {
    return new AbstractCursor.TimestampTzFromNumberAccessor(new LocalGetter(),
        null, precision).getString();
  }

  /**
   * Test {@code getString()} returns null for a null value.
   */
  @Test public void testStringNull() throws SQLException {
    value = null;
    assertThat(instance.getString(), nullValue());
  }

  /**
   * Test {@code getTimestamp()} is not affected by the time zone suffix.
   */
  @Test public void testTimestamp() throws SQLException {
    value = DateTimeUtils.timestampStringToUnixDate("2014-09-30 15:28:27.356");
    assertThat(instance.getTimestamp(localCalendar),
        is(Timestamp.valueOf("2014-09-30 15:28:27.356")));
  }

  /**
   * Test a column whose JDBC type is {@link Types#TIMESTAMP} but whose type
   * name is {@code TIMESTAMP_TZ} gets an accessor that includes the time zone
   * in {@code getString()}, while a plain TIMESTAMP column does not.
   */
  @Test public void testAccessorSelectionByTypeName() throws SQLException {
    assertThat(getStringForColumn("TIMESTAMP_TZ"),
        is("1970-01-01 00:00:00 UTC"));
    assertThat(getStringForColumn("TIMESTAMP"),
        is("1970-01-01 00:00:00"));
  }

  private static String getStringForColumn(String typeName) throws SQLException {
    final ColumnMetaData metaData =
        ColumnMetaData.dummy(
            ColumnMetaData.scalar(Types.TIMESTAMP, typeName,
                ColumnMetaData.Rep.LONG),
            true);
    final ListIteratorCursor cursor =
        new ListIteratorCursor(
            Collections.singletonList(
                Collections.<Object>singletonList(0L)).iterator());
    final List<Cursor.Accessor> accessors =
        cursor.createAccessors(Collections.singletonList(metaData),
            Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT),
            null);
    assertThat(cursor.next(), is(true));
    return accessors.get(0).getString();
  }

  /** Returns the value from the test instance to the accessor. */
  private class LocalGetter implements AbstractCursor.Getter {
    @Override public Object getObject() {
      return value;
    }

    @Override public boolean wasNull() {
      return value == null;
    }
  }
}
