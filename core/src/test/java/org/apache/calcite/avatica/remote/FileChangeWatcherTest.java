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

import org.apache.calcite.avatica.util.Unsafe;

import org.apache.commons.io.FileUtils;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This file has been copied from the Apache ZooKeeper project.
 * "https://github.com/apache/zookeeper/blob/c42c8c94085ed1d94a22158fbdfe2945118a82bc
 *  /zookeeper-server/src/test/java/org/apache/zookeeper/common/FileChangeWatcherTest.java"
 */
public class FileChangeWatcherTest {
  private static File tempDir;
  private static File tempFile;

  private static final Logger LOG = LoggerFactory.getLogger(FileChangeWatcherTest.class);

  @BeforeClass public static void createTempFile() throws IOException {
    tempDir = Files.createTempDirectory("jwt_test_",
        PosixFilePermissions.asFileAttribute(
            PosixFilePermissions.fromString("rwx------"))).toFile();
    tempDir.deleteOnExit();
    tempFile = File.createTempFile("jwt_testfile_", "", tempDir);
    tempFile.deleteOnExit();
  }

  @Test public void testCallbackWorksOnFileChanges() throws IOException, InterruptedException {
    FileChangeWatcher watcher = null;
    try {
      final List<WatchEvent<?>> events = new ArrayList<>();
      watcher = new FileChangeWatcher(tempDir.toPath(), event -> {
        LOG.info("Got an update: " + event.kind() + " " + event.context());
        // Filter out the extra ENTRY_CREATE events that are
        // sometimes seen at the start. Even though we create the watcher
        // after the file exists, sometimes we still get a create event.
        if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
          return;
        }
        synchronized (events) {
          events.add(event);
          Unsafe.notifyAll(events);
        }
      });
      watcher.start();
      watcher.waitForState(FileChangeWatcher.State.RUNNING);
      Thread.sleep(1000L); // XXX hack
      for (int i = 0; i < 3; i++) {
        LOG.info("Modifying file, attempt " + (i + 1));
        FileUtils.writeStringToFile(tempFile, "Hello world " + i + "\n", StandardCharsets.UTF_8,
            true);
        synchronized (events) {
          if (events.size() < i + 1) {
            Unsafe.wait(events, 3000L);
          }
          assertEquals("Wrong number of events", i + 1, events.size());
          WatchEvent<?> event = events.get(i);
          assertEquals(StandardWatchEventKinds.ENTRY_MODIFY, event.kind());
          assertEquals(tempFile.getName(), event.context().toString());
        }
      }
    } finally {
      if (watcher != null) {
        watcher.stop();
        watcher.waitForState(FileChangeWatcher.State.STOPPED);
      }
    }
  }

  @Test public void testCallbackWorksOnFileTouched() throws IOException, InterruptedException {
    FileChangeWatcher watcher = null;
    try {
      final List<WatchEvent<?>> events = new ArrayList<>();
      watcher = new FileChangeWatcher(tempDir.toPath(), event -> {
        LOG.info("Got an update: " + event.kind() + " " + event.context());
        // Filter out the extra ENTRY_CREATE events that are
        // sometimes seen at the start. Even though we create the watcher
        // after the file exists, sometimes we still get a create event.
        if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
          return;
        }
        synchronized (events) {
          events.add(event);
          Unsafe.notifyAll(events);
        }
      });
      watcher.start();
      watcher.waitForState(FileChangeWatcher.State.RUNNING);
      Thread.sleep(1000L); // XXX hack
      LOG.info("Touching file");
      FileUtils.touch(tempFile);
      synchronized (events) {
        if (events.isEmpty()) {
          Unsafe.wait(events, 3000L);
        }
        assertFalse(events.isEmpty());
        WatchEvent<?> event = events.get(0);
        assertEquals(StandardWatchEventKinds.ENTRY_MODIFY, event.kind());
        assertEquals(tempFile.getName(), event.context().toString());
      }
    } finally {
      if (watcher != null) {
        watcher.stop();
        watcher.waitForState(FileChangeWatcher.State.STOPPED);
      }
    }
  }

  @Test public void testCallbackWorksOnFileAdded() throws IOException, InterruptedException {
    FileChangeWatcher watcher = null;
    try {
      final List<WatchEvent<?>> events = new ArrayList<>();
      watcher = new FileChangeWatcher(tempDir.toPath(), event -> {
        LOG.info("Got an update: " + event.kind() + " " + event.context());
        synchronized (events) {
          events.add(event);
          Unsafe.notifyAll(events);
        }
      });
      watcher.start();
      watcher.waitForState(FileChangeWatcher.State.RUNNING);
      Thread.sleep(1000L); // XXX hack
      File tempFile2 = File.createTempFile("jwt_testfile_", "", tempDir);
      tempFile2.deleteOnExit();
      synchronized (events) {
        if (events.isEmpty()) {
          Unsafe.wait(events, 3000L);
        }
        assertFalse(events.isEmpty());
        WatchEvent<?> event = events.get(0);
        assertEquals(StandardWatchEventKinds.ENTRY_CREATE, event.kind());
        assertEquals(tempFile2.getName(), event.context().toString());
      }
    } finally {
      if (watcher != null) {
        watcher.stop();
        watcher.waitForState(FileChangeWatcher.State.STOPPED);
      }
    }
  }

  @Test public void testCallbackWorksOnFileDeleted() throws IOException, InterruptedException {
    FileChangeWatcher watcher = null;
    try {
      final List<WatchEvent<?>> events = new ArrayList<>();
      watcher = new FileChangeWatcher(tempDir.toPath(), event -> {
        LOG.info("Got an update: " + event.kind() + " " + event.context());
        // Filter out the extra ENTRY_CREATE events that are
        // sometimes seen at the start. Even though we create the watcher
        // after the file exists, sometimes we still get a create event.
        if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
          return;
        }
        synchronized (events) {
          events.add(event);
          Unsafe.notifyAll(events);
        }
      });
      watcher.start();
      watcher.waitForState(FileChangeWatcher.State.RUNNING);
      Thread.sleep(1000L); // XXX hack
      tempFile.delete();
      synchronized (events) {
        if (events.isEmpty()) {
          Unsafe.wait(events, 3000L);
        }
        assertFalse(events.isEmpty());
        WatchEvent<?> event = events.get(0);
        assertEquals(StandardWatchEventKinds.ENTRY_DELETE, event.kind());
        assertEquals(tempFile.getName(), event.context().toString());
      }
    } finally {
      if (watcher != null) {
        watcher.stop();
        watcher.waitForState(FileChangeWatcher.State.STOPPED);
      }
    }
  }

  @Test public void testCallbackErrorDoesNotCrashWatcherThread()
      throws IOException, InterruptedException {
    FileChangeWatcher watcher = null;
    try {
      final AtomicInteger callCount = new AtomicInteger(0);
      watcher = new FileChangeWatcher(tempDir.toPath(), event -> {
        LOG.info("Got an update: " + event.kind() + " " + event.context());
        int oldValue;
        synchronized (callCount) {
          oldValue = callCount.getAndIncrement();
          Unsafe.notifyAll(callCount);
        }
        if (oldValue == 0) {
          throw new RuntimeException("This error should not crash the watcher thread");
        }
      });
      watcher.start();
      watcher.waitForState(FileChangeWatcher.State.RUNNING);
      Thread.sleep(1000L); // XXX hack
      LOG.info("Modifying file");
      FileUtils.writeStringToFile(tempFile, "Hello world\n", StandardCharsets.UTF_8, true);
      synchronized (callCount) {
        while (callCount.get() == 0) {
          Unsafe.wait(callCount, 3000L);
        }
      }
      LOG.info("Modifying file again");
      FileUtils.writeStringToFile(tempFile, "Hello world again\n", StandardCharsets.UTF_8, true);
      synchronized (callCount) {
        if (callCount.get() == 1) {
          Unsafe.wait(callCount, 3000L);
        }
      }
      // The value of callCount can exceed 1 only if the callback thread
      // survives the exception thrown by the first callback.
      assertTrue(callCount.get() > 1);
    } finally {
      if (watcher != null) {
        watcher.stop();
        watcher.waitForState(FileChangeWatcher.State.STOPPED);
      }
    }
  }
}
