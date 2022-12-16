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

import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for
 * {@link MetaImpl#createIterable(Meta.StatementHandle, QueryState, Meta.Signature, List, Meta.Frame)} method.
 */
public class MetaImplCreateIterableTest {

  private static class MetaImplWithHardCodedResult extends MetaImpl {
    private final List<Object> result;
    /**
     * Number of times the fetch method is called.
     */
    int fetchCounter = 0;

    MetaImplWithHardCodedResult(AvaticaConnection connection, List<Object> result) {
      super(connection);
      this.result = result;
    }

    @Override public StatementHandle prepare(ConnectionHandle ch, String sql, long maxRowCount) {
      return null;
    }

    @Override public ExecuteResult prepareAndExecute(StatementHandle h, String sql,
        long maxRowCount,
        PrepareCallback callback) {
      return null;
    }

    @Override public ExecuteResult prepareAndExecute(StatementHandle h, String sql,
        long maxRowCount,
        int maxRowsInFirstFrame, PrepareCallback callback) {
      return null;
    }

    @Override public ExecuteBatchResult prepareAndExecuteBatch(StatementHandle h,
        List<String> sqlCommands) {
      return null;
    }

    @Override public ExecuteBatchResult executeBatch(StatementHandle h,
        List<List<TypedValue>> parameterValues) {
      return null;
    }

    @Override public Frame fetch(StatementHandle h, long offset, int fetchMaxRowCount) {
      fetchCounter++;
      int start = (int) offset;
      int end = start + fetchMaxRowCount;
      boolean done = false;
      if (end >= result.size()) {
        end = result.size();
        done = true;
      }
      List<Object> next = result.subList(start, end);
      return new Frame(offset, done, next);
    }

    @Override public ExecuteResult execute(StatementHandle h, List<TypedValue> parameterValues,
        long maxRowCount)
        throws NoSuchStatementException {
      return null;
    }

    @Override public ExecuteResult execute(StatementHandle h, List<TypedValue> parameterValues,
        int maxRowsInFirstFrame)
        throws NoSuchStatementException {
      return null;
    }

    @Override public void closeStatement(StatementHandle h) {

    }

    @Override public boolean syncResults(StatementHandle sh, QueryState state, long offset) {
      return false;
    }

    @Override public void commit(ConnectionHandle ch) {

    }

    @Override public void rollback(ConnectionHandle ch) {

    }
  }

  @Test public void testFullIterationReturnsCorrectCount() throws SQLException {
    MetaImplWithHardCodedResult metaImpl = new MetaImplWithHardCodedResult(mockConnection(100),
        IntStream.range(0, 550).boxed().collect(Collectors.toList()));
    int cnt = 0;
    for (Object o : metaImpl.createIterable(null, new QueryState(""), null, null, null)) {
      cnt++;
    }
    assertEquals(550, cnt);
  }

  @Test public void testFullIterationTriggersExpectedFetches() throws SQLException {
    MetaImplWithHardCodedResult metaImpl = new MetaImplWithHardCodedResult(mockConnection(50),
        IntStream.range(0, 550).boxed().collect(Collectors.toList()));
    for (Object o : metaImpl.createIterable(null, new QueryState(""), null, null, null)) {
      // Ignore
    }
    assertEquals(11, metaImpl.fetchCounter);
  }

  private static AvaticaConnection mockConnection(int fetchSize) throws SQLException {
    AvaticaConnection connection = mock(AvaticaConnection.class);
    AvaticaStatement stmt = mock(AvaticaStatement.class);
    when(connection.lookupStatement(any())).thenReturn(stmt);
    when(stmt.getFetchSize()).thenReturn(fetchSize);
    return connection;
  }
}
