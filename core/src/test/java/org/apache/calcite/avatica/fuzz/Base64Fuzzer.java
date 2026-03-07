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
package org.apache.calcite.avatica.fuzz;

import org.apache.calcite.avatica.util.Base64;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import java.io.IOException;

/**
 * Fuzzer for Base64 utility class.
 */
public final class Base64Fuzzer {
  private Base64Fuzzer() {
  }

  /**
   * Fuzzes Base64 encode and decode methods.
   *
   * @param data fuzzed data provider
   */
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      if (data.consumeBoolean()) {
        Base64.encodeBytes(data.consumeRemainingAsBytes());
      } else {
        Base64.decode(data.consumeRemainingAsBytes());
      }
    } catch (IOException | IllegalArgumentException e) {
      // Known exception
    }
  }
}
