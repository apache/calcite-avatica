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
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link SqlType}.
 */
public class SqlTypeTest {

  @Test public void ensureAnyIsGivenForNonStandardSqlTypes() {
    // Arrange
    // Oracle TIMESTAMP WITH TIME ZONE
    int timestampWithTimeZoneTypeValue = -101;
    // SQL server DateTimeOffset
    int dateTimeOffset = -155;

    // Act
    SqlType tsSqlType = SqlType.valueOf(timestampWithTimeZoneTypeValue);
    SqlType dtoSqlType = SqlType.valueOf(dateTimeOffset);

    // Assert
    assertEquals(SqlType.ANY, tsSqlType);
    assertEquals(SqlType.ANY, dtoSqlType);
  }

  @Test public void ensureStandardSqlTypesAreFound() {
    // Arrange
    Map<Integer, SqlType> typeMap = new HashMap<>();

    for (SqlType sqlType : SqlType.values()) {
      typeMap.put(sqlType.id, sqlType);
    }

    Set<Integer> typeValues = typeMap.keySet();

    // Act and Assert
    for (Integer typeValue : typeValues) {
      assertEquals(typeMap.get(typeValue), SqlType.valueOf(typeValue));
    }
  }
}
