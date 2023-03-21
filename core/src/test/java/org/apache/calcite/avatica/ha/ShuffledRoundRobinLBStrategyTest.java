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

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

public class ShuffledRoundRobinLBStrategyTest {

  ConnectionConfig mockedConnectionConfig = Mockito.mock(ConnectionConfig.class);
  ShuffledRoundRobinLBStrategy shuffledRoundRobinLBStrategy =
      Mockito.spy(ShuffledRoundRobinLBStrategy.INSTANCE);

  @Test
  public void getLbURL() throws NoSuchFieldException, IllegalAccessException {
    String inputString = "http://host1.com,http://host2.com,http://host3.com";
    Mockito.when(mockedConnectionConfig.getLbURLs()).thenReturn(inputString);
    String firstURL = shuffledRoundRobinLBStrategy.getLbURL(mockedConnectionConfig);

    String[] expectedUrls = getShuffledURLsFromStateState(
        shuffledRoundRobinLBStrategy);

    Assert.assertEquals(expectedUrls[0], firstURL);

    for (int i = 1; i < expectedUrls.length; i++) {
      Assert.assertEquals(expectedUrls[i],
          shuffledRoundRobinLBStrategy.getLbURL(mockedConnectionConfig));
    }
  }
  private String[] getShuffledURLsFromStateState(
      ShuffledRoundRobinLBStrategy shuffledRoundRobinLBStrategy)
      throws NoSuchFieldException, IllegalAccessException {
    Field fConfigToUrlListMap = ShuffledRoundRobinLBStrategy
        .class.getDeclaredField("configToUrlListMap");
    fConfigToUrlListMap.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, String[]> configToUrlListMap = (Map<String, String[]>) fConfigToUrlListMap
        .get(shuffledRoundRobinLBStrategy);
    Optional<String[]> oCachedUrls = configToUrlListMap.values().stream().findFirst();
    Assert.assertTrue(oCachedUrls.isPresent());
    return oCachedUrls.get();
  }
}
