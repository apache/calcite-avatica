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

import org.apache.calcite.avatica.Meta.Signature;
import org.apache.calcite.avatica.Meta.StatementHandle;
import org.apache.calcite.avatica.remote.JsonService;
import org.apache.calcite.avatica.remote.looker.LookerRemoteMeta.LookerFrame;

import com.looker.sdk.LookerSDK;
import com.looker.sdk.SqlInterfaceQuery;
import com.looker.sdk.SqlInterfaceQueryMetadata;
import com.looker.sdk.WriteSqlInterfaceQueryCreate;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.calcite.avatica.remote.looker.LookerSdkFactory.safeSdkCall;

/**
 * Implementation of {@link org.apache.calcite.avatica.remote.Service} that uses the Looker SDK to
 * send Avatica request/responses to a Looker instance via JSON.
 */
public class LookerRemoteService extends JsonService {

  /**
   * Keep track of next statement ID. This is similar to `CalciteMetaImpl` in that it only
   * increments. Statements are scoped to connections, so it is unlikely we will ever encounter
   * overflow. If we do, the client must make a new connection.
   */
  private final AtomicInteger statementCounter = new AtomicInteger(0);

  /**
   * These are statements that have been prepared but not yet executed. We need to know the query id
   * to run on the Looker instance when a client decides to `execute` them. Multiple statements can
   * run the same query, so it is important to index on the statement id rather than query id.
   */
  final ConcurrentMap<Integer, SqlQueryWithSignature> preparedStmtToQueryMap =
      new ConcurrentHashMap<>();

  /**
   * The authenticated SDK used to communicate with a Looker instance.
   */
  public LookerSDK sdk;

  void setSdk(LookerSDK sdk) {
    this.sdk = sdk;
  }

  void checkSdk() {
    assert null != sdk : "No authenticated SDK for this connection!";
  }

  private class SqlQueryWithSignature {

    SqlInterfaceQuery query;
    Signature signature;

    SqlQueryWithSignature(SqlInterfaceQuery query) {
      this.query = query;

      try {
        this.signature = JsonService.MAPPER.readValue(query.getSignature(), Signature.class);
      } catch (IOException e) {
        throw handle(e);
      }
    }
  }

  /**
   * Helper method to create a {@link ExecuteResponse} for this request. Since we are using the
   * Looker SDK we need to create this response client side.
   */
  ExecuteResponse lookerExecuteResponse(String connectionId, int statementId, Signature signature,
      LookerFrame lookerFrame) {
    ResultSetResponse rs = new ResultSetResponse(connectionId, statementId, false, signature,
        lookerFrame, -1, null);
    return new ExecuteResponse(Arrays.asList(new ResultSetResponse[]{rs}), false, null);
  }

  private SqlQueryWithSignature prepareQuery(String sql) {
    checkSdk();
    WriteSqlInterfaceQueryCreate queryRequest = new WriteSqlInterfaceQueryCreate(
        sql, /*jdbcClient=*/true);
    SqlInterfaceQuery preparedQuery = safeSdkCall(
        () -> sdk.create_sql_interface_query(queryRequest));

    return new SqlQueryWithSignature(preparedQuery);
  }

  /**
   * Handles all non-overridden {@code apply} methods.
   *
   * Calls the {@code sql_interface_metadata} endpoint of the instance which behaves similarly to a
   * standard Avatica server.
   */
  @Override
  public String apply(String request) {
    checkSdk();
    SqlInterfaceQueryMetadata response = safeSdkCall(() -> sdk.sql_interface_metadata(request));

    return response.getResults();
  }

  /**
   * Handles CreateStatementRequest. We want to control the statement id since Looker instances do
   * not keep track of the statement.
   */
  @Override
  public CreateStatementResponse apply(CreateStatementRequest request) {
    return new CreateStatementResponse(request.connectionId, statementCounter.getAndIncrement(),
        /*rpcMetadata=*/ null);
  }

  /**
   * Handles PrepareRequests by preparing a query via {@link LookerSDK#create_sql_query} whose
   * response contains a query id. This id is used to execute the query via
   * {@link LookerSDK#run_sql_query} with the 'json_bi' format.
   *
   * @param request the base Avatica request to convert into a Looker SDK call.
   * @return a {@link PrepareResponse} containing a new {@link StatementHandle}.
   */
  @Override
  public PrepareResponse apply(PrepareRequest request) {
    checkSdk();
    int currentStatementId = statementCounter.getAndIncrement();

    SqlQueryWithSignature preparedQuery = prepareQuery(request.sql);
    StatementHandle stmt = new StatementHandle(request.connectionId, currentStatementId,
        preparedQuery.signature);
    preparedStmtToQueryMap.put(currentStatementId, preparedQuery);

    return new PrepareResponse(stmt, null);
  }

  /**
   * Handles ExecuteRequests by setting up a {@link LookerFrame} to stream the response.
   *
   * @param request the base Avatica request. Used to locate the query to run in the statement map.
   * @return a {@link ExecuteResponse} containing a prepared {@link LookerFrame}.
   */
  @Override
  public ExecuteResponse apply(ExecuteRequest request) {
    checkSdk();
    SqlQueryWithSignature preparedQuery = preparedStmtToQueryMap.get(request.statementHandle.id);

    return lookerExecuteResponse(request.statementHandle.connectionId, request.statementHandle.id,
        preparedQuery.signature, LookerFrame.create(preparedQuery.query.getId()));
  }

  /**
   * Handles PrepareAndExecuteRequests by preparing a query via {@link LookerSDK#create_sql_query}
   * whose response contains a query id. This id is used to execute the query via
   * {@link LookerSDK#run_sql_query} with the 'json_bi' format.
   *
   * @param request the base Avatica request to convert into a Looker SDK call.
   * @return a {@link ExecuteResponse} containing a prepared {@link LookerFrame}.
   */
  @Override
  public ExecuteResponse apply(PrepareAndExecuteRequest request) {
    checkSdk();
    SqlQueryWithSignature preparedQuery = prepareQuery(request.sql);

    return lookerExecuteResponse(request.connectionId, request.statementId, preparedQuery.signature,
        LookerFrame.create(preparedQuery.query.getId()));
  }

  /**
   * If the statement is closed, clean up the prepared statement map
   */
  @Override
  public CloseStatementResponse apply(CloseStatementRequest request) {
    preparedStmtToQueryMap.remove(request.statementId);
    return super.apply(request);
  }
}
