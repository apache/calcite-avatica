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

import org.apache.calcite.avatica.remote.JsonService;
import org.apache.calcite.avatica.remote.Service;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;

/**
 * Fuzzer for JsonHandler (JsonService).
 */
public class JsonHandlerFuzzer {

  private JsonHandlerFuzzer() {
  }

  static {
    // Prevent failure on completely unknown properties that the fuzzer might invent
    JsonService.MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  /**
   * Fuzzes JSON serialization/deserialization for Avatica Request/Response objects.
   *
   * @param data fuzzed data
   */
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // The goal here is to hit the deeply nested deserialization logic of
      // Avatica's Request and Response models.
      // Avatica uses Jackson to parse strings into classes like
      // Service.ExecuteRequest, Service.CatalogsRequest, etc.

      boolean isRequest = data.consumeBoolean();

      if (isRequest) {
        String subType = data.pickValue(new String[]{
            "getCatalogs", "getSchemas", "getTables", "getTableTypes", "getTypeInfo", "getColumns",
            "execute", "prepare", "prepareAndExecute", "fetch", "createStatement", "closeStatement",
            "openConnection", "closeConnection", "connectionSync", "databaseProperties", "syncResults",
            "commit", "rollback", "prepareAndExecuteBatch", "executeBatch"
        });

        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("request", subType);

        // Add random key/value pairs
        int numFields = data.consumeInt(0, 10);
        for (int i = 0; i < numFields; i++) {
          switch (data.consumeInt(1, 4)) {
          case 1:
            map.put(data.consumeString(10), data.consumeString(20));
            break;
          case 2:
            map.put(data.consumeString(10), data.consumeInt());
            break;
          case 3:
            map.put(data.consumeString(10), data.consumeBoolean());
            break;
          case 4:
            map.put(data.consumeString(10), null);
            break;
          default:
            break;
          }
        }

        String jsonPayload = JsonService.MAPPER.writeValueAsString(map);
        JsonService.MAPPER.readValue(jsonPayload, Service.Request.class);
      } else {
        String subType = data.pickValue(new String[]{
            "openConnection", "resultSet", "prepare", "fetch", "createStatement", "closeStatement",
            "closeConnection", "connectionSync", "databaseProperties", "executeResults", "error",
            "syncResults", "rpcMetadata", "commit", "rollback", "executeBatch"
        });

        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("response", subType);

        // Add random key/value pairs
        int numFields = data.consumeInt(0, 10);
        for (int i = 0; i < numFields; i++) {
          switch (data.consumeInt(1, 4)) {
          case 1:
            map.put(data.consumeString(10), data.consumeString(20));
            break;
          case 2:
            map.put(data.consumeString(10), data.consumeInt());
            break;
          case 3:
            map.put(data.consumeString(10), data.consumeBoolean());
            break;
          case 4:
            map.put(data.consumeString(10), null);
            break;
          default:
            break;
          }
        }

        String jsonPayload = JsonService.MAPPER.writeValueAsString(map);
        JsonService.MAPPER.readValue(jsonPayload, Service.Response.class);
      }

    } catch (JsonParseException | JsonMappingException e) {
      // Known Jackson exceptions for invalid JSON structure or unmappable types
    } catch (IOException e) {
      // General IO issues reading the string
    } catch (IllegalArgumentException | IllegalStateException e) {
      // Known issues when Jackson encounters valid JSON but violates Avatica's preconditions
    }
  }
}
