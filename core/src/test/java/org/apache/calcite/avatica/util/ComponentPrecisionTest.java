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
import org.apache.calcite.avatica.proto.Common;
import org.apache.calcite.avatica.util.Cursor.Accessor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test cases for
 * <a href="https://issues.apache.org/jira/browse/CALCITE-7710">[CALCITE-7710]
 * Avatica component types do not carry precision or scale</a>.
 */
public class ComponentPrecisionTest {

  private static final ColumnMetaData.AvaticaType TIMESTAMP3_TYPE =
      ColumnMetaData.scalar(Types.TIMESTAMP, "TIMESTAMP",
          ColumnMetaData.Rep.PRIMITIVE_LONG, 3, ColumnMetaData.AvaticaType.NOT_SPECIFIED);
  private static final ColumnMetaData.AvaticaType TIME3_TYPE =
      ColumnMetaData.scalar(Types.TIME, "TIME",
          ColumnMetaData.Rep.PRIMITIVE_INT, 3, ColumnMetaData.AvaticaType.NOT_SPECIFIED);

  /** Creates an accessor for a single ARRAY column over a single row holding
   * the given list. The calendar is UTC, so that no time zone offset is
   * applied when the elements are converted. */
  private static Accessor arrayAccessorFor(
      ColumnMetaData.AvaticaType componentType, List<Object> value)
      throws SQLException {
    final ColumnMetaData metaData = MetaImpl.columnMetaData("A", 0,
        ColumnMetaData.array(componentType, "ARRAY", ColumnMetaData.Rep.ARRAY),
        true);
    final List<List<Object>> rows =
        Collections.singletonList(Collections.singletonList((Object) value));
    final Cursor cursor = new ListIteratorCursor(rows.iterator());
    final List<Accessor> accessors =
        cursor.createAccessors(Collections.singletonList(metaData),
            Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT), null);
    assertThat(cursor.next(), is(true));
    return accessors.get(0);
  }

  /** An ARRAY of TIMESTAMP(3) renders the fractional seconds of its
   * elements; the component precision travels on the AvaticaType, because a
   * component has no ColumnMetaData. */
  @Test public void testArrayOfTimestampRendersFraction() throws SQLException {
    // 1577872800123 is 2020-01-01 10:00:00.123
    final Accessor accessor =
        arrayAccessorFor(TIMESTAMP3_TYPE,
            Collections.singletonList((Object) 1577872800123L));
    assertThat(accessor.getString(), is("[2020-01-01 10:00:00.123]"));
  }

  /** An ARRAY of TIME(3) renders the fractional seconds of its elements. */
  @Test public void testArrayOfTimeRendersFraction() throws SQLException {
    // 36000123 is 10:00:00.123
    final Accessor accessor =
        arrayAccessorFor(TIME3_TYPE, Collections.singletonList((Object) 36000123));
    assertThat(accessor.getString(), is("[10:00:00.123]"));
  }

  /** Renders a TIMESTAMP element under every precision from 0 to 6. The
   * internal representation has millisecond resolution, so precisions below
   * 3 truncate the fraction and precisions above 3 pad it with zeros. */
  @Test public void testArrayOfTimestampEveryPrecision() throws SQLException {
    final String[] expected = {
        "[2020-01-01 10:00:00]",
        "[2020-01-01 10:00:00.1]",
        "[2020-01-01 10:00:00.12]",
        "[2020-01-01 10:00:00.123]",
        "[2020-01-01 10:00:00.1230]",
        "[2020-01-01 10:00:00.12300]",
        "[2020-01-01 10:00:00.123000]",
    };
    for (int precision = 0; precision <= 6; precision++) {
      final ColumnMetaData.AvaticaType type =
          ColumnMetaData.scalar(Types.TIMESTAMP, "TIMESTAMP",
              ColumnMetaData.Rep.PRIMITIVE_LONG, precision,
              ColumnMetaData.AvaticaType.NOT_SPECIFIED);
      final Accessor accessor =
          arrayAccessorFor(type, Collections.singletonList((Object) 1577872800123L));
      assertThat("precision " + precision, accessor.getString(), is(expected[precision]));
    }
  }

  /** Renders a TIME element under every precision from 0 to 6. */
  @Test public void testArrayOfTimeEveryPrecision() throws SQLException {
    final String[] expected = {
        "[10:00:00]",
        "[10:00:00.1]",
        "[10:00:00.12]",
        "[10:00:00.123]",
        "[10:00:00.1230]",
        "[10:00:00.12300]",
        "[10:00:00.123000]",
    };
    for (int precision = 0; precision <= 6; precision++) {
      final ColumnMetaData.AvaticaType type =
          ColumnMetaData.scalar(Types.TIME, "TIME", ColumnMetaData.Rep.PRIMITIVE_INT, precision,
              ColumnMetaData.AvaticaType.NOT_SPECIFIED);
      final Accessor accessor =
          arrayAccessorFor(type, Collections.singletonList((Object) 36000123));
      assertThat("precision " + precision, accessor.getString(), is(expected[precision]));
    }
  }

  /** The scale of a DECIMAL component is applied when converting a
   * non-BigDecimal element via {@code getBigDecimal()}; it does not affect
   * {@code getString()}, which renders the raw number. */
  @Test public void testArrayOfDecimalAppliesScale() throws SQLException {
    final ColumnMetaData.AvaticaType decimalType =
        ColumnMetaData.scalar(Types.DECIMAL, "DECIMAL", ColumnMetaData.Rep.NUMBER, 10, 2);
    final Accessor accessor =
        arrayAccessorFor(decimalType, Collections.singletonList((Object) 157L));
    final List<?> converted = (List<?>) accessor.getObject();
    assertThat(converted.get(0), is((Object) new java.math.BigDecimal("157.00")));
  }

  /** A component of unspecified scale is not rescaled: the sentinel must
   * never reach {@code BigDecimal.setScale}, where a negative value would
   * round the number. */
  @Test public void testArrayOfDecimalUnspecifiedScaleDoesNotRescale()
      throws SQLException {
    final ColumnMetaData.AvaticaType decimalType =
        ColumnMetaData.scalar(Types.DECIMAL, "DECIMAL", ColumnMetaData.Rep.NUMBER);
    final Accessor accessor =
        arrayAccessorFor(decimalType, Collections.singletonList((Object) 157L));
    final List<?> converted = (List<?>) accessor.getObject();
    assertThat(converted.get(0), is((Object) new java.math.BigDecimal("157")));
  }

  /** Precision and scale survive a protobuf round trip; their absence is
   * preserved as {@link ColumnMetaData.AvaticaType#NOT_SPECIFIED}. */
  @Test public void testProtobufRoundTrip() {
    final ColumnMetaData.AvaticaType decimal =
        ColumnMetaData.scalar(Types.DECIMAL, "DECIMAL", ColumnMetaData.Rep.NUMBER, 10, 2);
    assertThat(ColumnMetaData.AvaticaType.fromProto(decimal.toProto()), is(decimal));

    // A negative scale is a legal value, distinct from the sentinel, and
    // survives the signed wire representation
    final ColumnMetaData.AvaticaType negativeScale = ColumnMetaData.scalar(Types.DECIMAL, "DECIMAL",
            ColumnMetaData.Rep.NUMBER, 10, -2);
    assertThat(ColumnMetaData.AvaticaType.fromProto(negativeScale.toProto()), is(negativeScale));

    final ColumnMetaData.AvaticaType array =
        ColumnMetaData.array(TIMESTAMP3_TYPE, "ARRAY", ColumnMetaData.Rep.ARRAY);
    assertThat(ColumnMetaData.AvaticaType.fromProto(array.toProto()), is(array));

    final ColumnMetaData.AvaticaType unspecified =
        ColumnMetaData.scalar(Types.TIMESTAMP, "TIMESTAMP", ColumnMetaData.Rep.PRIMITIVE_LONG);
    final ColumnMetaData.AvaticaType roundTripped =
        ColumnMetaData.AvaticaType.fromProto(unspecified.toProto());
    assertThat(roundTripped.precision, is(ColumnMetaData.AvaticaType.NOT_SPECIFIED));
    assertThat(roundTripped, is(unspecified));
  }

  /** TIMESTAMP(0) and a timestamp of unspecified precision are distinct,
   * in Java and through the protobuf round trip. */
  @Test public void testPrecisionZeroIsDistinctFromUnspecified() {
    final ColumnMetaData.AvaticaType zero =
        ColumnMetaData.scalar(Types.TIMESTAMP, "TIMESTAMP",
            ColumnMetaData.Rep.PRIMITIVE_LONG, 0, ColumnMetaData.AvaticaType.NOT_SPECIFIED);
    final ColumnMetaData.AvaticaType unspecified =
        ColumnMetaData.scalar(Types.TIMESTAMP, "TIMESTAMP", ColumnMetaData.Rep.PRIMITIVE_LONG);
    assertThat(zero, is(not(unspecified)));
    assertThat(ColumnMetaData.AvaticaType.fromProto(zero.toProto()), is(zero));
    assertThat(ColumnMetaData.AvaticaType.fromProto(zero.toProto()), is(not(unspecified)));
  }

  /** A protobuf message from an older peer, which has no precision and
   * scale fields, deserializes to unspecified precision, and values render
   * exactly as they did before the fields existed. */
  @Test public void testProtobufFromOldPeer() throws SQLException {
    // Exactly the bytes an older peer serializes: fields 1-3 only
    final Common.AvaticaType oldProto = Common.AvaticaType.newBuilder()
        .setId(Types.TIMESTAMP)
        .setName("TIMESTAMP")
        .setRep(Common.Rep.PRIMITIVE_LONG)
        .build();
    final ColumnMetaData.AvaticaType type = ColumnMetaData.AvaticaType.fromProto(oldProto);
    assertThat(type.precision, is(ColumnMetaData.AvaticaType.NOT_SPECIFIED));
    assertThat(type.scale, is(ColumnMetaData.AvaticaType.NOT_SPECIFIED));

    // Renders without a fraction
    final Accessor accessor =
        arrayAccessorFor(type, Collections.singletonList((Object) 1577872800123L));
    assertThat(accessor.getString(), is("[2020-01-01 10:00:00]"));
  }

  /** A peer with an older version that does not specify precision emits messages byte-identical
   * messages */
  @Test public void testWireFormatUnchangedWhenUnspecified() {
    final Common.AvaticaType oldProto = Common.AvaticaType.newBuilder()
        .setId(Types.TIMESTAMP)
        .setName("TIMESTAMP")
        .setRep(Common.Rep.PRIMITIVE_LONG)
        .build();
    final ColumnMetaData.AvaticaType unspecified =
        ColumnMetaData.scalar(Types.TIMESTAMP, "TIMESTAMP", ColumnMetaData.Rep.PRIMITIVE_LONG);
    assertThat(unspecified.toProto().toByteArray(), is(oldProto.toByteArray()));
  }

  /** JSON without the new properties, as an older peer produces, yields
   * unspecified precision and scale; then they are present, they are preserved. */
  @Test public void testJsonCompatibility() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final ColumnMetaData.AvaticaType old =
        mapper.readValue(
            "{\"type\":\"scalar\",\"id\":93,\"name\":\"TIMESTAMP\","
                + "\"rep\":\"PRIMITIVE_LONG\"}", ColumnMetaData.AvaticaType.class);
    assertThat(old.precision, is(ColumnMetaData.AvaticaType.NOT_SPECIFIED));
    assertThat(old.scale, is(ColumnMetaData.AvaticaType.NOT_SPECIFIED));

    final String json = mapper.writeValueAsString(TIMESTAMP3_TYPE);
    assertThat(mapper.readValue(json, ColumnMetaData.AvaticaType.class), is(TIMESTAMP3_TYPE));
  }
}
