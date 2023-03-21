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
package org.apache.calcite.avatica.ha;

import org.apache.calcite.avatica.AvaticaConnection;
import org.apache.calcite.avatica.BuiltInConnectionProperty;
import org.apache.calcite.avatica.remote.AvaticaCommonsHttpClientImpl;
import org.apache.calcite.avatica.remote.AvaticaHttpClient;
import org.apache.calcite.avatica.remote.AvaticaServersForTest;
import org.apache.calcite.avatica.remote.Driver;
import org.apache.calcite.avatica.remote.RemoteProtobufService;
import org.apache.calcite.avatica.server.AvaticaProtobufHandler;
import org.apache.calcite.avatica.server.HttpServer;
import org.apache.calcite.avatica.server.Main;

import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.core5.util.Timeout;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.UnknownHostException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConnectionPropertiesHATest {
  private static final AvaticaServersForTest SERVERS = new AvaticaServersForTest();
  private static final String[] SERVER_ARGS = {
    AvaticaServersForTest.FullyRemoteJdbcMetaFactory.class.getName()
  };
  public static final int NO_OF_SERVERS = 5;
  public static final String HTTP_LOCALHOST = "http://localhost:";
  public static final String COMMA = ",";
  public static final String OS_NAME_LOWERCASE =
      System.getProperty("os.name").toLowerCase(Locale.ROOT);
  public static final String WINDOWS_OS_PREFIX = "windows";
  private static String lbURLs = "";
  private static final int START_PORT = 10000;
  private static String[] urls;

  @BeforeClass
  public static void beforeClass()
      throws ClassNotFoundException,
          InvocationTargetException,
          InstantiationException,
          IllegalAccessException,
          NoSuchMethodException {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < NO_OF_SERVERS; i++) {
      int port = START_PORT + i;
      Main.start(SERVER_ARGS, port, AvaticaProtobufHandler::new);
      sb.append(HTTP_LOCALHOST).append(port).append(COMMA);
    }
    lbURLs = sb.substring(0, sb.length() - 1);
    urls = lbURLs.split(COMMA);
  }

  @Test
  public void connectionPropertiesNoHATest() throws Exception {
    Properties properties = new Properties();
    String url = SERVERS.getJdbcUrl(10000, Driver.Serialization.PROTOBUF);
    AvaticaConnection conn1 = (AvaticaConnection) DriverManager.getConnection(url, properties);
    Assert.assertNotNull(conn1);
  }

  @Test
  public void connectionPropertiesHATestRandomSelectLB() {
    Properties properties = new Properties();
    properties.put(BuiltInConnectionProperty.USE_CLIENT_SIDE_LB.name(), "true");
    properties.put(BuiltInConnectionProperty.LB_URLS.name(), lbURLs);
    properties.put(
        BuiltInConnectionProperty.LB_STRATEGY.name(), RandomSelectLBStrategy.class.getName());

    String url = SERVERS.getJdbcUrl(START_PORT, Driver.Serialization.PROTOBUF);

    for (int i = 0; i < NO_OF_SERVERS; i++) {
      try {
        getConnectionURI((AvaticaConnection) DriverManager.getConnection(url, properties));
      } catch (Exception e) {
        Assert.fail(); // Verify that exception is not generated.
      }
    }
  }

  @Test
  public void connectionPropertiesHATestRoundRobinLB() throws Exception {

    resetRoundRobinLBStrategyState();

    Properties properties = new Properties();
    properties.put(BuiltInConnectionProperty.USE_CLIENT_SIDE_LB.name(), "true");
    properties.put(BuiltInConnectionProperty.LB_URLS.name(), lbURLs);
    properties.put(
        BuiltInConnectionProperty.LB_STRATEGY.name(), RoundRobinLBStrategy.class.getName());

    String url = SERVERS.getJdbcUrl(START_PORT, Driver.Serialization.PROTOBUF);

    String uri1 =
        getConnectionURI((AvaticaConnection) DriverManager.getConnection(url, properties));
    Assert.assertEquals(urls[0], uri1);

    String uri2 =
        getConnectionURI((AvaticaConnection) DriverManager.getConnection(url, properties));
    Assert.assertEquals(urls[1], uri2);

    String uri3 =
        getConnectionURI((AvaticaConnection) DriverManager.getConnection(url, properties));
    Assert.assertEquals(urls[2], uri3);
  }

  @Test
  public void connectionPropertiesHATestShuffledRoundRobinLB() throws Exception {
    resetShuffledRoundRobinLBStrategyState();

    Properties properties = new Properties();
    properties.put(BuiltInConnectionProperty.USE_CLIENT_SIDE_LB.name(), "true");
    properties.put(BuiltInConnectionProperty.LB_URLS.name(), lbURLs);
    properties.put(
        BuiltInConnectionProperty.LB_STRATEGY.name(), ShuffledRoundRobinLBStrategy.class.getName());

    String url = SERVERS.getJdbcUrl(START_PORT, Driver.Serialization.PROTOBUF);

    String firstConnectiondURL =
        getConnectionURI((AvaticaConnection) DriverManager.getConnection(url, properties));

    Assert.assertNotNull(firstConnectiondURL);

    for (int i = 0; i < NO_OF_SERVERS; i++) {
      try {
        getConnectionURI((AvaticaConnection) DriverManager.getConnection(url, properties));
      } catch (Exception e) {
        Assert.fail(); // In System test verify that exception is not generated.
      }
    }
  }

  @Test
  public void connectionPropertiesHATestInvalidLB() throws Exception {
    Properties properties = new Properties();
    properties.put(BuiltInConnectionProperty.USE_CLIENT_SIDE_LB.name(), "true");
    properties.put(BuiltInConnectionProperty.LB_URLS.name(), lbURLs);
    properties.put(BuiltInConnectionProperty.LB_STRATEGY.name(), "com.incorrect.badLb");
    String url = SERVERS.getJdbcUrl(START_PORT, Driver.Serialization.PROTOBUF);
    try {
      DriverManager.getConnection(url, properties);
    } catch (RuntimeException re) {
      Assert.assertTrue(re.getCause() instanceof ClassNotFoundException);
    }
  }

  @Test
  public void testConnectionPropertiesHATestLongURlList() throws Exception {
    resetRoundRobinLBStrategyState();
    Properties properties = new Properties();
    properties.put(BuiltInConnectionProperty.USE_CLIENT_SIDE_LB.name(), "true");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      sb.append("http://localhost:").append(START_PORT + i).append(",");
    }
    properties.put(BuiltInConnectionProperty.LB_URLS.name(), sb.substring(0, sb.length() - 1));
    properties.put(
        BuiltInConnectionProperty.LB_STRATEGY.name(), RoundRobinLBStrategy.class.getName());
    String url = SERVERS.getJdbcUrl(START_PORT, Driver.Serialization.PROTOBUF);

    AvaticaConnection conn = (AvaticaConnection) DriverManager.getConnection(url, properties);
    String uri1 = getConnectionURI(conn);
    Assert.assertEquals(urls[0], uri1);
  }

  @Test
  public void testConnectionPropertiesHATestInvalidLBUrl() throws Exception {
    resetRoundRobinLBStrategyState();
    Properties properties = new Properties();
    properties.put(BuiltInConnectionProperty.USE_CLIENT_SIDE_LB.name(), "true");
    properties.put(BuiltInConnectionProperty.LB_URLS.name(), "http://invalid:" + START_PORT);
    properties.put(
        BuiltInConnectionProperty.LB_STRATEGY.name(), RoundRobinLBStrategy.class.getName());
    String url = SERVERS.getJdbcUrl(START_PORT, Driver.Serialization.PROTOBUF);
    try {
      DriverManager.getConnection(url, properties);
    } catch (RuntimeException re) {
      Assert.assertTrue(re.getCause() instanceof UnknownHostException);
    }
  }

  @Test
  public void testConnectionPropertiesHALBFailover() throws Exception {
    resetRoundRobinLBStrategyState();

    Properties properties = new Properties();
    properties.put(BuiltInConnectionProperty.USE_CLIENT_SIDE_LB.name(), "true");
    properties.put(BuiltInConnectionProperty.LB_CONNECTION_FAILOVER_RETRIES.name(), "1");
    properties.put(BuiltInConnectionProperty.LB_CONNECTION_FAILOVER_SLEEP_TIME.name(), "100");
    properties.put(BuiltInConnectionProperty.HTTP_CONNECTION_TIMEOUT.name(), "300");
    properties.put(
        BuiltInConnectionProperty.LB_STRATEGY.name(), RoundRobinLBStrategy.class.getName());

    // Invalid URL at first position in  lb_urls
    StringBuilder sb = new StringBuilder("http://invalidurl:").append(START_PORT).append(",");

    // Put Valid URL at second position in lb_urls. This should be returned during failover.
    sb.append(urls[0]).append(",");
    properties.put(BuiltInConnectionProperty.LB_URLS.name(), sb.substring(0, sb.length() - 1));

    String url = SERVERS.getJdbcUrl(START_PORT, Driver.Serialization.PROTOBUF);
    AvaticaConnection connection = (AvaticaConnection) DriverManager.getConnection(url, properties);
    String uri = getConnectionURI(connection);
    Assert.assertEquals(urls[0], uri);
  }

  @Test
  public void testConnectionPropertiesHAHttpConnectionTimeout5Sec() throws Exception {
    // Skip the test for Windows.
    Assume.assumeFalse(OS_NAME_LOWERCASE.startsWith(WINDOWS_OS_PREFIX));
    Properties properties = new Properties();

    properties.put(BuiltInConnectionProperty.USE_CLIENT_SIDE_LB.name(), "true");
    properties.put(BuiltInConnectionProperty.HTTP_CONNECTION_TIMEOUT.name(), "5000");
    properties.put(BuiltInConnectionProperty.LB_CONNECTION_FAILOVER_RETRIES.name(), "0");
    // 240.0.0.1 is special URL which should result in connection timeout.
    properties.put(BuiltInConnectionProperty.LB_URLS.name(), "http://240.0.0.1:" + 9000);
    String url = SERVERS.getJdbcUrl(START_PORT, Driver.Serialization.PROTOBUF);
    long startTime = System.currentTimeMillis();
    try {
      DriverManager.getConnection(url, properties);
    } catch (RuntimeException re) {
      long endTime = System.currentTimeMillis();
      long elapsedTime = endTime - startTime;
      Assert.assertTrue(elapsedTime < Timeout.ofMinutes(3).toMilliseconds());
      Assert.assertTrue(elapsedTime >= 5000);
      Assert.assertTrue(re.getCause() instanceof ConnectTimeoutException);
    }
  }

  @Test
  public void testConnectionPropertiesCreateStatementAfterDisconnect() throws Exception {
    resetRoundRobinLBStrategyState();
    // Start a new server at port 100 port from the startport
    int test_server_port = START_PORT + 100;
    HttpServer avaticaServer =
        Main.start(SERVER_ARGS, test_server_port, AvaticaProtobufHandler::new);

    Properties properties = new Properties();
    properties.put(BuiltInConnectionProperty.USE_CLIENT_SIDE_LB.name(), "true");
    properties.put(BuiltInConnectionProperty.LB_CONNECTION_FAILOVER_RETRIES.name(), "2");
    properties.put(BuiltInConnectionProperty.LB_CONNECTION_FAILOVER_SLEEP_TIME.name(), "100");
    properties.put(BuiltInConnectionProperty.HTTP_CONNECTION_TIMEOUT.name(), "300");
    properties.put(
        BuiltInConnectionProperty.LB_STRATEGY.name(), RoundRobinLBStrategy.class.getName());
    StringBuilder sb = new StringBuilder();
    // First URL will be server we started in this test
    sb.append("http://localhost:").append(test_server_port).append(",");
    for (int i = 0; i < NO_OF_SERVERS; i++) {
      sb.append("http://localhost:").append(START_PORT + i).append(",");
    }
    properties.put(BuiltInConnectionProperty.LB_URLS.name(), sb.substring(0, sb.length() - 1));

    // Create a connection
    String url = SERVERS.getJdbcUrl(test_server_port, Driver.Serialization.PROTOBUF);
    AvaticaConnection conn = (AvaticaConnection) DriverManager.getConnection(url, properties);

    // Create statement
    Statement stmt = conn.createStatement();

    String tableName = "TEST_TABLE";
    // Execute some queries
    assertFalse(stmt.execute("DROP TABLE IF EXISTS " + tableName));
    assertFalse(stmt.execute("CREATE TABLE " + tableName + " (pk integer, msg varchar(10))"));
    assertEquals(1, stmt.executeUpdate("INSERT INTO " + tableName + " VALUES(1, 'abcd')"));

    ResultSet results = stmt.executeQuery("SELECT count(1) FROM " + tableName);
    assertNotNull(results);
    assertTrue(results.next());
    assertEquals(1, results.getInt(1));

    // Stop a server
    avaticaServer.stop();

    // Execute query on statement - It fails with SQL exception.
    try {
      stmt.execute("SELECT count(1) FROM " + tableName);
    } catch (Exception e) {
      assertTrue(e instanceof SQLException);
      assertTrue(
          e.getMessage().toLowerCase(Locale.ROOT).contains("connection refused")
              || e.getMessage().toLowerCase(Locale.ROOT).contains("connection abort"));
    }

    // Create statement with conn - Fails with HttpHostConnectException.
    try {
      Statement stmt2 = conn.createStatement();
      stmt2.execute("SELECT count(1) FROM " + tableName);
      fail("Should have thrown connection refused error.");
    } catch (Exception e) {
      assertTrue(e instanceof RuntimeException);
      assertNotNull(e.getCause());
      assertTrue(e.getCause() instanceof HttpHostConnectException);
      assertTrue(e.getMessage().contains("Connection refused"));
    }
  }

  @Test
  public void testShuffledRoundRobinLBStrategyThreadSafe() throws Exception {
    resetShuffledRoundRobinLBStrategyState();

    Properties properties = new Properties();
    properties.put(BuiltInConnectionProperty.USE_CLIENT_SIDE_LB.name(), "true");
    properties.put(
        BuiltInConnectionProperty.LB_STRATEGY.name(), ShuffledRoundRobinLBStrategy.class.getName());
    StringBuilder sb = new StringBuilder();
    // First URL will be server we started in this test
    for (int i = 0; i < NO_OF_SERVERS; i++) {
      sb.append("http://localhost:").append(START_PORT + i).append(",");
    }
    properties.put(BuiltInConnectionProperty.LB_URLS.name(), sb.substring(0, sb.length() - 1));

    // Create a connection
    String url = SERVERS.getJdbcUrl(START_PORT, Driver.Serialization.PROTOBUF);
    Callable<AvaticaConnection> callable =
        () -> (AvaticaConnection) DriverManager.getConnection(url, properties);
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    Future<AvaticaConnection> result1 = executorService.submit(callable);
    Future<AvaticaConnection> result2 = executorService.submit(callable);
    executorService.shutdown();

    AvaticaConnection connection1 = result1.get();
    AvaticaConnection connection2 = result2.get();
    assertNotNull(connection1);
    assertNotNull(connection2);

    // Verify that both threads got connections with different hosts
    assertNotEquals(getConnectionURI(connection1), getConnectionURI(connection2));
  }

  @Test
  public void testRoundRobinLBStrategyThreadSafe() throws Exception {
    resetRoundRobinLBStrategyState();
    Properties properties = new Properties();
    properties.put(BuiltInConnectionProperty.USE_CLIENT_SIDE_LB.name(), "true");
    properties.put(
        BuiltInConnectionProperty.LB_STRATEGY.name(), RoundRobinLBStrategy.class.getName());
    StringBuilder sb = new StringBuilder();
    // First URL will be server we started in this test
    for (int i = 0; i < NO_OF_SERVERS; i++) {
      sb.append("http://localhost:").append(START_PORT + i).append(",");
    }
    properties.put(BuiltInConnectionProperty.LB_URLS.name(), sb.substring(0, sb.length() - 1));

    // Create a connection
    String url = SERVERS.getJdbcUrl(START_PORT, Driver.Serialization.PROTOBUF);
    Callable<AvaticaConnection> callable =
        () -> (AvaticaConnection) DriverManager.getConnection(url, properties);
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    Future<AvaticaConnection> result1 = executorService.submit(callable);
    Future<AvaticaConnection> result2 = executorService.submit(callable);
    executorService.shutdown();

    AvaticaConnection connection1 = result1.get();
    AvaticaConnection connection2 = result2.get();
    assertNotNull(connection1);
    assertNotNull(connection2);

    // Verify URLs are not same when connections are created in different threads.
    String url1 = getConnectionURI(connection1);
    String url2 = getConnectionURI(connection2);

    //Verify that both threads got connections with different hosts
    assertNotEquals(url1, url2);

    //Verify that T1 picked-up URL0 and T2 picked-up URL1 or vice versa
    assertTrue(urls[0].equals(url1)  && urls[1].equals(url2)
        || urls[0].equals(url2) && urls[1].equals(url1));
  }

  private String getConnectionURI(AvaticaConnection conn)
      throws NoSuchFieldException, IllegalAccessException {
    Field fService = AvaticaConnection.class.getDeclaredField("service");
    fService.setAccessible(true);
    RemoteProtobufService service = (RemoteProtobufService) fService.get(conn);

    Field fClient = RemoteProtobufService.class.getDeclaredField("client");
    fClient.setAccessible(true);
    AvaticaHttpClient client = (AvaticaHttpClient) fClient.get(service);

    Field fUri = AvaticaCommonsHttpClientImpl.class.getDeclaredField("uri");
    fUri.setAccessible(true);
    URI uri = (URI) fUri.get(client);

    return uri.toString();
  }

  @SuppressWarnings("unchecked")
  private void resetRoundRobinLBStrategyState()
      throws NoSuchFieldException, IllegalAccessException {
    Field configToIndexServedMapField =
        RoundRobinLBStrategy.class.getDeclaredField("configToIndexServedMap");
    configToIndexServedMapField.setAccessible(true);
    Map<String, Integer> configToIndexServedMap =
        (Map<String, Integer>) configToIndexServedMapField.get(RoundRobinLBStrategy.INSTANCE);
    configToIndexServedMap.clear();

    Field configToUrlListMapField =
        RoundRobinLBStrategy.class.getDeclaredField("configToIndexServedMap");
    configToIndexServedMapField.setAccessible(true);
    Map<String, Integer> configToUrlListMap =
        (Map<String, Integer>) configToUrlListMapField.get(RoundRobinLBStrategy.INSTANCE);
    configToUrlListMap.clear();

  }
  @SuppressWarnings("unchecked")
  private void resetShuffledRoundRobinLBStrategyState()
      throws NoSuchFieldException, IllegalAccessException {
    Field configToIndexServedMapField =
        ShuffledRoundRobinLBStrategy.class.getDeclaredField("configToIndexServedMap");
    configToIndexServedMapField.setAccessible(true);
    Map<String, Integer> configToIndexServedMap =
        (Map<String, Integer>) configToIndexServedMapField
            .get(ShuffledRoundRobinLBStrategy.INSTANCE);
    configToIndexServedMap.clear();

    Field configToUrlListMapField =
        ShuffledRoundRobinLBStrategy.class.getDeclaredField("configToIndexServedMap");
    configToIndexServedMapField.setAccessible(true);
    Map<String, Integer> configToUrlListMap =
        (Map<String, Integer>) configToUrlListMapField
            .get(ShuffledRoundRobinLBStrategy.INSTANCE);
    configToUrlListMap.clear();
  }
}
