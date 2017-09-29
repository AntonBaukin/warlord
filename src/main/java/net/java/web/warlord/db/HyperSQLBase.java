package net.java.web.warlord.db;

/* Java */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/* C3p0 */

import com.mchange.v2.c3p0.AbstractComboPooledDataSource;

/* Warlord */

import net.java.web.warlord.EX;


/**
 * Dialect of Hypersonic SQL embedded database.
 *
 * @author anton.baukin@gmail.com.
 */
public class HyperSQLBase extends DatabaseEmbedded
{
	/* Database Dialect */

	public String     getName()
	{
		return "HyperSQL";
	}

	protected String  getUrlPrefix()
	{
		return "jdbc:hsqldb:";
	}


	/* Database Connectivity */

	public String     driver()
	{
		return "org.hsqldb.jdbc.JDBCDriver";
	}

	public Connection connect()
	{
		try
		{
			return DriverManager.getConnection(initDbURL(), "SA", "");
		}
		catch(Throwable e)
		{
			throw EX.wrap(e, "Error while connecting HyperSQL database!");
		}
	}

	public void       init(Connection c)
	  throws SQLException
	{
		populate(c, "hsqldb.sql");
	}

	public void       init(AbstractComboPooledDataSource ds)
	{
		ds.setUser("SA");   //<-- default user
		ds.setPassword(""); //<-- empty password
	}

	public void       test(Connection c)
	  throws SQLException
	{
		try(Statement s = c.createStatement())
		{
			s.execute("select 1 from INFORMATION_SCHEMA.SYSTEM_USERS");
		}
	}
}