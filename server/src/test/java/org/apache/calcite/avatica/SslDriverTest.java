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

import org.apache.calcite.avatica.remote.Driver;
import org.apache.calcite.avatica.server.HttpServer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 * Test case for Avatica with TLS connectors.
 */
@RunWith(Parameterized.class)
public class SslDriverTest extends HttpBaseTest {

  public SslDriverTest(String jdbcUrl) {
    super(jdbcUrl);
  }

  @Parameters public static List<Object[]> parameters() throws Exception {
    // Skip TLS testing on IBM Java due the combination of:
    // - Jetty 9.4.12+ ignores SSL_* ciphers due to security - eclipse/jetty.project#2807
    // - IBM uses SSL_* cipher names for ALL ciphers not following RFC cipher names
    //   See eclipse/jetty.project#2807 for details
    assumeFalse(
        "Skip TLS testing on IBM Java due eclipse/jetty.project#2807",
        System.getProperty("java.vendor").contains("IBM")
    );

    final ArrayList<Object[]> parameters = new ArrayList<>();
    setupClass();
    for (Driver.Serialization serialization : new Driver.Serialization[] {
        Driver.Serialization.JSON, Driver.Serialization.PROTOBUF}) {
      for (boolean emptyPassword : new boolean[] {true, false}) {
        File keyStore = emptyPassword ? EMPTY_PW_KEYSTORE : KEYSTORE;
        String password = emptyPassword ? KEYSTORE_EMPTY_PASSWORD : KEYSTORE_PASSWORD;
        // Build and start the server, using TLS
        HttpServer httpServer = new HttpServer.Builder()
            .withPort(0)
            .withTLS(keyStore, password, keyStore, password)
            .withHandler(localService, serialization)
            .build();
        httpServer.start();
        SERVERS_TO_STOP.add(httpServer);

        String url = "jdbc:avatica:remote:url=https://localhost:" + httpServer.getPort()
            + ";serialization=" + serialization + ";truststore=" + keyStore.getAbsolutePath();
        if (!emptyPassword) {
          url += ";truststore_password=" + password;
        }
        LOG.info("JDBC URL {}", url);

        parameters.add(new Object[] {url});
      }
    }

    return parameters;
  }

  @Test
  public void testReadWrite() throws Exception {
    final String tableName = "testReadWrite";
    try (Connection conn = DriverManager.getConnection(jdbcUrl);
        Statement stmt = conn.createStatement()) {
      assertFalse(stmt.execute("DROP TABLE IF EXISTS " + tableName));
      assertFalse(stmt.execute("CREATE TABLE " + tableName + "(pk integer)"));
      assertEquals(1, stmt.executeUpdate("INSERT INTO " + tableName + " VALUES(1)"));
      assertEquals(1, stmt.executeUpdate("INSERT INTO " + tableName + " VALUES(2)"));
      assertEquals(1, stmt.executeUpdate("INSERT INTO " + tableName + " VALUES(3)"));

      ResultSet results = stmt.executeQuery("SELECT count(1) FROM " + tableName);
      assertTrue(results.next());
      assertEquals(3, results.getInt(1));
    }
  }

}

// End SslDriverTest.java
