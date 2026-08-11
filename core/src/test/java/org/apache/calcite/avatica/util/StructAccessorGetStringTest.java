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
import org.apache.calcite.avatica.MetaImpl;
import org.apache.calcite.avatica.util.Cursor.Accessor;

import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test case for
 * <a href="https://issues.apache.org/jira/browse/CALCITE-7707">[CALCITE-7707]
 * Conversion of a ROW containing a DATE to string renders the date in the
 * JVM time zone instead of the connection time zone</a>.
 */
public class StructAccessorGetStringTest {
  /**
   * {@code getString()} on a struct must render DATE and TIMESTAMP fields
   * from their internal representations, free of any time zone, like
   * top-level columns; the result must not depend on the connection calendar
   * or on the JVM default time zone. NULL fields render as "null".
   */
  @Test public void testGetStringRendersDatetimeFieldsTimeZoneFree()
      throws SQLException {
    final ColumnMetaData structMetaData = MetaImpl.columnMetaData("S", 0,
        ColumnMetaData.struct(
            Arrays.asList(
                MetaImpl.columnMetaData("D", 0,
                    ColumnMetaData.scalar(Types.DATE, "DATE",
                        ColumnMetaData.Rep.PRIMITIVE_INT),
                    true),
                MetaImpl.columnMetaData("TS", 1,
                    ColumnMetaData.scalar(Types.TIMESTAMP, "TIMESTAMP",
                        ColumnMetaData.Rep.PRIMITIVE_LONG),
                    true),
                MetaImpl.columnMetaData("I", 2, int.class, false))),
        false);
    final List<List<Object>> rows =
        Arrays.asList(
            // 18262 is 2020-01-01
            // 1577872800000 is 2020-01-01 10:00:00
            Collections.singletonList(
                (Object) new Object[] {18262, 1577872800000L, 1}),
            Collections.singletonList(
                (Object) new Object[] {null, null, 2}));
    // A connection calendar 12 hours east of the JVM default; the rendered
    // string must be the same as with the default calendar
    final Calendar connectionCalendar = Calendar.getInstance(
        new SimpleTimeZone(
            TimeZone.getDefault().getOffset(0)
                + 12 * (int) DateTimeUtils.MILLIS_PER_HOUR,
            "EAST12"),
        Locale.ROOT);
    try (Cursor cursor = new ListIteratorCursor(rows.iterator())) {
      final List<Accessor> accessors =
          cursor.createAccessors(Collections.singletonList(structMetaData),
              connectionCalendar, null);
      assertThat(cursor.next(), is(true));
      assertThat(accessors.get(0).getString(),
          is("{2020-01-01, 2020-01-01 10:00:00, 1}"));
      assertThat(cursor.next(), is(true));
      assertThat(accessors.get(0).getString(), is("{null, null, 2}"));
    }
  }
}
