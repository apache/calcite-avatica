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
package org.apache.calcite.avatica.remote.looker;

import org.apache.calcite.avatica.AvaticaConnection;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.Types;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LookerDriverTest {

  static final String TEST_SQL =
      "SELECT " + "`users.created_date`, `users.created_year`, `users.created_time`, `users.name`\n"
          + "`users.age`, `users.is45or30`, SUM(`users.age`), AGGREGATE(`users.average_age`)\n"
          + "FROM example.users\n" + "GROUP BY 1, 2, 3, 4, 5, 6";

  static final String TEST_LIST_MEASURE_SQL = "SELECT `users.created_year`\n"
      + "        , AGGREGATE(`users.names`) as `names`\n"
      + "        , AGGREGATE(`users.ages`) as `ages`\n"
      + "       FROM `example`.`users`\n"
      + "       GROUP BY `users.created_year`\n"
      + "       LIMIT 2";

  static final String TEST_ARRAY_PRIMITIVES_SQL = "SELECT \"testing arrays\" as `test_name`\n"
      + "        , array[1,2,3] as `int_arrs`\n"
      + "        , array[1.1, 2.2, 3.3] as `float_arr`\n"
      + "        , array[\"this\", \"and\", \"that\"] as `varchar_arr`\n"
      + "        , array[true, false, false] as `boolean_arr`\n"
      + "       FROM `example`.`users`\n"
      + "       LIMIT 1";

  @Test
  public void driverIsRegistered() throws SQLException {
    Driver driver = DriverManager.getDriver("jdbc:looker:url=foobar.com");

    assertThat(driver, is(instanceOf(LookerDriver.class)));
  }

  @Test
  public void driverThrowsAuthExceptionForBlankProperties() throws SQLException {
    Properties props = new Properties();
    try {
      Driver driver = DriverManager.getDriver("jdbc:looker:url=foobar.com");
      driver.connect("jdbc:looker:url=foobar.com", props);

      fail("Should have thrown an auth exception!");
    } catch (SQLInvalidAuthorizationSpecException e) {
      assertThat(e.getMessage(),
          is("Invalid connection params.\nMissing either API3 credentials" + " or access token"));
    }
  }

  @Test
  public void createsAvaticaConnections() throws SQLException {
    Properties props = new Properties();
    props.put("token", "foobar");

    Driver driver = DriverManager.getDriver("jdbc:looker:url=foobar.com");
    Connection connection = driver.connect("jdbc:looker:url=foobar.com", props);

    assertThat(connection, is(instanceOf(AvaticaConnection.class)));
  }

  @Test
  public void failsGracefullyOnBadJsonResponse() throws SQLException {
    // 250 chars is enough to start the stream, but we'll eventually hit an incomplete JSON object
    String incompleteJson = LookerTestCommon.stubbedJsonResults.substring(0, 250);
    Driver driver = new StubbedLookerDriver().withStubbedResponse(LookerTestCommon.stubbedSignature,
        incompleteJson);
    Connection connection = driver.connect(LookerTestCommon.getUrl(),
        LookerTestCommon.getBaseProps());

    try {
      connection.createStatement().executeQuery(TEST_SQL);

      fail("Should have thrown an exception during stream parsing!");
    } catch (SQLException e) {

      Assert.assertThat(e.getMessage(),
          containsString("Error while executing SQL \"" + TEST_SQL + "\""));

      assertNotNull(e.getCause());
      Assert.assertThat(e.getCause().getMessage(), containsString("Unexpected end-of-input"));
    }
  }

  @Test
  public void resultsCanBeParsedIntoResultSet() throws SQLException {
    // Set up the driver with some pre-recorded responses from Looker. These are large JSON strings
    // so have been placed in the LookerTestCommon class to make this test easier to read.
    Driver driver = new StubbedLookerDriver().withStubbedResponse(LookerTestCommon.stubbedSignature,
        LookerTestCommon.stubbedJsonResults);
    Connection connection = driver.connect(LookerTestCommon.getUrl(),
        LookerTestCommon.getBaseProps());

    ResultSet test = connection.createStatement().executeQuery(TEST_SQL);
    ResultSetMetaData rsMetaData = test.getMetaData();

    // verify column types
    Assert.assertThat(rsMetaData.getColumnCount(), is(8));

    // DATE
    Assert.assertThat(rsMetaData.getColumnType(1), is(Types.DATE));
    Assert.assertThat(rsMetaData.getColumnName(1), is("users.created_date"));

    // YEAR
    Assert.assertThat(rsMetaData.getColumnType(2), is(Types.INTEGER));
    Assert.assertThat(rsMetaData.getColumnName(2), is("users.created_year"));

    // TIMESTAMP
    Assert.assertThat(rsMetaData.getColumnType(3), is(Types.TIMESTAMP));
    Assert.assertThat(rsMetaData.getColumnName(3), is("users.created_time"));

    // STRING
    Assert.assertThat(rsMetaData.getColumnType(4), is(Types.VARCHAR));
    Assert.assertThat(rsMetaData.getColumnName(4), is("users.name"));

    // DOUBLE dimension
    Assert.assertThat(rsMetaData.getColumnType(5), is(Types.DOUBLE));
    Assert.assertThat(rsMetaData.getColumnName(5), is("users.age"));

    // BOOLEAN
    Assert.assertThat(rsMetaData.getColumnType(6), is(Types.BOOLEAN));
    Assert.assertThat(rsMetaData.getColumnName(6), is("users.is45or30"));

    // DOUBLE custom measure
    Assert.assertThat(rsMetaData.getColumnType(7), is(Types.DOUBLE));
    Assert.assertThat(rsMetaData.getColumnName(7), is("EXPR$6"));

    // DOUBLE LookML measure
    Assert.assertThat(rsMetaData.getColumnType(8), is(Types.DOUBLE));
    // TODO: investigate why measures are not being aliased as their LookML field name
    Assert.assertThat(rsMetaData.getColumnName(8), is("EXPR$7"));

    // verify every row can be fetched with the appropriate getter method
    while (test.next()) {
      assertNotNull(test.getDate(1));
      assertNotNull(test.getInt(2));
      assertNotNull(test.getTimestamp(3));
      assertNotNull(test.getString(4));
      assertNotNull(test.getDouble(5));
      assertNotNull(test.getBoolean(6));
      assertNotNull(test.getDouble(7));
      assertNotNull(test.getDouble(8));
    }
  }

  @Test
  public void listMeasureResultsCanBeParsedIntoResultSet() throws SQLException {
    // Set up the driver with some pre-recorded responses from Looker. These are large JSON strings
    // so have been placed in the LookerTestCommon class to make this test easier to read.
    Driver driver = new StubbedLookerDriver().withStubbedResponse(
        LookerTestCommon.stubbedListMeasuresSignature,
        LookerTestCommon.stubbedListMeasureResponse);
    Connection connection = driver.connect(LookerTestCommon.getUrl(),
        LookerTestCommon.getBaseProps());

    ResultSet test = connection.createStatement().executeQuery(TEST_LIST_MEASURE_SQL);
    ResultSetMetaData rsMetaData = test.getMetaData();

    // verify column types
    Assert.assertThat(rsMetaData.getColumnCount(), is(3));

    // YEAR
    Assert.assertThat(rsMetaData.getColumnType(1), is(Types.INTEGER));
    Assert.assertThat(rsMetaData.getColumnName(1), is("users.created_year"));

    // Names
    Assert.assertThat(rsMetaData.getColumnType(2), is(Types.ARRAY));
    Assert.assertThat(rsMetaData.getColumnName(2), is("names"));

    // Ages
    Assert.assertThat(rsMetaData.getColumnType(3), is(Types.ARRAY));
    Assert.assertThat(rsMetaData.getColumnName(3), is("ages"));

    // verify every row can be fetched with the appropriate getter method
    while (test.next()) {
      assertNotNull(test.getInt(1));
      assertNotNull(test.getArray(2));
      assertNotNull(test.getArray(3));
    }
  }

  @Test
  public void canProcessAllArrayTypes() throws SQLException {
    // Set up the driver with some pre-recorded responses from Looker. These are large JSON strings
    // so have been placed in the LookerTestCommon class to make this test easier to read.
    Driver driver = new StubbedLookerDriver().withStubbedResponse(
        LookerTestCommon.stubbedArrayPrimitivesSig,
        LookerTestCommon.stubbedArrayPrimitivesResponse);
    Connection connection = driver.connect(LookerTestCommon.getUrl(),
        LookerTestCommon.getBaseProps());

    ResultSet test = connection.createStatement().executeQuery(TEST_ARRAY_PRIMITIVES_SQL);
    ResultSetMetaData rsMetaData = test.getMetaData();
    int columnCount = rsMetaData.getColumnCount();

    // Verify we got our arrays, skipping index 1 becuase it isn't an array
    for (int i = 2; i <= columnCount; i++) {
      Assert.assertThat(rsMetaData.getColumnType(i), is(Types.ARRAY));
    }

    while (test.next()) {
      Object actualArrayValue = test.getArray(2).getArray();

      int[] expectedInts = new int[]{1, 2, 3};
      Assert.assertThat(actualArrayValue, is(equalTo(expectedInts)));

      actualArrayValue = test.getArray(3).getArray();
      BigDecimal[] expectedDecimals = new BigDecimal[]{
          BigDecimal.valueOf(1.1), BigDecimal.valueOf(2.2), BigDecimal.valueOf(3.3)
      };
      Assert.assertThat(
          actualArrayValue,
          is(equalTo(expectedDecimals))
      );

      String[] expectedStrs = new String[]{"this", "and", null, "that"};
      actualArrayValue = test.getArray(4).getArray();
      Assert.assertThat(
          actualArrayValue,
          is(equalTo(expectedStrs))
      );

      boolean[] expectedBools = new boolean[]{true, false, false};
      actualArrayValue = test.getArray(5).getArray();
      Assert.assertThat(
          actualArrayValue,
          is(equalTo(expectedBools))
      );
    }
  }


  @Test
  public void canUsePreparedStatement() throws SQLException {
    Driver driver = new StubbedLookerDriver().withStubbedResponse(LookerTestCommon.stubbedSignature,
        LookerTestCommon.stubbedJsonResults);
    Connection connection = driver.connect(LookerTestCommon.getUrl(),
        LookerTestCommon.getBaseProps());

    // This time use a prepared statement
    PreparedStatement prepareStatement = connection.prepareStatement(TEST_SQL);
    ResultSetMetaData metaData = prepareStatement.getMetaData();

    // verify column types are accessible prior to execution
    Assert.assertThat(metaData.getColumnCount(), is(8));

    // DATE
    Assert.assertThat(metaData.getColumnType(1), is(Types.DATE));
    Assert.assertThat(metaData.getColumnName(1), is("users.created_date"));

    // YEAR
    Assert.assertThat(metaData.getColumnType(2), is(Types.INTEGER));
    Assert.assertThat(metaData.getColumnName(2), is("users.created_year"));

    // TIMESTAMP
    Assert.assertThat(metaData.getColumnType(3), is(Types.TIMESTAMP));
    Assert.assertThat(metaData.getColumnName(3), is("users.created_time"));

    // STRING
    Assert.assertThat(metaData.getColumnType(4), is(Types.VARCHAR));
    Assert.assertThat(metaData.getColumnName(4), is("users.name"));

    // DOUBLE dimension
    Assert.assertThat(metaData.getColumnType(5), is(Types.DOUBLE));
    Assert.assertThat(metaData.getColumnName(5), is("users.age"));

    // BOOLEAN
    Assert.assertThat(metaData.getColumnType(6), is(Types.BOOLEAN));
    Assert.assertThat(metaData.getColumnName(6), is("users.is45or30"));

    // DOUBLE custom measure
    Assert.assertThat(metaData.getColumnType(7), is(Types.DOUBLE));
    Assert.assertThat(metaData.getColumnName(7), is("EXPR$6"));

    // DOUBLE LookML measure
    Assert.assertThat(metaData.getColumnType(8), is(Types.DOUBLE));
    // TODO: investigate why measures are not being aliased as their LookML field name
    Assert.assertThat(metaData.getColumnName(8), is("EXPR$7"));

    // verify execution on a prepared statement works
    assertTrue(prepareStatement.execute());
    ResultSet results = prepareStatement.getResultSet();

    while (results.next()) {
      assertNotNull(results.getDate(1));
      assertNotNull(results.getInt(2));
      assertNotNull(results.getTimestamp(3));
      assertNotNull(results.getString(4));
      assertNotNull(results.getDouble(5));
      assertNotNull(results.getBoolean(6));
      assertNotNull(results.getDouble(7));
      assertNotNull(results.getDouble(8));
    }
  }
}
