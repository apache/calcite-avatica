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
package org.apache.calcite.avatica.shadetest;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class ShadingTest {

  // Note that many of these files could be excluded.
  // This is for regression testing, and not the minimum file set.
  String[] allowedPathPatterns = { "^META-INF", "^org/apache/calcite/",  ".*\\.proto",
      "^org/slf4j/", "^org/publicsuffix/" };

  @Test
  public void validateShadedJar() throws Exception {

    String patternString = String.join("|", allowedPathPatterns);
    Pattern allowedNames = Pattern.compile(patternString);

    try (JarFile jar = new JarFile(getShadedJarFile())) {
      jar.stream()
          .filter(c -> !c.isDirectory())
          .forEach(c -> {
            assertTrue(c.getName() + " does not match allowed names",
                allowedNames.matcher(c.getName()).find());
          });
    }
  }

  private File getShadedJarFile() throws IOException {

    Path libPath = FileSystems.getDefault().getPath("").resolve("build/libs/").toAbsolutePath();

    PathMatcher matcher = FileSystems.getDefault()
        .getPathMatcher("glob:avatica-*-shadow.jar");

    Optional<Path> found = Files.list(libPath).filter(c -> matcher.matches(c.getFileName()))
        .findFirst();
    assertTrue("Could not find jar in " + libPath, found.isPresent());

    return found.get().toFile();
  }
}
