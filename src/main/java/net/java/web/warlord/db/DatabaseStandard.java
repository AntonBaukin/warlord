package net.java.web.warlord.db;

/* Java */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/* Warlord */

import net.java.web.warlord.EX;


/**
 * Database that follows SQL and Jdbc standards on BLOBs.
 *
 * @author anton.baukin@gmail.com
 */
public abstract class DatabaseStandard extends DatabaseDefault
{
	/* Database Dialect */

	public Lob    createLob(InputStream i)
	{
		return new LobStd(i);
	}

	public long   readLob(Object b, OutputStream s)
	  throws SQLException, IOException
	{
		//?: {no content}
		if(b == null) return 0L;

		try(InputStream i = ((Blob)b).getBinaryStream())
		{
			byte x[] = new byte[512];
			long z   = 0L;

			for(int w;(w = i.read(x)) > 0;z += w)
				s.write(x, 0, w);

			return z;
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
		finally
		{
			//!: free the blob
			((Blob)b).free();
		}
	}

	public Object result(ResultSet rs, ResultSetMetaData m, int c)
	  throws SQLException
	{
		return rs.getObject(c);
	}
}