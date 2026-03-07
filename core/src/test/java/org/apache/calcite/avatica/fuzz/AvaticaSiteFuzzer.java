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
package org.apache.calcite.avatica.fuzz;

import org.apache.calcite.avatica.AvaticaParameter;
import org.apache.calcite.avatica.AvaticaSite;
import org.apache.calcite.avatica.remote.TypedValue;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fuzzer for AvaticaSite.
 */
public class AvaticaSiteFuzzer {

  private AvaticaSiteFuzzer() {
  }

  /**
   * Fuzzes AvaticaSite methods.
   *
   * @param data fuzzed data
   */
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // Construct dependencies for an AvaticaSite
      AvaticaParameter param = new AvaticaParameter(
          data.consumeBoolean(),
          data.consumeInt(),
          data.consumeInt(), // scale
          data.consumeInt(),
          data.consumeString(10), // typeName
          data.consumeString(10), // className
          data.consumeString(10)  // name
      );

      Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
      TypedValue[] slots = new TypedValue[1];

      // Target object
      AvaticaSite site = new AvaticaSite(param, calendar, 0, slots);

      // Determine what to fuzz
      int choice = data.consumeInt(1, 16);

      switch (choice) {
      case 1:
        site.setByte(data.consumeByte());
        break;
      case 2:
        site.setChar(data.consumeChar());
        break;
      case 3:
        site.setShort(data.consumeShort());
        break;
      case 4:
        site.setInt(data.consumeInt());
        break;
      case 5:
        site.setLong(data.consumeLong());
        break;
      case 6:
        site.setBoolean(data.consumeBoolean());
        break;
      case 7:
        site.setNString(data.consumeString(50));
        break;
      case 8:
        site.setFloat(data.consumeFloat());
        break;
      case 9:
        site.setDouble(data.consumeDouble());
        break;
      case 10:
        site.setBigDecimal(new BigDecimal(data.consumeDouble()));
        break;
      case 11:
        site.setString(data.consumeString(50));
        break;
      case 12:
        site.setBytes(data.consumeBytes(50));
        break;
      case 13:
        site.setTimestamp(new Timestamp(data.consumeLong()), calendar);
        break;
      case 14:
        site.setTime(new Time(data.consumeLong()), calendar);
        break;
      case 15:
        // Raw object mapping
        Object obj = null;
        int objType = data.consumeInt(1, 4);
        if (objType == 1) {
          obj = data.consumeBoolean();
        } else if (objType == 2) {
          obj = data.consumeString(50);
        } else if (objType == 3) {
          obj = data.consumeLong();
        } else if (objType == 4) {
          obj = data.consumeBytes(50);
        }

        site.setObject(obj, data.consumeInt(-10, 100)); // Types constants fall in this range
        break;
      case 16:
        // Test the JDBC ResultSet getter mapping using a dynamic proxy
        org.apache.calcite.avatica.util.Cursor.Accessor accessor =
            (org.apache.calcite.avatica.util.Cursor.Accessor) Proxy.newProxyInstance(
                org.apache.calcite.avatica.util.Cursor.Accessor.class.getClassLoader(),
                new Class<?>[] {org.apache.calcite.avatica.util.Cursor.Accessor.class},
                (proxy, method, args) -> {
                String name = method.getName();
                if (name.equals("wasNull")) {
                  return data.consumeBoolean();
                }
                if (name.equals("getString") || name.equals("getNString")) {
                  return data.consumeString(50);
                }
                if (name.equals("getBoolean")) {
                  return data.consumeBoolean();
                }
                if (name.equals("getByte")) {
                  return data.consumeByte();
                }
                if (name.equals("getShort")) {
                  return data.consumeShort();
                }
                if (name.equals("getInt")) {
                  return data.consumeInt();
                }
                if (name.equals("getLong")) {
                  return data.consumeLong();
                }
                if (name.equals("getFloat")) {
                  return data.consumeFloat();
                }
                if (name.equals("getDouble")) {
                  return data.consumeDouble();
                }
                if (name.equals("getBigDecimal")) {
                  return new BigDecimal(data.consumeDouble());
                }
                if (name.equals("getBytes")) {
                  return data.consumeBytes(50);
                }
                if (name.equals("getDate")) {
                  return new Date(data.consumeLong());
                }
                if (name.equals("getTime")) {
                  return new Time(data.consumeLong());
                }
                if (name.equals("getTimestamp")) {
                  return new Timestamp(data.consumeLong());
                }

                if (name.equals("getUByte")) {
                  return org.joou.UByte.valueOf(data.consumeInt(0, 255));
                }
                if (name.equals("getUShort")) {
                  return org.joou.UShort.valueOf(data.consumeInt(0, 65535));
                }
                if (name.equals("getUInt")) {
                  return org.joou.UInteger.valueOf(data.consumeLong(0, 4294967295L));
                }
                if (name.equals("getULong")) {
                  return org.joou.ULong.valueOf(data.consumeLong(0, Long.MAX_VALUE));
                }

                return null;
              }
            );

        try {
          AvaticaSite.get(accessor, data.consumeInt(-10, 100), data.consumeBoolean(), calendar);
        } catch (SQLException e) {
          // Expected to throw SQLException for unsupported conversions
        }
        break;
      default:
        break;
      }

    } catch (IllegalArgumentException | UnsupportedOperationException e) {
      // UnsupportedOperationException is explicitly thrown by AvaticaSite.notImplemented()
      // and unsupportedCast() when types don't align.
    } catch (RuntimeException e) {
      // TypedValue bindings often throw RuntimeException directly for "not implemented"
      if (!"not implemented".equals(e.getMessage())) {
        throw e;
      }
    }
  }
}
