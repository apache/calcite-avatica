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

import org.apache.calcite.avatica.util.ArrayImpl;
import org.apache.calcite.avatica.util.Cursor;

import org.hamcrest.Matcher;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Custom Hamcrest matchers for the Avatica project.
 */
public class AvaticaMatchers {

  private static final double DELTA = 1e-15;

  private AvaticaMatchers() {
    // private constructor
  }

  /**
   * Creates a matcher that matches if the result set of the examined accessor
   * ({@link org.apache.calcite.avatica.util.AbstractCursor.ArrayAccessor}) is logically equal to
   * the specified values.
   *
   * Equality between individual elements is usually determined by {@link Object#equals(Object)} but
   * it can be more permissive when comparing approximate numbers (e.g., Float, Double) to account
   * for some margin of error.
   */
  public static Matcher<Cursor.Accessor> isArrayAccessorResult(List<Object> value, Class<?> type) {
    BiPredicate<Object, Object> comparisonPredicate = Object::equals;
    if (Float.class.equals(type)) {
      comparisonPredicate = (f1, f2) -> Math.abs((float) f1 - (float) f2) <= DELTA;
    } else if (Double.class.equals(type)) {
      comparisonPredicate = (d1, d2) -> Math.abs((double) d1 - (double) d2) <= DELTA;
    } else if (ArrayImpl.class.equals(type)) {
      comparisonPredicate = (a1, a2) -> a1.toString().equals(a2.toString());
    }
    return new IsArrayAccessorResultSetEqual(value, comparisonPredicate);
  }
}

// End AvaticaMatchers.java
