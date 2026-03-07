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

import org.apache.calcite.avatica.proto.Common;
import org.apache.calcite.avatica.remote.TypedValue;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fuzzer for TypedValue.
 */
public class TypedValueFuzzer {

  private TypedValueFuzzer() {
  }

  /**
   * Fuzzes TypedValue conversion methods.
   *
   * @param data fuzzed data
   */
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // Use the Fuzzer to generate random Protobuf arrays
      boolean isFromProto = data.consumeBoolean();

      if (isFromProto) {
        // Parse it into Common.TypedValue
        Common.TypedValue protoValue = Common.TypedValue.parseFrom(data.consumeRemainingAsBytes());

        // Attempt to convert it into a local Avatica TypedValue and then to JDBC representations
        TypedValue typedValue = TypedValue.fromProto(protoValue);

        // Convert to local and jdbc formats
        typedValue.toLocal();
        typedValue.toJdbc(Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT));

        // Attempt Protobuf serialization back
        typedValue.toProto();
      } else {
        // Fuzz the direct POJO creator
        String typeName = data.pickValue(new String[]{
            "STRING", "BOOLEAN", "BYTE", "SHORT", "INTEGER", "LONG", "FLOAT", "DOUBLE", "DATE", "TIME", "TIMESTAMP"
        });

        Object fakeValue = null;
        switch (typeName) {
        case "STRING":
          fakeValue = data.consumeString(50);
          break;
        case "BOOLEAN":
          fakeValue = data.consumeBoolean();
          break;
        case "BYTE":
          fakeValue = data.consumeByte();
          break;
        case "SHORT":
          fakeValue = data.consumeShort();
          break;
        case "INTEGER":
          fakeValue = data.consumeInt();
          break;
        case "LONG":
          fakeValue = data.consumeLong();
          break;
        case "FLOAT":
          fakeValue = data.consumeFloat();
          break;
        case "DOUBLE":
          fakeValue = data.consumeDouble();
          break;
        case "DATE":
        case "TIME":
        case "TIMESTAMP":
          fakeValue = data.consumeLong();
          break;
        default:
          break;
        }

        // Fuzz create factory mapping the object value with random type identifier
        TypedValue created = TypedValue.create(typeName, fakeValue);

        // Call accessors
        created.toLocal();
        created.toJdbc(Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT));
        created.toProto();
      }

    } catch (java.io.IOException e) {
      // Known exception for invalid protobuf
    } catch (RuntimeException e) {
      // TypedValue parser is known to throw unchecked exceptions
      // when types don't align with values in the protobuf
      // E.g., asking for a Boolean from a protobuf field that was stored as a String.
    }
  }
}
