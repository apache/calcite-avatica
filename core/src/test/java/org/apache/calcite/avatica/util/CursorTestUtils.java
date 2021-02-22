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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilities methods for creating cursors from data for tests of the util package.
 */
public class CursorTestUtils {

  private CursorTestUtils() {
    // private constructor
  }

  static ListIteratorCursor createListBasedCursor(List<List<Object>> rowsValues) {
    Iterator<List<Object>> iterator = rowsValues.stream()
        .map(a -> (Object) a)
        .map(Collections::singletonList)
        .collect(Collectors.toList())
        .iterator();
    return new ListIteratorCursor(iterator);
  }

  static ArrayIteratorCursor createArrayBasedCursor(List<List<Object>> rowsValues) {
    Iterator<Object[]> iterator = Arrays.stream(rowsValues.toArray())
        .map(x -> Collections.singletonList(x).toArray())
        .iterator();
    return new ArrayIteratorCursor(iterator);
  }

  static ListIteratorCursor createArrayImplBasedCursor(
      List<List<Object>> rowsValues, ColumnMetaData.ScalarType arrayComponentType,
      ArrayImpl.Factory factory) {
    Iterator<List<Object>> iterator = rowsValues.stream()
        .map(vals -> factory.createArray(arrayComponentType, vals))
        .map(a -> (Object) a)
        .map(Collections::singletonList)
        .collect(Collectors.toList())
        .iterator();
    return new ListIteratorCursor(iterator);
  }
}

// End CursorTestUtils.java
