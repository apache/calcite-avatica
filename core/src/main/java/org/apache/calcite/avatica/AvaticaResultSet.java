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

import org.apache.calcite.avatica.remote.TypedValue;
import org.apache.calcite.avatica.util.ArrayFactoryImpl;
import org.apache.calcite.avatica.util.Cursor;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Implementation of {@link java.sql.ResultSet}
 * for the Avatica engine.
 */
public class AvaticaResultSet extends ArrayFactoryImpl implements ResultSet {
  protected final AvaticaStatement statement;
  protected final QueryState state;
  protected final Meta.Signature signature;
  protected final Meta.Frame firstFrame;
  protected final List<ColumnMetaData> columnMetaDataList;
  protected final ResultSetMetaData resultSetMetaData;
  protected final Calendar localCalendar;

  protected Cursor cursor;
  protected List<Cursor.Accessor> accessorList;
  private int row;
  private boolean beforeFirst;
  private boolean afterLast;
  private int fetchDirection;
  private int fetchSize;
  private int type;
  private int concurrency;
  private int holdability;
  private boolean closed;

  /** Creates an {@link AvaticaResultSet}. */
  public AvaticaResultSet(AvaticaStatement statement,
      QueryState state,
      Meta.Signature signature,
      ResultSetMetaData resultSetMetaData,
      TimeZone timeZone,
      Meta.Frame firstFrame) throws SQLException {
    super(timeZone);
    this.statement = statement;
    this.state = state;
    this.signature = signature;
    this.firstFrame = firstFrame;
    this.columnMetaDataList = signature.columns;
    if (null != statement) {
      this.type = statement.resultSetType;
      this.concurrency = statement.resultSetConcurrency;
      this.holdability = statement.resultSetHoldability;
      this.fetchSize = statement.getFetchSize();
      this.fetchDirection = statement.getFetchDirection();
    } else {
      this.type = 0;
      this.concurrency = 0;
      this.holdability = 0;
      this.fetchSize = AvaticaStatement.DEFAULT_FETCH_SIZE;
      this.fetchDirection = 0;
    }
    this.resultSetMetaData = resultSetMetaData;
    this.localCalendar = Calendar.getInstance(timeZone, Locale.ROOT);
  }

  private int findColumn0(String columnLabel) throws SQLException {
    for (ColumnMetaData columnMetaData : columnMetaDataList) {
      // Per JDBC 3.0 specification, match is case-insensitive and if there is
      // more than one column with a particular name, take the first.
      if (columnMetaData.label.equalsIgnoreCase(columnLabel)) {
        return columnMetaData.ordinal; // 0-based
      }
    }
    throw AvaticaConnection.HELPER.createException("column '" + columnLabel
        + "' not found");
  }

  protected void checkOpen() throws SQLException {
    if (isClosed()) {
      throw AvaticaConnection.HELPER.createException("ResultSet closed");
    }
  }
  /**
   * Returns the accessor for column with a given index.
   *
   * @param columnIndex 1-based column index
   * @return Accessor
   * @throws SQLException if index is not valid
   */
  private Cursor.Accessor getAccessor(int columnIndex) throws SQLException {
    checkOpen();
    try {
      return accessorList.get(columnIndex - 1);
    } catch (IndexOutOfBoundsException e) {
      throw AvaticaConnection.HELPER.createException(
          "invalid column ordinal: " + columnIndex);
    }
  }

  /**
   * Returns the accessor for column with a given label.
   *
   * @param columnLabel Column label
   * @return Accessor
   * @throws SQLException if there is no column with that label
   */
  private Cursor.Accessor getAccessor(String columnLabel) throws SQLException {
    checkOpen();
    return accessorList.get(findColumn0(columnLabel));
  }

  public void close() {
    closed = true;
    final Cursor cursor = this.cursor;
    if (cursor != null) {
      this.cursor = null;
      cursor.close();
    }
    if (statement != null) {
      statement.onResultSetClose(this);
    }
  }

  /** Sets the flag to indicate that cancel has been requested.
   *
   * <p>The implementation should check this flag periodically and cease
   * processing.
   *
   * <p>Not a JDBC method. */
  protected void cancel() {
    statement.cancelFlag.compareAndSet(false, true);
  }

  /**
   * Executes this result set. (Not a JDBC method.)
   *
   * <p>Note that execute cannot occur in the constructor, because the
   * constructor occurs while the statement is locked, to make sure that
   * execute/cancel don't happen at the same time.</p>
   *
   * @see org.apache.calcite.avatica.AvaticaConnection.Trojan#execute(AvaticaResultSet)
   *
   * @throws SQLException if execute fails for some reason.
   */
  protected AvaticaResultSet execute() throws SQLException {
    final Iterable<Object> iterable1 =
        statement.connection.meta.createIterable(statement.handle, state, signature,
            Collections.<TypedValue>emptyList(), firstFrame);
    this.cursor = MetaImpl.createCursor(signature.cursorFactory, iterable1);
    this.accessorList =
        cursor.createAccessors(columnMetaDataList, localCalendar, this);
    this.row = 0;
    this.beforeFirst = true;
    this.afterLast = false;
    return this;
  }

  public AvaticaResultSet execute2(Cursor cursor,
      List<ColumnMetaData> columnMetaDataList) {
    this.cursor = cursor;
    this.accessorList =
        cursor.createAccessors(columnMetaDataList, localCalendar, this);
    this.row = 0;
    this.beforeFirst = true;
    this.afterLast = false;
    return this;
  }

  /**
   * Returns the calendar used by this result set. Not a jdbc method.
   */
  public Calendar getLocalCalendar() {
    return localCalendar;
  }

  public boolean next() throws SQLException {
    // TODO: for timeout, see IteratorResultSet.next
    checkOpen();
    if (null != statement && statement.cancelFlag.get()) {
      throw AvaticaConnection.HELPER.createException("Statement canceled");
    }
    if (cursor.next()) {
      ++row;
      beforeFirst = false;
      return true;
    } else {
      row = 0;
      afterLast = true;
      return false;
    }
  }

  public int findColumn(String columnLabel) throws SQLException {
    checkOpen();
    return findColumn0(columnLabel) + 1;
  }

  public boolean wasNull() throws SQLException {
    checkOpen();
    return cursor.wasNull();
  }

  public String getString(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getString();
  }

  public boolean getBoolean(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getBoolean();
  }

  public byte getByte(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getByte();
  }

  public short getShort(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getShort();
  }

  public int getInt(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getInt();
  }

  public long getLong(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getLong();
  }

  public float getFloat(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getFloat();
  }

  public double getDouble(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getDouble();
  }

  @SuppressWarnings("deprecation")
  public BigDecimal getBigDecimal(
      int columnIndex, int scale) throws SQLException {
    return getAccessor(columnIndex).getBigDecimal(scale);
  }

  public byte[] getBytes(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getBytes();
  }

  public Date getDate(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getDate(localCalendar);
  }

  public Time getTime(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getTime(localCalendar);
  }

  public Timestamp getTimestamp(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getTimestamp(localCalendar);
  }

  public InputStream getAsciiStream(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getAsciiStream();
  }

  @SuppressWarnings("deprecation")
  public InputStream getUnicodeStream(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getUnicodeStream();
  }

  public InputStream getBinaryStream(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getBinaryStream();
  }

  public String getString(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getString();
  }

  public boolean getBoolean(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getBoolean();
  }

  public byte getByte(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getByte();
  }

  public short getShort(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getShort();
  }

  public int getInt(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getInt();
  }

  public long getLong(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getLong();
  }

  public float getFloat(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getFloat();
  }

  public double getDouble(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getDouble();
  }

  @SuppressWarnings("deprecation")
  public BigDecimal getBigDecimal(
      String columnLabel, int scale) throws SQLException {
    return getAccessor(columnLabel).getBigDecimal(scale);
  }

  public byte[] getBytes(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getBytes();
  }

  public Date getDate(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getDate(localCalendar);
  }

  public Time getTime(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getTime(localCalendar);
  }

  public Timestamp getTimestamp(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getTimestamp(localCalendar);
  }

  public InputStream getAsciiStream(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getAsciiStream();
  }

  @SuppressWarnings("deprecation")
  public InputStream getUnicodeStream(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getUnicodeStream();
  }

  public InputStream getBinaryStream(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getBinaryStream();
  }

  public SQLWarning getWarnings() throws SQLException {
    checkOpen();
    return null; // no warnings, since warnings are not supported
  }

  public void clearWarnings() throws SQLException {
    checkOpen();
  }

  public String getCursorName() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public ResultSetMetaData getMetaData() throws SQLException {
    checkOpen();
    return resultSetMetaData;
  }

  public Object getObject(int columnIndex) throws SQLException {
    final Cursor.Accessor accessor = getAccessor(columnIndex);
    final ColumnMetaData metaData = columnMetaDataList.get(columnIndex - 1);
    return AvaticaSite.get(accessor, metaData.type.id, localCalendar);
  }

  public Object getObject(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getObject();
  }

  public Reader getCharacterStream(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getCharacterStream();
  }

  public Reader getCharacterStream(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getCharacterStream();
  }

  public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getBigDecimal();
  }

  public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getBigDecimal();
  }

  public boolean isBeforeFirst() throws SQLException {
    checkOpen();
    return beforeFirst;
  }

  public boolean isAfterLast() throws SQLException {
    checkOpen();
    return afterLast;
  }

  public boolean isFirst() throws SQLException {
    checkOpen();
    return row == 1;
  }

  public boolean isLast() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void beforeFirst() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void afterLast() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public boolean first() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public boolean last() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public int getRow() throws SQLException {
    checkOpen();
    return row;
  }

  public boolean absolute(int row) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public boolean relative(int rows) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public boolean previous() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void setFetchDirection(int direction) throws SQLException {
    checkOpen();
    this.fetchDirection = direction;
  }

  public int getFetchDirection() throws SQLException {
    checkOpen();
    return fetchDirection;
  }

  public void setFetchSize(int fetchSize) throws SQLException {
    checkOpen();
    this.fetchSize = fetchSize;
  }

  public int getFetchSize() throws SQLException {
    checkOpen();
    return fetchSize;
  }

  public int getType() throws SQLException {
    checkOpen();
    return type;
  }

  public int getConcurrency() throws SQLException {
    checkOpen();
    return concurrency;
  }

  public boolean rowUpdated() throws SQLException {
    checkOpen();
    return false;
  }

  public boolean rowInserted() throws SQLException {
    checkOpen();
    return false;
  }

  public boolean rowDeleted() throws SQLException {
    checkOpen();
    return false;
  }

  public void updateNull(int columnIndex) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBoolean(int columnIndex, boolean x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateByte(int columnIndex, byte x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateShort(int columnIndex, short x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateInt(int columnIndex, int x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateLong(int columnIndex, long x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateFloat(int columnIndex, float x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateDouble(int columnIndex, double x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBigDecimal(
      int columnIndex, BigDecimal x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateString(int columnIndex, String x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBytes(int columnIndex, byte[] x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateDate(int columnIndex, Date x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateTime(int columnIndex, Time x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateTimestamp(
      int columnIndex, Timestamp x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateAsciiStream(
      int columnIndex, InputStream x, int length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBinaryStream(
      int columnIndex, InputStream x, int length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateCharacterStream(
      int columnIndex, Reader x, int length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateObject(
      int columnIndex, Object x, int scaleOrLength) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateObject(int columnIndex, Object x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateNull(String columnLabel) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBoolean(
      String columnLabel, boolean x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateByte(String columnLabel, byte x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateShort(String columnLabel, short x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateInt(String columnLabel, int x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateLong(String columnLabel, long x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateFloat(String columnLabel, float x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateDouble(String columnLabel, double x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBigDecimal(
      String columnLabel, BigDecimal x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateString(String columnLabel, String x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBytes(String columnLabel, byte[] x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateDate(String columnLabel, Date x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateTime(String columnLabel, Time x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateTimestamp(
      String columnLabel, Timestamp x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateAsciiStream(
      String columnLabel, InputStream x, int length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBinaryStream(
      String columnLabel, InputStream x, int length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateCharacterStream(
      String columnLabel, Reader reader, int length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateObject(
      String columnLabel, Object x, int scaleOrLength) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateObject(String columnLabel, Object x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void insertRow() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateRow() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void deleteRow() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void refreshRow() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void cancelRowUpdates() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void moveToInsertRow() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void moveToCurrentRow() throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public AvaticaStatement getStatement() throws SQLException {
    checkOpen();
    return statement;
  }

  public Object getObject(
      int columnIndex, Map<String, Class<?>> map) throws SQLException {
    return getAccessor(columnIndex).getObject(map);
  }

  public Ref getRef(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getRef();
  }

  public Blob getBlob(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getBlob();
  }

  public Clob getClob(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getClob();
  }

  public Array getArray(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getArray();
  }

  public Object getObject(
      String columnLabel, Map<String, Class<?>> map) throws SQLException {
    return getAccessor(columnLabel).getObject(map);
  }

  public Ref getRef(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getRef();
  }

  public Blob getBlob(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getBlob();
  }

  public Clob getClob(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getClob();
  }

  public Array getArray(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getArray();
  }

  public Date getDate(int columnIndex, Calendar cal) throws SQLException {
    return getAccessor(columnIndex).getDate(cal);
  }

  public Date getDate(String columnLabel, Calendar cal) throws SQLException {
    return getAccessor(columnLabel).getDate(cal);
  }

  public Time getTime(int columnIndex, Calendar cal) throws SQLException {
    return getAccessor(columnIndex).getTime(cal);
  }

  public Time getTime(String columnLabel, Calendar cal) throws SQLException {
    return getAccessor(columnLabel).getTime(cal);
  }

  public Timestamp getTimestamp(
      int columnIndex, Calendar cal) throws SQLException {
    return getAccessor(columnIndex).getTimestamp(cal);
  }

  public Timestamp getTimestamp(
      String columnLabel, Calendar cal) throws SQLException {
    return getAccessor(columnLabel).getTimestamp(cal);
  }

  public URL getURL(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getURL();
  }

  public URL getURL(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getURL();
  }

  public void updateRef(int columnIndex, Ref x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateRef(String columnLabel, Ref x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBlob(int columnIndex, Blob x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBlob(String columnLabel, Blob x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateClob(int columnIndex, Clob x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateClob(String columnLabel, Clob x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateArray(int columnIndex, Array x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateArray(String columnLabel, Array x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public RowId getRowId(int columnIndex) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public RowId getRowId(String columnLabel) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateRowId(int columnIndex, RowId x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateRowId(String columnLabel, RowId x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public int getHoldability() throws SQLException {
    checkOpen();
    return holdability;
  }

  public boolean isClosed() throws SQLException {
    return closed;
  }

  public void updateNString(
      int columnIndex, String nString) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateNString(
      String columnLabel, String nString) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateNClob(
      String columnLabel, NClob nClob) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public NClob getNClob(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getNClob();
  }

  public NClob getNClob(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getNClob();
  }

  public SQLXML getSQLXML(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getSQLXML();
  }

  public SQLXML getSQLXML(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getSQLXML();
  }

  public void updateSQLXML(
      int columnIndex, SQLXML xmlObject) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateSQLXML(
      String columnLabel, SQLXML xmlObject) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public String getNString(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getNString();
  }

  public String getNString(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getNString();
  }

  public Reader getNCharacterStream(int columnIndex) throws SQLException {
    return getAccessor(columnIndex).getNCharacterStream();
  }

  public Reader getNCharacterStream(String columnLabel) throws SQLException {
    return getAccessor(columnLabel).getNCharacterStream();
  }

  public void updateNCharacterStream(
      int columnIndex, Reader x, long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateNCharacterStream(
      String columnLabel, Reader reader, long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateAsciiStream(
      int columnIndex, InputStream x, long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBinaryStream(
      int columnIndex, InputStream x, long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateCharacterStream(
      int columnIndex, Reader x, long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateAsciiStream(
      String columnLabel, InputStream x, long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBinaryStream(
      String columnLabel, InputStream x, long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateCharacterStream(
      String columnLabel, Reader reader, long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBlob(
      int columnIndex,
      InputStream inputStream,
      long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBlob(
      String columnLabel,
      InputStream inputStream,
      long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateClob(
      int columnIndex, Reader reader, long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateClob(
      String columnLabel, Reader reader, long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateNClob(
      int columnIndex, Reader reader, long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateNClob(
      String columnLabel, Reader reader, long length) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateNCharacterStream(
      int columnIndex, Reader x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateNCharacterStream(
      String columnLabel, Reader reader) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateAsciiStream(
      int columnIndex, InputStream x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBinaryStream(
      int columnIndex, InputStream x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateCharacterStream(
      int columnIndex, Reader x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateAsciiStream(
      String columnLabel, InputStream x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBinaryStream(
      String columnLabel, InputStream x) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateCharacterStream(
      String columnLabel, Reader reader) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBlob(
      int columnIndex, InputStream inputStream) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateBlob(
      String columnLabel, InputStream inputStream) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateClob(int columnIndex, Reader reader) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateClob(
      String columnLabel, Reader reader) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateNClob(
      int columnIndex, Reader reader) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public void updateNClob(
      String columnLabel, Reader reader) throws SQLException {
    throw AvaticaConnection.HELPER.unsupported();
  }

  public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
    return getAccessor(columnIndex).getObject(type);
  }

  public <T> T getObject(
      String columnLabel, Class<T> type) throws SQLException {
    return getAccessor(columnLabel).getObject(type);
  }

  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return iface.cast(this);
    }
    throw AvaticaConnection.HELPER.createException(
        "does not implement '" + iface + "'");
  }

  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return iface.isInstance(this);
  }
}

// End AvaticaResultSet.java
