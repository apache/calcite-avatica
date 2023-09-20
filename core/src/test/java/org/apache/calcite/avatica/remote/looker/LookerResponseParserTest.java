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
package org.apache.calcite.avatica.remote.looker;

import org.apache.calcite.avatica.ColumnMetaData;
import org.apache.calcite.avatica.ColumnMetaData.AvaticaType;
import org.apache.calcite.avatica.ColumnMetaData.Rep;
import org.apache.calcite.avatica.util.ByteString;
import org.apache.calcite.avatica.util.StructImpl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Array;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class LookerResponseParserTest {

  private static Map<Rep, Object> supportedRepValues;
  private static Map<Rep, Object> unsupportedRepValues;

  private static ObjectMapper mapper = new ObjectMapper();

  static {
    Map buildingMap = new HashMap<Rep, Object>();
    // Primitive types
    buildingMap.put(Rep.PRIMITIVE_BOOLEAN, true);
    buildingMap.put(Rep.PRIMITIVE_BYTE, (byte) 10);
    buildingMap.put(Rep.PRIMITIVE_SHORT, (short) 5);
    buildingMap.put(Rep.PRIMITIVE_INT, 100);
    buildingMap.put(Rep.PRIMITIVE_LONG, (long) 10000);
    buildingMap.put(Rep.PRIMITIVE_FLOAT, (float) 1.99);
    buildingMap.put(Rep.PRIMITIVE_DOUBLE, 1.99);
    // Non-Primitive types
    buildingMap.put(Rep.BOOLEAN, true);
    buildingMap.put(Rep.BYTE, new Byte((byte) 10));
    buildingMap.put(Rep.SHORT, new Short((short) 10));
    buildingMap.put(Rep.INTEGER, new Integer(100));
    buildingMap.put(Rep.LONG, new Long(10000));
    buildingMap.put(Rep.FLOAT, new Float(1.99));
    buildingMap.put(Rep.DOUBLE, new Double(1.99));
    buildingMap.put(Rep.STRING, "hello");
    buildingMap.put(Rep.NUMBER, new BigDecimal(1000000));
    // TODO: We shouldn't need to support OBJECT but MEASUREs are appearing as generic objects in
    //  the signature
    buildingMap.put(Rep.OBJECT, 1000);
    supportedRepValues = new HashMap(buildingMap);
    buildingMap.clear();

    // Unsupported datetime types
    buildingMap.put(Rep.JAVA_SQL_TIME, new Time(1000000));
    buildingMap.put(Rep.JAVA_SQL_DATE, new Date(1000000));
    buildingMap.put(Rep.JAVA_SQL_TIMESTAMP, new Timestamp(100000));
    buildingMap.put(Rep.JAVA_UTIL_DATE, new java.util.Date(1000000));
    // Unsupported object types
    buildingMap.put(Rep.ARRAY, new Array[]{});
    buildingMap.put(Rep.BYTE_STRING, new ByteString(new byte[]{'h', 'e', 'l', 'l', 'o'}));
    buildingMap.put(Rep.PRIMITIVE_CHAR, 'c');
    buildingMap.put(Rep.CHARACTER, new Character('c'));
    buildingMap.put(Rep.MULTISET, new ArrayList());
    buildingMap.put(Rep.STRUCT, new StructImpl(new ArrayList()));
    unsupportedRepValues = new HashMap(buildingMap);
    buildingMap.clear();
  }

  private JsonParser makeTestParserFromValue(Object value) throws IOException {
    String template = "{ \"value\": %s }";
    try {
      String valAsJson = mapper.writeValueAsString(value);
      String testInput = String.format(Locale.ROOT, template, valAsJson);

      // start input stream and move to `value`
      InputStream in = new ByteArrayInputStream(testInput.getBytes(Charset.defaultCharset()));
      JsonParser jp = new JsonFactory().createParser(in);
      jp.nextFieldName(); // move to "value:" key
      jp.nextValue(); // move to value itself

      return jp;
    } catch (IOException e) {
      throw e;
    }
  }

  private ColumnMetaData makeDummyMetadata(Rep rep) {
    // MEASUREs appear as Objects but typeId is the underlying data type (usually int or double)
    // See relevant TODO in LookerRemoteMeta#deserializeValue
    int typeId = rep == Rep.OBJECT ? 4 : rep.typeId;
    AvaticaType type = new AvaticaType(typeId, rep.name(), rep);

    return ColumnMetaData.dummy(type, false);
  }

  @Test
  public void deserializeValueTestingIsExhaustive() {
    HashMap allMap = new HashMap();
    allMap.putAll(supportedRepValues);
    allMap.putAll(unsupportedRepValues);

    Arrays.stream(Rep.values()).forEach(val -> assertNotNull(allMap.get(val)));
  }

  @Test
  public void deserializeValueThrowsErrorOnUnsupportedType() {
    unsupportedRepValues.forEach((rep, value) -> {
      try {
        JsonParser parser = makeTestParserFromValue(value);

        // should throw an IOException
        LookerResponseParser.deserializeValue(parser, makeDummyMetadata(rep));
        fail("Should have thrown an IOException!");

      } catch (IOException e) {
        assertThat(e.getMessage(), is("Unable to parse " + rep.name() + " from stream!"));
      }
    });
  }

  @Test
  public void deserializeValueWorksForSupportedTypes() {
    supportedRepValues.forEach((rep, value) -> {
      try {
        JsonParser parser = makeTestParserFromValue(value);
        Object deserializedValue = LookerResponseParser.deserializeValue(parser,
            makeDummyMetadata(rep));

        assertThat(value, is(equalTo(deserializedValue)));

      } catch (IOException e) {
        fail(e.getMessage());
      }
    });
  }

  @Test
  public void returnsNullIfValueIsNull() {
    try {
      JsonParser parser = makeTestParserFromValue(null);
      Object deserializedValue = LookerResponseParser.deserializeValue(parser,
          makeDummyMetadata(Rep.DOUBLE));

      assertNull(deserializedValue);

    } catch (IOException e) {
      fail("Should not throw an exception!");
    }
  }
}
