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
import org.apache.calcite.avatica.ConnectionProperty;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.BearerToken;
import org.apache.hc.client5.http.auth.Credentials;

import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

import static org.apache.calcite.avatica.remote.BearerTokenProviderFactoryTest.TestTokenProvider.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BearerTokenProviderFactoryTest {
  @Test
  public void testConstantBearerToken() throws Exception {
    Properties props = new Properties();
    props.setProperty(BuiltInConnectionProperty.AUTHENTICATION.name(), "BEARER");
    props.setProperty(BuiltInConnectionProperty.BEARER_TOKEN.name(), "testtoken");
    ConnectionConfig config = new ConnectionConfigImpl(props);

    BearerTokenProvider tokenProvider = BearerTokenProviderFactory.getBearerTokenProvider(config);
    assertTrue("TokenProvider was not ConstantBearerTokenProvider",
            tokenProvider instanceof ConstantBearerTokenProvider);
    assertEquals("TokenProvider was not initialized",
            "testtoken", tokenProvider.obtain("user"));
  }

  @Test
  public void testCustomBearerToken() throws Exception {
    Properties props = new Properties();
    final TestConnectionProperty testProperty = new TestConnectionProperty();
    props.setProperty(BuiltInConnectionProperty.TOKEN_PROVIDER_CLASS.name(),
            TestTokenProvider.class.getName());
    props.setProperty(testProperty.name(), "CustomToken");
    ConnectionConfig config = new ConnectionConfigImpl(props);
    BearerTokenProvider tokenProvider = BearerTokenProviderFactory.getBearerTokenProvider(config);
    assertTrue("TokenProvider was not TestTokenProvider",
            tokenProvider instanceof TestTokenProvider);
    assertEquals("CustomToken", tokenProvider.obtain(USERNAME_1));
    assertEquals(INVALID_TOKEN, tokenProvider.obtain(USERNAME_2));
    assertNull(tokenProvider.obtain(USERNAME_3));
    assertNull(tokenProvider.obtain(null));
  }

  @Test
  public void testSetTokenProvider() throws Exception {
    URL url = new URI("http://localhost:8765").toURL();
    Properties props = new Properties();
    ConnectionConfig config = new ConnectionConfigImpl(props);

    final TestConnectionProperty testProperty = new TestConnectionProperty();
    props.setProperty(BuiltInConnectionProperty.TOKEN_PROVIDER_CLASS.name(),
        TestTokenProvider.class.getName());
    props.setProperty(testProperty.name(), "CustomToken");

    AvaticaHttpClientFactory httpClientFactory = new AvaticaHttpClientFactoryImpl();
    AvaticaHttpClient client = httpClientFactory.getClient(url, config, null);
    assertTrue("Client was an instance of " + client.getClass(),
        client instanceof AvaticaCommonsHttpClientImpl);

    BearerTokenProvider tokenProvider = BearerTokenProviderFactory.getBearerTokenProvider(config);
    assertTrue("TokenProvider was not TestTokenProvider",
        tokenProvider instanceof TestTokenProvider);

    // for user 1 tokenProvider returns a good token
    ((AvaticaCommonsHttpClientImpl) client).setTokenProvider(USERNAME_1, tokenProvider);
    Credentials res1 = ((AvaticaCommonsHttpClientImpl) client).context.getCredentialsProvider()
        .getCredentials(new AuthScope(null, -1), ((AvaticaCommonsHttpClientImpl) client).context);
    assertEquals(((BearerToken) res1).getToken(), "CustomToken");

    // for user 2 tokenProvider returns an invalid token
    ((AvaticaCommonsHttpClientImpl) client).setTokenProvider(USERNAME_2, tokenProvider);
    Credentials res2 = ((AvaticaCommonsHttpClientImpl) client).context.getCredentialsProvider()
        .getCredentials(new AuthScope(null, -1), ((AvaticaCommonsHttpClientImpl) client).context);
    assertEquals(((BearerToken) res2).getToken(), INVALID_TOKEN);

    // for user 3 tokenProvider returns null
    ((AvaticaCommonsHttpClientImpl) client).setTokenProvider(USERNAME_3, tokenProvider);
    Credentials res3 = ((AvaticaCommonsHttpClientImpl) client).context.getCredentialsProvider()
        .getCredentials(new AuthScope(null, -1), ((AvaticaCommonsHttpClientImpl) client).context);
    assertTrue(res3 instanceof AvaticaCommonsHttpClientImpl.EmptyCredentials);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testCustomBearerTokenInvalid() throws Exception {
    Properties props = new Properties();
    props.setProperty(
            BuiltInConnectionProperty.TOKEN_PROVIDER_CLASS.name(),
            TestTokenProvider.class.getName());
    ConnectionConfig config = new ConnectionConfigImpl(props);
    BearerTokenProviderFactory.getBearerTokenProvider(config);
  }


  @Test(expected = RuntimeException.class)
  public void testInvalidBearerToken() throws Exception {
    Properties props = new Properties();
    props.setProperty(BuiltInConnectionProperty.HTTP_CLIENT_IMPL.name(),
            Properties.class.getName()); // Properties is intentionally *not* a valid class
    ConnectionConfig config = new ConnectionConfigImpl(props);
    BearerTokenProviderFactory.getBearerTokenProvider(config);
  }

  public static class TestTokenProvider implements BearerTokenProvider {
    public static final String USERNAME_1 = "USER1";
    public static final String USERNAME_2 = "USER2";
    public static final String USERNAME_3 = "USER3";
    public static final String INVALID_TOKEN = "INV";

    private final TestConnectionProperty testProperty = new TestConnectionProperty();
    private String token;

    @Override
    public void init(ConnectionConfig config) throws IOException {
      token = config.customPropertyValue(testProperty).getString();
      if (token == null || token.trim().isEmpty()) {
        throw new UnsupportedOperationException("Config option "
                + testProperty.name()
                + " must be specified to use ConstantBearerTokenProvider");
      }
    }

    @Override
    public synchronized String obtain(String username) {
      try {
        if (USERNAME_2.contentEquals(Objects.requireNonNull(username))) {
          return INVALID_TOKEN;
        } else if (USERNAME_1.contentEquals(Objects.requireNonNull(username))) {
          return token;
        } else {
          return null;
        }
      } catch (NullPointerException exception) {
        return null;
      }
    }

    public static class TestConnectionProperty implements ConnectionProperty {
      private final String name = "TEST_TOKEN_PROVIDER_PROPERTY";

      public String name() {
        return name.toUpperCase(Locale.ROOT);
      }

      public String camelName() {
        return name.toLowerCase(Locale.ROOT);
      }

      public Object defaultValue() {
        return null;
      }

      public Type type() {
        return Type.STRING;
      }

      public Class valueClass() {
        return Type.STRING.defaultValueClass();
      }

      public ConnectionConfigImpl.PropEnv wrap(Properties properties) {
        final HashMap<String, ConnectionProperty> map = new HashMap<>();
        map.put(name, this);
        return new ConnectionConfigImpl.PropEnv(
                ConnectionConfigImpl.parse(properties, map), this);
      }

      public boolean required() {
        return false;
      }
    }
  }
}
