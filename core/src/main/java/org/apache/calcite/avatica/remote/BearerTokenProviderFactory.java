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

import java.io.IOException;
import java.lang.reflect.Constructor;

public class BearerTokenProviderFactory {
  public static final String TOKEN_PROVIDER_IMPL_DEFAULT =
          ConstantBearerTokenProvider.class.getName();

  private BearerTokenProviderFactory() {}

  public static BearerTokenProvider getBearerTokenProvider(ConnectionConfig config)
          throws IOException {
    String tokenProviderClassName = config.bearerTokenProviderClass();
    if (null == tokenProviderClassName) {
      tokenProviderClassName = TOKEN_PROVIDER_IMPL_DEFAULT;
    }
    BearerTokenProvider tokenProvider = instantiateTokenProvider(tokenProviderClassName);
    tokenProvider.init(config);
    return tokenProvider;
  }

  private static BearerTokenProvider instantiateTokenProvider(String className) {
    BearerTokenProvider tokenProvider = null;
    Exception tokenProviderCreationException = null;

    try {
      Class<? extends BearerTokenProvider> clz =
              Class.forName(className).asSubclass(BearerTokenProvider.class);
      Constructor<? extends BearerTokenProvider> constructor = clz.getConstructor();
      tokenProvider = constructor.newInstance();
    } catch (Exception e) {
      tokenProviderCreationException = e;
    }

    if (tokenProvider == null) {
      throw new RuntimeException("Failed to construct BearerTokenProvider implementation "
              + className, tokenProviderCreationException);
    } else {
      return tokenProvider;
    }
  }

}
