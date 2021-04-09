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

import org.apache.calcite.avatica.Meta.Signature;
import org.apache.calcite.avatica.util.DateTimeUtils;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.Collections;

import static org.junit.Assert.assertTrue;

/**
 * Tests for {@code AvaticaResultSet} relative to close behavior
 */
@RunWith(Parameterized.class)
public class AvaticaClosedResultSetWithNullStatementTest extends AvaticaClosedTestBase<ResultSet> {
  // Mapping between Connection method and the verifier to check close behavior
  private static MethodVerifier methodVerifier(Method method) {
    String name = method.getName();
    // All update methods are not supported yet
    if (name.startsWith("update")) {
      return ASSERT_UNSUPPORTED;
    }

    switch (name) {
    case "absolute":
    case "afterLast":
    case "beforeFirst":
    case "cancelRowUpdates":
    case "deleteRow":
    case "first":
    case "getCursorName":
    case "getRowId":
    case "insertRow":
    case "isLast":
    case "last":
    case "moveToCurrentRow":
    case "moveToInsertRow":
    case "previous":
    case "refreshRow":
    case "relative":
      return ASSERT_UNSUPPORTED;

    default:
      return ASSERT_CLOSED;
    }
  };

  @Parameters(name = "{index}: {0}")
  public static Iterable<? extends Object[]> getParameters() {
    return getMethodsToTest(ResultSet.class, AvaticaResultSet.class, METHOD_FILTER,
        AvaticaClosedResultSetWithNullStatementTest::methodVerifier);
  }

  public AvaticaClosedResultSetWithNullStatementTest(Method method, MethodVerifier verifier) {
    super(method, verifier);
  }

  @Override protected ResultSet newInstance() throws Exception {
    Signature signature = new Signature(Collections.emptyList(), "", Collections.emptyList(),
        Collections.emptyMap(), null, Meta.StatementType.SELECT);
    AvaticaResultSet resultSet = new AvaticaResultSet(null, new QueryState(""), signature,
        null, DateTimeUtils.UTC_ZONE, null);
    resultSet.close();
    assertTrue("Resultset is not closed", resultSet.isClosed());

    return resultSet;
  }
}

// End AvaticaClosedResultSetWithNullStatementTest.java
