package net.java.web.warlord.db;

/* Java */

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * Large binary data wrapper.
 * Implemented by a Dialect.
 *
 * @author anton.baukin@gmail.com.
 */
public interface Lob extends AutoCloseable
{
	/* Large Object Wrapper */

	/**
	 * Assigns parameter to the statement.
	 */
	void set(PreparedStatement s, int c)
	  throws SQLException, IOException;
}