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
import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import javax.security.auth.Subject;

/**
 * This class is heavily based on SecurityUtils in Jetty 12.0
 *
 * <p>Collections of utility methods to deal with the scheduled removal
 * of the security classes defined by <a href="https://openjdk.org/jeps/411">JEP 411</a>.</p>
 */
public class SecurityUtils {
  private static final MethodHandle CALL_AS = lookupCallAs();
  private static final MethodHandle CURRENT = lookupCurrent();
  private static final MethodHandle DO_PRIVILEGED = lookupDoPrivileged();
  private static final MethodHandle GET_SUBJECT = lookupGetSubject();
  private static final MethodHandle GET_CONTEXT = lookupGetContext();

  private static MethodHandle lookupCallAs() {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      // Subject.doAs() is deprecated for removal and replaced by Subject.callAs().
      // Lookup first the new API, since for Java versions where both exist, the
      // new API delegates to the old API (for example Java 18, 19 and 20).
      // Otherwise (Java 17), lookup the old API.
      return lookup.findStatic(Subject.class, "callAs",
        MethodType.methodType(Object.class, Subject.class, Callable.class));
    } catch (Throwable x) {
      try {
        // Lookup the old API.
        MethodType oldSignature =
            MethodType.methodType(Object.class, Subject.class, PrivilegedAction.class);
        MethodHandle doAs = lookup.findStatic(Subject.class, "doAs", oldSignature);
        // Convert the Callable used in the new API to the PrivilegedAction used in the old
        // API.
        MethodType convertSignature =
            MethodType.methodType(PrivilegedAction.class, Callable.class);
        MethodHandle converter =
            lookup.findStatic(SecurityUtils.class, "callableToPrivilegedAction",
              convertSignature);
        return MethodHandles.filterArguments(doAs, 1, converter);
      } catch (Throwable t) {
        return null;
      }
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
    } catch (Throwable x) {
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
    } catch (Throwable x) {
      try {
        // This is a bit awkward, but the code is more symmetrical this way
        return lookup.findStatic(SecurityUtils.class, "getSubjectFallback",
          MethodType.methodType(Subject.class));
      } catch (Throwable t) {
        return null;
      }
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
    } catch (Throwable t) {
      return null;
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
    } catch (Throwable x) {
      return null;
    }
  }

  /**
   * Get the current security manager, if available.
   * @return the current security manager, if available, null otherwise
   */
  public static Object getSecurityManager() {
    try {
      // Use reflection to work with Java versions that have and don't have SecurityManager.
      return System.class.getMethod("getSecurityManager").invoke(null);
    } catch (InvocationTargetException x) {
      return unwrapInvocationTargetException(x);
    } catch (Throwable ignored) {
      return null;
    }
  }

  /**
   * <p>
   * Checks the given permission, if the {@link #getSecurityManager() security manager} is set.
   * </p>
   * @param permission the permission to check
   * @throws SecurityException if the permission check fails
   */
  public static void checkPermission(Permission permission) throws SecurityException {
    Object securityManager = SecurityUtils.getSecurityManager();
    if (securityManager == null) {
      return;
    }
    try {
      securityManager.getClass().getMethod("checkPermission").invoke(securityManager,
          permission);
    } catch (InvocationTargetException x) {
      unwrapInvocationTargetException(x);
    } catch (Throwable ignored) {
    }
  }

  /**
   * <p>
   * Runs the given action with the calling context restricted to just the calling frame, not all
   * the frames in the stack.
   * </p>
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
    } catch (InvocationTargetException x) {
      return unwrapInvocationTargetException(x);
    } catch (Throwable x) {
      throw new RuntimeException(x);
    }
  }

  /**
   * <p>
   * Runs the given action as the given subject.
   * </p>
   * @param subject the subject this action runs as
   * @param action the action to run
   * @return the result of the action
   * @param <T> the type of the result
   */
  public static <T> T callAs(Subject subject, Callable<T> action) {
    try {
      if (CALL_AS == null) {
        throw new RuntimeException(
            "Was unable to run either of Subject.callAs() or Subject.doAs()");
      }
      return (T) CALL_AS.invoke(subject, action);
    } catch (InvocationTargetException x) {
      return unwrapInvocationTargetException(x);
    } catch (Throwable x) {
      throw new CompletionException(x);
    }
  }

  /**
   * <p>
   * Gets the current subject
   * </p>
   * @return the current subject
   */
  public static Subject currentSubject() {
    if (CURRENT == null) {
      throw new RuntimeException(
          "Was unable to run either of Subject.current() or Subject.getSubject()");
    }
    try {
      MethodHandle methodHandle = CURRENT;
      return (Subject) methodHandle.invoke();
    } catch (InvocationTargetException x) {
      return unwrapInvocationTargetException(x);
    } catch (Throwable x) {
      throw new RuntimeException("Error while trying to get the current user", x);
    }

  }

  @SuppressWarnings("unused")
  private static <T> PrivilegedAction<T> callableToPrivilegedAction(Callable<T> callable) {
    return () -> {
      try {
        return callable.call();
      } catch (InvocationTargetException x) {
        return unwrapInvocationTargetException(x);
      } catch (RuntimeException | Error x) {
        throw x;
      } catch (Throwable x) {
        throw new RuntimeException(x);
      }
    };
  }

  @SuppressWarnings("unused")
  private static Subject getSubjectFallback() {
    try {
      Object context = GET_CONTEXT.invoke();
      return (Subject) GET_SUBJECT.invoke(context);
    } catch (InvocationTargetException x) {
      return unwrapInvocationTargetException(x);
    } catch (Throwable x) {
      throw new RuntimeException("Error trying to get the current Subject", x);
    }
  }

  private static <T> T unwrapInvocationTargetException(InvocationTargetException x) {
    Throwable cause = x.getCause();
    if (cause == null) {
      throw new AssertionError("InvocationTargetException has null cause", x);
    } else if (cause instanceof RuntimeException) {
      throw (RuntimeException) cause;
    } else if (cause instanceof Error) {
      throw (Error) cause;
    } else {
      // methods invoked in this class only throw RuntimeExceptions
      throw new AssertionError("Unexpected exception", x);
    }
  }

  private SecurityUtils() {
  }
}
