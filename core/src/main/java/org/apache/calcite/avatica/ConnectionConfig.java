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

import org.apache.calcite.avatica.ha.LBStrategy;
import org.apache.calcite.avatica.remote.AvaticaHttpClientFactory;
import org.apache.calcite.avatica.remote.Service;

import java.io.File;

/**
 * Connection configuration.
 */
public interface ConnectionConfig {
  /** @see BuiltInConnectionProperty#SCHEMA */
  String schema();
  /** @see BuiltInConnectionProperty#TIME_ZONE */
  String timeZone();
  /** @see BuiltInConnectionProperty#FACTORY */
  Service.Factory factory();
  /** @see BuiltInConnectionProperty#URL */
  String url();
  /** @see BuiltInConnectionProperty#SERIALIZATION */
  String serialization();
  /** @see BuiltInConnectionProperty#AUTHENTICATION */
  String authentication();
  /** @see BuiltInConnectionProperty#AVATICA_USER */
  String avaticaUser();
  /** @see BuiltInConnectionProperty#AVATICA_PASSWORD */
  String avaticaPassword();
  /** @see BuiltInConnectionProperty#HTTP_CLIENT_FACTORY */
  AvaticaHttpClientFactory httpClientFactory();
  /** @see BuiltInConnectionProperty#HTTP_CLIENT_IMPL */
  String httpClientClass();
  /** @see BuiltInConnectionProperty#PRINCIPAL */
  String kerberosPrincipal();
  /** @see BuiltInConnectionProperty#KEYTAB */
  File kerberosKeytab();
  /** @see BuiltInConnectionProperty#KEYSTORE_TYPE */
  String keystoreType();
  /** @see BuiltInConnectionProperty#TRUSTSTORE */
  File truststore();
  /** @see BuiltInConnectionProperty#TRUSTSTORE_PASSWORD */
  String truststorePassword();
  /** @see BuiltInConnectionProperty#KEYSTORE */
  File keystore();
  /** @see BuiltInConnectionProperty#KEYSTORE_PASSWORD */
  String keystorePassword();
  /** @see BuiltInConnectionProperty#KEY_PASSWORD */
  String keyPassword();
  /** @see BuiltInConnectionProperty#HOSTNAME_VERIFICATION */
  @SuppressWarnings("deprecation")
  org.apache.calcite.avatica.remote.
      HostnameVerificationConfigurable.HostnameVerification hostnameVerification();
  /** @see BuiltInConnectionProperty#TRANSPARENT_RECONNECTION */
  boolean transparentReconnectionEnabled();
  /** @see BuiltInConnectionProperty#FETCH_SIZE */
  int fetchSize();
  /** @see BuiltInConnectionProperty#USE_CLIENT_SIDE_LB #**/
  boolean useClientSideLb();
  /** @see BuiltInConnectionProperty#LB_URLS **/
  String getLbURLs();
  /** @see BuiltInConnectionProperty#LB_STRATEGY **/
  LBStrategy getLBStrategy();
  /** @see BuiltInConnectionProperty#LB_CONNECTION_FAILOVER_RETRIES **/
  int getLBConnectionFailoverRetries();
  /** @see BuiltInConnectionProperty#LB_CONNECTION_FAILOVER_SLEEP_TIME **/
  long getLBConnectionFailoverSleepTime();
  /** @see BuiltInConnectionProperty#HTTP_CONNECTION_TIMEOUT **/
  long getHttpConnectionTimeout();
  /** @see BuiltInConnectionProperty#HTTP_RESPONSE_TIMEOUT **/
  long getHttpResponseTimeout();
  /** @see BuiltInConnectionProperty#TOKEN_FILE */
  String getTokenFile();
  /** @see BuiltInConnectionProperty#BEARER_TOKEN */
  String getBearerToken();
  /** @see BuiltInConnectionProperty#TOKEN_PROVIDER_CLASS */
  String getBearerTokenProviderClass();

  ConnectionPropertyValue customPropertyValue(ConnectionProperty property);
}

// End ConnectionConfig.java
