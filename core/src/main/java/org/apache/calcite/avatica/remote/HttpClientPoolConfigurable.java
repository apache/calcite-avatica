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

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * Allows a http connection pool to be provided to enable TLS authentication.
 * On clients with this interface setHttpClientPool() MUST be called before using them.
 */
public interface HttpClientPoolConfigurable {
  /**
   * Sets a PoolingHttpClientConnectionManager containing the collection of SSL/TLS server
   * keys and truststores to use for HTTPS calls.
   *
   * @param pool The http connection pool
   */
  void setHttpClientPool(PoolingHttpClientConnectionManager pool);
}

// End HttpClientPoolConfigurable.java
