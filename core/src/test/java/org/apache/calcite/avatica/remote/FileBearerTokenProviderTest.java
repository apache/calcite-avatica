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

import org.apache.calcite.avatica.BuiltInConnectionProperty;
import org.apache.calcite.avatica.ConnectionConfig;
import org.apache.calcite.avatica.ConnectionConfigImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class FileBearerTokenProviderTest {
  File tokensFile;
  ConnectionConfig conf;
  @Before
  public void setup() throws IOException {
    tokensFile = File.createTempFile("bearertoken_", ".txt");

    Properties props = new Properties();
    props.put(BuiltInConnectionProperty.TOKEN_FILE.camelName(), tokensFile.getAbsolutePath());
    conf = new ConnectionConfigImpl(props);
  }

  @After
  public void teardown() {
    tokensFile.delete();
  }

  @Test
  public void testTokens() throws IOException {
    // Arrange
    try (Writer fileWriter = new OutputStreamWriter(
        new FileOutputStream(tokensFile), StandardCharsets.UTF_8)) {
      fileWriter.write("user1,token1\n");
      fileWriter.write("user2,token2\n");
      fileWriter.write("user3,token3\n");
    }
    FileBearerTokenProvider tokenProvider = new FileBearerTokenProvider();
    tokenProvider.init(conf);

    // Act
    String token1 = tokenProvider.obtain("user1");
    String token2 = tokenProvider.obtain("user2");
    String token3 = tokenProvider.obtain("user3");

    // Assert
    assertEquals("token1", token1);
    assertEquals("token2", token2);
    assertEquals("token3", token3);
  }

  @Test
  public void testInvalidLine() throws IOException {
    // Arrange
    try (Writer fileWriter = new OutputStreamWriter(
        new FileOutputStream(tokensFile), StandardCharsets.UTF_8)) {
      fileWriter.write("user1,token1\n");
      fileWriter.write("user2,,token2\n");
      fileWriter.write("user3\n");
    }
    FileBearerTokenProvider tokenProvider = new FileBearerTokenProvider();
    tokenProvider.init(conf);

    // Act
    String token1 = tokenProvider.obtain("user1");
    String token2 = tokenProvider.obtain("user2");
    String token3 = tokenProvider.obtain("user3");

    // Assert
    assertEquals("token1", token1);
    assertNull(token2);
    assertNull(token3);
  }

  @Test
  public void testEmptyLine() throws IOException {
    // Arrange
    try (Writer fileWriter = new OutputStreamWriter(
        new FileOutputStream(tokensFile), StandardCharsets.UTF_8)) {
      fileWriter.write("user1,token1\n");
      fileWriter.write("\n");
      fileWriter.write("user3,token3\n");
    }
    FileBearerTokenProvider tokenProvider = new FileBearerTokenProvider();
    tokenProvider.init(conf);

    // Act
    String token1 = tokenProvider.obtain("user1");
    String token2 = tokenProvider.obtain("user2");
    String token3 = tokenProvider.obtain("user3");

    // Assert
    assertEquals("token1", token1);
    assertNull(token2);
    assertEquals("token3", token3);
  }

  @Test
  public void testMultiple() throws IOException {
    // Arrange
    try (Writer fileWriter = new OutputStreamWriter(
        new FileOutputStream(tokensFile), StandardCharsets.UTF_8)) {
      fileWriter.write("user1,token1\n");
      fileWriter.write("user2,token2\n");
      fileWriter.write("user1,token3\n");
    }
    FileBearerTokenProvider tokenProvider = new FileBearerTokenProvider();
    tokenProvider.init(conf);

    // Act
    String token1 = tokenProvider.obtain("user1");
    String token2 = tokenProvider.obtain("user2");

    // Assert
    assertEquals("token3", token1);
    assertEquals("token2", token2);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testMissingConfig() throws IOException {
    FileBearerTokenProvider tokenProvider = new FileBearerTokenProvider();
    Properties props = new Properties();
    ConnectionConfig emptyConf = new ConnectionConfigImpl(props);
    tokenProvider.init(emptyConf);
  }

  @Test
  public void testFileChanged() throws IOException, InterruptedException {
    // Arrange
    try (Writer fileWriter = new OutputStreamWriter(
        new FileOutputStream(tokensFile), StandardCharsets.UTF_8)) {
      fileWriter.write("user1,token1\n");
    }
    FileBearerTokenProvider tokenProvider = new FileBearerTokenProvider();
    tokenProvider.init(conf);

    // Act & Assert
    assertEquals("token1", tokenProvider.obtain("user1"));
    assertNull(tokenProvider.obtain("user2"));
    try (Writer fileWriter = new OutputStreamWriter(
        new FileOutputStream(tokensFile), StandardCharsets.UTF_8)) {
      fileWriter.write("user2,token2\n");
      fileWriter.write("user1,token3\n");
    }
    boolean success;
    int attempts = 0;
    do {
      success = Objects.equals(tokenProvider.obtain("user1"), "token3")
          && Objects.equals(tokenProvider.obtain("user2"), "token2");
      ++attempts;
      Thread.sleep(1000);
    } while (attempts < 5 && !success);
    if (!success) {
      fail("Tokens have not been reloaded from the file that we changed");
    }
  }

  @Test
  public void testMissingFile() throws IOException {
    // Arrange
    tokensFile.delete();
    FileBearerTokenProvider tokenProvider = new FileBearerTokenProvider();
    tokenProvider.init(conf);

    // Act
    String token1 = tokenProvider.obtain("user1");

    // Assert
    assertNull(token1);
  }

  @Test
  public void testDelayedFileCreation() throws IOException, InterruptedException {
    // Arrange
    tokensFile.delete();
    FileBearerTokenProvider tokenProvider = new FileBearerTokenProvider();
    tokenProvider.init(conf);

    // Act & Assert
    assertNull(tokenProvider.obtain("user1"));
    tokensFile = new File(tokensFile.getAbsolutePath());
    tokensFile.deleteOnExit();
    try (Writer fileWriter = new OutputStreamWriter(
        new FileOutputStream(tokensFile), StandardCharsets.UTF_8)) {
      fileWriter.write("user1,token1\n");
    }
    boolean success;
    int attempts = 0;
    do {
      success = Objects.equals(tokenProvider.obtain("user1"), "token1");
      ++attempts;
      Thread.sleep(1000);
    } while (attempts < 5 && !success);
    if (!success) {
      fail("Token has not been reloaded from the file that we created");
    }
  }
}
