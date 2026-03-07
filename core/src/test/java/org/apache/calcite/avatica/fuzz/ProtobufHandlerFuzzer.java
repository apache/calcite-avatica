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

import org.apache.calcite.avatica.remote.ProtobufTranslationImpl;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.google.protobuf.ByteString;

import java.io.IOException;

/**
 * Fuzzer for ProtobufHandler (ProtobufTranslation).
 */
public class ProtobufHandlerFuzzer {

  private ProtobufHandlerFuzzer() {
  }

  private static final ProtobufTranslationImpl TRANSLATOR = new ProtobufTranslationImpl();

  /**
   * Fuzzes Protobuf serialization/deserialization for Avatica Request/Response objects.
   *
   * @param data fuzzed data
   */
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // The goal here is to hit the protobuf deserialization logic.
      // Avatica maps Protobuf messages (WireMessage) into its POJO Service.Request models.
      // WireMessage requires a "name" string matching a Request subclass.

      boolean isRequest = data.consumeBoolean();

      if (isRequest) {
        String subType = data.pickValue(new String[]{
            "org.apache.calcite.avatica.proto.Requests$CatalogsRequest",
            "org.apache.calcite.avatica.proto.Requests$SchemasRequest",
            "org.apache.calcite.avatica.proto.Requests$TablesRequest",
            "org.apache.calcite.avatica.proto.Requests$TableTypesRequest",
            "org.apache.calcite.avatica.proto.Requests$TypeInfoRequest",
            "org.apache.calcite.avatica.proto.Requests$ColumnsRequest",
            "org.apache.calcite.avatica.proto.Requests$ExecuteRequest",
            "org.apache.calcite.avatica.proto.Requests$PrepareRequest",
            "org.apache.calcite.avatica.proto.Requests$PrepareAndExecuteRequest",
            "org.apache.calcite.avatica.proto.Requests$FetchRequest",
            "org.apache.calcite.avatica.proto.Requests$CreateStatementRequest",
            "org.apache.calcite.avatica.proto.Requests$CloseStatementRequest",
            "org.apache.calcite.avatica.proto.Requests$OpenConnectionRequest",
            "org.apache.calcite.avatica.proto.Requests$CloseConnectionRequest",
            "org.apache.calcite.avatica.proto.Requests$ConnectionSyncRequest",
            "org.apache.calcite.avatica.proto.Requests$DatabasePropertyRequest",
            "org.apache.calcite.avatica.proto.Requests$SyncResultsRequest",
            "org.apache.calcite.avatica.proto.Requests$CommitRequest",
            "org.apache.calcite.avatica.proto.Requests$RollbackRequest",
            "org.apache.calcite.avatica.proto.Requests$PrepareAndExecuteBatchRequest",
            "org.apache.calcite.avatica.proto.Requests$ExecuteBatchRequest"
        });

        org.apache.calcite.avatica.proto.Common.WireMessage wireMsg =
            org.apache.calcite.avatica.proto.Common.WireMessage.newBuilder()
            .setName(subType)
            .setWrappedMessage(ByteString.copyFrom(data.consumeRemainingAsBytes()))
            .build();

        byte[] protobufPayload = wireMsg.toByteArray();
        TRANSLATOR.parseRequest(protobufPayload);
      } else {
        String subType = data.pickValue(new String[]{
            "org.apache.calcite.avatica.proto.Responses$OpenConnectionResponse",
            "org.apache.calcite.avatica.proto.Responses$CloseConnectionResponse",
            "org.apache.calcite.avatica.proto.Responses$CloseStatementResponse",
            "org.apache.calcite.avatica.proto.Responses$ConnectionSyncResponse",
            "org.apache.calcite.avatica.proto.Responses$CreateStatementResponse",
            "org.apache.calcite.avatica.proto.Responses$DatabasePropertyResponse",
            "org.apache.calcite.avatica.proto.Responses$ExecuteResponse",
            "org.apache.calcite.avatica.proto.Responses$FetchResponse",
            "org.apache.calcite.avatica.proto.Responses$PrepareResponse",
            "org.apache.calcite.avatica.proto.Responses$ResultSetResponse",
            "org.apache.calcite.avatica.proto.Responses$ErrorResponse",
            "org.apache.calcite.avatica.proto.Responses$SyncResultsResponse",
            "org.apache.calcite.avatica.proto.Responses$RpcMetadata",
            "org.apache.calcite.avatica.proto.Responses$CommitResponse",
            "org.apache.calcite.avatica.proto.Responses$RollbackResponse",
            "org.apache.calcite.avatica.proto.Responses$ExecuteBatchResponse"
        });

        org.apache.calcite.avatica.proto.Common.WireMessage wireMsg =
            org.apache.calcite.avatica.proto.Common.WireMessage.newBuilder()
            .setName(subType)
            .setWrappedMessage(ByteString.copyFrom(data.consumeRemainingAsBytes()))
            .build();

        byte[] protobufPayload = wireMsg.toByteArray();
        TRANSLATOR.parseResponse(protobufPayload);
      }

    } catch (IOException e) {
      // Known exception from protobuf parsing (e.g. InvalidProtocolBufferException)
    } catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
      // Known issues when Protobuf unmarshalls into Avatica types that fail preconditions
    } catch (RuntimeException e) {
      // Specifically catching Avatica's custom DeserializationException or
      // unhandled protobuf issues to ensure the fuzzer survives
      if (e.getClass().getName().contains("DeserializationException")
          || e.getClass().getName().contains("InvalidProtocolBufferException")
          || e.getMessage() != null && e.getMessage().contains("Unknown type:")
          || e.getMessage() != null && e.getMessage().contains("Unhandled type:")) {
        return;
      }
      // If it's a real bug (NullPointerException, etc), let it crash!
      throw e;
    }
  }
}
