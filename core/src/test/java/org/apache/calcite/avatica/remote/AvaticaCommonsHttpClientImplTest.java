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

import org.apache.calcite.avatica.AvaticaUtils;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.io.entity.StringEntity;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Test class for {@link AvaticaCommonsHttpClientImpl}
 */
public class AvaticaCommonsHttpClientImplTest {

  @Test public void testRetryOnHttp503() throws Exception {
    final byte[] requestBytes = "fake_request".getBytes(UTF_8);
    final CloseableHttpResponse badResponse = mock(CloseableHttpResponse.class);
    final CloseableHttpResponse goodResponse = mock(CloseableHttpResponse.class);
    final StringEntity responseEntity = new StringEntity("success");
    final Answer<CloseableHttpResponse> failThenSucceed = new Answer<CloseableHttpResponse>() {
      private int iteration = 0;
      @Override public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
        iteration++;
        if (1 == iteration) {
          return badResponse;
        } else {
          return goodResponse;
        }
      }
    };

    final AvaticaCommonsHttpClientImpl client =
            spy(new AvaticaCommonsHttpClientImpl(new URL("http://127.0.0.1")));

    doAnswer(failThenSucceed).when(client)
            .execute(any(HttpPost.class), any(HttpClientContext.class));

    when(badResponse.getCode()).thenReturn(HttpURLConnection.HTTP_UNAVAILABLE);

    when(goodResponse.getCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(goodResponse.getEntity()).thenReturn(responseEntity);

    byte[] responseBytes = client.send(requestBytes);
    assertEquals("success", AvaticaUtils.newStringUtf8(responseBytes));
  }

  @Test public void testRetryOnMissingHttpResponse() throws Exception {
    final byte[] requestBytes = "fake_request".getBytes(UTF_8);
    final CloseableHttpResponse badResponse = mock(CloseableHttpResponse.class);
    final CloseableHttpResponse goodResponse = mock(CloseableHttpResponse.class);
    final StringEntity responseEntity = new StringEntity("success");
    final Answer<CloseableHttpResponse> failThenSucceed = new Answer<CloseableHttpResponse>() {
      private int iteration = 0;
      @Override public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
        iteration++;
        if (1 == iteration) {
          throw new NoHttpResponseException("The server didn't respond!");
        } else {
          return goodResponse;
        }
      }
    };

    final AvaticaCommonsHttpClientImpl client =
            spy(new AvaticaCommonsHttpClientImpl(new URL("http://127.0.0.1")));

    doAnswer(failThenSucceed).when(client)
            .execute(any(HttpPost.class), any(HttpClientContext.class));

    when(badResponse.getCode()).thenReturn(HttpURLConnection.HTTP_UNAVAILABLE);

    when(goodResponse.getCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(goodResponse.getEntity()).thenReturn(responseEntity);

    byte[] responseBytes = client.send(requestBytes);
    assertEquals("success", AvaticaUtils.newStringUtf8(responseBytes));
  }

}

// End AvaticaCommonsHttpClientImplTest.java
