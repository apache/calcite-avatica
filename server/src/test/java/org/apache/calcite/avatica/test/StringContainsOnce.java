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
package org.apache.calcite.avatica.test;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.core.SubstringMatcher;

/**
 * Tests if the argument is a string that contains a substring exactly once.
 */
public class StringContainsOnce extends SubstringMatcher {
  public StringContainsOnce(String substring) {
    super(substring);
  }

  @Override public void describeMismatchSafely(String item, Description mismatchDescription) {
    int cnt = countMatches(item);
    if (cnt == 0) {
      mismatchDescription.appendText("pattern is not found in \"");
    } else if (cnt == 2) {
      mismatchDescription.appendText("pattern is present more than once in \"");
    }
    mismatchDescription.appendText(item);
    mismatchDescription.appendText("\"");
  }

  @Override protected boolean evalSubstringOf(String s) {
    return countMatches(s) == 1;
  }

  private int countMatches(String s) {
    int indexOf = s.indexOf(substring);
    if (indexOf < 0) {
      return 0;
    }
    // There should be just a single match
    return s.indexOf(substring, indexOf + 1) == -1 ? 1 : 2;
  }

  @Override protected String relationship() {
    return "containing exactly once";
  }

  /**
   * Creates a matcher that matches if the examined {@link String} contains the specified
   * {@link String} anywhere exactly once.
   *
   * <p>For example:
   * <pre>assertThat("myStringOfNote", containsStringOnce("ring"))</pre>
   * @param substring substring
   * @return matcher
   */
  @Factory
  public static Matcher<String> containsStringOnce(String substring) {
    return new StringContainsOnce(substring);
  }
}

// End StringContainsOnce.java
