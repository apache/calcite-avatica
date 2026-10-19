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

import org.apache.calcite.avatica.remote.Driver.Serialization;
import org.apache.calcite.avatica.server.HttpServer;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import javax.net.ssl.SSLException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

/**
 * Test case for the {@code hostname_verification} connection property of the remote driver.
 */
public class SslHostnameVerificationTest extends HttpBaseTest {

  private static final File MISMATCHED_KEYSTORE =
      new File(TARGET_DIR, "avatica-test-mismatched-hostname.jks");

  private static String url;

  public SslHostnameVerificationTest() {
    super(url);
  }

  @BeforeClass
  public static void setup() throws Exception {
    // Skip TLS testing on IBM Java, see SslDriverTest
    assumeFalse(
        "Skip TLS testing on IBM Java due eclipse/jetty.project#2807",
        System.getProperty("java.vendor").contains("IBM")
    );

    setupClass();
    // The certificate is issued to a different hostname than the one we connect to
    createSelfSignedKeyStore(MISMATCHED_KEYSTORE, "not-localhost.test", KEYSTORE_PASSWORD);

    HttpServer httpServer = new HttpServer.Builder()
        .withPort(0)
        .withTLS(MISMATCHED_KEYSTORE, KEYSTORE_PASSWORD, MISMATCHED_KEYSTORE, KEYSTORE_PASSWORD)
        .withHandler(localService, Serialization.PROTOBUF)
        .build();
    httpServer.start();
    SERVERS_TO_STOP.add(httpServer);

    url = "jdbc:avatica:remote:url=https://localhost:" + httpServer.getPort()
        + ";serialization=PROTOBUF;truststore=" + MISMATCHED_KEYSTORE.getAbsolutePath()
        + ";truststore_password=" + KEYSTORE_PASSWORD;
    LOG.info("JDBC URL {}", url);
  }

  @Test
  public void testHostnameVerificationNone() throws Exception {
    String urlWithNone = jdbcUrl + ";hostname_verification=NONE";
    try (Connection conn = DriverManager.getConnection(urlWithNone);
        Statement stmt = conn.createStatement()) {
      // Just make sure that the https connection is established
      assertTrue(stmt.execute("SELECT * FROM EMP"));
    }
  }

  @Test
  public void testHostnameVerificationStrict() throws Exception {
    String urlWithStrict = jdbcUrl + ";hostname_verification=STRICT";
    try (Connection conn = DriverManager.getConnection(urlWithStrict);
        Statement stmt = conn.createStatement()) {
      stmt.execute("SELECT * FROM EMP");
      fail("Exception should have been thrown for the url: " + urlWithStrict);
    } catch (Exception e) {
      assertTrue("Expected an SSLException as cause of: " + e,
          e.getCause() instanceof SSLException);
    }
  }
}
