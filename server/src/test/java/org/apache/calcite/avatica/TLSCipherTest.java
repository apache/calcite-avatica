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
import org.apache.calcite.avatica.server.HttpServer.Builder;

import org.eclipse.jetty.util.ssl.SslContextFactory.Server;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertArrayEquals;

/**
 * Simple unit tests for testing that the protocol/cipher suite parameters are properly propagated
 * to Jetty.
 */
public class TLSCipherTest extends HttpBaseTest {

  public TLSCipherTest() {
    super("dummy");
  }

  @BeforeClass
  public static void setup() throws SQLException {
    setupClass();
  }

  @Test
  public void testTLSv11() {
    String[] protocolList = new String[] { "TLSv1.1" };

    Builder httpServerBuilder =
        new HttpServer.Builder()
            .withPort(0)
            .withTLS(KEYSTORE, KEYSTORE_PASSWORD, KEYSTORE, KEYSTORE_PASSWORD, null,
              protocolList, null)
            .withHandler(localService, Serialization.PROTOBUF);

    Server sslFactory = httpServerBuilder.buildSSLContextFactory();
    assertArrayEquals(protocolList, sslFactory.getIncludeProtocols());
  }

  @Test
  public void testTLSv1112() {
    String[] protocolList = new String[] { "TLSv1.1", "TLSv1.2" };

    Builder httpServerBuilder =
        new HttpServer.Builder()
            .withPort(0)
            .withTLS(KEYSTORE, KEYSTORE_PASSWORD, KEYSTORE, KEYSTORE_PASSWORD, null,
              protocolList, null)
            .withHandler(localService, Serialization.PROTOBUF);

    Server sslFactory = httpServerBuilder.buildSSLContextFactory();
    assertArrayEquals(protocolList, sslFactory.getIncludeProtocols());
  }

  @Test
  public void testSingleCipherSuite() {
    String[] cipherSuiteList = new String[] { "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384" };

    Builder httpServerBuilder =
        new HttpServer.Builder()
            .withPort(0)
            .withTLS(KEYSTORE, KEYSTORE_PASSWORD, KEYSTORE, KEYSTORE_PASSWORD, null,
              null, cipherSuiteList)
            .withHandler(localService, Serialization.PROTOBUF);

    Server sslFactory = httpServerBuilder.buildSSLContextFactory();
    assertArrayEquals(cipherSuiteList, sslFactory.getIncludeCipherSuites());
  }

  @Test
  public void testMultipleCipherSuites() {
    String[] cipherSuiteList =
        new String[] { "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256" };

    Builder httpServerBuilder =
        new HttpServer.Builder()
            .withPort(0)
            .withTLS(KEYSTORE, KEYSTORE_PASSWORD, KEYSTORE, KEYSTORE_PASSWORD, null,
              null, cipherSuiteList)
            .withHandler(localService, Serialization.PROTOBUF);

    Server sslFactory = httpServerBuilder.buildSSLContextFactory();
    assertArrayEquals(cipherSuiteList, sslFactory.getIncludeCipherSuites());
  }

  @Test
  public void testProtocolAndCipherSuites() {
    String[] protocolList = new String[] { "TLSv1.2" };
    String[] cipherSuiteList =
        new String[] { "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256" };

    Builder httpServerBuilder =
        new HttpServer.Builder()
            .withPort(0)
            .withTLS(KEYSTORE, KEYSTORE_PASSWORD, KEYSTORE, KEYSTORE_PASSWORD, null,
              protocolList, cipherSuiteList)
            .withHandler(localService, Serialization.PROTOBUF);

    Server sslFactory = httpServerBuilder.buildSSLContextFactory();
    assertArrayEquals(protocolList, sslFactory.getIncludeProtocols());
    assertArrayEquals(cipherSuiteList, sslFactory.getIncludeCipherSuites());
  }

}
