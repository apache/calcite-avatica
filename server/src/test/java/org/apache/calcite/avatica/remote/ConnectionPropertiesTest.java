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
package org.apache.calcite.avatica.remote;

import org.apache.calcite.avatica.AvaticaClientRuntimeException;
import org.apache.calcite.avatica.AvaticaConnection;
import org.apache.calcite.avatica.BuiltInConnectionProperty;
import org.apache.calcite.avatica.ConnectionSpec;
import org.apache.calcite.avatica.jdbc.JdbcMeta;
import org.apache.calcite.avatica.server.HttpServer;

import com.google.common.cache.Cache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Tests covering {@link ConnectionPropertiesTest}. */
@RunWith(Parameterized.class)
public class ConnectionPropertiesTest {
  private static final AvaticaServersForTest SERVERS = new AvaticaServersForTest();
  private static final Properties PROPERTIES = new Properties();


  private final HttpServer server;
  private final String url;
  private final int port;
  private final Driver.Serialization serialization;
  private final Properties clientReconnectProps = new Properties();

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> parameters() throws Exception {
    PROPERTIES.put(JdbcMeta.ConnectionCacheSettings.EXPIRY_DURATION.key(), "1");
    PROPERTIES.put(JdbcMeta.ConnectionCacheSettings.EXPIRY_UNIT.key(), TimeUnit.SECONDS.name());
    SERVERS.startServers(PROPERTIES);
    return SERVERS.getJUnitParameters();
  }

  public ConnectionPropertiesTest(Driver.Serialization serialization,
                                    HttpServer server) {
    this.server = server;
    this.port = this.server.getPort();
    this.serialization = serialization;
    this.url = SERVERS.getJdbcUrl(port, serialization);
    this.clientReconnectProps.setProperty(
        BuiltInConnectionProperty.TRANSPARENT_RECONNECTION.camelName(), "true");
  }

  @Test
  public void testConnectionPropertiesSync() throws Exception {
    ConnectionSpec.getDatabaseLock().lock();
    try {
      AvaticaConnection conn =
              (AvaticaConnection) DriverManager.getConnection(url, clientReconnectProps);
      conn.setAutoCommit(false);
      conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

      // sync connection properties
      conn.createStatement();
      Connection remoteConn = getConnection(
              AvaticaServersForTest.PropertyRemoteJdbcMetaFactory.getInstance(PROPERTIES), conn.id);

      assertFalse(remoteConn.getAutoCommit());
      assertEquals(remoteConn.getTransactionIsolation(),
              Connection.TRANSACTION_REPEATABLE_READ);

      // after 1s, remote connection expired and reopen
      Thread.sleep(10000);

      conn.createStatement();
      Connection remoteConn1 = getConnection(
              AvaticaServersForTest.PropertyRemoteJdbcMetaFactory.getInstance(PROPERTIES), conn.id);

      assertFalse(remoteConn1.getAutoCommit());
      assertEquals(remoteConn1.getTransactionIsolation(),
              Connection.TRANSACTION_REPEATABLE_READ);
    } finally {
      ConnectionSpec.getDatabaseLock().unlock();
    }
  }

  @Test
  public void testConnectionPropertiesSyncNoReconnect() throws Exception {
    ConnectionSpec.getDatabaseLock().lock();
    try {
      AvaticaConnection conn = (AvaticaConnection) DriverManager.getConnection(url);
      conn.setAutoCommit(false);
      conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

      // sync connection properties
      conn.createStatement();
      Connection remoteConn = getConnection(
              AvaticaServersForTest.PropertyRemoteJdbcMetaFactory.getInstance(PROPERTIES), conn.id);

      assertFalse(remoteConn.getAutoCommit());
      assertEquals(remoteConn.getTransactionIsolation(),
              Connection.TRANSACTION_REPEATABLE_READ);

      // after 1s, remote connection expired and reopen
      Thread.sleep(10000);

      try {
        conn.createStatement();
        fail("Should have thrown AvaticaClientRuntimeException");
      } catch (Exception e) {
        assertTrue(e instanceof AvaticaClientRuntimeException);
      }
    } finally {
      ConnectionSpec.getDatabaseLock().unlock();
    }
  }

  private static Connection getConnection(JdbcMeta m, String id) throws Exception {
    Field f = JdbcMeta.class.getDeclaredField("connectionCache");
    f.setAccessible(true);
    //noinspection unchecked
    Cache<String, Connection> connectionCache = (Cache<String, Connection>) f.get(m);
    return connectionCache.getIfPresent(id);
  }
}

// End ConnectionPropertiesTest.java
