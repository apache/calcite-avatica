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
package org.apache.calcite.avatica.server;

import org.apache.calcite.avatica.SpnegoTestUtil;

import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"ResultOfMethodCallIgnored", "rawtypes"})
public class HttpServerVersionNotReturnedTest {
  private static final Logger LOG = LoggerFactory.getLogger(HttpServerVersionNotReturnedTest.class);

  private static SimpleKdcServer kdc;
  private static HttpServer httpServer;


  private static int kdcPort;

  private static File serverKeytab;

  private static boolean isKdcStarted = false;
  private static boolean isHttpServerStarted = false;

  private static URL httpServerUrl;

  @BeforeClass public static void setupKdc() throws Exception {
    kdc = new SimpleKdcServer();
    File target = SpnegoTestUtil.TARGET_DIR;
    assertTrue(target.exists());

    File kdcDir = new File(target, HttpServerVersionNotReturnedTest.class.getSimpleName());
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

    try (FileInputStream fis = new FileInputStream(new File(kdcDir, "krb5.conf"));
        InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
        BufferedReader r = new BufferedReader(isr)) {
      String line;
      while ((line = r.readLine()) != null) {
        LOG.debug("KRB5 Config line: {}", line);
      }
    }

    File keytabDir = new File(target, HttpServerVersionNotReturnedTest.class.getSimpleName()
        + "_keytabs");
    if (keytabDir.exists()) {
      SpnegoTestUtil.deleteRecursively(keytabDir);
    }
    keytabDir.mkdirs();
    setupUsers(keytabDir);


    // Kerby sets "java.security.krb5.conf" for us!
    System.clearProperty("java.security.auth.login.config");
    System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

    // Create and start an HTTP server configured only to allow SPNEGO requests
    // We use `withAutomaticLogin(File)` here which should invalidate the need to do JAAS config
    httpServer = new HttpServer.Builder()
        .withPort(0)
        .withAutomaticLogin(serverKeytab)
        .withSpnego(SpnegoTestUtil.SERVER_PRINCIPAL, SpnegoTestUtil.REALM)
        .withHandler(new SpnegoTestUtil.AuthenticationRequiredAvaticaHandler())
        .build();
    httpServer.start();
    isHttpServerStarted = true;

    httpServerUrl = new URL("http://" + SpnegoTestUtil.KDC_HOST + ":" + httpServer.getPort());
    LOG.info("HTTP server running at {}", httpServerUrl);
  }

  @AfterClass public static void stopKdc() throws Exception {
    if (isHttpServerStarted) {
      LOG.info("Stopping HTTP server at {}", httpServerUrl);
      httpServer.stop();
    }

    if (isKdcStarted) {
      LOG.info("Stopping KDC on {}", kdcPort);
      kdc.stop();
    }
  }

  private static void setupUsers(File keytabDir) throws KrbException {
    String serverPrincipal = SpnegoTestUtil.SERVER_PRINCIPAL.substring(0,
        SpnegoTestUtil.SERVER_PRINCIPAL.indexOf('@'));
    serverKeytab = new File(keytabDir, serverPrincipal.replace('/', '_') + ".keytab");
    if (serverKeytab.exists()) {
      SpnegoTestUtil.deleteRecursively(serverKeytab);
    }
    LOG.info("Creating {} with keytab {}", SpnegoTestUtil.SERVER_PRINCIPAL, serverKeytab);
    SpnegoTestUtil.setupUser(kdc, serverKeytab, SpnegoTestUtil.SERVER_PRINCIPAL);
  }

  @Test public void testServerVersionNotReturned() throws Exception {
    LOG.info("Connecting to {}", httpServerUrl.toString());
    HttpURLConnection conn = (HttpURLConnection) httpServerUrl.openConnection();
    conn.setRequestMethod("GET");
    assertEquals("Unauthorized response status code", 401,conn.getResponseCode());
    String field = conn.getHeaderField("server");
    assertNull("Server information was not expected", field);
  }
}
