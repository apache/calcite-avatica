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
import org.apache.calcite.avatica.remote.HostnameVerificationConfigurable.HostnameVerification;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

/**
 * Creates and returns a PoolingHttpClientConnectionManager object.
 * If a pool exists for a given set of keystore, trustore, and hostanmeVerification
 * parameters, then the existing pool is returned.
 *
 */
public class CommonsHttpClientPoolCache {

  // Some basic exposed configurations
  private static final String MAX_POOLED_CONNECTION_PER_ROUTE_KEY =
      "avatica.pooled.connections.per.route";
  private static final String MAX_POOLED_CONNECTION_PER_ROUTE_DEFAULT = "25";
  private static final String MAX_POOLED_CONNECTIONS_KEY = "avatica.pooled.connections.max";
  private static final String MAX_POOLED_CONNECTIONS_DEFAULT = "100";

  private static final Logger LOG = LoggerFactory.getLogger(CommonsHttpClientPoolCache.class);

  private CommonsHttpClientPoolCache() {
    //do not instantiate
  }

  private static final ConcurrentHashMap<String, PoolingHttpClientConnectionManager> CACHED_POOLS =
      new ConcurrentHashMap<>();

  public static PoolingHttpClientConnectionManager getPool(ConnectionConfig config) {
    String sslDisc = extractSSLParameters(config);

    return CACHED_POOLS.computeIfAbsent(sslDisc, k -> setupPool(config));
  }

  private static PoolingHttpClientConnectionManager setupPool(ConnectionConfig config) {
    Registry<ConnectionSocketFactory> csfr = createCSFRegistry(config);
    PoolingHttpClientConnectionManager pool = new PoolingHttpClientConnectionManager(csfr);
    final String maxCnxns =
        System.getProperty(MAX_POOLED_CONNECTIONS_KEY, MAX_POOLED_CONNECTIONS_DEFAULT);
    pool.setMaxTotal(Integer.parseInt(maxCnxns));
    // Increase default max connection per route to 25
    final String maxCnxnsPerRoute = System.getProperty(MAX_POOLED_CONNECTION_PER_ROUTE_KEY,
        MAX_POOLED_CONNECTION_PER_ROUTE_DEFAULT);
    pool.setDefaultMaxPerRoute(Integer.parseInt(maxCnxnsPerRoute));
    LOG.debug("Created new pool {}", pool);
    return pool;
  }

  private static Registry<ConnectionSocketFactory> createCSFRegistry(ConnectionConfig config) {
    RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
    configureHttpRegistry(registryBuilder);
    configureHttpsRegistry(registryBuilder, config);

    return registryBuilder.build();
  }

  private static void configureHttpsRegistry(
      RegistryBuilder<ConnectionSocketFactory> registryBuilder, ConnectionConfig config) {
    try {
      SSLContext sslContext = getSSLContext(config);
      final HostnameVerifier verifier = getHostnameVerifier(config.hostnameVerification());
      SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(sslContext, verifier);
      registryBuilder.register("https", sslFactory);
    } catch (Exception e) {
      LOG.error("HTTPS registry configuration failed");
      throw new RuntimeException(e);
    }
  }

  private static SSLContext getSSLContext(ConnectionConfig config) throws Exception {
    SSLContextBuilder sslContextBuilder = SSLContexts.custom();
    if (null != config.keystoreType()) {
      sslContextBuilder.setKeyStoreType(config.keystoreType());
    }
    if (null != config.truststore() && null != config.truststorePassword()) {
      loadTrustStore(sslContextBuilder, config);
    }
    if (null != config.keystore() && null != config.keystorePassword()
        && null != config.keyPassword()) {
      loadKeyStore(sslContextBuilder, config);
    }
    return sslContextBuilder.build();
  }

  private static void loadKeyStore(SSLContextBuilder sslContextBuilder, ConnectionConfig config)
      throws Exception {
    sslContextBuilder.loadKeyMaterial(config.keystore(), config.keystorePassword().toCharArray(),
        config.keyPassword().toCharArray());
  }

  private static void loadTrustStore(SSLContextBuilder sslContextBuilder, ConnectionConfig config)
      throws Exception {
    sslContextBuilder.loadTrustMaterial(config.truststore(),
        config.truststorePassword().toCharArray());
    // Avoid printing sensitive information such as passwords in the logs
    LOG.info("Trustore loaded from: {}", config.truststore());
  }

  private static void configureHttpRegistry(
      RegistryBuilder<ConnectionSocketFactory> registryBuilder) {
    registryBuilder.register("http", PlainConnectionSocketFactory.getSocketFactory());
  }

  /**
   * Creates the {@code HostnameVerifier} given the provided {@code verification}.
   *
   * @param verification The intended hostname verification action.
   * @return A verifier for the request verification.
   * @throws IllegalArgumentException if the provided verification cannot be
   *                                  handled.
   */
  private static HostnameVerifier getHostnameVerifier(HostnameVerification verification) {
    // Normally, the configuration logic would give us a default of STRICT if it was
    // not provided by the user. It's easy for us to do a double-check.
    if (verification == null) {
      verification = HostnameVerification.STRICT;
    }
    switch (verification) {
    case STRICT:
      return HttpsSupport.getDefaultHostnameVerifier();
    case NONE:
      return NoopHostnameVerifier.INSTANCE;
    default:
      throw new IllegalArgumentException("Unhandled HostnameVerification: " + verification.name());
    }
  }

  private static String extractSSLParameters(ConnectionConfig config) {
    //Check the mtimes, in case the file was reused (as it happens in the test suite)
    long keyMtime = 0;
    if (config.keystore() != null && config.keystore().canRead()) {
      try {
        keyMtime = Files.readAttributes(config.keystore().toPath(),
            BasicFileAttributes.class).lastModifiedTime().toMillis();
      } catch (IOException e) {
        //Fall through
      }
    }
    long trustMtime = 0;
    if (config.truststore() != null && config.truststore().canRead()) {
      try {
        trustMtime = Files.readAttributes(config.truststore().toPath(),
            BasicFileAttributes.class).lastModifiedTime().toMillis();
      } catch (IOException e) {
        //Fall through
      }
    }
    StringBuilder sb = new StringBuilder();
    sb.append(config.hostnameVerification().toString()).append(":")
    .append(config.keystoreType()).append(":")
    .append(config.truststore()).append(":")
    .append(config.truststorePassword()).append(":")
    .append(config.keystore()).append(":")
    .append(config.keystorePassword()).append(":")
    .append(config.keyPassword()).append(":")
    .append(keyMtime).append(":")
    .append(trustMtime);
    return sb.toString();
  }
}

// End CommonsHttpClientPoolCache.java
