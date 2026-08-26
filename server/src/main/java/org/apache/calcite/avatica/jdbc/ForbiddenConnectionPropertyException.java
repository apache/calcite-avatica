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
package org.apache.calcite.avatica.jdbc;

/**
 * Thrown when {@link JdbcMeta#openConnection} rejects a client-supplied
 * connection property because the server-side configuration forbids it.
 *
 * <p>Rejection is driven by
 * {@link JdbcMeta#CLIENT_PROPERTIES_DENYLIST_KEY} and
 * {@link JdbcMeta#CLIENT_PROPERTIES_ALLOWLIST_KEY}.
 */
public class ForbiddenConnectionPropertyException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Which rule rejected a property. */
  public enum Rule {
    DENYLIST(JdbcMeta.CLIENT_PROPERTIES_DENYLIST_KEY),
    ALLOWLIST(JdbcMeta.CLIENT_PROPERTIES_ALLOWLIST_KEY);

    private final String configKey;

    Rule(String configKey) {
      this.configKey = configKey;
    }

    /** The server-side configuration key that drives this rule. */
    public String configKey() {
      return configKey;
    }
  }

  private final String propertyName;
  private final Rule rule;

  public ForbiddenConnectionPropertyException(String propertyName, Rule rule) {
    super("Client-supplied connection property '" + propertyName
        + "' is forbidden by the server's "
        + rule.name().toLowerCase(java.util.Locale.ROOT)
        + " (" + rule.configKey() + ")");
    this.propertyName = propertyName;
    this.rule = rule;
  }

  /** The name of the client-supplied property that was rejected. */
  public String getPropertyName() {
    return propertyName;
  }

  /** Which rule rejected the property. */
  public Rule getRule() {
    return rule;
  }
}

// End ForbiddenConnectionPropertyException.java
