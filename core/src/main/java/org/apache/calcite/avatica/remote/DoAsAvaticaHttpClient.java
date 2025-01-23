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

import org.apache.calcite.avatica.util.SecurityUtil;

import java.security.PrivilegedAction;
import java.util.Objects;

/**
 * HTTP client implementation which invokes the wrapped HTTP client in a doAs with the provided
 * Subject.
 */
public class DoAsAvaticaHttpClient implements AvaticaHttpClient {
  private final AvaticaHttpClient wrapped;
  private final KerberosConnection kerberosUtil;

  public DoAsAvaticaHttpClient(AvaticaHttpClient wrapped, KerberosConnection kerberosUtil) {
    this.wrapped = Objects.requireNonNull(wrapped);
    this.kerberosUtil = Objects.requireNonNull(kerberosUtil);
  }

  @Override public byte[] send(final byte[] request) {
    return SecurityUtil.callAs(kerberosUtil.getSubject(), new PrivilegedAction<byte[]>() {
      @Override public byte[] run() {
        return wrapped.send(request);
      }
    });
  }
}

// End DoAsAvaticaHttpClient.java
