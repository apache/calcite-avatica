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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

/**
 * Test class for AvaticaResultSet, make sure we drop SQLException
 * for non supported function: previous and testUpdateNull, for example
 */
public class AvaticaResultSetThrowsSqlExceptionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  /**
   * A fake test driver for test.
   */
  private static final class TestDriver extends UnregisteredDriver {

    @Override protected DriverVersion createDriverVersion() {
      return new DriverVersion("test", "test 0.0.0", "test", "test 0.0.0", false, 0, 0, 0, 0);
    }

    @Override protected String getConnectStringPrefix() {
      return "jdbc:test";
    }

    @Override public Meta createMeta(AvaticaConnection connection) {
      return new AvaticaResultSetConversionsTest.TestMetaImpl(connection);
    }
  }

  /**
   * Auxiliary method returning a result set on a test table.
   * @return a result set on a test table.
   * @throws SQLException in case of database error
   */
  private ResultSet getResultSet() throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("timeZone", "GMT");

    final TestDriver driver = new TestDriver();
    final Connection connection = driver.connect("jdbc:test", properties);

    return connection.createStatement().executeQuery("SELECT * FROM TABLE");
  }

  @Test
  public void testPrevious() throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("timeZone", "GMT");

    final TestDriver driver = new TestDriver();
    try (Connection connection = driver.connect("jdbc:test", properties);
       ResultSet resultSet =
               connection.createStatement().executeQuery("SELECT * FROM TABLE")) {
      thrown.expect(SQLFeatureNotSupportedException.class);
      resultSet.previous();
    }
  }

  @Test
  public void testUpdateNull() throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("timeZone", "GMT");

    final TestDriver driver = new TestDriver();
    try (Connection connection = driver.connect("jdbc:test", properties);
         ResultSet resultSet =
                 connection.createStatement().executeQuery("SELECT * FROM TABLE")) {
      thrown.expect(SQLFeatureNotSupportedException.class);
      resultSet.updateNull(1);
    }
  }

  @Test
  public void testCommonCursorStates() throws SQLException {
    final ResultSet resultSet = getResultSet();

    // right after statement execution, result set is before first row
    assert resultSet.isBeforeFirst();

    // retrieve each row until the last one
    while (!resultSet.isAfterLast()) {
      assert resultSet.next() != resultSet.isAfterLast();
    }

    // result set is not closed yet, despite fully consumed
    assert !resultSet.isClosed();

    resultSet.close();

    // result set is now closed
    assert resultSet.isClosed();

    // once closed, next should fail
    thrown.expect(SQLException.class);
    resultSet.next();
  }

  /**
   * Auxiliary method for testing column access.
   * @param resultSet the result set
   * @param index the index of the column to be accessed
   * @param shouldThrow true iff the column access should throw an exception
   * @return true iff the method invocation succeeded
   * @throws SQLException in case of database error
   */
  private boolean getColumn(final ResultSet resultSet,
                            final int index,
                            final boolean shouldThrow) throws SQLException {
    try {
      switch (index) {
      case 1:
        resultSet.getBoolean(index);   // BOOLEAN
        break;
      case 2:
        resultSet.getByte(index);      // TINYINT
        break;
      case 3:
        resultSet.getShort(index);     // SMALLINT
        break;
      case 4:
        resultSet.getInt(index);       // INTEGER
        break;
      case 5:
        resultSet.getLong(index);      // BIGINT
        break;
      case 6:
        resultSet.getFloat(index);     // REAL
        break;
      case 7:
        resultSet.getDouble(index);    // FLOAT
        break;
      case 8:
        resultSet.getString(index);    // VARCHAR
        break;
      case 9:
        resultSet.getDate(index);      // DATE
        break;
      case 10:
        resultSet.getTime(index);      // TIME
        break;
      case 11:
        resultSet.getTimestamp(index); // TIMESTAMP
        break;
      default:
        resultSet.getObject(index);
      }
    } catch (SQLException e) {
      if (!shouldThrow) {
        throw e;
      }
      return true;
    }

    return !shouldThrow;
  }

  @Test
  public void testGetColumnsBeforeNext() throws SQLException {
    try (ResultSet resultSet = getResultSet()) {
      // we have not called next, so each column getter should throw SQLException
      for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
        //System.out.println(resultSet.getMetaData().getColumnTypeName(i));
        assert getColumn(resultSet, i, true);
      }
    }
  }

  @Test
  public void testGetColumnsAfterNext() throws SQLException {
    try (ResultSet resultSet = getResultSet()) {
      // result set is composed by 1 row, we call next before accessing columns
      resultSet.next();

      // after calling next, column getters should succeed
      for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); ++i) {
        assert getColumn(resultSet, i, false);
      }
    }
  }
}
// End AvaticaResultSetThrowsSqlExceptionTest.java
