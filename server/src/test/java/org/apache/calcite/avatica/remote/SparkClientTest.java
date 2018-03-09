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

import org.apache.calcite.avatica.ConnectionSpec;
import org.apache.calcite.avatica.server.HttpServer;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for Spark Client
 */
@RunWith(Parameterized.class)
public class SparkClientTest {
  private static final AvaticaServersForTest SERVERS = new AvaticaServersForTest();
  private final Driver.Serialization serialization;

  @Parameters(name = "{0}")
  public static List<Object[]> parameters() throws Exception {
    SERVERS.startServers();
    return SERVERS.getJUnitParameters();
  }

  @AfterClass public static void afterClass() throws Exception {
    if (null != SERVERS) {
      SERVERS.stopServers();
    }
  }

  private final String url;

  public SparkClientTest(Driver.Serialization serialization, HttpServer server) {
    int port = server.getPort();
    this.serialization = serialization;
    this.url = SERVERS.getJdbcUrl(port, serialization);
  }

  @Test public void testSpark() throws Exception {
    ConnectionSpec.getDatabaseLock().lock();

    final String table = "test";
    try (Connection conn = DriverManager.getConnection(this.url);
         Statement stmt = conn.createStatement()) {
      assertFalse(stmt.execute("DROP TABLE IF EXISTS " + table));
      assertFalse(stmt.execute("CREATE TABLE " + table + "(SERIALIZATION VARCHAR)"));
      assertFalse(
          stmt.execute("INSERT INTO " + table + " VALUES ('" + serialization.toString() + "')"));

      assertTrue(stmt.execute("SELECT * from " + table));

      try (ResultSet rs = stmt.getResultSet()) {
        assertTrue(rs.next());
        assertEquals(serialization.toString(), rs.getString("SERIALIZATION"));
        assertFalse(rs.next());
      }

      try (SparkSession spark = SparkSession.builder()
          .master("local[1]")
          .config("spark.driver.cores", 1)
          .getOrCreate();
      ) {
        Dataset<Row> jdbcDF = spark.read().jdbc(this.url, table, new Properties());
        StructField serializationField =
            DataTypes.createStructField("SERIALIZATION", DataTypes.StringType, true);
        StructType expectedSchema = DataTypes.createStructType(
            Collections.singletonList(serializationField));
        assertEquals(expectedSchema, jdbcDF.schema());

        assertEquals(1, jdbcDF.count());

        Row expectedRow = RowFactory.create(serialization.toString());
        assertEquals(expectedRow, jdbcDF.first());
      }

      assertFalse(stmt.execute("DROP TABLE IF EXISTS " + table));
    } finally {
      ConnectionSpec.getDatabaseLock().unlock();
    }
  }
}
// End SparkClientTest.java
