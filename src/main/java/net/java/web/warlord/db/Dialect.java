package net.java.web.warlord.db;

/* Java */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


/**
 * Database dialect abstractions.
 *
 * @author anton.baukin@gmail.com
 */
public interface Dialect
{
	/* Dialect */

	/**
	 * This dialect name. This string must be the same
	 * as of 'dialect' attribute in XML files with the
	 * SQL queries (see {@link QueryCache}).
	 */
	String getName();

	Lob    createLob(InputStream i);

	/**
	 * Writes the bytes from the result column value
	 * (given as general object) to the stream.
	 */
	long   readLob(Object b, OutputStream s)
	  throws SQLException, IOException;

	/**
	 * Returns the best typed object
	 * of the result column value.
	 */
	Object result(ResultSet rs, ResultSetMetaData m, int c)
	  throws SQLException;
}