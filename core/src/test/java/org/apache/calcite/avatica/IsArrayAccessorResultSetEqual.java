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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Is the result set of an {@link org.apache.calcite.avatica.util.AbstractCursor.ArrayAccessor}
 * equal to the provided
 * elements.
 *
 * Two elements are considered equal if they satisfy the provided predicate.
 */
class IsArrayAccessorResultSetEqual extends BaseMatcher<Cursor.Accessor> {
  final List<Object> expectedElements;
  final BiPredicate<Object, Object> p;

  IsArrayAccessorResultSetEqual(List<Object> expected, BiPredicate<Object, Object> p) {
    this.expectedElements = expected;
    this.p = p;
  }

  @Override public boolean matches(Object item) {
    Cursor.Accessor accessor = (Cursor.Accessor) item;
    try {
      ResultSet rs = accessor.getArray().getResultSet();
      // Result set is not closed on purpose (attempt to close it throws exception)
      int size = 0;
      while (rs.next()) {
        // Array's result set has one row per array element.
        // Each row has two columns:
        // * column 1 is the array offset (1-based);
        // * column 2 is the array value.
        int index = rs.getInt(1);
        Object actual = rs.getObject(2);
        Object expected = expectedElements.get(index - 1);
        if (!p.test(actual, expected)) {
          return false;
        }
        size++;
      }
      return expectedElements.size() == size;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to extract value from accessor.", e);
    }
  }

  @Override public void describeTo(Description description) {
    description.appendText("ArrayAccessor result set should match " + expectedElements);
  }

  @Override public void describeMismatch(Object item, Description description) {
    Cursor.Accessor accessor = (Cursor.Accessor) item;
    try {
      ResultSet rs = accessor.getArray().getResultSet();
      List<Object> rsvalues = new ArrayList<>();
      while (rs.next()) {
        rsvalues.add(rs.getObject(2));
      }
      description.appendText("was ").appendValue(rsvalues);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create matcher description.", e);
    }
  }

}

// End IsArrayAccessorResultSetEqual.java
