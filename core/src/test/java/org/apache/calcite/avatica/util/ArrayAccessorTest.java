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
import org.apache.calcite.avatica.ColumnMetadataTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Test class for verifying functionality in array accessor from abstract cursor.
 */
@RunWith(Parameterized.class)
public class ArrayAccessorTest {

  private static final double DELTA = 1e-15;
  private static final ArrayImpl.Factory ARRAY_FACTORY =
      new ArrayFactoryImpl(Unsafe.localCalendar().getTimeZone());

  private static final List<Function<List<List<Object>>, Cursor>> CURSOR_BUILDER =
      Arrays.asList(CursorTestUtils::createArrayBasedCursor,
          CursorTestUtils::createListBasedCursor);

  private Function<List<List<Object>>, Cursor> cursorBuilder;

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

  @Test public void listIteratorFromIntegerArray() throws Exception {
    AssertTestUtils.Validator validator =
        (Object o1, Object o2) -> assertEquals((int) o1, (int) o2);

    ColumnMetaData.ScalarType intType =
        ColumnMetadataTestUtils.getScalarTypeByTypeId(Types.INTEGER, false);

    ColumnMetaData arrayMetadata = ColumnMetadataTestUtils.createArrayColumnMetaData(intType);

    List<List<Object>> rowsValues = Arrays.asList(Arrays.asList(1, 2),
        Collections.singletonList(3), Arrays.asList(4, 5, 6));

    try (Cursor cursor = cursorBuilder.apply(rowsValues)) {
      AssertTestUtils.assertRowsValuesMatchCursorContentViaArrayAccessor(
          rowsValues, intType, cursor, arrayMetadata, ARRAY_FACTORY, validator);
    }
  }

  @Test public void resultSetFromRealArray() throws Exception {
    AssertTestUtils.Validator validator =
        (Object o1, Object o2) -> assertEquals((float) o1, (float) o2, DELTA);

    ColumnMetaData.ScalarType realType =
        ColumnMetadataTestUtils.getScalarTypeByTypeId(Types.REAL, false);

    ColumnMetaData arrayMetadata = ColumnMetadataTestUtils.createArrayColumnMetaData(realType);

    List<List<Object>> rowsValues = Arrays.asList(
        Arrays.asList(1.123f, 0.2f),
        Arrays.asList(4.1f, 5f, 66.12345f)
    );

    try (Cursor cursor = cursorBuilder.apply(rowsValues)) {
      AssertTestUtils.assertRowsValuesMatchCursorContentViaArrayAccessor(
          rowsValues, realType, cursor, arrayMetadata, ARRAY_FACTORY, validator);
    }
  }

  @Test public void resultSetFromDoubleArray() throws Exception {
    AssertTestUtils.Validator validator =
        (Object o1, Object o2) -> assertEquals((double) o1, (double) o2, DELTA);

    ColumnMetaData.ScalarType doubleType =
        ColumnMetadataTestUtils.getScalarTypeByTypeId(Types.DOUBLE, false);

    ColumnMetaData arrayMetadata = ColumnMetadataTestUtils.createArrayColumnMetaData(doubleType);

    List<List<Object>> rowsValues = Arrays.asList(
        Arrays.asList(1.123d, 0.123456789012d),
        Arrays.asList(4.134555d, 54444d, 66.12345d)
    );

    try (Cursor cursor = cursorBuilder.apply(rowsValues)) {
      AssertTestUtils.assertRowsValuesMatchCursorContentViaArrayAccessor(
          rowsValues, doubleType, cursor, arrayMetadata, ARRAY_FACTORY, validator);
    }
  }

  @Test public void resultSetFromFloatArray() throws Exception {
    AssertTestUtils.Validator validator =
        (Object o1, Object o2) -> assertEquals((double) o1, (double) o2, DELTA);

    ColumnMetaData.ScalarType floatType =
        ColumnMetadataTestUtils.getScalarTypeByTypeId(Types.FLOAT, false);

    ColumnMetaData arrayMetadata = ColumnMetadataTestUtils.createArrayColumnMetaData(floatType);

    List<List<Object>> rowsValues = Arrays.asList(
        Arrays.asList(1.123d, 0.123456789012d),
        Arrays.asList(4.134555d, 54444d, 66.12345d)
    );

    try (Cursor cursor = cursorBuilder.apply(rowsValues)) {
      AssertTestUtils.assertRowsValuesMatchCursorContentViaArrayAccessor(
          rowsValues, floatType, cursor, arrayMetadata, ARRAY_FACTORY, validator);
    }
  }
}

// End ArrayAccessorTest.java
