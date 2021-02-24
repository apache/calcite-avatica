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

import org.junit.Test;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.calcite.avatica.CursorFactoryDeduceTest.DOUBLE_TYPE;
import static org.apache.calcite.avatica.CursorFactoryDeduceTest.INT_TYPE;
import static org.apache.calcite.avatica.CursorFactoryDeduceTest.ROWS;
import static org.apache.calcite.avatica.CursorFactoryDeduceTest.STRING_TYPE;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@code MetaImpl} relative to the {@code collect} method.
 */
public class MetaImplCollectTest {

  /**
   * Collecting from records where columns are the fields of a Java object.
   */
  @Test
  public void collectRecord() {
    List<ColumnMetaData> columnsMetaDataList = Arrays.asList(
        MetaImpl.columnMetaData("stringField", 2, STRING_TYPE, true),
        MetaImpl.columnMetaData("doubleField", 3, DOUBLE_TYPE, true),
        MetaImpl.columnMetaData("intField", 1, INT_TYPE, true));

    Meta.CursorFactory cursorFactory =
        Meta.CursorFactory.deduce(columnsMetaDataList, CursorFactoryDeduceTest.SimplePOJO.class);

    List<List<Object>> rows = new ArrayList<>();
    MetaImpl.collect(cursorFactory, ROWS, rows);

    for (int i = 0; i < ROWS.size(); i++) {
      CursorFactoryDeduceTest.SimplePOJO inputRow =
          (CursorFactoryDeduceTest.SimplePOJO) ROWS.get(i);
      List<Object> collectedRow = rows.get(i);
      assertEquals(inputRow.stringField, collectedRow.get(0));
      assertEquals(inputRow.doubleField, collectedRow.get(1));
      assertEquals(inputRow.intField, collectedRow.get(2));
    }
  }

  /**
   * Collecting from records where columns are (a subset of) the fields of a Java object.
   */
  @Test public void collectProjectedRecord() {
    List<ColumnMetaData> columnsMetaDataList = Arrays.asList(
        MetaImpl.columnMetaData("stringField", 2, STRING_TYPE, true),
        MetaImpl.columnMetaData("doubleField", 3, DOUBLE_TYPE, true));

    Meta.CursorFactory cursorFactory =
        Meta.CursorFactory.deduce(columnsMetaDataList, CursorFactoryDeduceTest.SimplePOJO.class);

    List<List<Object>> rows = new ArrayList<>();
    MetaImpl.collect(cursorFactory, ROWS, rows);

    for (int i = 0; i < ROWS.size(); i++) {
      CursorFactoryDeduceTest.SimplePOJO inputRow =
          (CursorFactoryDeduceTest.SimplePOJO) ROWS.get(i);
      List<Object> collectedRow = rows.get(i);
      assertEquals(inputRow.stringField, collectedRow.get(0));
      assertEquals(inputRow.doubleField, collectedRow.get(1));
    }
  }

  /**
   * Collect from a single column having a Java object as type.
   */
  @Test public void collectObject() {
    ColumnMetaData.Rep rep = ColumnMetaData.Rep.of(CursorFactoryDeduceTest.SimplePOJO.class);
    ColumnMetaData.AvaticaType pojoType =
        ColumnMetaData.scalar(Types.OTHER, "OTHER", rep);

    ColumnMetaData pojoMetadata =
        MetaImpl.columnMetaData("POJO", 1, pojoType, true);

    List<ColumnMetaData> columnMetaDataList = Collections.singletonList(pojoMetadata);

    Meta.CursorFactory cursorFactory =
        Meta.CursorFactory.deduce(columnMetaDataList, CursorFactoryDeduceTest.SimplePOJO.class);

    List<List<Object>> rows = new ArrayList<>();
    MetaImpl.collect(cursorFactory, ROWS, rows);

    for (int i = 0; i < ROWS.size(); i++) {
      assertEquals(ROWS.get(i), rows.get(i).get(0));
    }
  }
}

// End MetaImplCollectTest.java
