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
package org.apache.calcite.avatica.jdbc;

import java.sql.ResultSet;
import java.util.Optional;

/**
 * Limits the size of single {@link JdbcMeta.Frame} during its construction.
 */
interface FrameLimiter {

  /**
   * Create a new {@link Context} to start limiting the size of a single {@link JdbcMeta.Frame}
   *
   * @param resultSet the {@link ResultSet} being used to construct the frame
   * @return the context, configured according to the specification of this FrameLimiter instance
   */
  Context start(ResultSet resultSet);

  /**
   * Return the optional row count limit for this FrameLimiter.
   */
  default Optional<Integer> getRowCountLimit() {
    return Optional.empty();
  }

  /**
   * A context configured by its creating {@link FrameLimiter}, limits a frame within a
   * single fetch operation.
   */
  interface Context {

    /**
     * Determine if the limit configured in the FrameLimiter has been reached in the
     * current Frame creation operation.
     *
     * @return true if the limit has been reached, otherwise false
     */
    boolean limitReached();

    /**
     * Add a single row that will be included in the Frame
     *
     * @param row the row being added
     */
    void addRow(Object[] row);
  }
}
// End FrameLimiter.java
