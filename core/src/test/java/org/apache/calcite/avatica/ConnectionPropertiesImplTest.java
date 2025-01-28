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

import org.junit.Test;

import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test class for {@link ConnectionPropertiesImpl}.
 */
public class ConnectionPropertiesImplTest {

  @Test
  public void testMerge() {
    ConnectionPropertiesImpl connectionProperties1 = new ConnectionPropertiesImpl(
        Boolean.FALSE, Boolean.TRUE, Integer.MAX_VALUE, "catalog", "schema");
    ConnectionPropertiesImpl connectionProperties2 = new ConnectionPropertiesImpl(
        Boolean.FALSE, Boolean.TRUE, Integer.MAX_VALUE, "catalog", "schema");
    assertThat(
        Objects.equals(connectionProperties1.isReadOnly(),
            connectionProperties2.isReadOnly()), is(true));
    assertThat(
        Objects.equals(connectionProperties1.isAutoCommit(),
            connectionProperties2.isAutoCommit()), is(true));
    assertThat(
        Objects.equals(connectionProperties1.getTransactionIsolation(),
            connectionProperties2.getTransactionIsolation()), is(true));
    ConnectionPropertiesImpl merged = connectionProperties1.merge(connectionProperties2);
    assertThat(merged.isDirty(), is(false));
    assertThat(Objects.equals(merged, connectionProperties1), is(true));

    ConnectionPropertiesImpl connectionProperties3 = new ConnectionPropertiesImpl(
        null, null, null, "catalog", "schema");
    assertThat(
        Objects.equals(connectionProperties1.isReadOnly(),
            connectionProperties3.isReadOnly()), is(false));
    assertThat(
        Objects.equals(connectionProperties1.isAutoCommit(),
            connectionProperties3.isAutoCommit()), is(false));
    assertThat(
        Objects.equals(connectionProperties1.getTransactionIsolation(),
            connectionProperties3.getTransactionIsolation()), is(false));
    ConnectionPropertiesImpl merged1 = connectionProperties3.merge(connectionProperties1);
    assertThat(merged1.isDirty(), is(true));
    assertThat(Objects.equals(merged1, connectionProperties1), is(false));
  }
}

// End ConnectionPropertiesImplTest.java
