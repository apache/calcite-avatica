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
import org.apache.calcite.avatica.server.AvaticaJaasKrbUtil;
import org.apache.calcite.avatica.server.HttpServer;

import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.client.KrbConfig;
import org.apache.kerby.kerberos.kerb.client.KrbConfigKey;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * End to end test case for SPNEGO with Avatica.
 */
@RunWith(Parameterized.class)
public class AvaticaSpnegoTest extends HttpBaseTest {
  private static final Logger LOG = LoggerFactory.getLogger(AvaticaSpnegoTest.class);

  private static SimpleKdcServer kdc;
  private static KrbConfig clientConfig;
  private static File keytabDir;

  private static int kdcPort;
  private static File clientKeytab;
  private static File serverKeytab;

  private static boolean isKdcStarted = false;

  private static void setupKdc() throws Exception {
    if (isKdcStarted) {
      return;
    }
    if (System.getProperty("avatica.http.spnego.use_canonical_hostname") == null) {
      System.setProperty("avatica.http.spnego.use_canonical_hostname", "false");
    }
    kdc = new SimpleKdcServer();
    File target = SpnegoTestUtil.TARGET_DIR;
    assertTrue(target.exists());

    File kdcDir = new File(target, AvaticaSpnegoTest.class.getSimpleName());
    if (kdcDir.exists()) {
      SpnegoTestUtil.deleteRecursively(kdcDir);
    }
    kdcDir.mkdirs();
    kdc.setWorkDir(kdcDir);

    kdc.setKdcHost(SpnegoTestUtil.KDC_HOST);
    kdcPort = SpnegoTestUtil.getFreePort();
    kdc.setAllowTcp(true);
    kdc.setAllowUdp(false);
    kdc.setKdcTcpPort(kdcPort);

    LOG.info("Starting KDC server at {}:{}", SpnegoTestUtil.KDC_HOST, kdcPort);

    kdc.init();
    kdc.start();
    isKdcStarted = true;

    keytabDir = new File(target, AvaticaSpnegoTest.class.getSimpleName()
        + "_keytabs");
    if (keytabDir.exists()) {
      SpnegoTestUtil.deleteRecursively(keytabDir);
    }
    keytabDir.mkdirs();
    setupServerUser(keytabDir);

    clientConfig = new KrbConfig();
    clientConfig.setString(KrbConfigKey.KDC_HOST, SpnegoTestUtil.KDC_HOST);
    clientConfig.setInt(KrbConfigKey.KDC_TCP_PORT, kdcPort);
    clientConfig.setString(KrbConfigKey.DEFAULT_REALM, SpnegoTestUtil.REALM);

    // Kerby sets "java.security.krb5.conf" for us!
    System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
    //System.setProperty("sun.security.spnego.debug", "true");
    //System.setProperty("sun.security.krb5.debug", "true");
  }

  @AfterClass public static void stopKdc() throws KrbException {
    if (isKdcStarted) {
      LOG.info("Stopping KDC on {}", kdcPort);
      kdc.stop();
    }
  }

  private static void setupServerUser(File keytabDir) throws KrbException {
    // Create the client user
    String clientPrincipal = SpnegoTestUtil.CLIENT_PRINCIPAL.substring(0,
        SpnegoTestUtil.CLIENT_PRINCIPAL.indexOf('@'));
    clientKeytab = new File(keytabDir, clientPrincipal.replace('/', '_') + ".keytab");
    if (clientKeytab.exists()) {
      SpnegoTestUtil.deleteRecursively(clientKeytab);
    }
    LOG.info("Creating {} with keytab {}", clientPrincipal, clientKeytab);
    SpnegoTestUtil.setupUser(kdc, clientKeytab, clientPrincipal);

    // Create the server user
    String serverPrincipal = SpnegoTestUtil.SERVER_PRINCIPAL.substring(0,
        SpnegoTestUtil.SERVER_PRINCIPAL.indexOf('@'));
    serverKeytab = new File(keytabDir, serverPrincipal.replace('/', '_') + ".keytab");
    if (serverKeytab.exists()) {
      SpnegoTestUtil.deleteRecursively(serverKeytab);
    }
    LOG.info("Creating {} with keytab {}", SpnegoTestUtil.SERVER_PRINCIPAL, serverKeytab);
    SpnegoTestUtil.setupUser(kdc, serverKeytab, SpnegoTestUtil.SERVER_PRINCIPAL);
  }

  @Parameters public static List<Object[]> parameters() throws Exception {
    final ArrayList<Object[]> parameters = new ArrayList<>();

    setupClass();

    // Start the KDC
    setupKdc();

    for (boolean tls : new Boolean[] {false, true}) {
      for (Driver.Serialization serialization : new Driver.Serialization[] {
          Driver.Serialization.JSON, Driver.Serialization.PROTOBUF}) {
        if (tls && System.getProperty("java.vendor").contains("IBM")) {
          // Skip TLS testing on IBM Java due the combination of:
          // - Jetty 9.4.12+ ignores SSL_* ciphers due to security - eclipse/jetty.project#2807
          // - IBM uses SSL_* cipher names for ALL ciphers not following RFC cipher names
          //   See eclipse/jetty.project#2807 for details
          LOG.info("Skipping HTTPS test on IBM Java");
          parameters.add(new Object[] {null});
          continue;
        }

        // Build and start the server
        HttpServer.Builder httpServerBuilder = new HttpServer.Builder();
        if (tls) {
          httpServerBuilder = httpServerBuilder
              .withTLS(KEYSTORE, KEYSTORE_PASSWORD, KEYSTORE, KEYSTORE_PASSWORD);
        }
        HttpServer httpServer = httpServerBuilder
            .withPort(0)
            .withAutomaticLogin(serverKeytab)
            .withSpnego(SpnegoTestUtil.SERVER_PRINCIPAL, SpnegoTestUtil.REALM)
            .withHandler(localService, serialization)
            .build();
        httpServer.start();
        SERVERS_TO_STOP.add(httpServer);

        String url = "jdbc:avatica:remote:url=" + (tls ? "https://" : "http://")
            + SpnegoTestUtil.KDC_HOST + ":" + httpServer.getPort()
            + ";authentication=SPNEGO;serialization=" + serialization;
        if (tls) {
          url += ";truststore=" + KEYSTORE.getAbsolutePath()
              + ";truststore_password=" + KEYSTORE_PASSWORD;
        }
        LOG.info("JDBC URL {}", url);

        parameters.add(new Object[] {url});
      }
    }

    return parameters;
  }

  public AvaticaSpnegoTest(String jdbcUrl) {
    super(jdbcUrl);
  }

  @Test public void testAuthenticatedClient() throws Exception {
    ConnectionSpec.getDatabaseLock().lock();
    try {
      final String tableName = "allowed_clients";
      // Create the subject for the client
      final Subject clientSubject = AvaticaJaasKrbUtil.loginUsingKeytab(
          SpnegoTestUtil.CLIENT_PRINCIPAL, clientKeytab);

      // The name of the principal

      // Run this code, logged in as the subject (the client)
      Subject.doAs(clientSubject, new PrivilegedExceptionAction<Void>() {
        @Override public Void run() throws Exception {
          try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            try (Statement stmt = conn.createStatement()) {
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
          return null;
        }
      });
    } finally {
      ConnectionSpec.getDatabaseLock().unlock();
    }
  }

  @Test public void testAutomaticLogin() throws Exception {
    final String tableName = "automaticAllowedClients";
    // Avatica should log in for us with this info
    String url = jdbcUrl + ";principal=" + SpnegoTestUtil.CLIENT_PRINCIPAL + ";keytab="
        + clientKeytab;
    LOG.info("Updated JDBC url: {}", url);
    try (Connection conn = DriverManager.getConnection(url);
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

// End AvaticaSpnegoTest.java
