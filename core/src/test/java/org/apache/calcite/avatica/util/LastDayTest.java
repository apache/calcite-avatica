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
package org.apache.calcite.avatica.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.apache.calcite.avatica.util.DateTimeUtils.dateStringToUnixDate;
import static org.apache.calcite.avatica.util.DateTimeUtils.lastDay;
import static org.apache.calcite.avatica.util.DateTimeUtils.unixDateToString;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@code lastDay} methods in {@link DateTimeUtils}.
 */
@RunWith(Parameterized.class)
public class LastDayTest {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"2019-02-10", "2019-02-28"},
        {"2019-06-10", "2019-06-30"},
        {"2019-07-10", "2019-07-31"},
        {"2019-09-10", "2019-09-30"},
        {"2019-12-10", "2019-12-31"},
        {"9999-12-10", "9999-12-31"},
        {"1900-01-01", "1900-01-31"},
        {"1935-02-01", "1935-02-28"},
        {"1965-09-01", "1965-09-30"},
        {"1970-01-01", "1970-01-31"},
        {"2019-02-28", "2019-02-28"},
        {"2019-12-31", "2019-12-31"},
        {"2019-01-01", "2019-01-31"},
        {"2019-06-30", "2019-06-30"},
        {"2020-02-20", "2020-02-29"},
        {"2020-02-29", "2020-02-29"},
        {"9999-12-31", "9999-12-31"}
    });
  }


  private final String inputDate;
  private final String expectedDay;

  public LastDayTest(String inputDate, String expectedDay) {
    this.inputDate = inputDate;
    this.expectedDay = expectedDay;
  }

  @Test
  public void testLastDayFromDateReturnsExpectedDay() {
    int lastDayFromDate = lastDay(dateStringToUnixDate(inputDate));
    assertEquals(expectedDay, unixDateToString(lastDayFromDate));
  }

}
// End LastDayTest.java
