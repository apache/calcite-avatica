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

import org.junit.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.Assert.assertEquals;

/**
 * Test class for verifying functionality in abstract cursors.
 */
public class AbstractCursorTest {

  private static final double DELTA = 1e-15;

  @Test
  public void resultSetFromIntegerArray() throws Exception {
    BiFunction<Object, Object, Void> validator = (Object o1, Object o2) -> {
      assertEquals((int) o1, (int) o2);
      return null;
    };

    ColumnMetaData.ScalarType intType =
        ColumnMetaData.scalar(Types.INTEGER, "INTEGER", ColumnMetaData.Rep.INTEGER);

    List<List<Object>> rowsValues = Arrays.asList(Arrays.asList(1, 2),
        Collections.singletonList(3), Arrays.asList(4, 5, 6));

    UtilTestCommon.assertResultSetFromArray(intType, rowsValues, validator, false);
  }

  @Test public void resultSetFromRealArray() throws Exception {
    BiFunction<Object, Object, Void> validator = (Object o1, Object o2) -> {
      assertEquals((float) o1, (float) o2, DELTA);
      return null;
    };

    ColumnMetaData.ScalarType realType =
        ColumnMetaData.scalar(Types.REAL, "REAL", ColumnMetaData.Rep.FLOAT);

    List<List<Object>> rowsValues = Arrays.asList(
        Arrays.asList(1.123f, 0.2f),
        Arrays.asList(4.1f, 5f, 66.12345f)
    );

    UtilTestCommon.assertResultSetFromArray(realType, rowsValues, validator, false);
  }

  @Test public void resultSetFromDoubleArray() throws Exception {
    BiFunction<Object, Object, Void> validator = (Object o1, Object o2) -> {
      assertEquals((double) o1, (double) o2, DELTA);
      return null;
    };

    ColumnMetaData.ScalarType doubleType =
        ColumnMetaData.scalar(Types.DOUBLE, "DOUBLE", ColumnMetaData.Rep.DOUBLE);

    List<List<Object>> rowsValues = Arrays.asList(
        Arrays.asList(1.123d, 0.123456789012d),
        Arrays.asList(4.134555d, 54444d, 66.12345d)
    );

    UtilTestCommon.assertResultSetFromArray(doubleType, rowsValues, validator, false);
  }

  @Test public void resultSetFromFloatArray() throws Exception {
    BiFunction<Object, Object, Void> validator = (Object o1, Object o2) -> {
      assertEquals((double) o1, (double) o2, DELTA);
      return null;
    };

    ColumnMetaData.ScalarType floatType =
        ColumnMetaData.scalar(Types.FLOAT, "DOUBLE", ColumnMetaData.Rep.FLOAT);

    List<List<Object>> rowsValues = Arrays.asList(
        Arrays.asList(1.123d, 0.123456789012d),
        Arrays.asList(4.134555d, 54444d, 66.12345d)
    );

    UtilTestCommon.assertResultSetFromArray(floatType, rowsValues, validator, false);
  }
}

// End AbstractCursorTest.java
