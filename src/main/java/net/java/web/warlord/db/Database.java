package net.java.web.warlord.db;

/* Java */

import java.sql.Connection;
import java.sql.SQLException;

/* C3p0 */

import com.mchange.v2.c3p0.AbstractComboPooledDataSource;


/**
 * Extends Dialect to Database initialization,
 * connectivity and control abstractions.
 *
 * @author anton.baukin@gmail.com.
 */
interface Database extends Dialect
{
	/* Database Connectivity */

	/**
	 * Returns class name of the driver.
	 */
	String     driver();

	void       start();

	void       close();

	/**
	 * Creates URI to connect to the database.
	 * The database name and the additional parameters
	 * are defined by the application configuration.
	 * Note that this call may have side-effects.
	 */
	String     initDbURL();

	/**
	 * Creates direct driver database connection.
	 * To use only during the database initialization.
	 */
	Connection connect();

	/**
	 * Initializes the database with the given connection.
	 * Warning! This operation repeats on each start!
	 */
	void       init(Connection c)
	  throws SQLException;

	/**
	 * Initializes the given data source before starting it.
	 * This method is mostly for the embedded databases:
	 * you may set the user name and the password.
	 */
	void       init(AbstractComboPooledDataSource ds);

	/**
	 * Executes test on the provided connection.
	 * (Runs SQL query that depends on the database.)
	 */
	void       test(Connection c)
	  throws SQLException;
}