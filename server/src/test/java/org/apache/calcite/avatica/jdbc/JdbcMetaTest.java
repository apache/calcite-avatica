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
package org.apache.calcite.avatica.jdbc;

import org.apache.calcite.avatica.AvaticaPreparedStatement;
import org.apache.calcite.avatica.Meta.ConnectionHandle;
import org.apache.calcite.avatica.Meta.Signature;
import org.apache.calcite.avatica.Meta.StatementHandle;

import com.google.common.cache.Cache;

import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link JdbcMeta}.
 */
public class JdbcMetaTest {

  @Test public void testExceptionPropagation() throws SQLException {
    JdbcMeta meta = new JdbcMeta("url");
    final Throwable e = new Exception();
    final RuntimeException rte;
    try {
      meta.propagate(e);
      fail("Expected an exception to be thrown");
    } catch (RuntimeException caughtException) {
      rte = caughtException;
      assertThat(rte.getCause(), is(e));
    }
  }

  @Test public void testPrepareSetsMaxRows() throws Exception {
    final String id = UUID.randomUUID().toString();
    final String sql = "SELECT * FROM FOO";
    final int maxRows = 500;
    final ConnectionHandle ch = new ConnectionHandle(id);
    final AtomicInteger statementIdGenerator = new AtomicInteger(0);

    JdbcMeta meta = Mockito.mock(JdbcMeta.class);
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement statement = Mockito.mock(PreparedStatement.class);
    ResultSetMetaData resultSetMetaData = Mockito.mock(ResultSetMetaData.class);
    ParameterMetaData parameterMetaData = Mockito.mock(ParameterMetaData.class);
    @SuppressWarnings("unchecked")
    Cache<Integer, StatementInfo> statementCache =
        (Cache<Integer, StatementInfo>) Mockito.mock(Cache.class);

    Mockito.when(meta.getStatementIdGenerator()).thenReturn(statementIdGenerator);
    Mockito.when(meta.getStatementCache()).thenReturn(statementCache);
    Mockito.when(meta.getConnection(id)).thenReturn(connection);
    Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
    Mockito.when(statement.isWrapperFor(AvaticaPreparedStatement.class)).thenReturn(false);
    Mockito.when(statement.getMetaData()).thenReturn(resultSetMetaData);
    Mockito.when(statement.getParameterMetaData()).thenReturn(parameterMetaData);
    // Call the real methods
    Mockito.doCallRealMethod().when(meta).setMaxRows(statement, maxRows);
    Mockito.doCallRealMethod().when(meta).prepare(ch, sql, maxRows);

    meta.prepare(ch, sql, maxRows);

    Mockito.verify(statement).setMaxRows(maxRows);
  }

  @Test public void testPrepareAndExecuteSetsMaxRows() throws Exception {
    final String id = UUID.randomUUID().toString();
    final int statementId = 12345;
    final String sql = "SELECT * FROM FOO";
    final int maxRows = 500;

    JdbcMeta meta = Mockito.mock(JdbcMeta.class);
    PreparedStatement statement = Mockito.mock(PreparedStatement.class);
    @SuppressWarnings("unchecked")
    Cache<Integer, StatementInfo> statementCache =
        (Cache<Integer, StatementInfo>) Mockito.mock(Cache.class);
    Signature signature = Mockito.mock(Signature.class);

    final StatementInfo statementInfo = new StatementInfo(statement);
    final StatementHandle statementHandle = new StatementHandle(id, statementId, signature);

    Mockito.when(meta.getStatementCache()).thenReturn(statementCache);
    Mockito.when(statementCache.getIfPresent(statementId)).thenReturn(statementInfo);
    Mockito.when(statement.getResultSet()).thenReturn(null);
    // The real methods
    Mockito.when(meta.prepareAndExecute(statementHandle, sql, maxRows, 50, null))
        .thenCallRealMethod();
    Mockito.doCallRealMethod().when(meta).setMaxRows(statement, maxRows);

    // Call our method
    meta.prepareAndExecute(statementHandle, sql, maxRows, 50, null);

    // Verify we called setMaxRows with the right value
    Mockito.verify(statement).setMaxRows(maxRows);
  }

  @Test public void testPrepareNoParams() throws Exception {
    final String id = UUID.randomUUID().toString();
    final int statementId = 12345;
    final int maxRows = 500;

    JdbcMeta meta = Mockito.mock(JdbcMeta.class);

    @SuppressWarnings("unchecked")
    Cache<Integer, StatementInfo> statementCache =
        (Cache<Integer, StatementInfo>) Mockito.mock(Cache.class);

    PreparedStatement statement = Mockito.mock(PreparedStatement.class);
    Signature signature = Mockito.mock(Signature.class);

    final StatementInfo statementInfo = new StatementInfo(statement);
    final StatementHandle statementHandle = new StatementHandle(id, statementId, signature);

    Mockito.when(meta.getStatementCache()).thenReturn(statementCache);
    Mockito.when(statementCache.getIfPresent(statementId)).thenReturn(statementInfo);
    Mockito.when(statement.getResultSet()).thenReturn(null);
    // The real methods
    Mockito.when(meta.execute(statementHandle, null, maxRows)).thenCallRealMethod();

    // Call our method
    meta.execute(statementHandle, null, maxRows);

    // Make sure we call execute and there is no exception
    Mockito.verify(statement).execute();
  }

  @Test public void testConcurrentConnectionOpening() throws Exception {
    final Map<String, String> properties = Collections.emptyMap();
    final Connection conn = Mockito.mock(Connection.class);
    // Override JdbcMeta to shim in a fake Connection object. Irrelevant for the test
    JdbcMeta meta = new JdbcMeta("jdbc:url") {
      @Override protected Connection createConnection(String url, Properties info) {
        return conn;
      }
    };

    ConnectionHandle ch1 = new ConnectionHandle("id1");
    meta.openConnection(ch1, properties);

    assertEquals(conn, meta.getConnectionCache().getIfPresent(ch1.id));
    try {
      meta.openConnection(ch1, properties);
      fail("Should not be allowed to open two connections with the same ID");
    } catch (RuntimeException e) {
      // pass
    }
    // Cached object should not change
    assertEquals(conn, meta.getConnectionCache().getIfPresent(ch1.id));
  }

  @Test public void testRacingConnectionOpens() throws Exception {
    final Map<String, String> properties = Collections.emptyMap();
    final Connection conn1 = Mockito.mock(Connection.class);
    final Connection conn2 = Mockito.mock(Connection.class);
    final ConnectionHandle ch1 = new ConnectionHandle("id1");
    // Override JdbcMeta to shim in a fake Connection object. Irrelevant for the test
    JdbcMeta meta = new JdbcMeta("jdbc:url") {
      @Override protected Connection createConnection(String url, Properties info) {
        // Hack to mimic the race condition of a connection being cached by another thread.
        getConnectionCache().put(ch1.id, conn1);
        // Return our "newly created" connectino
        return conn2;
      }
    };

    try {
      meta.openConnection(ch1, properties);
      fail("Should see an exception when the cache already contained our connection after"
          + " creating it");
    } catch (RuntimeException e) {
      // pass
    }
    // Our opened connection should get closed when this race condition happens
    Mockito.verify(conn2).close();
  }

  private static Set<String> setOf(String... names) {
    return new HashSet<>(java.util.Arrays.asList(names));
  }

  @Test public void testCheckClientPropertiesNoRulesConfigured() {
    // With both lists null, every client name is passed through.
    JdbcMeta.checkClientProperties(null, null,
        Collections.singletonMap("anything", "v"));
  }

  @Test public void testCheckClientPropertiesDenylistRejectsListedName() {
    final ForbiddenConnectionPropertyException e =
        assertThrows(ForbiddenConnectionPropertyException.class, () ->
            JdbcMeta.checkClientProperties(setOf("propA", "propB"), null,
                Collections.singletonMap("propA", "v")));
    assertThat(e.getPropertyName(), is("propA"));
    assertThat(e.getRule(), is(ForbiddenConnectionPropertyException.Rule.DENYLIST));
    assertThat(e.getMessage(), containsString("propA"));
    assertThat(e.getMessage(), containsString("denylist"));
    assertThat(e.getMessage(),
        containsString(JdbcMeta.CLIENT_PROPERTIES_DENYLIST_KEY));
  }

  @Test public void testCheckClientPropertiesDenylistPermitsUnlistedName() {
    // No exception when the name is not on the denylist.
    JdbcMeta.checkClientProperties(setOf("propA"), null,
        Collections.singletonMap("propOther", "v"));
  }

  @Test public void testCheckClientPropertiesAllowlistRejectsUnlistedName() {
    final ForbiddenConnectionPropertyException e =
        assertThrows(ForbiddenConnectionPropertyException.class, () ->
            JdbcMeta.checkClientProperties(null, setOf("propA", "propB"),
                Collections.singletonMap("propC", "v")));
    assertThat(e.getPropertyName(), is("propC"));
    assertThat(e.getRule(), is(ForbiddenConnectionPropertyException.Rule.ALLOWLIST));
    assertThat(e.getMessage(), containsString("propC"));
    assertThat(e.getMessage(), containsString("allowlist"));
    assertThat(e.getMessage(),
        containsString(JdbcMeta.CLIENT_PROPERTIES_ALLOWLIST_KEY));
  }

  @Test public void testCheckClientPropertiesAllowlistPermitsListedName() {
    JdbcMeta.checkClientProperties(null, setOf("propA", "propB"),
        Collections.singletonMap("propA", "v"));
  }

  @Test public void testCheckClientPropertiesDenylistAppliedBeforeAllowlistForCertainProperty() {
    // A name on both lists is rejected by the denylist, not the allowlist,
    // the denylist rule always wins because it is checked first.
    final ForbiddenConnectionPropertyException e =
        assertThrows(ForbiddenConnectionPropertyException.class, () ->
            JdbcMeta.checkClientProperties(setOf("propA"), setOf("propA", "propB"),
                Collections.singletonMap("propA", "v")));
    assertThat(e.getRule(), is(ForbiddenConnectionPropertyException.Rule.DENYLIST));
  }

  @Test public void testCheckClientPropertiesEmptyMapBypassesChecks() {
    // An empty client map trivially satisfies both lists; the check must
    // not fabricate a rejection when there is nothing to check.
    JdbcMeta.checkClientProperties(null, setOf("propA"), Collections.emptyMap());
    JdbcMeta.checkClientProperties(setOf("propA"), setOf("propA"),
        Collections.emptyMap());
  }

  @Test public void testCheckClientPropertiesNullMapBypassesChecks() {
    JdbcMeta.checkClientProperties(setOf("propA"), setOf("propA"), null);
  }

  @Test public void testParsePropertyNameListNullOrBlankIsUnconfigured() {
    // A null or all-blank value means the rule was not configured; the
    // parser must return null so the check treats it as absent, not as an
    // empty allowlist that would reject everything.
    assertThat(JdbcMeta.parsePropertyNameList(null), nullValue());
    assertThat(JdbcMeta.parsePropertyNameList(""), nullValue());
    assertThat(JdbcMeta.parsePropertyNameList("   "), nullValue());
    assertThat(JdbcMeta.parsePropertyNameList(" , , "), nullValue());
  }

  @Test public void testParsePropertyNameListTrimsAndDropsBlanks() {
    // Whitespace around entries is trimmed; blank entries are dropped so
    // that operators can format their config with padding.
    final Set<String> parsed =
        JdbcMeta.parsePropertyNameList(" , propA ,  ,propB ,");
    assertThat(parsed, is(setOf("propA", "propB")));
  }

  /** End-to-end wiring check: constructing a JdbcMeta reads the JVM system
   * property, so a rejected property short-circuits openConnection before
   * any backend Connection is created. */
  @Test public void testSystemPropertyRestrictsOpenConnection() throws SQLException {
    final String previous =
        System.setProperty(JdbcMeta.CLIENT_PROPERTIES_DENYLIST_KEY, "propX");
    try {
      final JdbcMeta meta = new JdbcMeta("jdbc:url") {
        @Override protected Connection createConnection(String url, Properties info) {
          throw new AssertionError("createConnection must not be reached "
              + "when a client property is rejected");
        }
      };
      final ForbiddenConnectionPropertyException e =
          assertThrows(ForbiddenConnectionPropertyException.class, () ->
              meta.openConnection(new ConnectionHandle("id-sysprop"),
                  Collections.singletonMap("propX", "v")));
      assertThat(e.getPropertyName(), is("propX"));
      assertThat(e.getRule(),
          is(ForbiddenConnectionPropertyException.Rule.DENYLIST));
    } finally {
      if (previous == null) {
        System.clearProperty(JdbcMeta.CLIENT_PROPERTIES_DENYLIST_KEY);
      } else {
        System.setProperty(JdbcMeta.CLIENT_PROPERTIES_DENYLIST_KEY, previous);
      }
    }
  }
}

// End JdbcMetaTest.java
