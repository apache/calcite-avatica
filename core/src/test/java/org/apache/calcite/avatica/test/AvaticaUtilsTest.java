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

import org.apache.calcite.avatica.AvaticaUtils;
import org.apache.calcite.avatica.ConnectionConfigImpl;
import org.apache.calcite.avatica.ConnectionProperty;
import org.apache.calcite.avatica.util.ByteString;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import static org.apache.calcite.avatica.AvaticaUtils.skipFully;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/**
 * Unit test for Avatica utilities.
 */
public class AvaticaUtilsTest {
  /** Used by {@link #testInstantiatePlugin()}. */
  public static final ThreadLocal<String> STRING_THREAD_LOCAL =
      ThreadLocal.withInitial(() -> "not initialized");

  /** Also used by {@link #testInstantiatePlugin()}. */
  public static final ThreadLocal<Float> FLOAT_THREAD_LOCAL =
      ThreadLocal.withInitial(() -> Float.MIN_VALUE);

  /** Tests {@link AvaticaUtils#instantiatePlugin(Class, String)}. */
  @Test public void testInstantiatePlugin() {
    final String s =
        AvaticaUtils.instantiatePlugin(String.class, "java.lang.String");
    assertThat(s, is(""));

    final BigInteger b =
        AvaticaUtils.instantiatePlugin(BigInteger.class, "java.math.BigInteger#ONE");
    assertThat(b, is(BigInteger.ONE));

    // Class not found
    try {
      final BigInteger b2 =
          AvaticaUtils.instantiatePlugin(BigInteger.class, "org.apache.calcite.Abc");
      fail("expected error, got " + b2);
    } catch (Throwable e) {
      assertThat(e.getMessage(),
          is("Property 'org.apache.calcite.Abc' not valid as "
              + "'org.apache.calcite.Abc' not found in the classpath"));
    }

    // No instance of ABC
    try {
      final String s2 = AvaticaUtils.instantiatePlugin(String.class, "java.lang.String#ABC");
      fail("expected error, got " + s2);
    } catch (Throwable e) {
      assertThat(e.getMessage(),
          is("Property 'java.lang.String#ABC' not valid as "
              + "there is no 'ABC' field in the class of 'java.lang.String'"));
    }

    // The instance type is not the plugin type
    try {
      final String s2 = AvaticaUtils.instantiatePlugin(String.class, "java.math.BigInteger#ONE");
      fail("expected error, got " + s2);
    } catch (Throwable e) {
      assertThat(e.getMessage(),
          is("Property 'java.math.BigInteger#ONE' not valid as "
              + "cannot convert java.math.BigInteger to java.lang.String"));
    }

    // No default constructor or INSTANCE member
    try {
      final Integer i =
          AvaticaUtils.instantiatePlugin(Integer.class, "java.lang.Integer");
      fail("expected error, got " + i);
    } catch (Throwable e) {
      assertThat(e.getMessage(),
          is("Property 'java.lang.Integer' not valid as the default constructor is necessary,"
              + " but not found in the class of 'java.lang.Integer'"));
    }

    // Not valid for plugin type
    try {
      final BigInteger b2 =
          AvaticaUtils.instantiatePlugin(BigInteger.class, "java.lang.String");
      fail("expected error, got " + b2);
    } catch (Throwable e) {
      assertThat(e.getMessage(),
          is("Property 'java.lang.String' not valid for plugin type java.math.BigInteger"));
    }

    // Read from thread-local
    try {
      STRING_THREAD_LOCAL.set("abc");
      final String s2 =
          AvaticaUtils.instantiatePlugin(String.class,
              AvaticaUtilsTest.class.getName() + "#STRING_THREAD_LOCAL");
      assertThat(s2, is("abc"));
    } finally {
      STRING_THREAD_LOCAL.remove();
    }

    // Read from thread-local, wrong type
    try {
      STRING_THREAD_LOCAL.set("abc");
      final Integer i =
          AvaticaUtils.instantiatePlugin(Integer.class,
              AvaticaUtilsTest.class.getName() + "#STRING_THREAD_LOCAL");
      fail("expected error, got " + i);
    } catch (Throwable e) {
      assertThat(e.getMessage(),
          is("Property 'org.apache.calcite.avatica.test.AvaticaUtilsTest"
              + "#STRING_THREAD_LOCAL' not valid as cannot convert java.lang.String "
              + "to java.lang.Integer"));
    } finally {
      STRING_THREAD_LOCAL.remove();
    }

    // Read from thread-local, wrong type (array type, because it's tricky to
    // print correctly).
    try {
      STRING_THREAD_LOCAL.set("abc");
      final BigDecimal[] bigDecimals =
          AvaticaUtils.instantiatePlugin(BigDecimal[].class,
              AvaticaUtilsTest.class.getName() + "#STRING_THREAD_LOCAL");
      fail("expected error, got " + Arrays.toString(bigDecimals));
    } catch (Throwable e) {
      assertThat(e.getMessage(),
          is("Property 'org.apache.calcite.avatica.test.AvaticaUtilsTest"
              + "#STRING_THREAD_LOCAL' not valid as cannot convert "
              + "java.lang.String to java.math.BigDecimal[]"));
    } finally {
      STRING_THREAD_LOCAL.remove();
    }

    // Read from thread-local, wrong type (private enum type, because it's
    // tricky to print correctly).
    try {
      STRING_THREAD_LOCAL.set("abc");
      final Weight weight =
          AvaticaUtils.instantiatePlugin(Weight.class,
              AvaticaUtilsTest.class.getName() + "#STRING_THREAD_LOCAL");
      fail("expected error, got " + weight);
    } catch (Throwable e) {
      assertThat(e.getMessage(),
          is("Property 'org.apache.calcite.avatica.test.AvaticaUtilsTest"
              + "#STRING_THREAD_LOCAL' not valid as cannot convert "
              + "java.lang.String to "
              + "org.apache.calcite.avatica.test.AvaticaUtilsTest.Weight"));
    } finally {
      STRING_THREAD_LOCAL.remove();
    }

    // Read from thread-local, wrong type (primitive type).
    try {
      STRING_THREAD_LOCAL.set("abc");
      final float f =
          AvaticaUtils.instantiatePlugin(float.class,
              AvaticaUtilsTest.class.getName() + "#STRING_THREAD_LOCAL");
      fail("expected error, got " + f);
    } catch (Throwable e) {
      assertThat(e.getMessage(),
          is("Property 'org.apache.calcite.avatica.test.AvaticaUtilsTest"
              + "#STRING_THREAD_LOCAL' not valid as cannot convert "
              + "java.lang.String to float"));
    } finally {
      STRING_THREAD_LOCAL.remove();
    }

    // Read from thread-local, primitive type.
    try {
      FLOAT_THREAD_LOCAL.set(2.5f);
      final float f =
          AvaticaUtils.instantiatePlugin(float.class,
              AvaticaUtilsTest.class.getName() + "#FLOAT_THREAD_LOCAL");
      fail("expected error, got " + f);
    } catch (Throwable e) {
      assertThat(e.getMessage(),
          is("Property 'org.apache.calcite.avatica.test.AvaticaUtilsTest"
              + "#FLOAT_THREAD_LOCAL' not valid as cannot convert "
              + "java.lang.Float to float"));
    } finally {
      FLOAT_THREAD_LOCAL.remove();
    }
  }

  /** Unit test for
   * {@link org.apache.calcite.avatica.AvaticaUtils#unique(java.lang.String)}. */
  @Test public void testUnique() {
    // Below, the "probably" comments indicate the strings that will be
    // generated the first time you run the test.
    final Set<String> list = new LinkedHashSet<>();
    list.add(AvaticaUtils.unique("a")); // probably "a"
    assertThat(list.size(), is(1));
    list.add(AvaticaUtils.unique("a")); // probably "a_1"
    assertThat(list.size(), is(2));
    list.add(AvaticaUtils.unique("b")); // probably "b"
    assertThat(list.size(), is(3));
    list.add(AvaticaUtils.unique("a_1")); // probably "a_1_3"
    assertThat(list.size(), is(4));
    list.add(AvaticaUtils.unique("A")); // probably "A"
    assertThat(list.size(), is(5));
    list.add(AvaticaUtils.unique("a")); // probably "a_5"
    assertThat(list.size(), is(6));
  }

  /** Tests connect string properties. */
  @Test public void testConnectionProperty() {
    final ConnectionPropertyImpl n = new ConnectionPropertyImpl("N",
        BigDecimal.valueOf(100), ConnectionProperty.Type.NUMBER);

    final Properties properties = new Properties();
    ConnectionConfigImpl.PropEnv env = n.wrap(properties);
    assertThat(env.getInt(), is(100));
    assertThat(env.getInt(-45), is(-45));
    properties.setProperty(n.name, "123");
    assertThat(env.getInt(), is(100));
    env = n.wrap(properties);
    assertThat(env.getInt(), is(123));
    assertThat(env.getInt(-45), is(123));

    properties.setProperty(n.name, "10k");
    env = n.wrap(properties);
    assertThat(env.getInt(), is(10 * 1024));

    properties.setProperty(n.name, "-0.5k");
    env = n.wrap(properties);
    assertThat(env.getInt(), is(-512));

    properties.setProperty(n.name, "10m");
    env = n.wrap(properties);
    assertThat(env.getInt(), is(10 * 1024 * 1024));
    assertThat(env.getLong(), is(10L * 1024 * 1024));
    assertThat(env.getDouble(), is(10D * 1024 * 1024));

    properties.setProperty(n.name, "-2M");
    env = n.wrap(properties);
    assertThat(env.getInt(), is(-2 * 1024 * 1024));

    properties.setProperty(n.name, "10g");
    env = n.wrap(properties);
    assertThat(env.getLong(), is(10L * 1024 * 1024 * 1024));

    final ConnectionPropertyImpl b = new ConnectionPropertyImpl("B",
        true, ConnectionProperty.Type.BOOLEAN);

    env = b.wrap(properties);
    assertThat(env.getBoolean(), is(true));
    assertThat(env.getBoolean(true), is(true));
    assertThat(env.getBoolean(false), is(false));

    properties.setProperty(b.name, "false");
    env = b.wrap(properties);
    assertThat(env.getBoolean(), is(false));

    final ConnectionPropertyImpl s = new ConnectionPropertyImpl("S",
        "foo", ConnectionProperty.Type.STRING);

    env = s.wrap(properties);
    assertThat(env.getString(), is("foo"));
    assertThat(env.getString("baz"), is("baz"));

    properties.setProperty(s.name, "  ");
    env = s.wrap(properties);
    assertThat(env.getString(), is("  "));

    try {
      final ConnectionPropertyImpl t =
          new ConnectionPropertyImpl("T", null, ConnectionProperty.Type.ENUM);
      fail("should throw if you specify an enum property without a class, got "
          + t);
    } catch (AssertionError e) {
      assertThat(e.getMessage(), is("must specify value class for an ENUM"));
      // ok
    }

    // An enum with a default value
    final ConnectionPropertyImpl t = new ConnectionPropertyImpl("T", Size.BIG,
        ConnectionProperty.Type.ENUM, Size.class);
    env = t.wrap(properties);
    assertThat(env.getEnum(Size.class), is(Size.BIG));
    assertThat(env.getEnum(Size.class, Size.SMALL), is(Size.SMALL));
    assertThat(env.getEnum(Size.class, null), nullValue());
    try {
      final Weight envEnum = env.getEnum(Weight.class, null);
      fail("expected error, got " + envEnum);
    } catch (AssertionError e) {
      assertThat(e.getMessage(),
          is("wrong value class; expected " + Size.class));
    }

    // An enum with a default value that is an anonymous enum,
    // not specifying value type.
    final ConnectionPropertyImpl v = new ConnectionPropertyImpl("V",
        Size.SMALL, ConnectionProperty.Type.ENUM);
    env = v.wrap(properties);
    assertThat(env.getEnum(Size.class), is(Size.SMALL));
    assertThat(env.getEnum(Size.class, Size.BIG), is(Size.BIG));
    assertThat(env.getEnum(Size.class, null), nullValue());
    try {
      final Weight envEnum = env.getEnum(Weight.class, null);
      fail("expected error, got " + envEnum);
    } catch (AssertionError e) {
      assertThat(e.getMessage(),
          is("wrong value class; expected " + Size.class));
    }

    // An enum with no default value
    final ConnectionPropertyImpl u = new ConnectionPropertyImpl("U", null,
        ConnectionProperty.Type.ENUM, Size.class);
    env = u.wrap(properties);
    assertThat(env.getEnum(Size.class), nullValue());
    assertThat(env.getEnum(Size.class, Size.SMALL), is(Size.SMALL));
    assertThat(env.getEnum(Size.class, null), nullValue());
    try {
      final Weight envEnum = env.getEnum(Weight.class, null);
      fail("expected error, got " + envEnum);
    } catch (AssertionError e) {
      assertThat(e.getMessage(),
          is("wrong value class; expected " + Size.class));
    }
  }

  @Test public void testLongToIntegerTranslation() {
    long[] longValues = new long[] {Integer.MIN_VALUE, -5, 0, 1, Integer.MAX_VALUE,
        ((long) Integer.MAX_VALUE) + 1L, Long.MAX_VALUE};
    int[] convertedValues = AvaticaUtils.toSaturatedInts(longValues);
    int[] intValues = new int[] {Integer.MIN_VALUE, -5, 0, 1, Integer.MAX_VALUE,
        Integer.MAX_VALUE, Integer.MAX_VALUE};
    assertArrayEquals(convertedValues, intValues);
  }

  @Test public void testByteString() {
    final byte[] bytes = {3, 14, 15, 92, 0, 65, 35, 0};
    final ByteString s = new ByteString(bytes);
    final ByteString s2 = new ByteString(bytes.clone());
    final ByteString s3 = new ByteString(new byte[0]);
    final ByteString s4 = new ByteString(new byte[] {0});
    final ByteString s5 = new ByteString(new byte[]{15, 92});

    // length
    assertThat(s.length(), is(8));
    assertThat(s3.length(), is(0));
    assertThat(s4.length(), is(1));

    // equals and hashCode
    assertThat(s.hashCode(), is(s2.hashCode()));
    assertThat(s.equals(s2), is(true));
    assertThat(s2.equals(s), is(true));
    assertThat(s.equals(s3), is(false));
    assertThat(s3.equals(s), is(false));

    // toString
    assertThat(s.toString(), is("030e0f5c00412300"));
    assertThat(s3.toString(), is(""));
    assertThat(s4.toString(), is("00"));

    // indexOf
    assertThat(s.indexOf(s3), is(0));
    assertThat(s.indexOf(s3, 5), is(5));
    assertThat(s.indexOf(s3, 15), is(-1));
    assertThat(s.indexOf(s4), is(4));
    assertThat(s.indexOf(s4, 4), is(4));
    assertThat(s.indexOf(s4, 5), is(7));
    assertThat(s.indexOf(s5), is(2));
    assertThat(s.indexOf(s5, 2), is(2));
    assertThat(s.indexOf(s5, 3), is(-1));
    assertThat(s.indexOf(s5, 7), is(-1));

    // substring
    assertThat(s.substring(8), is(s3));
    assertThat(s.substring(7), is(s4));
    assertThat(s.substring(0), is(s));

    // getBytes
    assertThat(s.getBytes().length, is(8));
    assertThat(Arrays.equals(s.getBytes(), bytes), is(true));
    assertThat(s.getBytes()[3], is((byte) 92));
    final byte[] copyBytes = s.getBytes();
    copyBytes[3] = 11;
    assertThat(s.getBytes()[3], is((byte) 92));
    assertThat(s, is(s2));

    // startsWith
    assertThat(s.startsWith(s), is(true));
    assertThat(s.startsWith(s.substring(0, 1)), is(true));
    assertThat(s.startsWith(s.substring(0, 3)), is(true));
    assertThat(s.startsWith(s3), is(true)); // ""
    assertThat(s.startsWith(s4), is(false)); // "\0"
    assertThat(s3.startsWith(s3), is(true)); // "" starts with ""

    // startsWith offset 0
    assertThat(s.startsWith(s, 0), is(true));
    assertThat(s.startsWith(s.substring(0, 1), 0), is(true));
    assertThat(s.startsWith(s.substring(0, 3), 0), is(true));
    assertThat(s.startsWith(s3, 0), is(true)); // ""
    assertThat(s.startsWith(s4, 0), is(false)); // "\0"
    assertThat(s3.startsWith(s3, 0), is(true)); // "" starts with ""

    // startsWith, other offsets
    assertThat(s.startsWith(s, 1), is(false));
    assertThat(s.startsWith(s3, 1), is(true));
    assertThat(s.startsWith(s3, s.length() - 1), is(true));
    assertThat(s.startsWith(s3, s.length()), is(true));
    assertThat(s.startsWith(s3, s.length() + 1), is(false));

    // endsWith
    assertThat(reverse(s).endsWith(reverse(s)), is(true));
    assertThat(reverse(s).endsWith(reverse(s.substring(0, 1))), is(true));
    assertThat(reverse(s).endsWith(reverse(s.substring(0, 3))), is(true));
    assertThat(reverse(s).endsWith(reverse(s3)), is(true)); // ""
    assertThat(reverse(s).endsWith(reverse(s4)), is(false)); // "\0"
    assertThat(reverse(s3).endsWith(reverse(s3)), is(true));
  }

  private ByteString reverse(ByteString s) {
    final byte[] bytes = s.getBytes();
    for (int i = 0, j = bytes.length - 1; i < j; i++, j--) {
      byte b = bytes[i];
      bytes[i] = bytes[j];
      bytes[j] = b;
    }
    return new ByteString(bytes);
  }

  @Test public void testSkipFully() throws IOException {
    InputStream in = of("");
    assertEquals(0, in.available());
    skipFully(in);
    assertEquals(0, in.available());

    in = of("asdf");
    assertEquals(4, in.available());
    skipFully(in);
    assertEquals(0, in.available());

    in = of("asdfasdf");
    for (int i = 0; i < 4; i++) {
      assertNotEquals(-1, in.read());
    }
    assertEquals(4, in.available());
    skipFully(in);
    assertEquals(0, in.available());
  }

  /** Returns an InputStream of UTF-8 encoded bytes from the provided string */
  InputStream of(String str) {
    return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
  }

  /** Dummy implementation of {@link ConnectionProperty}. */
  private static class ConnectionPropertyImpl implements ConnectionProperty {
    private final String name;
    private final Object defaultValue;
    private final Class<?> valueClass;
    private Type type;

    ConnectionPropertyImpl(String name, Object defaultValue, Type type) {
      this(name, defaultValue, type, null);
    }

    ConnectionPropertyImpl(String name, Object defaultValue, Type type,
        Class valueClass) {
      this.name = name;
      this.defaultValue = defaultValue;
      this.type = type;
      this.valueClass = type.deduceValueClass(defaultValue, valueClass);
      if (!type.valid(defaultValue, this.valueClass)) {
        throw new AssertionError(name);
      }
    }

    public String name() {
      return name.toUpperCase(Locale.ROOT);
    }

    public String camelName() {
      return name.toLowerCase(Locale.ROOT);
    }

    public Object defaultValue() {
      return defaultValue;
    }

    public Type type() {
      return type;
    }

    public Class valueClass() {
      return valueClass;
    }

    public ConnectionConfigImpl.PropEnv wrap(Properties properties) {
      final HashMap<String, ConnectionProperty> map = new HashMap<>();
      map.put(name, this);
      return new ConnectionConfigImpl.PropEnv(
          ConnectionConfigImpl.parse(properties, map), this);
    }

    public boolean required() {
      return false;
    }
  }

  /** How large? */
  private enum Size {
    BIG,
    SMALL {
    }
  }

  /** How heavy? */
  private enum Weight {
    HEAVY, LIGHT
  }
}

// End AvaticaUtilsTest.java
