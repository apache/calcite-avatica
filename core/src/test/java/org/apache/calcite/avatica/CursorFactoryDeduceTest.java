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
package org.apache.calcite.avatica;

import org.apache.calcite.avatica.util.Cursor;
import org.apache.calcite.avatica.util.Unsafe;

import org.junit.Test;

import java.math.BigInteger;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@code Meta.CursorFactory} relative to deducing a cursor factory
 * from the columns metadata.
 */
public class CursorFactoryDeduceTest {

  static final ColumnMetaData.AvaticaType INT_TYPE =
      ColumnMetaData.scalar(Types.INTEGER, "INT", ColumnMetaData.Rep.PRIMITIVE_INT);
  static final ColumnMetaData.AvaticaType STRING_TYPE =
      ColumnMetaData.scalar(Types.VARCHAR, "STRING", ColumnMetaData.Rep.STRING);
  static final ColumnMetaData.AvaticaType NVARCHAR_STRING_TYPE =
      ColumnMetaData.scalar(Types.NVARCHAR, "STRING", ColumnMetaData.Rep.STRING);
  static final ColumnMetaData.AvaticaType DOUBLE_TYPE =
      ColumnMetaData.scalar(Types.DOUBLE, "DOUBLE", ColumnMetaData.Rep.DOUBLE);

  static final List<Object> ROWS = IntStream.range(1, 5)
      .mapToObj(i -> (Object) new SimplePOJO((byte) i, (short) i, i, i,
          new BigInteger(Integer.toString(i)), Integer.toString(i), (double) i))
      .collect(Collectors.toList());

  /**
   * Simple POJO for testing cursors over Java objects.
   */
  protected static class SimplePOJO {
    public byte byteField;
    public short shortField;
    public int intField;
    public long longField;
    public BigInteger bigIntField;
    public String stringField;
    public Double doubleField;

    SimplePOJO(byte byteField, short shortField, int intField, long longField,
               BigInteger bigIntField, String stringField, Double doubleField) {
      this.byteField = byteField;
      this.shortField = shortField;
      this.intField = intField;
      this.longField = longField;
      this.bigIntField = bigIntField;
      this.stringField = stringField;
      this.doubleField = doubleField;
    }

    @Override public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null) {
        return false;
      }

      if (getClass() != o.getClass()) {
        return false;
      }

      SimplePOJO pjo = (SimplePOJO) o;

      return Objects.equals(byteField, pjo.byteField)
          && Objects.equals(shortField, pjo.shortField)
          && Objects.equals(intField, pjo.intField)
          && Objects.equals(longField, pjo.longField)
          && Objects.equals(bigIntField, pjo.bigIntField)
          && Objects.equals(stringField, pjo.stringField)
          && Objects.equals(doubleField, pjo.doubleField);
    }

    @Override public int hashCode() {
      return Objects.hash(byteField, shortField, intField,
          longField, bigIntField, stringField, doubleField);
    }
  }

  /**
   * Deducing the cursor from a single column having a Java object as type.
   */
  @Test public void deduceObjectCursorFactory() throws Exception {
    ColumnMetaData.Rep rep = ColumnMetaData.Rep.of(SimplePOJO.class);
    ColumnMetaData.AvaticaType pojoType =
        ColumnMetaData.scalar(Types.OTHER, "OTHER", rep);

    ColumnMetaData pojoMetadata =
        MetaImpl.columnMetaData("POJO", 1, pojoType, true);

    List<ColumnMetaData> columnMetaDataList = Collections.singletonList(pojoMetadata);

    Meta.CursorFactory cursorFactory =
        Meta.CursorFactory.deduce(columnMetaDataList, SimplePOJO.class);

    try (Cursor cursor = MetaImpl.createCursor(cursorFactory, ROWS)) {
      List<Cursor.Accessor> accessors =
          cursor.createAccessors(columnMetaDataList, Unsafe.localCalendar(), null);

      assertEquals(1, accessors.size());
      Cursor.Accessor accessor = accessors.get(0);

      for (Object row : ROWS) {
        assertTrue(cursor.next());
        assertEquals(row, accessor.getObject());
      }

      assertFalse(cursor.next());
    }
  }

  /**
   * Deducing the cursor when columns are the fields of a Java object.
   */
  @Test public void deduceRecordCursorFactory() throws Exception {
    List<ColumnMetaData> columnsMetaDataList = Arrays.asList(
        MetaImpl.columnMetaData("intField", 1, INT_TYPE, true),
        MetaImpl.columnMetaData("stringField", 2, STRING_TYPE, true),
        MetaImpl.columnMetaData("doubleField", 3, DOUBLE_TYPE, true));

    Meta.CursorFactory cursorFactory =
        Meta.CursorFactory.deduce(columnsMetaDataList, SimplePOJO.class);

    try (Cursor cursor = MetaImpl.createCursor(cursorFactory, ROWS)) {
      List<Cursor.Accessor> accessors =
          cursor.createAccessors(columnsMetaDataList, Unsafe.localCalendar(), null);

      assertEquals(columnsMetaDataList.size(), accessors.size());
      Cursor.Accessor intAccessor = accessors.get(0);
      Cursor.Accessor strAccessor = accessors.get(1);
      Cursor.Accessor doubleAccessor = accessors.get(2);

      for (Object row : ROWS) {
        assertTrue(cursor.next());
        SimplePOJO pjo = (SimplePOJO) row;
        assertEquals(pjo.intField, intAccessor.getObject());
        assertEquals(pjo.stringField, strAccessor.getObject());
        assertEquals(pjo.doubleField, doubleAccessor.getObject());
      }

      assertFalse(cursor.next());
    }
  }

  /**
   * Deducing the cursor when columns are the fields of a Java object,
   * different columns ordering.
   */
  @Test public void deduceRecordCursorFactoryDifferentFieldsOrdering() throws Exception {
    List<ColumnMetaData> columnsMetaDataList = Arrays.asList(
        MetaImpl.columnMetaData("stringField", 2, STRING_TYPE, true),
        MetaImpl.columnMetaData("doubleField", 3, DOUBLE_TYPE, true),
        MetaImpl.columnMetaData("intField", 1, INT_TYPE, true));

    Meta.CursorFactory cursorFactory =
        Meta.CursorFactory.deduce(columnsMetaDataList, SimplePOJO.class);

    try (Cursor cursor = MetaImpl.createCursor(cursorFactory, ROWS)) {
      List<Cursor.Accessor> accessors =
          cursor.createAccessors(columnsMetaDataList, Unsafe.localCalendar(), null);

      assertEquals(columnsMetaDataList.size(), accessors.size());
      Cursor.Accessor strAccessor = accessors.get(0);
      Cursor.Accessor doubleAccessor = accessors.get(1);
      Cursor.Accessor intAccessor = accessors.get(2);

      for (Object row : ROWS) {
        assertTrue(cursor.next());
        SimplePOJO pjo = (SimplePOJO) row;
        assertEquals(pjo.intField, intAccessor.getObject());
        assertEquals(pjo.stringField, strAccessor.getObject());
        assertEquals(pjo.doubleField, doubleAccessor.getObject());
      }

      assertFalse(cursor.next());
    }
  }

  /**
   * Deducing the cursor when columns are (a subset of) the fields of a Java object.
   */
  @Test public void deduceRecordCursorFactoryProjectedFields() throws Exception {
    List<ColumnMetaData> columnsMetaDataList = Arrays.asList(
        MetaImpl.columnMetaData("stringField", 1, STRING_TYPE, true),
        MetaImpl.columnMetaData("doubleField", 2, DOUBLE_TYPE, true));

    Meta.CursorFactory cursorFactory =
        Meta.CursorFactory.deduce(columnsMetaDataList, SimplePOJO.class);

    try (Cursor cursor = MetaImpl.createCursor(cursorFactory, ROWS)) {
      List<Cursor.Accessor> accessors =
          cursor.createAccessors(columnsMetaDataList, Unsafe.localCalendar(), null);

      assertEquals(columnsMetaDataList.size(), accessors.size());
      Cursor.Accessor strAccessor = accessors.get(0);
      Cursor.Accessor doubleAccessor = accessors.get(1);

      for (Object row : ROWS) {
        assertTrue(cursor.next());
        SimplePOJO pjo = (SimplePOJO) row;
        assertEquals(pjo.stringField, strAccessor.getObject());
        assertEquals(pjo.doubleField, doubleAccessor.getObject());
      }

      assertFalse(cursor.next());
    }
  }

  @Test public void deduceRecordCursorFactoryProjectedNvarcharField() throws Exception {
    List<ColumnMetaData> columnsMetaDataList = Arrays.asList(
        MetaImpl.columnMetaData("stringField", 1, NVARCHAR_STRING_TYPE, true),
        MetaImpl.columnMetaData("doubleField", 2, DOUBLE_TYPE, true)
    );
    Meta.CursorFactory cursorFactory =
        Meta.CursorFactory.deduce(columnsMetaDataList, SimplePOJO.class);

    try (Cursor cursor = MetaImpl.createCursor(cursorFactory, ROWS)) {
      List<Cursor.Accessor> accessors =
          cursor.createAccessors(columnsMetaDataList, Unsafe.localCalendar(), null);

      assertEquals(columnsMetaDataList.size(), accessors.size());
      Cursor.Accessor strAccessor = accessors.get(0);

      for (Object row : ROWS) {
        assertTrue(cursor.next());
        SimplePOJO pjo = (SimplePOJO) row;
        assertEquals(pjo.stringField, strAccessor.getObject());
      }

      assertFalse(cursor.next());
    }
  }

  @Test public void deduceRecordCursorFactoryProjectedSignedField() throws Exception {
    List<ColumnMetaData> columnsMetaDataList = Arrays.asList(
        MetaImpl.columnMetaData("byteField", 1, ColumnMetaData.scalar(
            Types.TINYINT, "TINYINT", ColumnMetaData.Rep.BYTE), true, true),
        MetaImpl.columnMetaData("shortField", 2, ColumnMetaData.scalar(
            Types.SMALLINT, "SMALLINT", ColumnMetaData.Rep.SHORT), true, true),
        MetaImpl.columnMetaData("intField", 3, ColumnMetaData.scalar(
            Types.INTEGER, "MEDIUMINT", ColumnMetaData.Rep.INTEGER), true, true),
        MetaImpl.columnMetaData("intField", 4, ColumnMetaData.scalar(
            Types.INTEGER, "INT", ColumnMetaData.Rep.INTEGER), true, true),
        MetaImpl.columnMetaData("longField", 5, ColumnMetaData.scalar(
            Types.BIGINT, "BIGINT", ColumnMetaData.Rep.LONG), true, true)
    );
    Meta.CursorFactory cursorFactory =
        Meta.CursorFactory.deduce(columnsMetaDataList, SimplePOJO.class);

    try (Cursor cursor = MetaImpl.createCursor(cursorFactory, ROWS)) {
      List<Cursor.Accessor> accessors =
          cursor.createAccessors(columnsMetaDataList, Unsafe.localCalendar(), null);

      assertEquals(columnsMetaDataList.size(), accessors.size());
      Cursor.Accessor byteAccessor = accessors.get(0);
      Cursor.Accessor shortAccessor = accessors.get(1);
      Cursor.Accessor intAccessor = accessors.get(2);
      Cursor.Accessor intAccessor2 = accessors.get(3);
      Cursor.Accessor longAccessor = accessors.get(4);

      for (Object row : ROWS) {
        assertTrue(cursor.next());
        SimplePOJO pjo = (SimplePOJO) row;
        assertEquals(pjo.byteField, byteAccessor.getObject());
        assertEquals(pjo.shortField, shortAccessor.getObject());
        assertEquals(pjo.intField, intAccessor.getObject());
        assertEquals(pjo.intField, intAccessor2.getObject());
        assertEquals(pjo.longField, longAccessor.getObject());
      }

      assertFalse(cursor.next());
    }
  }

  @Test public void deduceRecordCursorFactoryProjectedUnsignedField() throws Exception {
    List<ColumnMetaData> columnsMetaDataList = Arrays.asList(
        MetaImpl.columnMetaData("shortField", 1, ColumnMetaData.scalar(
            Types.TINYINT, "TINYINT_UNSIGNED", ColumnMetaData.Rep.UBYTE), true, false),
        MetaImpl.columnMetaData("intField", 2, ColumnMetaData.scalar(
            Types.SMALLINT, "SMALLINT_UNSIGNED", ColumnMetaData.Rep.USHORT), true, false),
        MetaImpl.columnMetaData("longField", 3, ColumnMetaData.scalar(
            Types.INTEGER, "MEDIUMINT_UNSIGNED", ColumnMetaData.Rep.UINTEGER), true, false),
        MetaImpl.columnMetaData("longField", 4, ColumnMetaData.scalar(
            Types.INTEGER, "INT_UNSIGNED", ColumnMetaData.Rep.UINTEGER), true, false),
        MetaImpl.columnMetaData("bigIntField", 5, ColumnMetaData.scalar(
            Types.BIGINT, "BIGINT_UNSIGNED", ColumnMetaData.Rep.ULONG), true, false)
    );
    Meta.CursorFactory cursorFactory =
        Meta.CursorFactory.deduce(columnsMetaDataList, SimplePOJO.class);

    try (Cursor cursor = MetaImpl.createCursor(cursorFactory, ROWS)) {
      List<Cursor.Accessor> accessors =
          cursor.createAccessors(columnsMetaDataList, Unsafe.localCalendar(), null);

      assertEquals(columnsMetaDataList.size(), accessors.size());
      Cursor.Accessor shortAccessor = accessors.get(0);
      Cursor.Accessor intAccessor = accessors.get(1);
      Cursor.Accessor longAccessor = accessors.get(2);
      Cursor.Accessor longAccessor2 = accessors.get(3);
      Cursor.Accessor bigIntAccessor = accessors.get(4);

      for (Object row : ROWS) {
        assertTrue(cursor.next());
        SimplePOJO pjo = (SimplePOJO) row;
        assertEquals(pjo.shortField, shortAccessor.getObject());
        assertEquals(pjo.intField, intAccessor.getObject());
        assertEquals(pjo.longField, longAccessor.getObject());
        assertEquals(pjo.longField, longAccessor2.getObject());
        assertEquals(pjo.bigIntField, bigIntAccessor.getObject());
      }

      assertFalse(cursor.next());
    }
  }
}

// End CursorFactoryDeduceTest.java
