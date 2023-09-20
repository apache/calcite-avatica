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
import org.apache.calcite.avatica.ConnectStringParser;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.Meta.Signature;
import org.apache.calcite.avatica.Meta.StatementHandle;
import org.apache.calcite.avatica.remote.JsonService;
import org.apache.calcite.avatica.remote.Service;
import org.apache.calcite.avatica.remote.looker.LookerRemoteMeta.LookerFrame;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/**
 * A testable Looker driver without requiring access to a Looker instance.
 *
 * {@link #withStubbedResponse} must be called before creating a connection.
 */
public class StubbedLookerDriver extends LookerDriver {

  String stubbedSignature;
  String stubbedResponse;

  /**
   * Sets stubbed responses for the test. The signature must match the stubbed response.
   *
   * @param signature {@link Signature} as a JSON string.
   * @param response a JSON response identical to one returned by a Looker API call to
   *     {@code GET /sql_interface_queries/:id/run/json_bi}.
   * @return the driver with a stubbed {@link Service} and {@link Meta}.
   */
  public LookerDriver withStubbedResponse(String signature, String response) {
    this.stubbedSignature = signature;
    this.stubbedResponse = response;

    return this;
  }

  @Override
  public Meta createMeta(AvaticaConnection connection) {
    assertNotNull(stubbedSignature);
    assertNotNull(stubbedResponse);

    final Service service = new StubbedLookerRemoteService(stubbedSignature);
    connection.setService(service);

    return new StubbedLookerRemoteMeta(connection, service, stubbedResponse);
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    if (!acceptsURL(url)) {
      return null;
    }

    final String prefix = getConnectStringPrefix();
    assert url.startsWith(prefix);
    final String urlSuffix = url.substring(prefix.length());
    final Properties info2 = ConnectStringParser.parse(urlSuffix, info);
    final AvaticaConnection connection = factory.newConnection(this, factory, url, info2);
    handler.onConnectionInit(connection);

    return connection;
  }

  public class StubbedLookerRemoteMeta extends LookerRemoteMeta {

    String stubbedResponse;

    StubbedLookerRemoteMeta(AvaticaConnection connection, Service service, String testResponse) {
      super(connection, service);
      this.stubbedResponse = testResponse;
    }

    @Override
    protected InputStream makeRunQueryRequest(String url) {
      return new ByteArrayInputStream(stubbedResponse.getBytes(StandardCharsets.UTF_8));
    }
  }

  public class StubbedLookerRemoteService extends LookerRemoteService {

    private String stubbedSignature;

    StubbedLookerRemoteService(String signature) {
      super();
      this.stubbedSignature = signature;
    }

    @Override
    public ConnectionSyncResponse apply(ConnectionSyncRequest request) {
      try {
        // value does not matter for this stub class but needed by the connection
        return decode("{\"response\": \"connectionSync\"}", ConnectionSyncResponse.class);
      } catch (IOException e) {
        throw handle(e);
      }
    }

    @Override
    public CreateStatementResponse apply(CreateStatementRequest request) {
      try {
        // value does not matter for this stub class but needed by the connection
        return decode("{\"response\": \"createStatement\"}", CreateStatementResponse.class);
      } catch (IOException e) {
        throw handle(e);
      }
    }

    @Override
    public PrepareResponse apply(PrepareRequest request) {
      try {
        Signature signature = JsonService.MAPPER.readValue(stubbedSignature, Signature.class);
        StatementHandle statementHandle = new StatementHandle(request.connectionId, 1, signature);
        return new PrepareResponse(statementHandle, null);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public ExecuteResponse apply(ExecuteRequest request) {
      PrepareAndExecuteRequest req = new PrepareAndExecuteRequest(
          request.statementHandle.connectionId, request.statementHandle.id, null, -1);
      return apply(req);
    }

    @Override
    public ExecuteResponse apply(PrepareAndExecuteRequest request) {
      try {
        Signature signature = JsonService.MAPPER.readValue(stubbedSignature, Signature.class);
        return lookerExecuteResponse(request.connectionId, request.statementId, signature,
            LookerFrame.create(1L));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
