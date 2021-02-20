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

import java.sql.Array;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Common auxiliary methods for tests in util package.
 */
public class UtilTestCommon {

  private UtilTestCommon() {
    // private constructor
  }

  static void assertResultSetFromArray(ColumnMetaData.ScalarType arrayComponentType,
                                       List<List<Object>> rowsValues,
                                       BiFunction<Object, Object, Void> validator,
                                       boolean useArrayImpl) throws Exception {

    ColumnMetaData.ArrayType arrayType =
        ColumnMetaData.array(arrayComponentType, arrayComponentType.name, arrayComponentType.rep);
    ColumnMetaData arrayMetaData =
        MetaImpl.columnMetaData("MY_ARRAY", 1, arrayType, false);
    ArrayImpl.Factory factory = new ArrayFactoryImpl(Unsafe.localCalendar().getTimeZone());

    List<List<Object>> rows;

    if (useArrayImpl) {
      rows = rowsValues.stream()
          .map(vals -> factory.createArray(arrayComponentType, vals))
          .map(a -> (Object) a)
          .map(Collections::singletonList)
          .collect(Collectors.toList());
    } else {
      rows = rowsValues.stream()
          .map(a -> (Object) a)
          .map(Collections::singletonList)
          .collect(Collectors.toList());
    }

    // Create two rows, each with one (array) column
    try (Cursor cursor = new ListIteratorCursor(rows.iterator())) {
      List<Cursor.Accessor> accessors = cursor.createAccessors(
          Collections.singletonList(arrayMetaData), Unsafe.localCalendar(), factory);
      assertEquals(1, accessors.size());
      Cursor.Accessor accessor = accessors.get(0);

      // Order is Avatica implementation specific
      for (List<Object> rowValues : rowsValues) {
        assertTrue(cursor.next());
        Array actualArray = accessor.getArray();
        // An Array's result set has one row per array element.
        // Each row has two columns. Column 1 is the array offset (1-based), Column 2 is the value.
        ResultSet actualArrayResultSet = actualArray.getResultSet();
        assertEquals(2, actualArrayResultSet.getMetaData().getColumnCount());
        assertEquals(arrayComponentType.id, actualArrayResultSet.getMetaData().getColumnType(2));
        assertTrue(actualArrayResultSet.next());

        for (int j = 0; j < rowValues.size(); ++j) {
          assertEquals(j + 1, actualArrayResultSet.getInt(1));
          // ResultSet.getObject() uses the column type internally, we can rely on that
          validator.apply(rowValues.get(j), actualArrayResultSet.getObject(2));
          assertEquals(j < rowValues.size() - 1, actualArrayResultSet.next());
        }
      }
      assertFalse(cursor.next());
    }
  }
}

// End UtilTestCommon.java
