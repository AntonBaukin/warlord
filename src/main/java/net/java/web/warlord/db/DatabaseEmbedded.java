package net.java.web.warlord.db;

/* Java */

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.LU;
import net.java.web.warlord.SU;
import net.java.web.warlord.servlet.Req;


/**
 * Standard database with the embedded server that
 * stores the database files within the application
 * server's folder that is defined here.
 *
 * @author anton.baukin@gmail.com
 */
public abstract class DatabaseEmbedded extends DatabaseStandard
{
	/* Database Connectivity */

	public String initDbURL()
	{
		String p = EX.asserts(getUrlPrefix());
		if(!p.endsWith(":")) p += ":";

		try
		{
			return p + findDbFolder().toURI().toString();
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}

	public void   start()
	{
		//~: side-effect to create the folder
		initDbURL();
	}

	public void   stop()
	{
		//?: {no shutdown command}
		if(SU.ises(getShutdownCommand()))
			return;

		//~: shutdown the database
		try(Connection c = connect())
		{
			try(Statement s = c.createStatement())
			{
				s.execute(getShutdownCommand());
			}
		}
		catch(Throwable e)
		{
			throw EX.wrap(e, "Error while shutting down ",
			  "embedded ", getName(), " database!");
		}
	}



	/* protected: embedded database */

	/**
	 * Returns the connection URL prefix, such
	 * as "jdbc:hsqldb:" for HyperSQL.
	 */
	protected abstract String getUrlPrefix();

	/**
	 * Returns SQL command to close the embedded database.
	 * Return null to disable the default stop behaviour.
	 */
	protected String getShutdownCommand()
	{
		return "shutdown";
	}

	/**
	 * Locates (and creates on demand) a folder somewhere
	 * around the server to populate with the embedded database
	 * files. This folder is referred as URL when connecting.
	 */
	protected File   findDbFolder()
	  throws IOException
	{
		String n = getAppConfiguredFolder();

		if(SU.ises(n)) //?: {no user-defined config}
			n = SU.cat(getServerRoot(), File.separator, getAppName(),
			  "-", getName().toLowerCase(), "-db");

		//?: {directory doesn't exist}
		File d; if(!(d = new File(n)).exists())
		{
			//~: create with all intermediate folders
			EX.assertx(d.mkdirs(), "Unable to create directory [",
			  d.getAbsolutePath(), "] to store embedded database files!");

			LU.info(LU.logger(this), "create directory for embedded",
			  " database files: [", d.getAbsolutePath(), "]");
		}

		//?: {is not a directory}
		EX.assertx(d.isDirectory(), "not a folder for embedded",
		  " database files [", d.getAbsolutePath(), "]!");

		//?: {can't write to that folder}
		EX.assertx(d.canWrite(), "can't create files in embedded",
		  " database folder [", d.getAbsolutePath(), "]!");

		return d.getCanonicalFile();
	}

	protected String getAppName()
	{
		//~: application context path
		String cp = Req.context().getContextPath();
		EX.assertx(cp.startsWith("/"));

		//~: application name
		return (cp.length() == 1)?("rootapp"):
		  cp.substring(1).replace("/", "-");
	}

	protected String getAppConfiguredFolder()
	{
		return System.getProperty(getAppName() + ".database");
	}

	protected String getServerRoot()
	{
		String s = System.getProperty("catalina.home");

		if(SU.ises(s)) //?: {not in tomcat home}
			s = System.getProperty("catalina.base");

		if(SU.ises(s)) //?: {not in tomcat distribution}
			s = System.getProperty("catalina.base");

		//<-- return or fallback to the work directory
		return SU.ises(s)?("."):(s);
	}
}