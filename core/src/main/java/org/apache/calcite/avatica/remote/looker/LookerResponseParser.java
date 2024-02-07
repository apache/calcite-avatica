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
import org.apache.calcite.avatica.ColumnMetaData.Rep;
import org.apache.calcite.avatica.Meta.Frame;
import org.apache.calcite.avatica.Meta.Signature;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.NumberInput;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static com.fasterxml.jackson.core.JsonToken.VALUE_NULL;

public class LookerResponseParser {

  /**
   * Constants used in JSON parsing
   */
  private static final String ROWS_KEY = "rows";
  private static final String VALUE_KEY = "value";

  private final BlockingQueue<LookerFrameEnvelope> queue;

  public LookerResponseParser(BlockingQueue<LookerFrameEnvelope> queue) {
    assert queue != null : "null queue!";

    this.queue = queue;
  }

  /**
   * Calls the correct method to read the current value on the stream. The {@code get} methods do
   * not advance the current token, so they can be called multiple times without changing the state
   * of the parser.
   *
   * @param columnMetaData the {@link ColumnMetaData} for this value. It is important to use the
   *     {@link Rep} rather than the type name since Avatica represents most datetime values as
   *     milliseconds since epoch via {@code long}s or {@code int}s.
   * @param parser a JsonParser whose current token is a value from the JSON response. Callers
   *     must ensure that the parser is ready to consume a value token. This method does not change
   *     the state of the parser.
   * @return the parsed value.
   */
  static Object deserializeValue(JsonParser parser, ColumnMetaData columnMetaData)
      throws IOException {
    // don't attempt to parse null values
    if (parser.currentToken() == VALUE_NULL) {
      return null;
    }
    switch (columnMetaData.type.rep) {
    case PRIMITIVE_BOOLEAN:
    case BOOLEAN:
      return parser.getValueAsBoolean();
    case PRIMITIVE_BYTE:
    case BYTE:
      return parser.getByteValue();
    case STRING:
      return parser.getValueAsString();
    case PRIMITIVE_SHORT:
    case SHORT:
      return parser.getShortValue();
    case PRIMITIVE_INT:
    case INTEGER:
      return parser.getValueAsInt();
    case PRIMITIVE_LONG:
    case LONG:
      return parser.getValueAsLong();
    case PRIMITIVE_FLOAT:
    case FLOAT:
      return parser.getFloatValue();
    case PRIMITIVE_DOUBLE:
    case DOUBLE:
      return parser.getValueAsDouble();
    case NUMBER:
      // NUMBER is represented as BigDecimal
      if (parser.currentToken() == JsonToken.VALUE_STRING) {
        return NumberInput.parseBigDecimal(parser.getValueAsString());
      } else {
        return parser.getDecimalValue();
      }
    // TODO: MEASURE types are appearing as Objects. This should have been solved by CALCITE-5549.
    //  Be sure that the signature of a prepared query matches the metadata we see from JDBC.
    case OBJECT:
      switch (columnMetaData.type.id) {
      case Types.INTEGER:
        return parser.getIntValue();
      case Types.BIGINT:
        return parser.getBigIntegerValue();
      case Types.DOUBLE:
        return parser.getDoubleValue();
      case Types.DECIMAL:
      case Types.NUMERIC:
        return parser.getDecimalValue();
      }
    default:
      throw new IOException("Unable to parse " + columnMetaData.type.rep + " from stream!");
    }
  }

  /**
   * Collects the values of an array whose elements are represented by {@code metaData.type.rep}.
   * Does not support nested arrays.
   *
   * @param metaData the {@link ColumnMetaData} for this value.
   * @param parser a JsonParser whose current token is JsonToken.START_ARRAY. The parser is advanced
   *        through the elements until the end of the array is reached. Parse does not advance past
   *        END_ARRAY.
   * @return An array of values with element type matching {@code metaData.type.rep}.
   */
  static Object[] deserializeArray(JsonParser parser, ColumnMetaData metaData) throws IOException {
    assert parser.currentToken() == JsonToken.START_ARRAY
        : "Invalid parsing state. Expecting start of array.";

    boolean isPrimitive = isPrimitive(metaData);
    ArrayList result = new ArrayList();
    while (parser.nextToken() != JsonToken.END_ARRAY) {
      Object deserialized = deserializeValue(parser, metaData);
      if (isPrimitive && deserialized == null) {
        throw new IOException("Primitive array cannot contain null values");
      }
      result.add(deserialized);
    }
    return result.toArray();
  }

  static boolean isPrimitive(ColumnMetaData metaData) {
    switch (metaData.type.rep) {
    case PRIMITIVE_BOOLEAN:
    case PRIMITIVE_BYTE:
    case PRIMITIVE_CHAR:
    case PRIMITIVE_SHORT:
    case PRIMITIVE_INT:
    case PRIMITIVE_LONG:
    case PRIMITIVE_FLOAT:
    case PRIMITIVE_DOUBLE:
      return true;
    }
    return false;
  }

  private void seekToRows(JsonParser parser) throws IOException {
    while (parser.nextToken() != null && !ROWS_KEY.equals(parser.currentName())) {
      // move position to start of `rows`
    }
  }

  private void seekToValue(JsonParser parser) throws IOException {
    while (parser.nextToken() != null && !VALUE_KEY.equals(parser.currentName())) {
      // seeking to `value` key for the field e.g. `"rows": [{"field_1": {"value": 123 }}]
    }
    // now move to the actual value
    parser.nextToken();
  }

  private void putExceptionOrFail(Exception e) {
    try {
      // `put` blocks until there is room on the queue but needs a catch
      queue.put(LookerFrameEnvelope.error(e));
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Takes an input stream from a Looker query request and parses it into a series of
   * {@link LookerFrameEnvelope}s. Each LookerFrameEnvelope is enqueued in the {@link #queue} of the parser.
   *
   * @param in the {@link InputStream} to parse.
   * @param signature the {@link Signature} for the statement. Needed to access column metadata
   * @param fetchSize the number of rows to populate per {@link Frame}
   */
  public void parseStream(InputStream in, Signature signature, int fetchSize) {
    assert in != null : "null InputStream!";

    try {

      JsonParser parser = new JsonFactory().createParser(in);

      int currentOffset = 0;

      while (parser.nextToken() != null) {
        if (currentOffset == 0) {
          // TODO: Handle `metadata`. We are currently ignoring it and seeking to `rows` array
          seekToRows(parser);
        }

        int rowsRead = 0;
        List<Object> rows = new ArrayList<>();

        while (rowsRead < fetchSize) {
          List<Object> columnValues = new ArrayList<>();

          for (int i = 0; i < signature.columns.size(); i++) {
            seekToValue(parser);

            if (parser.isClosed()) {
              // the stream is closed - all rows should be accounted for
              currentOffset += rowsRead;
              queue.put(LookerFrameEnvelope.ok(currentOffset, /*done=*/true, rows));
              return;
            }

            ColumnMetaData metaData = signature.columns.get(i);
            if (parser.currentToken() == JsonToken.START_ARRAY) {
              columnValues.add(deserializeArray(parser, metaData));
            } else {
              // add the value to the column list
              columnValues.add(deserializeValue(parser, metaData));
            }
          }

          // Meta.CursorFactory#deduce will select an OBJECT cursor if there is only a single
          // column in the signature. This is intended behavior. Since the rows of a frame are
          // simply an `Object` we could get illegal casts from an ArrayList to the underlying Rep
          // type. Discard the wrapping ArrayList for the single value to avoid this issue.
          Object row = signature.columns.size() == 1 ? columnValues.get(0) : columnValues;

          rows.add(row);
          rowsRead++;
        }
        // we fetched the allowed number of rows so add the complete frame to the queue
        currentOffset += rowsRead;
        queue.put(LookerFrameEnvelope.ok(currentOffset, /*done=*/false, rows));
      }
    } catch (Exception e) {
      // enqueue the first exception encountered
      putExceptionOrFail(e);
    }
  }
}
