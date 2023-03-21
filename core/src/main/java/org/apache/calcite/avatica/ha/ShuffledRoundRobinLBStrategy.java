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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Shuffled Round Robin strategy for client side load balancing.
 * It starts with some random position in a list of URLs, and then returns subsequent URL
 * in a RoundRobin manner.
 * It's implemented it as a singleton so that we can maintain state
 * i.e. which URL was last used from the list of URLs specified.
 */
public class ShuffledRoundRobinLBStrategy implements LBStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(ShuffledRoundRobinLBStrategy.class);

  public static final ShuffledRoundRobinLBStrategy INSTANCE = new ShuffledRoundRobinLBStrategy();
  private ShuffledRoundRobinLBStrategy() { }
  public static final String URL_SEPERATOR_CHAR = ",";

  Map<String, Integer> configToIndexServedMap = new HashMap<>();
  Map<String, String[]> configToUrlListMap = new HashMap<>();

  @Override
  public synchronized String getLbURL(ConnectionConfig config) {
    String key = getKey(config);
    String lbURLs = config.getLbURLs();
    if (!configToIndexServedMap.containsKey(key)) {
      configToIndexServedMap.put(key, 0);
      initialiseUrlList(key, lbURLs);
    }
    String[] urls = configToUrlListMap.get(key);
    int urlIndex = configToIndexServedMap.get(key);

    String url = urls[urlIndex];
    LOG.info("Selected URL:{}", url);
    urlIndex = (urlIndex + 1) % urls.length;
    configToIndexServedMap.put(key, urlIndex);
    return url;
  }

  private void initialiseUrlList(String key, String lbURLs) {
    String[] urls = lbURLs.split(URL_SEPERATOR_CHAR);
    List<String> list = Arrays.asList(urls);
    Collections.shuffle(list);
    urls = list.toArray(urls);
    configToUrlListMap.put(key, urls);
  }

  private static String getKey(ConnectionConfig config) {
    return config.getLbURLs();
  }
}
