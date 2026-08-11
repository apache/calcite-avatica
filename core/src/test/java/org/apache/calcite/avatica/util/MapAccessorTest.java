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

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test cases for
 * <a href="https://issues.apache.org/jira/browse/CALCITE-7708">[CALCITE-7708]
 * Avatica does not support the MAP type</a>.
 */
public class MapAccessorTest {

  private static final ColumnMetaData.AvaticaType VARCHAR_TYPE =
      ColumnMetaData.scalar(Types.VARCHAR, "VARCHAR",
          ColumnMetaData.Rep.STRING);
  private static final ColumnMetaData.AvaticaType DATE_TYPE =
      ColumnMetaData.scalar(Types.DATE, "DATE",
          ColumnMetaData.Rep.PRIMITIVE_INT);
  private static final ColumnMetaData.AvaticaType TIMESTAMP_TYPE =
      ColumnMetaData.scalar(Types.TIMESTAMP, "TIMESTAMP",
          ColumnMetaData.Rep.PRIMITIVE_LONG);

  /** Creates an accessor for a single MAP column over a single row holding
   * the given map. */
  private static Accessor accessorFor(ColumnMetaData.AvaticaType keyType,
      ColumnMetaData.AvaticaType valueType, Map<Object, Object> map,
      Calendar calendar) throws SQLException {
    final ColumnMetaData metaData = MetaImpl.columnMetaData("M", 0,
        ColumnMetaData.map(keyType, valueType), true);
    final List<List<Object>> rows =
        Collections.singletonList(Collections.singletonList((Object) map));
    final Cursor cursor = new ListIteratorCursor(rows.iterator());
    final List<Accessor> accessors =
        cursor.createAccessors(Collections.singletonList(metaData),
            calendar, null);
    assertThat(cursor.next(), is(true));
    return accessors.get(0);
  }

  private static Calendar defaultCalendar() {
    return Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT);
  }

  /** A map with a ROW (STRUCT) value renders the row like a top-level struct. */
  @Test public void testGetStringRendersStructValues() throws SQLException {
    final ColumnMetaData.AvaticaType structType =
        ColumnMetaData.struct(
            Arrays.asList(
                MetaImpl.columnMetaData("A", 0, int.class, false),
                MetaImpl.columnMetaData("B", 1, String.class, false)));
    final Map<Object, Object> map = new LinkedHashMap<>();
    map.put("k", new Object[] {1, "a"});
    final Accessor accessor =
        accessorFor(VARCHAR_TYPE, structType, map, defaultCalendar());
    assertThat(accessor.getString(), is("{k={1, a}}"));
  }

  /** A map with DATE and TIMESTAMP values renders them from their internal
   * representations, not as raw numbers. */
  @Test public void testGetStringRendersDatetimeValues()
      throws SQLException {
    final Map<Object, Object> dateMap = new LinkedHashMap<>();
    // 18262 is 2020-01-01
    dateMap.put("k", 18262);
    dateMap.put("n", null);
    final Accessor dateAccessor =
        accessorFor(VARCHAR_TYPE, DATE_TYPE, dateMap, defaultCalendar());
    assertThat(dateAccessor.getString(), is("{k=2020-01-01, n=null}"));

    final Map<Object, Object> timestampMap = new LinkedHashMap<>();
    // 1577872800000 is 2020-01-01 10:00:00
    timestampMap.put("k", 1577872800000L);
    final Accessor timestampAccessor =
        accessorFor(VARCHAR_TYPE, TIMESTAMP_TYPE, timestampMap,
            defaultCalendar());
    assertThat(timestampAccessor.getString(), is("{k=2020-01-01 10:00:00}"));
  }

  /** A map with an ARRAY-of-ROW value renders the rows inside the list. */
  @Test public void testGetStringRendersArrayOfStructValues()
      throws SQLException {
    final ColumnMetaData.AvaticaType structType =
        ColumnMetaData.struct(
            Arrays.asList(
                MetaImpl.columnMetaData("A", 0, int.class, false),
                MetaImpl.columnMetaData("B", 1, String.class, false)));
    final ColumnMetaData.AvaticaType arrayType =
        ColumnMetaData.array(structType, "ARRAY", ColumnMetaData.Rep.ARRAY);
    final Map<Object, Object> map = new LinkedHashMap<>();
    map.put("k",
        Collections.singletonList((Object) new Object[] {1, "a"}));
    final Accessor accessor =
        accessorFor(VARCHAR_TYPE, arrayType, map, defaultCalendar());
    assertThat(accessor.getString(), is("{k=[{1, a}]}"));
  }

  /** {@code getObject()} converts values to their JDBC representation, similar to
   * the elements of an ARRAY. */
  @Test public void testGetObjectConvertsValues() throws SQLException {
    final Map<Object, Object> map = new LinkedHashMap<>();
    map.put("k", 18262);
    final Accessor accessor =
        accessorFor(VARCHAR_TYPE, DATE_TYPE, map, defaultCalendar());
    final Object converted = ((Map<?, ?>) accessor.getObject()).get("k");
    assertThat(converted, instanceOf(Date.class));
  }

  /** A NULL map renders as null. */
  @Test public void testNullMap() throws SQLException {
    final Accessor accessor =
        accessorFor(VARCHAR_TYPE, DATE_TYPE, null, defaultCalendar());
    assertThat(accessor.getString(), is((String) null));
    assertThat(accessor.getObject(), is((Object) null));
  }

  /** NULL keys and NULL values are preserved by {@code getObject()} and
   * render as "null" in {@code getString()}. */
  @Test public void testNullKeysAndValues() throws SQLException {
    final Map<Object, Object> map = new LinkedHashMap<>();
    map.put(null, 18262);
    map.put("k", null);
    final Accessor accessor =
        accessorFor(VARCHAR_TYPE, DATE_TYPE, map, defaultCalendar());
    assertThat(accessor.getString(), is("{null=2020-01-01, k=null}"));
    final Map<?, ?> converted = (Map<?, ?>) accessor.getObject();
    assertThat(converted.get(null), instanceOf(Date.class));
    assertThat(converted.containsKey("k"), is(true));
    assertThat(converted.get("k"), is((Object) null));
  }

  /** {@link ColumnMetaData.MapType} survives a protobuf round trip. */
  @Test public void testMapTypeProtobufRoundTrip() {
    final ColumnMetaData.MapType mapType =
        ColumnMetaData.map(VARCHAR_TYPE, DATE_TYPE);
    final ColumnMetaData.AvaticaType roundTripped =
        ColumnMetaData.AvaticaType.fromProto(mapType.toProto());
    assertThat(roundTripped, is((ColumnMetaData.AvaticaType) mapType));

    // Nested: a map whose values are maps
    final ColumnMetaData.MapType nested =
        ColumnMetaData.map(VARCHAR_TYPE, mapType);
    assertThat(ColumnMetaData.AvaticaType.fromProto(nested.toProto()),
        is((ColumnMetaData.AvaticaType) nested));
  }
}

// End MapAccessorTest.java
