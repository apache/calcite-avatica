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

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class ConstantBearerTokenProviderTest {
  static final String TOKEN = "test token";

  ConnectionConfig conf;
  @Before
  public void setup() throws IOException {
    Properties props = new Properties();
    props.put(BuiltInConnectionProperty.BEARER_TOKEN.camelName(), TOKEN);
    conf = new ConnectionConfigImpl(props);
  }

  @Test
  public void testTokens() throws IOException {
    ConstantBearerTokenProvider tokenProvider = new ConstantBearerTokenProvider();
    tokenProvider.init(conf);
    String token1 = tokenProvider.obtain("user1");
    assertEquals(TOKEN, token1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testMissingConfig() throws IOException {
    ConstantBearerTokenProvider tokenProvider = new ConstantBearerTokenProvider();
    Properties props = new Properties();
    ConnectionConfig emptyConf = new ConnectionConfigImpl(props);
    tokenProvider.init(emptyConf);
  }
}
