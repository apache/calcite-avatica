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
import org.apache.calcite.avatica.util.Cursor.Accessor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

/**
 * Test class for StructImpl.
 */
@RunWith(Parameterized.class)
public class StructImplTest {

  /**
   * A class holding the necessary information for performing tests related with one table column.
   *
   * @param <T> the type of the expected values for the column
   */
  private static class ColumnInputBundle<T> {
    private final ColumnMetaData metaData;
    /**
     * The input values for the given column.
     *
     * These values are used to construct the rows of a ResultSet.
     */
    private final List<Object> inputValues;
    /**
     * The expected values for the given column.
     *
     * These values are used to verify that the result obtained from a ResultSet is correct. Note,
     * that inputValues and expectedValues are not necessarily equal.
     */
    private final List<T> expectedValues;

    ColumnInputBundle(List<Object> inputValues, List<T> expected, ColumnMetaData metaData) {
      this.inputValues = inputValues;
      this.expectedValues = expected;
      this.metaData = metaData;
    }

    @Override public String toString() {
      return "column=" + metaData.columnName + ", inputType=" + inputValues.get(0).getClass();
    }
  }

  private final ColumnInputBundle<Struct> columnInputBundle;

  public StructImplTest(ColumnInputBundle<Struct> columnInputBundle) {
    this.columnInputBundle = columnInputBundle;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<ColumnInputBundle<Struct>> data() throws SQLException {
    List<ColumnInputBundle<Struct>> data = new ArrayList<>();
    final int numRows = 5;

    ColumnMetaData oneAttrStructMeta = MetaImpl.columnMetaData(
        "ONE_ATTRIBUTE_STRUCT",
        0,
        ColumnMetaData.struct(
            Arrays.asList(
                MetaImpl.columnMetaData("INT", 0, int.class, false)
            )
        ),
        false);

    ColumnMetaData twoAttrStructMeta = MetaImpl.columnMetaData(
        "TWO_ATTRIBUTES_STRUCT",
        0,
        ColumnMetaData.struct(
            Arrays.asList(
                MetaImpl.columnMetaData("INT", 0, int.class, false),
                MetaImpl.columnMetaData("INT", 1, int.class, false)
            )
        ),
        false);

    List<Struct> oneAttrStructData = new ArrayList<>(numRows);
    List<Struct> twoAttrStructData = new ArrayList<>(numRows);
    for (int i = 0; i < numRows; i++) {
      oneAttrStructData.add(new StructImpl(Arrays.asList(i)));
      twoAttrStructData.add(new StructImpl(Arrays.asList(i, i)));
    }

    // A struct has various internal representations. The most common are the following:
    Class[] structTypes = new Class[]{Object[].class, Struct.class, List.class};
    // Generate column bundles for every possible representation that is supported currently.
    for (Class<?> type : structTypes) {
      data.add(newStructColBundle(oneAttrStructData, type, oneAttrStructMeta));
      data.add(newStructColBundle(twoAttrStructData, type, twoAttrStructMeta));
    }

    return data;
  }

  private static ColumnInputBundle newStructColBundle(
      List<Struct> data, Class<?> structType, ColumnMetaData meta) throws SQLException {
    List<Object> input = new ArrayList<>();
    List<Struct> expected = new ArrayList<>();
    for (Struct struct : data) {
      input.add(structOf(structType, struct));
      // The result obtained from StructAccessor is always of type Struct.class
      expected.add(structOf(Struct.class, struct));
    }
    return new ColumnInputBundle(input, expected, meta);
  }

  /**
   * Creates a struct in the representation dictated by the <code>structClass</code> parameter.
   */
  private static <T> T structOf(Class<T> structClass, Struct structData) throws SQLException {
    Object[] fieldValues = structData.getAttributes();
    if (structClass.equals(Object[].class)) {
      Object[] arrayStruct = new Object[fieldValues.length];
      for (int i = 0; i < fieldValues.length; i++) {
        arrayStruct[i] = fieldValues[i];
      }
      return (T) arrayStruct;
    } else if (structClass.equals(Struct.class)) {
      List<Object> listStruct = new ArrayList<>();
      for (int i = 0; i < fieldValues.length; i++) {
        listStruct.add(fieldValues[i]);
      }
      return (T) new StructImpl(listStruct);
    } else if (structClass.equals(List.class)) {
      List<Object> listStruct = new ArrayList<>();
      for (int i = 0; i < fieldValues.length; i++) {
        listStruct.add(fieldValues[i]);
      }
      return (T) listStruct;
    } else {
      throw new IllegalStateException("Cannot create struct of type" + structClass);
    }
  }

  @Test public void testStructAccessor() throws Exception {
    // Create rows based on the inputValues data
    List<List<Object>> rows = new ArrayList<>();
    for (Object o : columnInputBundle.inputValues) {
      rows.add(Collections.singletonList(o));
    }
    try (Cursor cursor = new ListIteratorCursor(rows.iterator())) {
      List<Accessor> accessors =
          cursor.createAccessors(
              Collections.singletonList(columnInputBundle.metaData), Unsafe.localCalendar(), null);
      Accessor accessor = accessors.get(0);
      int i = 0;
      while (cursor.next()) {
        Struct s = accessor.getObject(Struct.class);
        Object[] expectedStructAttributes = columnInputBundle.expectedValues.get(i).getAttributes();
        Object[] actualStructAttributes = s.getAttributes();
        assertArrayEquals(expectedStructAttributes, actualStructAttributes);
        i++;
      }
    }
  }
}

// End StructImplTest.java
