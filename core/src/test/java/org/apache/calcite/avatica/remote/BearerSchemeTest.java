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

import org.apache.calcite.avatica.BuiltInConnectionProperty;
import org.apache.calcite.avatica.ConnectionConfig;
import org.apache.calcite.avatica.ConnectionConfigImpl;

import org.apache.hc.client5.http.auth.AuthChallenge;
import org.apache.hc.client5.http.auth.AuthScheme;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.ChallengeType;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.impl.auth.CredentialsProviderBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.Assert.*;


/**
 * Bearer authentication test cases.
 * This file has been copied from the Apache HttpComponents Client project
 * https://github.com/apache/httpcomponents-client/blob/master/
 * httpclient5/src/test/java/org/apache/hc/client5/http/impl/auth/TestBearerScheme.java
 */
public class BearerSchemeTest {
  File tokensFile;
  ConnectionConfig conf;
  @Before
  public void setup() throws IOException {
    tokensFile = File.createTempFile("bearertoken_", ".txt");

    try (Writer fileWriter = new OutputStreamWriter(
        new FileOutputStream(tokensFile), StandardCharsets.UTF_8)) {
      fileWriter.write("testUser,token1\n");
    }

    Properties props = new Properties();
    props.put(BuiltInConnectionProperty.TOKEN_FILE.camelName(), tokensFile.getAbsolutePath());
    conf = new ConnectionConfigImpl(props);
  }

  @After
  public void teardown() {
    tokensFile.delete();
  }

  @Test
  public void testBearerAuthenticationEmptyChallenge() throws Exception {
    final AuthChallenge authChallenge = new AuthChallenge(ChallengeType.TARGET, "BEARER");
    final AuthScheme authscheme = new BearerScheme();
    authscheme.processChallenge(authChallenge, null);
    assertNull(authscheme.getRealm());
  }

  @Test
  public void testBearerAuthentication() throws Exception {
    final AuthChallenge authChallenge = new AuthChallenge(ChallengeType.TARGET, "Bearer",
        new BasicNameValuePair("realm", "test"));

    final AuthScheme authscheme = new BearerScheme();
    authscheme.processChallenge(authChallenge, null);

    final HttpHost host  = new HttpHost("somehost", 80);
    final FileBearerTokenProvider tokenProvider = new FileBearerTokenProvider();
    tokenProvider.init(conf);
    final CredentialsProvider credentialsProvider = CredentialsProviderBuilder.create()
        .add(new AuthScope(host, "test", null),
            new BearerCredentials("testUser", tokenProvider))
        .build();

    final HttpRequest request = new BasicHttpRequest("GET", "/");
    assertTrue(authscheme.isResponseReady(host, credentialsProvider, null));
    assertEquals("Bearer token1", authscheme.generateAuthResponse(host, request, null));

    assertEquals("test", authscheme.getRealm());
    assertTrue(authscheme.isChallengeComplete());
    assertFalse(authscheme.isConnectionBased());
  }

  @Test
  public void testNoTokenForUser() throws Exception {
    final AuthChallenge authChallenge = new AuthChallenge(ChallengeType.TARGET, "Bearer",
        new BasicNameValuePair("realm", "test"));

    final AuthScheme authscheme = new BearerScheme();
    authscheme.processChallenge(authChallenge, null);

    final HttpHost host  = new HttpHost("somehost", 80);
    final FileBearerTokenProvider tokenProvider = new FileBearerTokenProvider();
    tokenProvider.init(conf);
    final CredentialsProvider credentialsProvider = CredentialsProviderBuilder.create()
        .add(new AuthScope(host, "test", null),
            new BearerCredentials("testUser2", tokenProvider))
        .build();

    final HttpRequest request = new BasicHttpRequest("GET", "/");
    assertFalse(authscheme.isResponseReady(host, credentialsProvider, null));
  }

  @Test
  public void testSerialization() throws Exception {
    final AuthChallenge authChallenge = new AuthChallenge(ChallengeType.TARGET, "Bearer",
        new BasicNameValuePair("realm", "test"),
        new BasicNameValuePair("code", "read"));

    final AuthScheme authscheme = new BearerScheme();
    authscheme.processChallenge(authChallenge, null);

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final ObjectOutputStream out = new ObjectOutputStream(buffer);
    out.writeObject(authscheme);
    out.flush();
    final byte[] raw = buffer.toByteArray();
    final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(raw));
    final BearerScheme authscheme2 = (BearerScheme) in.readObject();

    assertEquals(authscheme2.getName(), authscheme2.getName());
    assertEquals(authscheme2.getRealm(), authscheme2.getRealm());
    assertEquals(authscheme.isChallengeComplete(), authscheme2.isChallengeComplete());
  }

}
