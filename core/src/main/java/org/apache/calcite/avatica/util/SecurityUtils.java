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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import javax.security.auth.Subject;

/**
 * This class is loosely based on SecurityUtils in Jetty 12.0
 *
 * <p>Collections of utility methods to deal with the scheduled removal
 * of the security classes defined by <a href="https://openjdk.org/jeps/411">JEP 411</a>.</p>
 */
public class SecurityUtils {
  private static final MethodHandle CALL_AS = lookupCallAs();
  private static final MethodHandle CURRENT = lookupCurrent();
  private static final MethodHandle DO_PRIVILEGED = lookupDoPrivileged();

  private SecurityUtils() {
  }

  private static MethodHandle lookupCallAs() {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      try {
        // Subject.doAs() is deprecated for removal and replaced by Subject.callAs().
        // Lookup first the new API, since for Java versions where both exist, the
        // new API delegates to the old API (for example Java 18, 19 and 20).
        // Otherwise (Java 17), lookup the old API.
        return lookup.findStatic(Subject.class, "callAs",
          MethodType.methodType(Object.class, Subject.class, Callable.class));
      } catch (NoSuchMethodException x) {
        try {
          // Lookup the old API.
          MethodType oldSignature =
              MethodType.methodType(Object.class, Subject.class, PrivilegedExceptionAction.class);
          MethodHandle doAs = lookup.findStatic(Subject.class, "doAs", oldSignature);
          // Convert the Callable used in the new API to the PrivilegedAction used in the old
          // API.
          MethodType convertSignature =
              MethodType.methodType(PrivilegedExceptionAction.class, Callable.class);
          MethodHandle converter =
              lookup.findStatic(SecurityUtils.class, "callableToPrivilegedExceptionAction",
                convertSignature);
          return MethodHandles.filterArguments(doAs, 1, converter);
        } catch (NoSuchMethodException e) {
          throw new AssertionError(e);
        }
      }
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  private static MethodHandle lookupDoPrivileged() {
    try {
      // Use reflection to work with Java versions that have and don't have AccessController.
      Class<?> klass =
          ClassLoader.getSystemClassLoader().loadClass("java.security.AccessController");
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      return lookup.findStatic(klass, "doPrivileged",
        MethodType.methodType(Object.class, PrivilegedAction.class));
    } catch (NoSuchMethodException | IllegalAccessException x) {
      // Assume that single methods won't be removed from AcessController
      throw new AssertionError(x);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private static MethodHandle lookupCurrent() {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      // Subject.getSubject(AccessControlContext) is deprecated for removal and replaced by
      // Subject.current().
      // Lookup first the new API, since for Java versions where both exists, the
      // new API delegates to the old API (for example Java 18, 19 and 20).
      // Otherwise (Java 17), lookup the old API.
      return lookup.findStatic(Subject.class, "current",
        MethodType.methodType(Subject.class));
    } catch (NoSuchMethodException e) {
      MethodHandle getContext = lookupGetContext();
      MethodHandle getSubject = lookupGetSubject();
      return MethodHandles.filterReturnValue(getContext, getSubject);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  private static MethodHandle lookupGetSubject() {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      Class<?> contextklass =
          ClassLoader.getSystemClassLoader()
              .loadClass("java.security.AccessControlContext");
      return lookup.findStatic(Subject.class, "getSubject",
        MethodType.methodType(Subject.class, contextklass));
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  private static MethodHandle lookupGetContext() {
    try {
      // Use reflection to work with Java versions that have and don't have AccessController.
      Class<?> controllerKlass =
          ClassLoader.getSystemClassLoader().loadClass("java.security.AccessController");
      Class<?> contextklass =
          ClassLoader.getSystemClassLoader()
              .loadClass("java.security.AccessControlContext");

      MethodHandles.Lookup lookup = MethodHandles.lookup();
      return lookup.findStatic(controllerKlass, "getContext",
        MethodType.methodType(contextklass));
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Maps to AccessController#doPrivileged if available, otherwise returns action.run().
   * @param action the action to run
   * @return the result of running the action
   * @param <T> the type of the result
   */
  public static <T> T doPrivileged(PrivilegedAction<T> action) {
    // Keep this method short and inlineable.
    if (DO_PRIVILEGED == null) {
      return action.run();
    }
    return doPrivileged(DO_PRIVILEGED, action);
  }

  private static <T> T doPrivileged(MethodHandle doPrivileged, PrivilegedAction<T> action) {
    try {
      return (T) doPrivileged.invoke(action);
    } catch (Throwable t) {
      throw inferAndCast(t);
    }
  }

  /**
   * Maps to Subject.callAs() if available, otherwise maps to Subject.doAs()
   * @param subject the subject this action runs as
   * @param action the action to run
   * @return the result of the action
   * @param <T> the type of the result
   */
  public static <T> T callAs(Subject subject, Callable<T> action) {
    try {
      return (T) CALL_AS.invoke(subject, action);
    } catch (PrivilegedActionException e) {
      throw new CompletionException(e.getCause());
    } catch (Throwable t) {
      throw inferAndCast(t);
    }
  }

  /**
   * Maps to Subject.currect() is available, otherwise maps to Subject.getSubject()
   * </p>
   * @return the current subject
   */
  public static Subject currentSubject() {
    try {
      return (Subject) CURRENT.invoke();
    } catch (Throwable t) {
      throw inferAndCast(t);
    }
  }

  @SuppressWarnings("unused")
  private static <T> PrivilegedExceptionAction<T> callableToPrivilegedExceptionAction(
      Callable<T> callable) {
    return callable::call;
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> RuntimeException inferAndCast(Throwable e) throws E {
    throw (E) e;
  }
}
