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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for {@link ConnectionPropertiesImpl}.
 */
public class ConnectionPropertiesImplTest {

  @Test
  public void testMerge() {
    ConnectionPropertiesImpl connectionProperties1 = new ConnectionPropertiesImpl(
                Boolean.FALSE, Boolean.TRUE,
                Integer.MAX_VALUE, "catalog", "schema");
    ConnectionPropertiesImpl connectionProperties2 = new ConnectionPropertiesImpl(
                Boolean.FALSE, Boolean.TRUE,
                Integer.MAX_VALUE, "catalog", "schema");
    Assert.assertEquals(
            connectionProperties1.isReadOnly().equals(connectionProperties2.isReadOnly()),
            true);

    ConnectionPropertiesImpl merged = connectionProperties1.merge(connectionProperties2);
    Assert.assertEquals(merged.isDirty(), false);
    Assert.assertEquals(connectionProperties1, merged);
  }
}

// End ConnectionPropertiesImplTest.java
