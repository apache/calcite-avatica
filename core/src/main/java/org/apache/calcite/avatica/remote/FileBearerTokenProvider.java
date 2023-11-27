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
package org.apache.calcite.avatica.remote;

import org.apache.calcite.avatica.BuiltInConnectionProperty;
import org.apache.calcite.avatica.ConnectionConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class FileBearerTokenProvider implements BearerTokenProvider {

  private static final Logger LOG = LoggerFactory.getLogger(FileBearerTokenProvider.class);

  private final Map<String, String> tokenMap = new HashMap<>();
  private String filename;

  @Override
  public void init(ConnectionConfig config) throws IOException {
    filename = config.tokenFile();
    if (filename == null || filename.trim().isEmpty()) {
      throw new UnsupportedOperationException("Config option "
          + BuiltInConnectionProperty.TOKEN_FILE
          + " must be specified to use file based Token Provider");
    }

    reload();
    newFileChangeWatcher(filename).start();
  }

  @Override
  public synchronized String obtain(String username) {
    return tokenMap.get(username);
  }

  private synchronized void reload() throws FileNotFoundException {
    try (Scanner scanner = new Scanner(new File(filename), StandardCharsets.UTF_8.name())) {
      tokenMap.clear();
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line.isEmpty()) {
          LOG.warn("Skip empty line in: {}", filename);
          continue;
        }
        String[] parts = line.split(",");
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
          LOG.warn("Skip invalid line in {}: {}", filename, line);
          continue;
        }
        if (tokenMap.put(parts[0], parts[1]) != null) {
          LOG.warn("Multiple tokens, latest takes precedence for user: {}", parts[0]);
        }
      }
      LOG.info("OAuth Bearer tokens have been updated from file: {}", filename);
    } catch (FileNotFoundException e) {
      LOG.warn("File not found: {}", e.getMessage());
    }
  }

  private FileChangeWatcher newFileChangeWatcher(String fileLocation) throws
      IOException {
    if (fileLocation == null || fileLocation.isEmpty()) {
      return null;
    }
    final Path filePath = Paths.get(fileLocation).toAbsolutePath();
    Path parentPath = filePath.getParent();
    if (parentPath == null) {
      throw new IOException(
          "File path does not have a parent: " + filePath);
    }
    return new FileChangeWatcher(
        parentPath,
        watchEvent -> {
          handleWatchEvent(filePath, watchEvent);
        });
  }

  /**
   * Handler for watch events that let us know a file we may care about has changed on disk.
   *
   * @param filePath the path to the file we are watching for changes.
   * @param event    the WatchEvent.
   */
  private void handleWatchEvent(Path filePath, WatchEvent<?> event) {
    boolean shouldReload = false;
    Path dirPath = filePath.getParent();
    if (event.kind().equals(StandardWatchEventKinds.OVERFLOW)) {
      // If we get notified about possibly missed events,
      // reload the key store / trust store just to be sure.
      shouldReload = true;
    } else if (event.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY)
        || event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {
      Path eventFilePath = dirPath.resolve((Path) event.context());
      if (filePath.equals(eventFilePath)) {
        shouldReload = true;
      }
    }
    // Note: we don't care about delete events
    if (shouldReload) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Attempting to reload tokens from file after receiving watch event: "
            + event.kind() + " with context: " + event.context());
      }
      try {
        reload();
      } catch (FileNotFoundException e) {
        LOG.error("Error reloading tokens from file", e);
      }
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Ignoring watch event and keeping previous tokens. Event kind: "
            + event.kind() + " with context: " + event.context());
      }
    }
  }
}
