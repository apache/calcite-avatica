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
package org.apache.calcite.avatica.server;

import org.apache.calcite.avatica.util.SecurityUtils;

import java.security.PrivilegedAction;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
import javax.security.auth.Subject;

/**
 * Encapsulates creating the new Thread in a doPrivileged and a doAs call.
 * The doPrivilieged block is taken from Jetty, and prevents some classloader leak isses.
 *
 * Saving the subject, and creating the Thread in the inner doAs call works around
 * doPriviliged resetting the kerberos subject, which breaks SPNEGO authentication.
 *
 * Also sets the daemon flag and name for the Thread, as the QueuedThreadPool parameters are
 * not applied with a custom ThreadFactory.
 *
 * see https://www.ibm.com/docs/en/was-zos/8.5.5\\
 * ?topic=service-java-authentication-authorization-authorization
 *
 * Also according to Jetty, the leak the doPrivileged call works around was fixed in JDK 18,
 * and the doPriviliged block is no longer needed in JDK 18 and later.
 * However, as long as the Jetty default ThreadFactory calls doPrivileged, we must replace it,
 * regardless of the JDK version we run on. See https://github.com/jetty/jetty.project/issues/12430
 */
class SubjectPreservingPrivilegedThreadFactory implements ThreadFactory {

  /**
   * @param Runnable object for the thread
   * @return a new thread, protected from classloader pinning, but keeping the current Subject
   */
  @Override
  public Thread newThread(Runnable runnable) {
    Subject subject = SecurityUtils.currentSubject();
    return SecurityUtils.doPrivileged(new PrivilegedAction<Thread>() {
      @Override public Thread run() {
        return SecurityUtils.callAs(subject, new Callable<Thread>() {
          @Override public Thread call() {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("avatica_qtp" + hashCode() + "-" + thread.getId());
            thread.setContextClassLoader(getClass().getClassLoader());
            return thread;
          }
        });
      }
    });
  }
}

// End SubjectPreservingPrivilegedThreadFactory.java
