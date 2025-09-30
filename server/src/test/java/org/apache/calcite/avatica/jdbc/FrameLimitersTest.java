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

import org.junit.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link FrameLimiters}
 */
public class FrameLimitersTest {

  private static final ResultSet RESULT_SET = Mockito.mock(ResultSet.class);
  private static final Object[] ROW = {1L, "ONE"};

  @Test public void testUnlimited() {
    FrameLimiter unlimited = FrameLimiters.unlimited();
    assertEquals(Optional.empty(), unlimited.getRowCountLimit());
    FrameLimiter.Context limiterContext = unlimited.start(RESULT_SET);
    assertFalse(limiterContext.limitReached());
    limiterContext.addRow(ROW);
    assertFalse(limiterContext.limitReached());
  }

  @Test public void testRowCountLimited() {
    FrameLimiter rowCountLimited = FrameLimiters.rowCountLimited(2);
    assertEquals(Optional.of(2), rowCountLimited.getRowCountLimit());
    FrameLimiter.Context limiterContext = rowCountLimited.start(RESULT_SET);
    assertFalse(limiterContext.limitReached());
    limiterContext.addRow(ROW);
    assertFalse(limiterContext.limitReached());
    limiterContext.addRow(ROW);
    assertTrue(limiterContext.limitReached());
  }

  @Test public void testTimelimited() {
    TestClock testClock = new TestClock();
    FrameLimiter timeLimited = FrameLimiters.timeLimited(2, testClock);
    assertEquals(Optional.empty(), timeLimited.getRowCountLimit());
    FrameLimiter.Context limiterContext = timeLimited.start(RESULT_SET);

    assertFalse(limiterContext.limitReached());
    limiterContext.addRow(ROW);
    testClock.addMillis(2);
    assertFalse(limiterContext.limitReached());
    limiterContext.addRow(ROW);
    testClock.addMillis(1);
    assertTrue(limiterContext.limitReached());
  }

  @Test
  public void testCombined() {
    TestClock testClock = new TestClock();
    FrameLimiter timeLimited = FrameLimiters.timeLimited(2, testClock);
    FrameLimiter rowCountLimited = FrameLimiters.rowCountLimited(3);

    FrameLimiter combined = FrameLimiters.combined(timeLimited, rowCountLimited);
    assertEquals(Optional.of(3), combined.getRowCountLimit());

    // Test that the clock-based limiting works
    FrameLimiter.Context limiterContext = combined.start(RESULT_SET);
    assertFalse(limiterContext.limitReached());
    limiterContext.addRow(ROW);
    testClock.addMillis(2);
    assertFalse(limiterContext.limitReached());
    limiterContext.addRow(ROW);
    testClock.addMillis(1);
    assertTrue(limiterContext.limitReached());

    // Test that count-based limiting works
    limiterContext = rowCountLimited.start(RESULT_SET);
    assertFalse(limiterContext.limitReached());
    limiterContext.addRow(ROW);
    assertFalse(limiterContext.limitReached());
    limiterContext.addRow(ROW);
    assertFalse(limiterContext.limitReached());
    limiterContext.addRow(ROW);
    assertTrue(limiterContext.limitReached());
  }

  /**
   * Clock implementation that allows specifying the current time
   */
  private static class TestClock extends Clock {

    private Instant currentTime = Instant.now();

    @Override public ZoneId getZone() {
      throw new UnsupportedOperationException();
    }

    @Override public Clock withZone(ZoneId zone) {
      throw new UnsupportedOperationException();
    }

    void addMillis(long millis) {
      this.currentTime = currentTime.plusMillis(millis);
    }

    @Override public Instant instant() {
      return this.currentTime;
    }
  }
}
// End FrameLimitersTest.java
