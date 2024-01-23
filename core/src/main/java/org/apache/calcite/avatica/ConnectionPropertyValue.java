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
package org.apache.calcite.avatica;

public interface ConnectionPropertyValue {
  /**
   * Returns the string value of this property, or null if not specified and
   * no default.
   */
  String getString();

  /**
   * Returns the string value of this property, or null if not specified and
   * no default.
   */
  String getString(String defaultValue);

  /**
   * Returns the int value of this property. Throws if not set and no
   * default.
   */
  int getInt();

  /**
   * Returns the int value of this property. Throws if not set and no
   * default.
   */
  int getInt(Number defaultValue);

  /**
   * Returns the long value of this property. Throws if not set and no
   * default.
   */
  long getLong();

  /**
   * Returns the long value of this property. Throws if not set and no
   * default.
   */
  long getLong(Number defaultValue);

  /**
   * Returns the double value of this property. Throws if not set and no
   * default.
   */
  double getDouble();

  /**
   * Returns the double value of this property. Throws if not set and no
   * default.
   */
  double getDouble(Number defaultValue);

  /**
   * Returns the boolean value of this property. Throws if not set and no
   * default.
   */
  boolean getBoolean();

  /**
   * Returns the boolean value of this property. Throws if not set and no
   * default.
   */
  boolean getBoolean(boolean defaultValue);

  /**
   * Returns the enum value of this property. Throws if not set and no
   * default.
   */
  <E extends Enum<E>> E getEnum(Class<E> enumClass);

  /**
   * Returns the enum value of this property. Throws if not set and no
   * default.
   */
  <E extends Enum<E>> E getEnum(Class<E> enumClass, E defaultValue);

  /**
   * Returns an instance of a plugin.
   *
   * <p>Throws if not set and no default.
   * Also throws if the class does not implement the required interface,
   * or if it does not have a public default constructor or an public static
   * field called {@code #INSTANCE}.
   */
  <T> T getPlugin(Class<T> pluginClass, T defaultInstance);

  /**
   * Returns an instance of a plugin, using a given class name if none is
   * set.
   *
   * <p>Throws if not set and no default.
   * Also throws if the class does not implement the required interface,
   * or if it does not have a public default constructor or an public static
   * field called {@code #INSTANCE}.
   */
  <T> T getPlugin(Class<T> pluginClass, String defaultClassName,
                  T defaultInstance);
}
