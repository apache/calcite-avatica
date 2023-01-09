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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Test class for {@link UnregisteredDriver}.
 */
public class UnregisteredDriverTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test public void testNullUrl() throws SQLException {
    final Driver driver = new UnregisteredTestDriver();
    thrown.expect(SQLException.class);
    thrown.expectMessage("url can not be null!");
    driver.connect(null, new Properties());
  }

  private static final class UnregisteredTestDriver extends UnregisteredDriver{

    @Override
    protected DriverVersion createDriverVersion() {
      return null;
    }

    @Override
    protected String getConnectStringPrefix() {
      return null;
    }

    @Override
    public Meta createMeta(AvaticaConnection connection) {
      return null;
    }
  }
}
