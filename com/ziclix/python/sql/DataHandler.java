
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import java.io.*;
import java.sql.*;
import java.math.*;
import org.python.core.*;

/**
 * The DataHandler is responsible mapping the JDBC data type to
 * a Jython object.  Depending on the version of the JDBC
 * implementation and the particulars of the driver, the type
 * mapping can be significantly different.
 *
 * This interface can also be used to change the behaviour of
 * the default mappings provided by the cursor.  This might be
 * useful in handling more complicated data types such as BLOBs,
 * CLOBs and Arrays.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class DataHandler {

	// default size for buffers
	private static final int INITIAL_SIZE = 1024 * 4;

	/**
	 * Handle most generic Java data types.
	 */
	public DataHandler() {}

	/**
	 * Returns the row id of the last executed statement.
	 *
	 * @param Statement stmt
	 *
	 * @return PyObject
	 *
	 * @throws SQLException
	 *
	 */
	public PyObject getRowId(Statement stmt) throws SQLException {
		return Py.None;
	}

	/**
	 * A callback prior to each execution of the statement.  If the statement is
	 * a PreparedStatement, all the parameters will have been set.
	 */
	public void preExecute(Statement stmt) throws SQLException {
		return;
	}

	/**
	 * A callback after successfully executing the statement.
	 */
	public void postExecute(Statement stmt) throws SQLException {
		return;
	}

	/**
	 * Any .execute() which uses prepared statements will receive a callback for deciding
	 * how to map the PyObject to the appropriate JDBC type.
	 *
	 * @param stmt the current PreparedStatement
	 * @param index the index for which this object is bound
	 * @param object the PyObject in question
	 * @throws SQLException
	 */
	public void setJDBCObject(PreparedStatement stmt, int index, PyObject object) throws SQLException {
		stmt.setObject(index, object.__tojava__(Object.class));
	}

	/**
	 * Any .execute() which uses prepared statements will receive a callback for deciding
	 * how to map the PyObject to the appropriate JDBC type.  The <i>type</i> is the JDBC
	 * type as obtained from <i>java.sql.Types</i>.
	 *
	 * @param stmt the current PreparedStatement
	 * @param index the index for which this object is bound
	 * @param object the PyObject in question
	 * @param type the <i>java.sql.Types</i> for which this PyObject should be bound
	 * @throws SQLException
	 */
	public void setJDBCObject(PreparedStatement stmt, int index, PyObject object, int type) throws SQLException {

		try {
			if (checkNull(stmt, index, object, type)) {
				return;
			}

			switch (type) {

				case Types.DATE :
					Date date = (Date)object.__tojava__(Date.class);

					stmt.setDate(index, date);
					break;

				case Types.TIME :
					Time time = (Time)object.__tojava__(Time.class);

					stmt.setTime(index, time);
					break;

				case Types.TIMESTAMP :
					Timestamp timestamp = (Timestamp)object.__tojava__(Timestamp.class);

					stmt.setTimestamp(index, timestamp);
					break;

				case Types.LONGVARCHAR :
					if (object instanceof PyFile) {
						object = ((PyFile)object).read();
					}

					String varchar = (String)object.__tojava__(String.class);
					Reader reader = new BufferedReader(new StringReader(varchar));

					stmt.setCharacterStream(index, reader, varchar.length());
					break;

				case Types.BIT :
					stmt.setBoolean(index, object.__nonzero__());
					break;

				default :
					if (object instanceof PyFile) {
						object = ((PyFile)object).read();
					}

					stmt.setObject(index, object.__tojava__(Object.class), type);
					break;
			}
		} catch (Exception e) {
			SQLException cause = null, ex = new SQLException("error setting index [" + index + "], type [" + type + "]");

			if (e instanceof SQLException) {
				cause = (SQLException)e;
			} else {
				cause = new SQLException(e.getMessage());
			}

			ex.setNextException(cause);

			throw ex;
		}
	}

	/**
	 * Given a ResultSet, column and type, return the appropriate
	 * Jython object.
	 *
	 * <p>Note: DO NOT iterate the ResultSet.
	 *
	 * @param set the current ResultSet set to the current row
	 * @param col the column number (adjusted properly for JDBC)
	 * @param type the column type
	 * @throws SQLException if the type is unmappable
	 */
	public PyObject getPyObject(ResultSet set, int col, int type) throws SQLException {

		PyObject obj = Py.None;

		switch (type) {

			case Types.CHAR :
			case Types.VARCHAR :
				String string = set.getString(col);

				obj = (string == null) ? Py.None : Py.newString(string);
				break;

			case Types.LONGVARCHAR :
				InputStream longvarchar = set.getAsciiStream(col);

				if (longvarchar == null) {
					obj = Py.None;
				} else {
					try {
						longvarchar = new BufferedInputStream(longvarchar);

						byte[] bytes = DataHandler.read(longvarchar);

						if (bytes != null) {
							obj = Py.newString(new String(bytes));
						}
					} finally {
						try {
							longvarchar.close();
						} catch (Exception e) {}
					}
				}
				break;

			case Types.NUMERIC :
			case Types.DECIMAL :
				BigDecimal bd = set.getBigDecimal(col, 10);

				obj = (bd == null) ? Py.None : Py.newFloat(bd.doubleValue());
				break;

			case Types.BIT :
				obj = set.getBoolean(col) ? Py.One : Py.Zero;
				break;

			case Types.INTEGER :
			case Types.TINYINT :
			case Types.SMALLINT :
				obj = Py.newInteger(set.getInt(col));
				break;

			case Types.BIGINT :
				obj = new PyLong(set.getLong(col));
				break;

			case Types.FLOAT :
			case Types.REAL :
			case Types.DOUBLE :
				obj = Py.newFloat(set.getFloat(col));
				break;

			case Types.TIME :
				obj = Py.java2py(set.getTime(col));
				break;

			case Types.TIMESTAMP :
				obj = Py.java2py(set.getTimestamp(col));
				break;

			case Types.DATE :
				obj = Py.java2py(set.getDate(col));
				break;

			case Types.NULL :
				obj = Py.None;
				break;

			case Types.OTHER :
				obj = Py.java2py(set.getObject(col));
				break;

			case Types.BINARY :
			case Types.VARBINARY :
			case Types.LONGVARBINARY :
				obj = Py.java2py(set.getBytes(col));
				break;

			default :
				Integer[] vals = { new Integer(col), new Integer(type) };
				String msg = zxJDBC.getString("errorSettingIndex", vals);

				throw new SQLException(msg);
		}

		return (set.wasNull() || (obj == null)) ? Py.None : obj;
	}

	/**
	 * Handles checking if the object is null or None and setting it on the statement.
	 *
	 * @return true if the object is null and was set on the statement, false otherwise
	 */
	public static final boolean checkNull(PreparedStatement stmt, int index, PyObject object, int type) throws SQLException {

		if ((object == null) || (Py.None == object)) {
			stmt.setNull(index, type);

			return true;
		}

		return false;
	}

	/**
	 * Since the driver needs to the know the length of all streams,
	 * read it into a byte[] array.
	 *
	 * @return the stream as a byte[]
	 */
	public static final byte[] read(InputStream stream) {

		int b = -1, read = 0;
		byte[] results = new byte[INITIAL_SIZE];

		try {
			while ((b = stream.read()) != -1) {
				if (results.length < (read + 1)) {
					byte[] tmp = results;

					results = new byte[results.length * 2];

					System.arraycopy(tmp, 0, results, 0, tmp.length);
				}

				results[read++] = (byte)b;
			}
		} catch (IOException e) {
			throw zxJDBC.newError(e);
		}

		byte[] tmp = results;

		results = new byte[read];

		System.arraycopy(tmp, 0, results, 0, read);

		return results;
	}

	/**
	 * Read all the chars from the Reader into the String.
	 *
	 * @return the contents of the Reader in a String
	 */
	public static final String read(Reader reader) {

		int c = 0;
		StringBuffer buffer = new StringBuffer(INITIAL_SIZE);

		try {
			while ((c = reader.read()) != -1) {
				buffer.append((char)c);
			}
		} catch (IOException e) {
			throw zxJDBC.newError(e);
		}

		return buffer.toString();
	}
}
