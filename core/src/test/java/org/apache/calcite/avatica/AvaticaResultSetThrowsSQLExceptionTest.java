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

import org.apache.calcite.avatica.remote.TypedValue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


/**
 * Test class for AvaticaResultSet, make sure we drop SQLException
 * for non supported function: previous and testUpdateNull, for example
 */
public class AvaticaResultSetThrowsSQLExceptionTest {

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
      return new TestMetaImpl(connection);
    }
  }

  /**
   * Fake meta implementation for test driver.
   */
  public static final class TestMetaImpl extends MetaImpl {
    public TestMetaImpl(AvaticaConnection connection) {
      super(connection);
    }

    @Override public StatementHandle prepare(ConnectionHandle ch, String sql, long maxRowCount) {
      throw new UnsupportedOperationException();
    }

    @SuppressWarnings("deprecation")
    @Override public ExecuteResult prepareAndExecute(StatementHandle h, String sql,
        long maxRowCount, PrepareCallback callback) throws NoSuchStatementException {
      throw new UnsupportedOperationException();
    }

    @Override public ExecuteResult prepareAndExecute(StatementHandle h, String sql,
        long maxRowCount, int maxRowsInFirstFrame, PrepareCallback callback)
        throws NoSuchStatementException {
      assertEquals("SELECT * FROM TABLE", sql);
      List<ColumnMetaData> columns = Arrays.asList(
          columnMetaData("bool", 0,
              ColumnMetaData.scalar(Types.BOOLEAN, "BOOLEAN",
                  ColumnMetaData.Rep.PRIMITIVE_BOOLEAN),
              DatabaseMetaData.columnNoNulls),
          columnMetaData("byte", 1,
              ColumnMetaData.scalar(Types.TINYINT, "TINYINT",
                  ColumnMetaData.Rep.PRIMITIVE_BYTE),
              DatabaseMetaData.columnNoNulls),
          columnMetaData("short", 2,
              ColumnMetaData.scalar(Types.SMALLINT, "SMALLINT",
                  ColumnMetaData.Rep.PRIMITIVE_SHORT),
              DatabaseMetaData.columnNoNulls),
          columnMetaData("int", 3,
              ColumnMetaData.scalar(Types.INTEGER, "INTEGER",
                  ColumnMetaData.Rep.PRIMITIVE_INT),
              DatabaseMetaData.columnNoNulls),
          columnMetaData("long", 4,
              ColumnMetaData.scalar(Types.BIGINT, "BIGINT",
                  ColumnMetaData.Rep.PRIMITIVE_LONG),
              DatabaseMetaData.columnNoNulls),
          columnMetaData("float", 5,
              ColumnMetaData.scalar(Types.REAL, "REAL",
                  ColumnMetaData.Rep.FLOAT),
              DatabaseMetaData.columnNoNulls),
          columnMetaData("double", 6,
              ColumnMetaData.scalar(Types.FLOAT, "FLOAT",
                  ColumnMetaData.Rep.DOUBLE),
              DatabaseMetaData.columnNoNulls),
          columnMetaData("string", 7,
              ColumnMetaData.scalar(Types.VARCHAR, "VARCHAR",
                  ColumnMetaData.Rep.STRING),
              DatabaseMetaData.columnNoNulls),
          columnMetaData("date", 8,
              ColumnMetaData.scalar(Types.DATE, "DATE",
                  ColumnMetaData.Rep.JAVA_SQL_DATE),
              DatabaseMetaData.columnNoNulls),
          columnMetaData("time", 9,
              ColumnMetaData.scalar(Types.TIME, "TIME",
                  ColumnMetaData.Rep.JAVA_SQL_TIME),
              DatabaseMetaData.columnNoNulls),
          columnMetaData("timestamp", 10,
              ColumnMetaData.scalar(Types.TIMESTAMP, "TIMESTAMP",
                  ColumnMetaData.Rep.JAVA_SQL_TIMESTAMP),
              DatabaseMetaData.columnNoNulls));

      List<Object> row = Collections.<Object>singletonList(
          new Object[] {
            true, (byte) 1, (short) 2, 3, 4L, 5.0f, 6.0d, "testvalue",
            new Date(1476130718123L), new Time(1476130718123L),
            new Timestamp(1476130718123L)
          });

      CursorFactory factory = CursorFactory.deduce(columns, null);
      Frame frame = new Frame(0, true, row);

      Signature signature = Signature.create(columns, sql,
          Collections.<AvaticaParameter>emptyList(), factory, StatementType.SELECT);
      try {
        synchronized (callback.getMonitor()) {
          callback.clear();
          callback.assign(signature, frame, -1);
        }
        callback.execute();
      } catch (SQLException e) {
        throw new RuntimeException();
      }
      MetaResultSet rs = MetaResultSet.create(h.connectionId, 0, false, signature, null);
      return new ExecuteResult(Collections.singletonList(rs));
    }

    @Override public ExecuteBatchResult prepareAndExecuteBatch(StatementHandle h,
        List<String> sqlCommands) throws NoSuchStatementException {
      throw new UnsupportedOperationException();
    }

    @Override public ExecuteBatchResult executeBatch(StatementHandle h,
        List<List<TypedValue>> parameterValues) throws NoSuchStatementException {
      throw new UnsupportedOperationException();
    }

    @Override public Frame fetch(StatementHandle h, long offset, int fetchMaxRowCount)
        throws NoSuchStatementException, MissingResultsException {
      throw new UnsupportedOperationException();
    }

    @SuppressWarnings("deprecation")
    @Override public ExecuteResult execute(StatementHandle h, List<TypedValue> parameterValues,
        long maxRowCount) throws NoSuchStatementException {
      throw new UnsupportedOperationException();
    }

    @Override public ExecuteResult execute(StatementHandle h, List<TypedValue> parameterValues,
        int maxRowsInFirstFrame) throws NoSuchStatementException {
      throw new UnsupportedOperationException();
    }

    @Override public void closeStatement(StatementHandle h) {
    }

    @Override public boolean syncResults(StatementHandle sh, QueryState state, long offset)
        throws NoSuchStatementException {
      throw new UnsupportedOperationException();
    }

    @Override public void commit(ConnectionHandle ch) {
      throw new UnsupportedOperationException();
    }

    @Override public void rollback(ConnectionHandle ch) {
      throw new UnsupportedOperationException();
    }
  }

  private static Connection connection = null;
  private static ResultSet resultSet = null;

  @Test
  public void testPrevious() throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("timeZone", "GMT");

    connection = new TestDriver().connect("jdbc:test", properties);
    resultSet = connection.createStatement().executeQuery("SELECT * FROM TABLE");
    thrown.expect(SQLException.class);
    resultSet.previous();
  }

  @Test
  public void testUpdateNull() throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("timeZone", "GMT");
    connection = new TestDriver().connect("jdbc:test", properties);

    resultSet = connection.createStatement().executeQuery("SELECT * FROM TABLE");
    thrown.expect(SQLException.class);
    resultSet.updateNull(1);
  }
}

// End AvaticaResultSetThrowsSQLExceptionTest.java
