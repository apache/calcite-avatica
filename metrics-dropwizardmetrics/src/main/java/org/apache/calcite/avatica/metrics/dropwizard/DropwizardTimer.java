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
package org.apache.calcite.avatica.metrics.dropwizard;

import com.codahale.metrics.Timer;

/**
 * Dropwizard Metrics implementation of {@link org.apache.calcite.avatica.metrics.Timer}.
 */
public class DropwizardTimer implements org.apache.calcite.avatica.metrics.Timer {

  private final Timer timer;

  public DropwizardTimer(Timer timer) {
    if (timer == null) {
      throw new NullPointerException();
    }
    this.timer = timer;
  }

  @Override public DropwizardContext start() {
    return new DropwizardContext(timer.time());
  }

  /**
   * Dropwizard Metrics implementation of {@link org.apache.calcite.avatica.metrics.Timer.Context}
   */
  public class DropwizardContext implements org.apache.calcite.avatica.metrics.Timer.Context {
    private final com.codahale.metrics.Timer.Context context;

    public DropwizardContext(com.codahale.metrics.Timer.Context context) {
      if (context == null) {
        throw new NullPointerException();
      }
      this.context = context;
    }

    @Override public void close() {
      this.context.stop();
    }
  }
}

// End DropwizardTimer.java
