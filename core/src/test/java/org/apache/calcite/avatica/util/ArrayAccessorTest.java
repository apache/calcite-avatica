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
import org.apache.calcite.avatica.ColumnMetaData.Rep;
import org.apache.calcite.avatica.MetaImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.calcite.avatica.AvaticaMatchers.isArrayAccessorResult;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test class for verifying functionality in array accessor from abstract cursor.
 */
@RunWith(Parameterized.class)
public class ArrayAccessorTest {

  private static final ArrayImpl.Factory ARRAY_FACTORY =
      new ArrayFactoryImpl(Unsafe.localCalendar().getTimeZone());

  private static final List<Function<List<List<Object>>, Cursor>> CURSOR_BUILDER =
      Arrays.asList(CursorTestUtils::createArrayBasedCursor,
          CursorTestUtils::createListBasedCursor);

  private final Function<List<List<Object>>, Cursor> cursorBuilder;

  @Parameterized.Parameters
  public static List<Object[]> parameters() throws Exception {
    return CURSOR_BUILDER.stream()
        .map(Collections::singletonList)
        .map(List::toArray)
        .collect(Collectors.toList());
  }

  public ArrayAccessorTest(Function<List<List<Object>>, Cursor> cursorBuilder) {
    this.cursorBuilder = cursorBuilder;
  }

  /** Shorthand to create a singleton array. */
  private static <E> List<E> list(E e) {
    return Collections.singletonList(e);
  }

  /** Shorthand to create an array with two entries. */
  private static <E> List<E> list(E e0, E e1) {
    return Arrays.asList(e0, e1);
  }

  /** Shorthand to create an array with three entries. */
  private static <E> List<E> list(E e0, E e1, E e2) {
    return Arrays.asList(e0, e1, e2);
  }

  @Test public void listIteratorFromIntegerArray() throws Exception {
    ColumnMetaData.ScalarType intType =
        ColumnMetaData.scalar(Types.INTEGER, "INTEGER", Rep.INTEGER);

    ColumnMetaData arrayMetadata = createArrayMetaData(intType);

    List<List<Object>> rowsValues = list(list(1, 2), list(3), list(4, 5, 6));

    try (Cursor cursor = cursorBuilder.apply(rowsValues)) {
      Cursor.Accessor accessor = createArrayAccessor(cursor, arrayMetadata);
      int rowid = 0;
      while (cursor.next()) {
        List<Object> expectedArray = rowsValues.get(rowid);
        assertThat(accessor, isArrayAccessorResult(expectedArray, Integer.class));
        rowid++;
      }
    }
  }

  @Test public void resultSetFromRealArray() throws Exception {
    ColumnMetaData.ScalarType realType = ColumnMetaData.scalar(Types.REAL, "REAL", Rep.FLOAT);

    ColumnMetaData arrayMetadata = createArrayMetaData(realType);

    List<List<Object>> rowsValues = list(
        list(1.123f, 0.2f),
        list(4.1f, 5f, 66.12345f));

    try (Cursor cursor = cursorBuilder.apply(rowsValues)) {
      Cursor.Accessor accessor = createArrayAccessor(cursor, arrayMetadata);
      int rowid = 0;
      while (cursor.next()) {
        List<Object> expectedArray = rowsValues.get(rowid);
        assertThat(accessor, isArrayAccessorResult(expectedArray, Float.class));
        rowid++;
      }
    }
  }

  @Test public void resultSetFromDoubleArray() throws Exception {
    ColumnMetaData.ScalarType doubleType =
        ColumnMetaData.scalar(Types.DOUBLE, "DOUBLE", Rep.DOUBLE);

    ColumnMetaData arrayMetadata = createArrayMetaData(doubleType);

    List<List<Object>> rowsValues = list(
        list(1.123d, 0.123456789012d),
        list(4.134555d, 54444d, 66.12345d));

    try (Cursor cursor = cursorBuilder.apply(rowsValues)) {
      Cursor.Accessor accessor = createArrayAccessor(cursor, arrayMetadata);
      int rowid = 0;
      while (cursor.next()) {
        List<Object> expectedArray = rowsValues.get(rowid);
        assertThat(accessor, isArrayAccessorResult(expectedArray, Double.class));
        rowid++;
      }
    }
  }

  @Test public void resultSetFromFloatArray() throws Exception {
    ColumnMetaData.ScalarType floatType = ColumnMetaData.scalar(Types.FLOAT, "FLOAT", Rep.DOUBLE);

    ColumnMetaData arrayMetadata = createArrayMetaData(floatType);

    List<List<Object>> rowsValues = list(
        list(1.123d, 0.123456789012d),
        list(4.134555d, 54444d, 66.12345d));

    try (Cursor cursor = cursorBuilder.apply(rowsValues)) {
      Cursor.Accessor accessor = createArrayAccessor(cursor, arrayMetadata);
      int rowid = 0;
      while (cursor.next()) {
        List<Object> expectedArray = rowsValues.get(rowid);
        assertThat(accessor, isArrayAccessorResult(expectedArray, Double.class));
        rowid++;
      }
    }
  }

  /** Test case for
   * <a href="https://issues.apache.org/jira/browse/CALCITE-3557">[CALCITE-3557]
   * ResultSet.getObject throws ClassCastException when applied to an ARRAY
   * or MULTISET inside a MULTISET</a>. */
  @Test public void resultSetFromMultisetArray() throws Exception {
    ColumnMetaData.ScalarType intType =
        ColumnMetaData.scalar(Types.INTEGER, "INTEGER", Rep.INTEGER);
    ColumnMetaData.ArrayType multisetArrayType =
        ColumnMetaData.array(ColumnMetaData.array(intType, "ARRAY INTEGER", Rep.ARRAY),
            "MULTISET ARRAY INTEGER", Rep.MULTISET);
    ColumnMetaData multisetMetaData =
        MetaImpl.columnMetaData("MY_MULTISET", 0, multisetArrayType, false);

    // MULTISET[ARRAY[1, 2]]
    List<Object> firstRow = list(list(new Object[]{1}, new Object[]{2}));
    // MULTISET[ARRAY[3, 4]]
    List<Object> secondRow = list(list(new Object[]{3}, new Object[]{4}));
    List<List<Object>> inputRowsValues = list(
        firstRow,
        secondRow);

    List<List<Object>> expectedRowsValues = list(
        list(list(1, 2)),
        list(list(3, 4)));

    try (Cursor cursor = cursorBuilder.apply(inputRowsValues)) {
      Cursor.Accessor accessor = createArrayAccessor(cursor, multisetMetaData);
      int rowid = 0;
      while (cursor.next()) {
        List<Object> expectedArray = expectedRowsValues.get(rowid);
        assertThat(accessor, isArrayAccessorResult(expectedArray, ArrayImpl.class));
        rowid++;
      }
    }
  }

  private static ColumnMetaData createArrayMetaData(ColumnMetaData.ScalarType componentType) {
    ColumnMetaData.ArrayType arrayType =
        ColumnMetaData.array(componentType, componentType.name, componentType.rep);
    return MetaImpl.columnMetaData("MY_ARRAY", 1, arrayType, false);
  }

  private static Cursor.Accessor createArrayAccessor(Cursor c, ColumnMetaData meta) {
    List<Cursor.Accessor> accessors =
        c.createAccessors(list(meta), Unsafe.localCalendar(), ARRAY_FACTORY);
    return accessors.get(0);
  }
}

// End ArrayAccessorTest.java
