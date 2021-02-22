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


import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for tests around column metadata.
 */
public class ColumnMetadataTestUtils {

  private static final Map<Integer, ColumnMetaData.ScalarType> TYPE_2_SCALARTYPE;
  private static final Map<Integer, ColumnMetaData.ScalarType> TYPE_2_PRIMITIVESCALARTYPE;

  static {
    Map<Integer, ColumnMetaData.ScalarType> builder = new HashMap<>();
    Map<Integer, ColumnMetaData.ScalarType> primitiveBuilder = new HashMap<>();

    builder.put(Types.BOOLEAN,
        ColumnMetaData.scalar(Types.BOOLEAN, "BOOLEAN",
            ColumnMetaData.Rep.BOOLEAN));
    primitiveBuilder.put(Types.BOOLEAN,
        ColumnMetaData.scalar(Types.BOOLEAN, "BOOLEAN",
            ColumnMetaData.Rep.PRIMITIVE_BOOLEAN));

    builder.put(Types.TINYINT,
        ColumnMetaData.scalar(Types.TINYINT, "TINYINT",
            ColumnMetaData.Rep.BYTE));
    primitiveBuilder.put(Types.TINYINT,
        ColumnMetaData.scalar(Types.TINYINT, "TINYINT",
        ColumnMetaData.Rep.PRIMITIVE_BYTE));

    builder.put(Types.SMALLINT,
        ColumnMetaData.scalar(Types.SMALLINT, "SMALLINT",
            ColumnMetaData.Rep.SHORT));
    primitiveBuilder.put(Types.SMALLINT,
        ColumnMetaData.scalar(Types.SMALLINT, "SMALLINT",
            ColumnMetaData.Rep.PRIMITIVE_SHORT));

    builder.put(Types.INTEGER,
        ColumnMetaData.scalar(Types.INTEGER, "INTEGER",
            ColumnMetaData.Rep.INTEGER));
    primitiveBuilder.put(Types.INTEGER,
        ColumnMetaData.scalar(Types.INTEGER, "INTEGER",
            ColumnMetaData.Rep.PRIMITIVE_INT));

    builder.put(Types.BIGINT,
        ColumnMetaData.scalar(Types.BIGINT, "BIGINT",
            ColumnMetaData.Rep.LONG));
    primitiveBuilder.put(Types.BIGINT,
        ColumnMetaData.scalar(Types.BIGINT, "BIGINT",
            ColumnMetaData.Rep.PRIMITIVE_LONG));

    builder.put(Types.REAL,
        ColumnMetaData.scalar(Types.REAL, "REAL",
            ColumnMetaData.Rep.FLOAT));
    primitiveBuilder.put(Types.REAL,
        ColumnMetaData.scalar(Types.REAL, "REAL",
            ColumnMetaData.Rep.PRIMITIVE_FLOAT));

    builder.put(Types.FLOAT,
        ColumnMetaData.scalar(Types.FLOAT, "FLOAT",
            ColumnMetaData.Rep.DOUBLE));
    primitiveBuilder.put(Types.FLOAT,
        ColumnMetaData.scalar(Types.FLOAT, "FLOAT",
            ColumnMetaData.Rep.PRIMITIVE_DOUBLE));

    builder.put(Types.DOUBLE,
        ColumnMetaData.scalar(Types.DOUBLE, "DOUBLE",
            ColumnMetaData.Rep.DOUBLE));
    primitiveBuilder.put(Types.DOUBLE,
        ColumnMetaData.scalar(Types.DOUBLE, "DOUBLE",
            ColumnMetaData.Rep.PRIMITIVE_DOUBLE));

    builder.put(Types.VARCHAR,
        ColumnMetaData.scalar(Types.VARCHAR, "VARCHAR",
            ColumnMetaData.Rep.STRING));

    builder.put(Types.DATE,
        ColumnMetaData.scalar(Types.DATE, "DATE",
            ColumnMetaData.Rep.JAVA_SQL_DATE));

    builder.put(Types.TIME,
        ColumnMetaData.scalar(Types.TIME, "TIME",
            ColumnMetaData.Rep.JAVA_SQL_TIME));

    builder.put(Types.TIMESTAMP,
        ColumnMetaData.scalar(Types.TIMESTAMP, "TIMESTAMP",
            ColumnMetaData.Rep.JAVA_SQL_TIMESTAMP));

    builder.put(Types.VARBINARY,
        ColumnMetaData.scalar(Types.VARBINARY, "VARBINARY",
            ColumnMetaData.Rep.BYTE_STRING));

    TYPE_2_SCALARTYPE = Collections.unmodifiableMap(builder);
    TYPE_2_PRIMITIVESCALARTYPE = Collections.unmodifiableMap(primitiveBuilder);
  }

  private ColumnMetadataTestUtils() {
    // private constructor
  }

  public static ColumnMetaData createArrayColumnMetaData(
      ColumnMetaData.ScalarType arrayComponentType) {
    ColumnMetaData.ArrayType arrayType =
        ColumnMetaData.array(arrayComponentType, arrayComponentType.name, arrayComponentType.rep);
    return MetaImpl.columnMetaData("MY_ARRAY", 1, arrayType, false);
  }

  public static ColumnMetaData.ScalarType getScalarTypeByTypeId(
      int typeId, boolean usePrimitiveType) {
    Map<Integer, ColumnMetaData.ScalarType> map =
        usePrimitiveType ? TYPE_2_PRIMITIVESCALARTYPE : TYPE_2_SCALARTYPE;

    if (!map.containsKey(typeId)) {
      throw new IllegalArgumentException("Unknown type id: " + typeId
          + (usePrimitiveType ? " (requested a primitive type)" : ""));
    }
    return map.get(typeId);
  }
}

// End ColumnMetadataTestUtils.java
