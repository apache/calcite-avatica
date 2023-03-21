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
package org.apache.calcite.avatica.ha;

import org.apache.calcite.avatica.ConnectionConfig;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class RoundRobinLBStrategyTest {

  ConnectionConfig mockedConnectionConfig = Mockito.mock(ConnectionConfig.class);
  RoundRobinLBStrategy roundRobinLBStrategy = RoundRobinLBStrategy.INSTANCE;

  @Test
  public void getLbURL() {
    String inputString = "http://host1.com,http://host2.com,http://host3.com";
    Mockito.when(mockedConnectionConfig.getLbURLs()).thenReturn(inputString);
    String[] urls = inputString.split(",");

    Assert.assertEquals(urls[0], roundRobinLBStrategy.getLbURL(mockedConnectionConfig));
    Assert.assertEquals(urls[1], roundRobinLBStrategy.getLbURL(mockedConnectionConfig));
    Assert.assertEquals(urls[2], roundRobinLBStrategy.getLbURL(mockedConnectionConfig));
  }

}
