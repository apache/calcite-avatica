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

import org.apache.calcite.avatica.ConnectionSpec;
import org.apache.calcite.avatica.jdbc.JdbcMeta;
import org.apache.calcite.avatica.remote.Driver;
import org.apache.calcite.avatica.remote.LocalService;
import org.apache.calcite.avatica.util.Sources;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test class for HTTP Digest authentication.
 */
public class DigestAuthHttpServerTest extends HttpAuthBase {

  private static final ConnectionSpec CONNECTION_SPEC = ConnectionSpec.HSQLDB;
  private static HttpServer server;
  private static String url;

  @BeforeClass public static void startServer() throws Exception {
    final String userPropertiesFile = Sources.of(DigestAuthHttpServerTest.class
        .getResource("/auth-users.properties")).file().getAbsolutePath();
    assertNotNull("Could not find properties file for digest auth users", userPropertiesFile);

    // Create a LocalService around HSQLDB
    final JdbcMeta jdbcMeta = new JdbcMeta(CONNECTION_SPEC.url,
        CONNECTION_SPEC.username, CONNECTION_SPEC.password);
    LocalService service = new LocalService(jdbcMeta);

    server = new HttpServer.Builder()
        .withDigestAuthentication(userPropertiesFile, new String[] { "users" })
        .withHandler(service, Driver.Serialization.PROTOBUF)
        .withPort(0)
        .build();
    server.start();

    url = "jdbc:avatica:remote:url=http://localhost:" + server.getPort()
        + ";authentication=DIGEST;serialization=PROTOBUF";

    // Create and grant permissions to our users
    createHsqldbUsers();
  }

  @AfterClass public static void stopServer() throws Exception {
    if (null != server) {
      server.stop();
    }
  }

  @Test public void testValidUser() throws Exception {
    // Valid both with avatica and hsqldb
    final Properties props = new Properties();
    props.put("avatica_user", "USER2");
    props.put("avatica_password", "password2");
    props.put("user", "USER2");
    props.put("password", "password2");

    readWriteData(url, "VALID_USER", props);
  }

  @Test public void testInvalidAvaticaValidDb() throws Exception {
    // Valid both with avatica and hsqldb
    final Properties props = new Properties();
    props.put("avatica_user", "USER2");
    props.put("avatica_password", "foobar");
    props.put("user", "USER2");
    props.put("password", "password2");

    try {
      readWriteData(url, "INVALID_AVATICA_VALID_DB", props);
      fail("Expected a failure");
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), containsString("HTTP/401"));
    }
  }

  @Test public void testValidAvaticaNoDb() throws Exception {
    // Valid both with avatica and hsqldb
    final Properties props = new Properties();
    props.put("avatica_user", "USER2");
    props.put("avatica_password", "password2");

    readWriteData(url, "VALID_AVATICA_NO_DB", props);
  }

  @Test public void testInvalidAvaticaNoDb() throws Exception {
    // Valid both with avatica and hsqldb
    final Properties props = new Properties();
    props.put("avatica_user", "USER2");
    props.put("avatica_password", "foobar");

    try {
      readWriteData(url, "INVALID_AVATICA_NO_DB", props);
      fail("Expected a failure");
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), containsString("HTTP/401"));
    }
  }

  @Test public void testInvalidUser() throws Exception {
    // Invalid avatica user
    final Properties props = new Properties();
    props.put("avatica_user", "foo");
    props.put("avatica_password", "bar");

    try {
      readWriteData(url, "INVALID_USER", props);
      fail("Expected a failure");
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), containsString("HTTP/401"));
    }
  }

  @Test public void testUserWithDisallowedRole() throws Exception {
    // User 4 is disallowed in avatica due to its roles
    final Properties props = new Properties();
    props.put("avatica_user", "USER4");
    props.put("avatica_password", "password4");

    try {
      readWriteData(url, "DISALLOWED_USER", props);
      fail("Expected a failure");
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), containsString("HTTP/403"));
    }
  }

  @Test public void testAllowedAvaticaDisabledHsqldbUser() throws Exception {
    // Valid Avatica user, but an invalid database user
    final Properties props = new Properties();
    props.put("avatica_user", "USER1");
    props.put("avatica_password", "password1");
    props.put("user", "USER1");
    props.put("password", "password1");

    try {
      readWriteData(url, "DISALLOWED_HSQLDB_USER", props);
      fail("Expected a failure");
    } catch (RuntimeException e) {
      assertEquals("Remote driver error: RuntimeException: "
          + "java.sql.SQLInvalidAuthorizationSpecException: invalid authorization specification"
          + " - not found: USER1"
          + " -> SQLInvalidAuthorizationSpecException: invalid authorization specification - "
          + "not found: USER1"
          + " -> HsqlException: invalid authorization specification - not found: USER1",
          e.getMessage());
    }
  }
  @Test
  public void testServerVersionNotReturnedForUnauthorisedAccess() throws Exception {
    URL httpServerUrl = new URI("http://localhost:" + server.getPort()).toURL();
    HttpURLConnection conn = (HttpURLConnection) httpServerUrl.openConnection();
    conn.setRequestMethod("GET");
    assertEquals("Unauthorized response status code", 401, conn.getResponseCode());
    assertNull("Server information was not expected", conn.getHeaderField("server"));
  }
}

// End DigestAuthHttpServerTest.java
