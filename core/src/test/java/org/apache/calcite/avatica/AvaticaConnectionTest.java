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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;

/**
 * Tests for AvaticaConnection
 */
public class AvaticaConnectionTest {
  @Test
  public void testIsValid() throws SQLException {
    AvaticaConnection connection = Mockito.mock(AvaticaConnection.class,
        Mockito.CALLS_REAL_METHODS);
    try {
      connection.isValid(-1);
      Assert.fail("Connection isValid should throw SQLException on negative timeout");
    } catch (SQLException expected) {
      Assert.assertEquals("timeout is less than 0", expected.getMessage());
    }

    Mockito.when(connection.isClosed()).thenReturn(false);
    Assert.assertTrue(connection.isValid(0));

    Mockito.when(connection.isClosed()).thenReturn(true);
    Assert.assertFalse(connection.isValid(0));
  }

  @Test
  public void testNumExecuteRetries() {
    AvaticaConnection connection = Mockito.mock(AvaticaConnection.class,
        Mockito.CALLS_REAL_METHODS);

    // Bad argument should throw an exception
    try {
      connection.getNumStatementRetries(null);
      Assert.fail("Calling getNumStatementRetries with a null object should throw an exception");
    } catch (NullPointerException e) {
      // Pass
    }

    Properties props = new Properties();

    // Verify the default value
    Assert.assertEquals(Long.parseLong(AvaticaConnection.NUM_EXECUTE_RETRIES_DEFAULT),
        connection.getNumStatementRetries(props));

    // Set a non-default value
    props.setProperty(AvaticaConnection.NUM_EXECUTE_RETRIES_KEY, "10");

    // Verify that we observe that value
    Assert.assertEquals(10, connection.getNumStatementRetries(props));
  }

  /** Test case for
   * <a href="https://issues.apache.org/jira/browse/CALCITE-6781">[CALCITE-6781]
   * The isUpdateCapable method of calcite.avatica will incorrectly traverse
   * the returned result value</a>.
   */
  @Test
  public void testIsUpdateCapableSkipsRowCountWhenResultSetHasNoRows() throws Exception {
    AvaticaConnection connection = Mockito.mock(
        AvaticaConnection.class, Mockito.CALLS_REAL_METHODS);
    AvaticaStatement statement = Mockito.mock(AvaticaStatement.class);
    AvaticaResultSet resultSet = Mockito.mock(AvaticaResultSet.class);

    Meta.Signature signature = new Meta.Signature(Collections.<ColumnMetaData>emptyList(), null,
        Collections.<AvaticaParameter>emptyList(), Collections.<String, Object>emptyMap(), null,
        Meta.StatementType.INSERT);

    Mockito.when(statement.getSignature()).thenReturn(signature);
    Mockito.when(resultSet.next()).thenReturn(false);
    statement.updateCount = -1;
    statement.openResultSet = resultSet;

    invokeIsUpdateCapable(connection, statement);

    Assert.assertEquals(-1, statement.updateCount);
    Assert.assertSame(resultSet, statement.openResultSet);
    Mockito.verify(resultSet, Mockito.never()).getObject(AvaticaConnection.ROWCOUNT_COLUMN_NAME);
  }

  private static void invokeIsUpdateCapable(
      AvaticaConnection connection, AvaticaStatement statement) throws Exception {
    Method method = AvaticaConnection.class
        .getDeclaredMethod("isUpdateCapable", AvaticaStatement.class);
    method.setAccessible(true);
    try {
      method.invoke(connection, statement);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof SQLException) {
        throw (SQLException) e.getCause();
      }
      throw e;
    }
  }

}

// End AvaticaConnectionTest.java
