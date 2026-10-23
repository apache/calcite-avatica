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

import org.apache.calcite.avatica.AvaticaConnection;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of {@link Service}
 * that encodes its requests and responses as JSON
 * and looks up responses from a pre-defined map.
 */
public class MockJsonService extends JsonService {
  private final Map<String, String> map;

  public MockJsonService(Map<String, String> map) {
    this.map = map;
  }

  @Override public String apply(String request) {
    String response = map.get(request);
    if (response == null) {
      throw new RuntimeException("No response for " + request);
    }
    return response;
  }

  /** Factory that creates a {@code MockJsonService}. */
  public static class Factory implements Service.Factory {
    public Service create(AvaticaConnection connection) {
      final String connectionId = connection.id;
      final Map<String, String> map1 = new HashMap<>();
      try {
        map1.put(
            "{\"request\":\"openConnection\",\"connectionId\":\"" + connectionId + "\",\"info\":{}}",
            "{\"response\":\"openConnection\"}");
        map1.put(
            "{\"request\":\"closeConnection\",\"connectionId\":\"" + connectionId + "\"}",
            "{\"response\":\"closeConnection\"}");
        map1.put(
            "{\"request\":\"getSchemas\",\"catalog\":null,\"schemaPattern\":{\"s\":null}}",
            "{\"response\":\"resultSet\", updateCount: -1, firstFrame: {offset: 0, done: true, rows: []}}");
        map1.put(
            JsonService.encode(new SchemasRequest(connectionId, null, null)),
            "{\"response\":\"resultSet\", updateCount: -1, firstFrame: {offset: 0, done: true, rows: []}}");
        map1.put(
            JsonService.encode(
                new TablesRequest(connectionId, null, null, null, Arrays.<String>asList())),
            "{\"response\":\"resultSet\", updateCount: -1, firstFrame: {offset: 0, done: true, rows: []}}");
        map1.put(
            "{\"request\":\"createStatement\",\"connectionId\":\"" + connectionId + "\"}",
            "{\"response\":\"createStatement\",\"id\":0}");
        map1.put(
            "{\"request\":\"prepareAndExecute\",\"statementId\":0,"
                + "\"sql\":\"select * from (\\n  values (1, 'a'), (null, 'b'), (3, 'c')) as t (c1, c2)\",\"maxRowCount\":-1}",
            "{\"response\":\"resultSet\", updateCount: -1, \"signature\": {\n"
                + " \"columns\": [\n"
                + "   {\"columnName\": \"C1\", \"type\": {type: \"scalar\", id: 4, rep: \"INTEGER\"}},\n"
                + "   {\"columnName\": \"C2\", \"type\": {type: \"scalar\", id: 12, rep: \"STRING\"}}\n"
                + " ], \"cursorFactory\": {\"style\": \"ARRAY\"}\n"
                + "}, \"rows\": [[1, \"a\"], [null, \"b\"], [3, \"c\"]]}");
        map1.put(
            "{\"request\":\"prepare\",\"statementId\":0,"
                + "\"sql\":\"select * from (\\n  values (1, 'a'), (null, 'b'), (3, 'c')) as t (c1, c2)\",\"maxRowCount\":-1}",
            "{\"response\":\"prepare\",\"signature\": {\n"
                + " \"columns\": [\n"
                + "   {\"columnName\": \"C1\", \"type\": {type: \"scalar\", id: 4, rep: \"INTEGER\"}},\n"
                + "   {\"columnName\": \"C2\", \"type\": {type: \"scalar\", id: 12, rep: \"STRING\"}}\n"
                + " ],\n"
                + " \"parameters\": [],\n"
                + " \"cursorFactory\": {\"style\": \"ARRAY\"}\n"
                + "}}");
        map1.put(
            JsonService.encode(new TablesRequest(
                connectionId, null, null, "additionalColumnsTest", Arrays.<String>asList())),
            "{\"response\":\"resultSet\",\"connectionId\":\"" + connectionId + "\",\"statementId\":0,\"ownStatement\":true,"
            + "\"signature\":{\"columns\":["
            + "{\"ordinal\":0,\"autoIncrement\":false,\"caseSensitive\":true,\"searchable\":true,\"currency\":false,\"nullable\":0,\"signed\":false,"
              + " \"displaySize\":128,\"label\":\"TABLE_CAT\",\"columnName\":\"TABLE_CAT\",\"schemaName\":\"INFORMATION_SCHEMA\""
              + ",\"precision\":128,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\",\"catalogName\":\"PUBLIC\",\"type\":"
              + "{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,\""
            + "definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
            + "{\"ordinal\":1,\"autoIncrement\":false,\"caseSensitive\":true,\"searchable\":true,\"currency\":false,\"nullable\":0,\"signed\":false,"
              + "\"displaySize\":128,\"label\":\"TABLE_SCHEM\",\"columnName\":\"TABLE_SCHEM\",\"schemaName\":\"INFORMATION_SCHEMA\",\"precision\":128,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\","
              + "\"catalogName\":\"PUBLIC\",\"type\":{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,"
              + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
            + "{\"ordinal\":2,\"autoIncrement\":false,\"caseSensitive\":true,\"searchable\":true,\"currency\":false,\"nullable\":0,\"signed\":false,"
              + "\"displaySize\":128,\"label\":\"TABLE_NAME\",\"columnName\":\"TABLE_NAME\",\"schemaName\":\"INFORMATION_SCHEMA\",\"precision\":128,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\","
              + "\"catalogName\":\"PUBLIC\",\"type\":{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,"
              + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
            + "{\"ordinal\":3,\"autoIncrement\":false,\"caseSensitive\":true,\"searchable\":true,\"currency\":false,\"nullable\":0,\"signed\":false,"
              + "\"displaySize\":65536,\"label\":\"TABLE_TYPE\",\"columnName\":\"TABLE_TYPE\",\"schemaName\":\"INFORMATION_SCHEMA\",\"precision\":65536,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\","
              + "\"catalogName\":\"PUBLIC\",\"type\":{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,"
              + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
            + "{\"ordinal\":4,\"autoIncrement\":false,\"caseSensitive\":true,\"searchable\":true,\"currency\":false,\"nullable\":1,\"signed\":false,"
              + "\"displaySize\":65536,\"label\":\"REMARKS\",\"columnName\":\"REMARKS\",\"schemaName\":\"INFORMATION_SCHEMA\",\"precision\":65536,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\","
              + "\"catalogName\":\"PUBLIC\",\"type\":{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,"
              + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
            + "{\"ordinal\":5,\"autoIncrement\":false,\"caseSensitive\":true,\"searchable\":true,\"currency\":false,\"nullable\":1,\"signed\":false,"
              + "\"displaySize\":128,\"label\":\"TYPE_CAT\",\"columnName\":\"TYPE_CAT\",\"schemaName\":\"INFORMATION_SCHEMA\",\"precision\":128,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\","
              + "\"catalogName\":\"PUBLIC\",\"type\":{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,"
              + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
            + "{\"ordinal\":6,\"autoIncrement\":false,\"caseSensitive\":true,\"searchable\":true,\"currency\":false,\"nullable\":1,\"signed\":false,"
              + "\"displaySize\":128,\"label\":\"TYPE_SCHEM\",\"columnName\":\"TYPE_SCHEM\",\"schemaName\":\"INFORMATION_SCHEMA\",\"precision\":128,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\","
              + "\"catalogName\":\"PUBLIC\",\"type\":{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,"
              + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
            + "{\"ordinal\":7,\"autoIncrement\":false,\"caseSensitive\":true,\"searchable\":true,\"currency\":false,\"nullable\":1,\"signed\":false,"
              + "\"displaySize\":128,\"label\":\"TYPE_NAME\",\"columnName\":\"TYPE_NAME\",\"schemaName\":\"INFORMATION_SCHEMA\",\"precision\":128,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\","
              + "\"catalogName\":\"PUBLIC\",\"type\":{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,"
              + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
            + "{\"ordinal\":8,\"autoIncrement\":false,\"caseSensitive\":true,\"searchable\":true,\"currency\":false,\"nullable\":1,\"signed\":false,"
              + "\"displaySize\":128,\"label\":\"SELF_REFERENCING_COL_NAME\",\"columnName\":\"SELF_REFERENCING_COL_NAME\",\"schemaName\":\"INFORMATION_SCHEMA\",\"precision\":128,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\","
              + "\"catalogName\":\"PUBLIC\",\"type\":{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,"
              + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
            + "{\"ordinal\":9,\"autoIncrement\":false,\"caseSensitive\":true,\"searchable\":true,\"currency\":false,\"nullable\":1,\"signed\":false,"
              + "\"displaySize\":65536,\"label\":\"REF_GENERATION\",\"columnName\":\"REF_GENERATION\",\"schemaName\":\"INFORMATION_SCHEMA\",\"precision\":65536,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\","
              + "\"catalogName\":\"PUBLIC\",\"type\":{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,"
              + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
            + "{\"ordinal\":10,\"autoIncrement\":false,\"caseSensitive\":true,\"searchable\":true,\"currency\":false,\"nullable\":1,\"signed\":false,"
              + "\"displaySize\":128,\"label\":\"HSQLDB_TYPE\",\"columnName\":\"HSQLDB_TYPE\",\"schemaName\":\"INFORMATION_SCHEMA\",\"precision\":128,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\","
              + "\"catalogName\":\"PUBLIC\",\"type\":{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,"
              + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
            + "{\"ordinal\":11,\"autoIncrement\":false,\"caseSensitive\":false,\"searchable\":true,\"currency\":false,\"nullable\":1,\"signed\":false,"
              + "\"displaySize\":5,\"label\":\"READ_ONLY\",\"columnName\":\"READ_ONLY\",\"schemaName\":\"INFORMATION_SCHEMA\",\"precision\":0,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\","
              + "\"catalogName\":\"PUBLIC\",\"type\":{\"type\":\"scalar\",\"id\":16,\"name\":\"BOOLEAN\",\"rep\":\"PRIMITIVE_BOOLEAN\"},\"readOnly\":true,\"writable\":false,"
              + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.Boolean\"},"
            + "{\"ordinal\":12,\"autoIncrement\":false,\"caseSensitive\":true,\"searchable\":true,\"currency\":false,\"nullable\":1,\"signed\":false,"
              + "\"displaySize\":65536,\"label\":\"COMMIT_ACTION\",\"columnName\":\"COMMIT_ACTION\",\"schemaName\":\"INFORMATION_SCHEMA\",\"precision\":65536,\"scale\":0,\"tableName\":\"SYSTEM_TABLES\","
              + "\"catalogName\":\"PUBLIC\",\"type\":{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,"
              + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
            + "{\"ordinal\": 13,\"autoIncrement\": false,\"caseSensitive\": true,\"searchable\": true,\"currency\": false,\"nullable\": 1,\"signed\": false,"
                + "\"displaySize\": 65536,\"label\": \"EXTRA_LABEL\",\"columnName\": \"EXTRA_LABEL\",\"schemaName\": \"INFORMATION_SCHEMA\",\"precision\": 65536,\"scale\": 0,\"tableName\": \"SYSTEM_TABLES\","
                + "\"catalogName\": \"PUBLIC\",\"type\":{\"type\": \"scalar\",\"id\": 12,\"name\": \"VARCHAR\",\"rep\": \"STRING\"},\"readOnly\": true,\"writable\": false,"
                + "\"definitelyWritable\": false,\"columnClassName\": \"java.lang.String\"}],\"sql\":null,"
            + "\"parameters\":[],"
            + "\"cursorFactory\":{\"style\":\"LIST\",\"clazz\":null,\"fieldNames\":null},\"statementType\":null},"
            + "\"firstFrame\":{\"offset\":0,\"done\":true,"
            + "\"rows\":["
              + "[\"PUBLIC\",\"SCOTT\",\"DEPT\",\"TABLE\",null,null,null,null,null,null,\"MEMORY\",false,null,\"EXTRA_LABEL1\"],"
              + "[\"PUBLIC\",\"SCOTT\",\"EMP\",\"TABLE\",null,null,null,null,null,null,\"MEMORY\",false,null,\"EXTRA_LABEL2\"],"
              + "[\"PUBLIC\",\"SCOTT\",\"BONUS\",\"TABLE\",null,null,null,null,null,null,\"MEMORY\",false,null,\"EXTRA_LABEL3\"],"
              + "[\"PUBLIC\",\"SCOTT\",\"SALGRADE\",\"TABLE\",null,null,null,null,null,null,\"MEMORY\",false,null,null]]},"
            + "\"updateCount\":-1,\"rpcMetadata\":null}"
        );
        map1.put(
            "{\"request\":\"closeStatement\",\"connectionId\":\"" + connectionId + "\",\"statementId\":0}",
            "{\"response\":\"closeStatement\"}"
        );
        map1.put(
            "{\"request\":\"getColumns\",\"connectionId\":\"" + connectionId + "\",\"catalog\":null,\"schemaPattern\":null,"
                + "\"tableNamePattern\":\"my_table\",\"columnNamePattern\":null}",
            "{\"response\":\"resultSet\",\"connectionId\":\"00000000-0000-0000-0000-000000000000\",\"statementId\":-1,\"ownStatement\":true,"
                + "\"signature\":{\"columns\":["
                  + "{\"ordinal\":0,\"autoIncrement\":false,\"caseSensitive\":false,\"searchable\":true,\"currency\":false,\"nullable\":1,\"signed\":false,"
                    + "\"displaySize\":40,\"label\":\"TABLE_NAME\",\"columnName\":\"TABLE_NAME\",\"schemaName\":\"\",\"precision\":0,\"scale\":0,\"tableName\":\"SYSTEM.TABLE\","
                    + "\"catalogName\":\"\",\"type\":{\"type\":\"scalar\",\"id\":12,\"name\":\"VARCHAR\",\"rep\":\"STRING\"},\"readOnly\":true,\"writable\":false,"
                    + "\"definitelyWritable\":false,\"columnClassName\":\"java.lang.String\"},"
                  + "{\"ordinal\":1,\"autoIncrement\":false,\"caseSensitive\":false,\"searchable\":true,\"currency\":false,\"nullable\":1,\"signed\":true,"
                    + "\"displaySize\":40,\"label\":\"ORDINAL_POSITION\",\"columnName\":\"ORDINAL_POSITION\",\"schemaName\":\"\",\"precision\":0,\"scale\":0,"
                    + "\"tableName\":\"SYSTEM.TABLE\",\"catalogName\":\"\",\"type\":{\"type\":\"scalar\",\"id\":-5,\"name\":\"BIGINT\",\"rep\":\"PRIMITIVE_LONG\"},"
                    + "\"readOnly\":true,\"writable\":false,\"definitelyWritable\":false,\"columnClassName\":\"java.lang.Long\"}"
                + "],\"sql\":null,"
                + "\"parameters\":[],"
                + "\"cursorFactory\":{\"style\":\"LIST\",\"clazz\":null,\"fieldNames\":null},\"statementType\":null},"
                + "\"firstFrame\":{\"offset\":0,\"done\":true,"
                + "\"rows\":[[\"my_table\",10]]"
                + "},\"updateCount\":-1}");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return new MockJsonService(map1);
    }
  }
}

// End MockJsonService.java
