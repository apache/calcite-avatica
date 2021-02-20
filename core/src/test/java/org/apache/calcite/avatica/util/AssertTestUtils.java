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

import java.sql.Array;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Utilities for assertions in tests of the util package.
 */
public class AssertTestUtils {

  private AssertTestUtils() {
    // private constructor
  }

  /**
   * A simple interface to validate expected and actual values.
   */
  interface Validator {
    void validate(Object expected, Object actual);
  }

  static void assertRowsValuesMatchCursorContentViaArrayAccessor(
      List<List<Object>> rowsValues, ColumnMetaData.ScalarType arrayContentMetadata,
      Cursor cursorOverArray, ColumnMetaData arrayMetaData, ArrayImpl.Factory factory,
      Validator validator) throws Exception {
    List<Cursor.Accessor> accessors = cursorOverArray.createAccessors(
        Collections.singletonList(arrayMetaData), Unsafe.localCalendar(), factory);
    assertEquals(1, accessors.size());
    Cursor.Accessor accessor = accessors.get(0);

    for (List<Object> rowValue : rowsValues) {
      assertTrue(cursorOverArray.next());
      Array actualArray = accessor.getArray();
      // An Array's result set has one row per array element.
      // Each row has two columns. Column 1 is the array offset (1-based), Column 2 is the value.
      ResultSet actualArrayResultSet = actualArray.getResultSet();
      assertEquals(2, actualArrayResultSet.getMetaData().getColumnCount());
      assertEquals(arrayContentMetadata.id, actualArrayResultSet.getMetaData().getColumnType(2));
      assertTrue(actualArrayResultSet.next());

      for (int j = 0; j < rowValue.size(); ++j) {
        assertEquals(j + 1, actualArrayResultSet.getInt(1));
        // ResultSet.getObject() uses the column type internally, we can rely on that
        validator.validate(rowValue.get(j), actualArrayResultSet.getObject(2));
        assertEquals(j < rowValue.size() - 1, actualArrayResultSet.next());
      }
    }
    assertFalse(cursorOverArray.next());
  }
}

// End AssertTestUtils.java
