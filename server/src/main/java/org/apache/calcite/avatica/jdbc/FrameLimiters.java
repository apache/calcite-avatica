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

import org.apache.calcite.avatica.AvaticaUtils;

import java.sql.ResultSet;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Collection of factory methods for creation of {@link FrameLimiter} instances.
 */
class FrameLimiters {

  private FrameLimiters() {}

  /**
   * Returns an unlimited {@link FrameLimiter}, which will always provide the
   * full contents of a ResultSet
   */
  static FrameLimiter unlimited() {
    return resultSet -> new FrameLimiter.Context() {
      @Override public boolean limitReached() {
        return false;
      }

      @Override public void addRow(Object[] row) {
        AvaticaUtils.discard(row);
      }
    };
  }

  /**
   * Returns a FrameLimiter which limits the frame size to a given number of rows.
   *
   * @param maxRowCount maximum number of rows to be included in the frame
   */
  static FrameLimiter rowCountLimited(int maxRowCount) {
    return new FrameLimiter() {

      @Override public Optional<Integer> getRowCountLimit() {
        return Optional.of(maxRowCount);
      }

      @Override public Context start(ResultSet resultSet) {
        return new FrameLimiter.Context() {

          int rowCount = 0;

          @Override public boolean limitReached() {
            return rowCount >= maxRowCount;
          }

          @Override public void addRow(Object[] row) {
            rowCount++;
          }
        };
      }
    };
  }

  /**
   * Returns a FrameLimiter which will stop frame creation after a given amount of time.
   *
   * @param maxFrameMillis maximum number of milliseconds to be used while constructing the frame
   */
  static FrameLimiter timeLimited(long maxFrameMillis) {
    return timeLimited(maxFrameMillis, Clock.systemUTC());
  }

  /**
   * Same as {@link #timeLimited(long)}, but allows specifying a custom Clock (for testing)
   *
   * @param maxFrameMillis maximum number of milliseconds to be used while constructing the frame
   * @param clock clock to be used for measuring frame construction duration
   */
  static FrameLimiter timeLimited(long maxFrameMillis, Clock clock) {
    return resultSet -> new FrameLimiter.Context() {

      long timeLimitMillis = clock.millis() + maxFrameMillis;

      @Override public boolean limitReached() {
        return clock.millis() > timeLimitMillis;
      }

      @Override public void addRow(Object[] row) {
        AvaticaUtils.discard(row);
      }
    };
  }

  /**
   * Returns a FrameLimiter that is the combination of a sequency of underlying FrameLimiters.
   *
   * The returned FrameLimiter will return <tt>true</tt> for
   * {@link FrameLimiter.Context#limitReached()} once any one of the underlying FrameLimiters limits
   * have been reached.
   *
   * @param frameLimiters underlying limiters upon which the combined limiter is to be created.
   */
  static FrameLimiter combined(FrameLimiter...frameLimiters) {
    if (frameLimiters.length == 0) {
      throw new IllegalArgumentException("No frame limiters supplied");
    } else if (frameLimiters.length == 1) {
      return frameLimiters[0];
    } else {
      List<FrameLimiter> frameLimiterList = Arrays.asList(frameLimiters);
      return new FrameLimiter() {
        @Override public Optional<Integer> getRowCountLimit() {
          return frameLimiterList.stream()
              .filter(limiter -> limiter.getRowCountLimit().isPresent())
              .findFirst().flatMap(FrameLimiter::getRowCountLimit);
        }

        @Override public Context start(ResultSet resultSet) {

          List<Context> contexts = frameLimiterList.stream()
              .map(frameLimiter -> frameLimiter.start(resultSet))
              .collect(Collectors.toList());

          return new Context() {
            @Override public boolean limitReached() {
              return contexts.stream().anyMatch(Context::limitReached);
            }

            @Override public void addRow(Object[] row) {
              contexts.forEach(c -> c.addRow(row));
            }
          };
        }
      };
    }
  }
}
// End FrameLimiters.java
