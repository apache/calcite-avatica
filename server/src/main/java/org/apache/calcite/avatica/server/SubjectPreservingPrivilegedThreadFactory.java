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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ThreadFactory;
import javax.security.auth.Subject;

/**
 * Encapsulates creating the new Thread in a doPrivileged and a doAs call.
 * The doPrivilieged block is taken from Jetty, and prevents some classloader leak isses.
 * Saving the subject, and creating the Thread in the inner doAs call works around
 * doPriviliged resetting the kerberos subject, which breaks SPNEGO authentication.
 *
 * Also sets the daemon flag and name for the Thread, as the QueuedThreadPool parameters are
 * not applied with a custom ThreadFactory.
 *
 * see https://www.ibm.com/docs/en/was-zos/8.5.5\\
 * ?topic=service-java-authentication-authorization-authorization
 */
class SubjectPreservingPrivilegedThreadFactory implements ThreadFactory {

  /**
   * @param Runnable object for the thread
   * @return a new thread, protected from classloader pinning, but keeping the current Subject
   */
  public Thread newThread(Runnable runnable) {
    Subject subject = Subject.getSubject(AccessController.getContext());
    return AccessController.doPrivileged(new PrivilegedAction<Thread>() {
      @Override public Thread run() {
        return Subject.doAs(subject, new PrivilegedAction<Thread>() {
          @Override public Thread run() {
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
