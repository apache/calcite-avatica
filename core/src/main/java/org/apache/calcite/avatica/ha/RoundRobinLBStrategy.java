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

import java.util.HashMap;
import java.util.Map;

/**
 * Round Robin strategy for client side load balancing.
 * Its implemented it as a singleton so that we can maintain state
 * i.e. which URL was last used from the list of URLs specified.
 */
public class RoundRobinLBStrategy implements LBStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(RoundRobinLBStrategy.class);

  public static final RoundRobinLBStrategy INSTANCE = new RoundRobinLBStrategy();
  private RoundRobinLBStrategy() { }
  public static final String URL_SEPERATOR_CHAR = ",";

  Map<String, Integer>  configToIndexServedMap = new HashMap<>();
  Map<String, String[]> configToUrlListMap = new HashMap<>();

  @Override
  public synchronized String getLbURL(ConnectionConfig config) {
    String key = getKey(config);
    if (!configToIndexServedMap.containsKey(key)) {
      configToIndexServedMap.put(key, 0);
      configToUrlListMap.put(key, config.getLbURLs().split(URL_SEPERATOR_CHAR));
    }
    String[] urls = configToUrlListMap.get(key);
    int urlIndex = configToIndexServedMap.get(key);
    configToIndexServedMap.put(key, (urlIndex + 1) % urls.length);
    String url = urls[urlIndex];
    LOG.info("Selected URL:{}", url);
    return url;
  }
  private static String getKey(ConnectionConfig config) {
    return config.getLbURLs();
  }
}
