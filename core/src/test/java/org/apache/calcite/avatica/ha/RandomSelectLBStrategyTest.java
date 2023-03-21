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

public class RandomSelectLBStrategyTest {

  ConnectionConfig mockedConnectionConfig = Mockito.mock(ConnectionConfig.class);
  RandomSelectLBStrategy randomSelectLBStrategy = new RandomSelectLBStrategy();

  int numberOfHost = 100;

  @Test
  public void getLbURL() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < numberOfHost; i++) {
      sb.append("http://host").append(i).append(",");
    }
    String inputString = sb.substring(0, sb.length() - 1);
    Mockito.when(mockedConnectionConfig.getLbURLs()).thenReturn(inputString);
    sb.delete(0, sb.length());
    for (int i = 0; i < numberOfHost; i++) {
      sb.append(randomSelectLBStrategy.getLbURL(mockedConnectionConfig)).append(",");
    }
    String actualString = sb.substring(0, sb.length() - 1);

    Assert.assertNotEquals(inputString, actualString);
  }
}
