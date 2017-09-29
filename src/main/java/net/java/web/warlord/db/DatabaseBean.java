package net.java.web.warlord.db;

/* Java */

import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;


/* C3p0 */

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.PooledDataSource;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.LU;
import net.java.web.warlord.servlet.Req;


/**
 * Bean that wraps {@link Database} instance
 * and handles start and shutdown procedures.
 *
 * @author anton.baukin@gmail.com
 */
public class DatabaseBean
{
	/**
	 * The database this bean is created for.
	 */
	public final Database database;

	public DatabaseBean(Database database)
	{
		this.database = EX.assertn(database);
	}


	/* Database Bean */

	public DataSource getDataSource()
	{
		return EX.assertn(dataSource);
	}

	protected DataSource dataSource;


	/* protected: database lifecycle */

	@PostConstruct
	protected void start()
	{
		//~: select the dialect
		LU.info(LU.logger(this), "using database",
		  " dialect: ", database.getName());

		try //~: probe for thr driver class
		{
			LU.debug(LU.logger(this), "native driver",
			  " class: ", database.driver());

			//!: load the class for side-effects
			Class.forName(database.driver());
		}
		catch(Throwable e)
		{
			throw EX.wrap(e, "Error while loading database",
			  " driver [", database.driver(), "]!");
		}

		//~: start the database
		database.start();

		//~: make the initial connection
		try(Connection c = database.connect())
		{
			database.init(c);
		}
		catch(Throwable e)
		{
			throw EX.wrap(e, "Error while opening initial ",
			  "connection and creating database schema!");
		}

		LU.info(LU.logger(this), "have initialized the database!");

		//~: create the data source
		this.dataSource = createDataSource();
	}

	@PreDestroy
	protected void shutdown()
	{
		RuntimeException error = null;

		try
		{
			//~: close the data source
			if(dataSource instanceof PooledDataSource)
				((PooledDataSource) dataSource).close();
		}
		catch(Throwable e)
		{
			error = EX.wrap(e, "Error while closing ",
			  "pooled Data Source!");
		}
		finally
		{
			dataSource = null;

			//~: shutdown the database
			try
			{
				database.close();
			}
			catch(Throwable e)
			{
				error = EX.wrap(e, "Error while shutting down the database!");
			}
		}

		if(error != null)
			throw error;
	}

	protected DataSource createDataSource()
	{
		try
		{
			Properties ps = new Properties();

			URL pu = EX.assertn(Req.context().
			  getResource("/WEB-INF/c3p0.xml"));

			//~: load the properties
			try(InputStream is = pu.openStream())
			{
				ps.loadFromXML(is);
			}

			//~: data source (private)
			ComboPooledDataSource ds =
			  new ComboPooledDataSource();

			//~: assign the proeprties
			ds.setProperties(ps);

			//~: the driver
			ds.setDriverClass(database.driver());

			//~: database url
			ds.setJdbcUrl(database.initDbURL());
			LU.info(LU.logger(this), "using database URL: ",
			  database.initDbURL());

			//~: callback to the dialect
			database.init(ds);

			//~: test the source
			LU.debug(LU.logger(this), "testing the database connection...");
			try(Connection c = ds.getConnection())
			{
				database.test(c);
			}

			return ds;
		}
		catch(Throwable e)
		{
			throw EX.wrap(e, "Error while creating C3p0 Data Source!");
		}
	}
}