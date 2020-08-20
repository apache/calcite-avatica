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
import org.apache.calcite.avatica.ColumnMetaData.ArrayType;
import org.apache.calcite.avatica.ColumnMetaData.Rep;
import org.apache.calcite.avatica.ColumnMetaData.ScalarType;
import org.apache.calcite.avatica.ColumnMetaData.StructType;
import org.apache.calcite.avatica.MetaImpl;
import org.apache.calcite.avatica.util.Cursor.Accessor;

import org.junit.Test;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.Struct;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for ArrayImpl.
 */
public class ArrayImplTest {

  @Test public void resultSetFromArray() throws Exception {
    // Define the struct type we're creating
    ScalarType intType = ColumnMetaData.scalar(Types.INTEGER, "INTEGER", Rep.INTEGER);
    ArrayType arrayType = ColumnMetaData.array(intType, "INTEGER", Rep.INTEGER);
    ColumnMetaData arrayMetaData = MetaImpl.columnMetaData("MY_ARRAY", 1, arrayType, false);
    ArrayImpl.Factory factory = new ArrayFactoryImpl(Unsafe.localCalendar().getTimeZone());
    // Create some arrays from the structs
    Array array1 = factory.createArray(intType, Arrays.<Object>asList(1, 2));
    Array array2 = factory.createArray(intType, Arrays.<Object>asList(3));
    Array array3 = factory.createArray(intType, Arrays.<Object>asList(4, 5, 6));
    List<List<Object>> rows = Arrays.asList(Collections.<Object>singletonList(array1),
        Collections.<Object>singletonList(array2), Collections.<Object>singletonList(array3));
    // Create two rows, each with one (array) column
    try (Cursor cursor = new ListIteratorCursor(rows.iterator())) {
      List<Accessor> accessors = cursor.createAccessors(Collections.singletonList(arrayMetaData),
          Unsafe.localCalendar(), factory);
      assertEquals(1, accessors.size());
      Accessor accessor = accessors.get(0);

      assertTrue(cursor.next());
      Array actualArray = accessor.getArray();
      // An Array's result set has one row per array element.
      // Each row has two columns. Column 1 is the array offset (1-based), Column 2 is the value.
      ResultSet actualArrayResultSet = actualArray.getResultSet();
      assertEquals(2, actualArrayResultSet.getMetaData().getColumnCount());
      assertTrue(actualArrayResultSet.next());
      // Order is Avatica implementation specific
      assertEquals(1, actualArrayResultSet.getInt(1));
      assertEquals(1, actualArrayResultSet.getInt(2));
      assertTrue(actualArrayResultSet.next());
      assertEquals(2, actualArrayResultSet.getInt(1));
      assertEquals(2, actualArrayResultSet.getInt(2));
      assertFalse(actualArrayResultSet.next());

      assertTrue(cursor.next());
      actualArray = accessor.getArray();
      actualArrayResultSet = actualArray.getResultSet();
      assertEquals(2, actualArrayResultSet.getMetaData().getColumnCount());
      assertTrue(actualArrayResultSet.next());
      assertEquals(1, actualArrayResultSet.getInt(1));
      assertEquals(3, actualArrayResultSet.getInt(2));
      assertFalse(actualArrayResultSet.next());

      assertTrue(cursor.next());
      actualArray = accessor.getArray();
      actualArrayResultSet = actualArray.getResultSet();
      assertEquals(2, actualArrayResultSet.getMetaData().getColumnCount());
      assertTrue(actualArrayResultSet.next());
      assertEquals(1, actualArrayResultSet.getInt(1));
      assertEquals(4, actualArrayResultSet.getInt(2));
      assertTrue(actualArrayResultSet.next());
      assertEquals(2, actualArrayResultSet.getInt(1));
      assertEquals(5, actualArrayResultSet.getInt(2));
      assertTrue(actualArrayResultSet.next());
      assertEquals(3, actualArrayResultSet.getInt(1));
      assertEquals(6, actualArrayResultSet.getInt(2));
      assertFalse(actualArrayResultSet.next());

      assertFalse(cursor.next());
    }
  }

  @Test public void arraysOfStructs() throws Exception {
    // Define the struct type we're creating
    ColumnMetaData intMetaData = MetaImpl.columnMetaData("MY_INT", 1, int.class, false);
    ColumnMetaData stringMetaData = MetaImpl.columnMetaData("MY_STRING", 2, String.class, true);
    StructType structType = ColumnMetaData.struct(Arrays.asList(intMetaData, stringMetaData));
    // Create some structs
    Struct struct1 = new StructImpl(Arrays.<Object>asList(1, "one"));
    Struct struct2 = new StructImpl(Arrays.<Object>asList(2, "two"));
    Struct struct3 = new StructImpl(Arrays.<Object>asList(3));
    Struct struct4 = new StructImpl(Arrays.<Object>asList(4, "four"));
    ArrayType arrayType = ColumnMetaData.array(structType, "OBJECT", Rep.STRUCT);
    ColumnMetaData arrayMetaData = MetaImpl.columnMetaData("MY_ARRAY", 1, arrayType, false);
    ArrayImpl.Factory factory = new ArrayFactoryImpl(Unsafe.localCalendar().getTimeZone());
    // Create some arrays from the structs
    Array array1 = factory.createArray(structType, Arrays.<Object>asList(struct1, struct2));
    Array array2 = factory.createArray(structType, Arrays.<Object>asList(struct3, struct4));
    List<List<Object>> rows = Arrays.asList(Collections.<Object>singletonList(array1),
        Collections.<Object>singletonList(array2));
    // Create two rows, each with one (array) column
    try (Cursor cursor = new ListIteratorCursor(rows.iterator())) {
      List<Accessor> accessors = cursor.createAccessors(Collections.singletonList(arrayMetaData),
          Unsafe.localCalendar(), factory);
      assertEquals(1, accessors.size());
      Accessor accessor = accessors.get(0);

      assertTrue(cursor.next());
      Array actualArray = accessor.getArray();
      // Avoiding explicit use of the getResultSet() method for now..
      Object[] arrayData = (Object[]) actualArray.getArray();
      assertEquals(2, arrayData.length);
      Struct actualStruct = (Struct) arrayData[0];
      Object[] o = actualStruct.getAttributes();
      assertEquals(2, o.length);
      assertEquals(1, o[0]);
      assertEquals("one", o[1]);

      actualStruct = (Struct) arrayData[1];
      o = actualStruct.getAttributes();
      assertEquals(2, o.length);
      assertEquals(2, o[0]);
      assertEquals("two", o[1]);

      assertTrue(cursor.next());
      actualArray = accessor.getArray();
      arrayData = (Object[]) actualArray.getArray();
      assertEquals(2, arrayData.length);
      actualStruct = (Struct) arrayData[0];
      o = actualStruct.getAttributes();
      assertEquals(1, o.length);
      assertEquals(3, o[0]);

      actualStruct = (Struct) arrayData[1];
      o = actualStruct.getAttributes();
      assertEquals(2, o.length);
      assertEquals(4, o[0]);
      assertEquals("four", o[1]);
    }
  }

  /**
   * The same test as arrayOfStructs(), except we use List instead of ArrayImpl.
   */
  @Test public void listOfStructs() throws Exception {
    ColumnMetaData intMetaData = MetaImpl.columnMetaData("MY_INT", 1, int.class, false);
    ColumnMetaData stringMetaData = MetaImpl.columnMetaData("MY_STRING", 2, String.class, true);
    StructType structType = ColumnMetaData.struct(Arrays.asList(intMetaData, stringMetaData));
    Struct struct1 = new StructImpl(Arrays.asList(1, "one"));
    Struct struct2 = new StructImpl(Arrays.asList(2, "two"));
    Struct struct3 = new StructImpl(Arrays.asList(3));
    Struct struct4 = new StructImpl(Arrays.asList(4, "four"));
    ArrayType arrayType = ColumnMetaData.array(structType, "OBJECT", Rep.STRUCT);
    ColumnMetaData arrayMetaData = MetaImpl.columnMetaData("MY_ARRAY", 1, arrayType, false);
    ArrayImpl.Factory factory = new ArrayFactoryImpl(Unsafe.localCalendar().getTimeZone());

    List<Object> list1 = Arrays.asList(struct1, struct2);
    List<Object> list2 = Arrays.asList(struct3, struct4);
    List<List<Object>> rows = Arrays.asList(Arrays.asList(list1), Arrays.asList(list2));

    try (Cursor cursor = new ListIteratorCursor(rows.iterator())) {
      List<Accessor> accessors = cursor.createAccessors(Arrays.asList(arrayMetaData),
          Unsafe.localCalendar(), factory);
      assertEquals(1, accessors.size());
      Accessor accessor = accessors.get(0);

      assertTrue(cursor.next());
      Array actualArray = accessor.getArray();

      Object[] arrayData = (Object[]) actualArray.getArray();
      assertEquals(2, arrayData.length);
      Struct actualStruct = (Struct) arrayData[0];
      Object[] o = actualStruct.getAttributes();
      assertEquals(2, o.length);
      assertEquals(1, o[0]);
      assertEquals("one", o[1]);

      actualStruct = (Struct) arrayData[1];
      o = actualStruct.getAttributes();
      assertEquals(2, o.length);
      assertEquals(2, o[0]);
      assertEquals("two", o[1]);

      assertTrue(cursor.next());
      actualArray = accessor.getArray();
      arrayData = (Object[]) actualArray.getArray();
      assertEquals(2, arrayData.length);
      actualStruct = (Struct) arrayData[0];
      o = actualStruct.getAttributes();
      assertEquals(1, o.length);
      assertEquals(3, o[0]);

      actualStruct = (Struct) arrayData[1];
      o = actualStruct.getAttributes();
      assertEquals(2, o.length);
      assertEquals(4, o[0]);
      assertEquals("four", o[1]);
    }
  }

  /**
   * Plain Java object class for the two tests that follow.
   */
  static class PlainJavaObject {
    public int intProperty;
    public String stringProperty;

    PlainJavaObject(int intProp, String stringProp) {
      intProperty = intProp;
      stringProperty = stringProp;
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

      PlainJavaObject pjo = (PlainJavaObject) o;

      return Objects.equals(stringProperty, pjo.stringProperty)
          && Objects.equals(intProperty, pjo.intProperty);
    }

    @Override public int hashCode() {
      return Objects.hash(stringProperty, intProperty);
    }
  }

  /**
   * Test case for when a column is an array of plain Java objects.
   * This is a common use case when data come from a dynamic schema source.
   */
  @Test public void arraysOfJavaObjects() throws Exception {
    final ColumnMetaData.Rep rep = ColumnMetaData.Rep.of(PlainJavaObject.class);
    ColumnMetaData.AvaticaType objectAvaticaType = ColumnMetaData.scalar(
        Types.OTHER,
        "OTHER",
        rep
    );

    String arrayTypeName = "JavaType(" + PlainJavaObject.class.toString() + ") ARRAY";
    ArrayType arrayType = ColumnMetaData.array(objectAvaticaType, arrayTypeName, rep);

    ColumnMetaData javaObjectArrayMetaData = MetaImpl.columnMetaData(
        "PLAIN_JAVA_OBJECT_ARRAY",
        1,
        arrayType,
        true
    );

    PlainJavaObject pjo1 = new PlainJavaObject(1, "one");
    PlainJavaObject pjo2 = new PlainJavaObject(2, "two");
    PlainJavaObject pjo3 = new PlainJavaObject(3, "three");
    PlainJavaObject pjo4 = new PlainJavaObject(4, "four");

    ArrayImpl.Factory factory = new ArrayFactoryImpl(Unsafe.localCalendar().getTimeZone());

    Array array1 = factory.createArray(objectAvaticaType, Arrays.asList(pjo1, pjo2));
    Array array2 = factory.createArray(objectAvaticaType, Arrays.asList(pjo3, pjo4));
    List<List<Object>> rows = Arrays.asList(Arrays.asList(array1), Arrays.asList(array2));

    try (Cursor cursor = new ListIteratorCursor(rows.iterator())) {
      List<Accessor> accessors = cursor.createAccessors(
          Arrays.asList(javaObjectArrayMetaData),
          Unsafe.localCalendar(),
          factory
      );
      assertEquals(1, accessors.size());
      Accessor accessor = accessors.get(0);

      assertTrue(cursor.next());
      Array actualArray = accessor.getArray();

      Object[] arrayData = (Object[]) actualArray.getArray();
      assertEquals(2, arrayData.length);
      assertEquals(pjo1, arrayData[0]);
      assertEquals(pjo2, arrayData[1]);

      assertTrue(cursor.next());
      actualArray = accessor.getArray();
      arrayData = (Object[]) actualArray.getArray();
      assertEquals(2, arrayData.length);
      assertEquals(pjo3, arrayData[0]);
      assertEquals(pjo4, arrayData[1]);
    }
  }

  /**
   * Test case for when a column is a list of plain Java objects.
   */
  @Test public void listOfJavaObjects() throws Exception {
    final ColumnMetaData.Rep rep = ColumnMetaData.Rep.of(PlainJavaObject.class);
    final ColumnMetaData.Rep rep2 = ColumnMetaData.Rep.of(PlainJavaObject.class);
    ColumnMetaData.AvaticaType objectAvaticaType = ColumnMetaData.scalar(
        Types.OTHER,
        "OTHER",
        rep
    );

    String arrayTypeName = "JavaType(" + PlainJavaObject.class.toString() + ") ARRAY";
    ArrayType arrayType = ColumnMetaData.array(objectAvaticaType, arrayTypeName, rep2);

    ColumnMetaData javaObjectArrayMetaData = MetaImpl.columnMetaData(
        "PLAIN_JAVA_OBJECT_ARRAY",
        1,
        arrayType,
        true
    );

    PlainJavaObject pjo1 = new PlainJavaObject(1, "one");
    PlainJavaObject pjo2 = new PlainJavaObject(2, "two");
    PlainJavaObject pjo3 = new PlainJavaObject(3, "three");
    PlainJavaObject pjo4 = new PlainJavaObject(4, "four");

    ArrayImpl.Factory factory = new ArrayFactoryImpl(Unsafe.localCalendar().getTimeZone());

    List<Object> list1 = Arrays.asList(pjo1, pjo2);
    List<Object> list2 = Arrays.asList(pjo3, pjo4);
    List<List<Object>> rows = Arrays.asList(Arrays.asList(list1), Arrays.asList(list2));

    try (Cursor cursor = new ListIteratorCursor(rows.iterator())) {
      List<Accessor> accessors = cursor.createAccessors(
          Arrays.asList(javaObjectArrayMetaData),
          Unsafe.localCalendar(),
          factory
      );
      assertEquals(1, accessors.size());
      Accessor accessor = accessors.get(0);

      assertTrue(cursor.next());
      Array actualArray = accessor.getArray();

      Object[] arrayData = (Object[]) actualArray.getArray();
      assertEquals(2, arrayData.length);
      assertEquals(pjo1, arrayData[0]);
      assertEquals(pjo2, arrayData[1]);

      assertTrue(cursor.next());
      actualArray = accessor.getArray();
      arrayData = (Object[]) actualArray.getArray();
      assertEquals(2, arrayData.length);
      assertEquals(pjo3, arrayData[0]);
      assertEquals(pjo4, arrayData[1]);
    }
  }

  @Test public void testArrayWithOffsets() throws Exception {
    // Define the struct type we're creating
    ScalarType intType = ColumnMetaData.scalar(Types.INTEGER, "INTEGER", Rep.INTEGER);
    ArrayImpl.Factory factory = new ArrayFactoryImpl(Unsafe.localCalendar().getTimeZone());
    // Create some arrays from the structs
    Array array1 = factory.createArray(intType, Arrays.<Object>asList(1, 2));
    Array array3 = factory.createArray(intType, Arrays.<Object>asList(4, 5, 6));

    Object[] data = (Object[]) array1.getArray(2, 1);
    assertEquals(1, data.length);
    assertEquals(2, data[0]);
    data = (Object[]) array3.getArray(1, 1);
    assertEquals(1, data.length);
    assertEquals(4, data[0]);
    data = (Object[]) array3.getArray(2, 2);
    assertEquals(2, data.length);
    assertEquals(5, data[0]);
    assertEquals(6, data[1]);
    data = (Object[]) array3.getArray(1, 3);
    assertEquals(3, data.length);
    assertEquals(4, data[0]);
    assertEquals(5, data[1]);
    assertEquals(6, data[2]);
  }
}

// End ArrayImplTest.java
