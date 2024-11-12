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

import org.apache.calcite.avatica.ConnectionConfig;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.auth.AuthSchemeFactory;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.KerberosConfig;
import org.apache.hc.client5.http.auth.KerberosCredentials;
import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicSchemeFactory;
import org.apache.hc.client5.http.impl.auth.DigestSchemeFactory;
import org.apache.hc.client5.http.impl.auth.SPNegoSchemeFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import org.ietf.jgss.GSSCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A common class to invoke HTTP requests against the Avatica server agnostic of the data being
 * sent and received across the wire.
 */
public class AvaticaCommonsHttpClientImpl implements AvaticaHttpClient, HttpClientPoolConfigurable,
    UsernamePasswordAuthenticateable, GSSAuthenticateable {
  private static final Logger LOG = LoggerFactory.getLogger(AvaticaCommonsHttpClientImpl.class);

  // SPNEGO specific settings
  private static final boolean USE_CANONICAL_HOSTNAME = Boolean
      .parseBoolean(System.getProperty("avatica.http.spnego.use_canonical_hostname", "true"));
  private static final boolean STRIP_PORT_ON_SERVER_LOOKUP = true;
  private static final KerberosConfig KERBEROS_CONFIG =
          KerberosConfig.custom().setStripPort(STRIP_PORT_ON_SERVER_LOOKUP)
          .setUseCanonicalHostname(USE_CANONICAL_HOSTNAME)
          .build();
  private static AuthScope anyAuthScope = new AuthScope(null, -1);

  protected final URI uri;
  protected BasicAuthCache authCache;
  protected CloseableHttpClient client;
  protected Registry<ConnectionSocketFactory> socketFactoryRegistry;
  protected PoolingHttpClientConnectionManager pool;

  protected UsernamePasswordCredentials credentials = null;
  protected CredentialsProvider credentialsProvider = null;
  protected Lookup<AuthSchemeFactory> authRegistry = null;
  protected Object userToken;
  protected HttpClientContext context;
  protected long connectTimeout;
  protected long responseTimeout;

  public AvaticaCommonsHttpClientImpl(URL url) {
    this.uri = toURI(Objects.requireNonNull(url));
  }

  protected void initializeClient(PoolingHttpClientConnectionManager pool,
                                  ConnectionConfig config) {
    this.authCache = new BasicAuthCache();
    this.connectTimeout = config.getHttpConnectionTimeout();
    this.responseTimeout = config.getHttpResponseTimeout();
    // A single thread-safe HttpClient, pooling connections via the
    // ConnectionManager
    RequestConfig requestConfig = createRequestConfig();
    HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(pool)
        .setDefaultRequestConfig(requestConfig);
    this.client = httpClientBuilder.build();

    this.context = HttpClientContext.create();
    // Set the credentials if they were provided.
    if (null != this.credentialsProvider) {
      context.setCredentialsProvider(credentialsProvider);
      context.setAuthSchemeRegistry(authRegistry);
      context.setAuthCache(authCache);
    }
    if (null != userToken) {
      context.setUserToken(userToken);
    }

  }

  // This is needed because we initialize the client object too early.
  private RequestConfig createRequestConfig() {
    RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
    requestConfigBuilder
        .setConnectTimeout(this.connectTimeout, TimeUnit.MILLISECONDS)
        .setResponseTimeout(this.responseTimeout, TimeUnit.MILLISECONDS);
    List<String> preferredSchemes = new ArrayList<>();
    // In HttpClient 5.3+ SPNEGO is not enabled by default
    if (authRegistry != null) {
      if (authRegistry.lookup(StandardAuthScheme.DIGEST) != null) {
        preferredSchemes.add(StandardAuthScheme.DIGEST);
      }
      if (authRegistry.lookup(StandardAuthScheme.BASIC) != null) {
        preferredSchemes.add(StandardAuthScheme.BASIC);
      }
      if (authRegistry.lookup(StandardAuthScheme.SPNEGO) != null) {
        preferredSchemes.add(StandardAuthScheme.SPNEGO);
      }
      requestConfigBuilder.setTargetPreferredAuthSchemes(preferredSchemes);
      requestConfigBuilder.setProxyPreferredAuthSchemes(preferredSchemes);
    }
    return requestConfigBuilder.build();
  }

  @Override public byte[] send(byte[] request) {
    while (true) {
      ByteArrayEntity entity = new ByteArrayEntity(request, ContentType.APPLICATION_OCTET_STREAM);

      // Create the client with the AuthSchemeRegistry and manager
      HttpPost post = new HttpPost(uri);
      post.setEntity(entity);

      try (CloseableHttpResponse response = execute(post, context)) {
        final int statusCode = response.getCode();
        if (HttpURLConnection.HTTP_OK == statusCode
            || HttpURLConnection.HTTP_INTERNAL_ERROR == statusCode) {
          userToken = context.getUserToken();
          return EntityUtils.toByteArray(response.getEntity());
        } else if (HttpURLConnection.HTTP_UNAVAILABLE == statusCode) {
          LOG.debug("Failed to connect to server (HTTP/503), retrying");
          continue;
        }

        throw new RuntimeException(
            "Failed to execute HTTP Request, got HTTP/" + statusCode);
      } catch (NoHttpResponseException e) {
        // This can happen when sitting behind a load balancer and a backend server dies
        LOG.debug("The server failed to issue an HTTP response, retrying");
        continue;
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        LOG.debug("Failed to execute HTTP request", e);
        throw new RuntimeException(e);
      }
    }
  }

  // Visible for testing
  CloseableHttpResponse execute(HttpPost post, HttpClientContext context)
      throws IOException, ClientProtocolException {
    return client.execute(post, context);
  }

  @Override public void setUsernamePassword(AuthenticationType authType, String username,
      String password) {
    this.credentials = new UsernamePasswordCredentials(Objects.requireNonNull(username),
        Objects.requireNonNull(password).toCharArray());

    this.credentialsProvider = new BasicCredentialsProvider();
    ((BasicCredentialsProvider) credentialsProvider).setCredentials(anyAuthScope, credentials);

    RegistryBuilder<AuthSchemeFactory> authRegistryBuilder = RegistryBuilder.create();
    switch (authType) {
    case BASIC:
      authRegistryBuilder.register(StandardAuthScheme.BASIC, new BasicSchemeFactory());
      break;
    case DIGEST:
      authRegistryBuilder.register(StandardAuthScheme.DIGEST, new DigestSchemeFactory());
      break;
    default:
      throw new IllegalArgumentException("Unsupported authentiation type: " + authType);
    }
    this.authRegistry = authRegistryBuilder.build();
    context.setCredentialsProvider(credentialsProvider);
    context.setAuthSchemeRegistry(authRegistry);
    context.setRequestConfig(createRequestConfig());
  }

  @Override public void setGSSCredential(GSSCredential credential) {

    this.authRegistry = RegistryBuilder.<AuthSchemeFactory>create()
        .register(StandardAuthScheme.SPNEGO,
                new SPNegoSchemeFactory(KERBEROS_CONFIG, SystemDefaultDnsResolver.INSTANCE))
        .build();

    this.credentialsProvider = new BasicCredentialsProvider();
    if (null != credential) {
      // Non-null credential should be used directly with KerberosCredentials.
      // This is never set by the JDBC driver, nor the tests
      ((BasicCredentialsProvider) this.credentialsProvider)
              .setCredentials(anyAuthScope, new KerberosCredentials(credential));
    } else {
      // A null credential implies that the user is logged in via JAAS using the
      // java.security.auth.login.config system property
      ((BasicCredentialsProvider) this.credentialsProvider)
              .setCredentials(anyAuthScope, EmptyCredentials.INSTANCE);
    }
    context.setCredentialsProvider(credentialsProvider);
    context.setAuthSchemeRegistry(authRegistry);
    context.setRequestConfig(createRequestConfig());
  }

  /**
   * A credentials implementation which returns null.
   */
  private static class EmptyCredentials implements Credentials {
    public static final EmptyCredentials INSTANCE = new EmptyCredentials();

    @Override public char[] getPassword() {
      return null;
    }

    @Override public Principal getUserPrincipal() {
      return null;
    }
  }

  private static URI toURI(URL url) throws RuntimeException {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override public void setHttpClientPool(PoolingHttpClientConnectionManager pool,
                                          ConnectionConfig config) {
    initializeClient(pool, config);
  }

}

// End AvaticaCommonsHttpClientImpl.java
