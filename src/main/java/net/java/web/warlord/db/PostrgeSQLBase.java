package net.java.web.warlord.db;

/* Java */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/* Postgres SQL */

import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.io.IO;


/**
 * Dialect of PostgreSQL database server.
 * In your sub-class define the method
 * of obtaining database URL.
 *
 * @author anton.baukin@gmail.com
 */
public abstract class PostrgeSQLBase extends DatabaseDefault
{
	/* Database Connectivity */

	public String driver()
	{
		return "org.postgresql.Driver";
	}

	public void   init(Connection c)
	  throws SQLException
	{
		populate(c, "postgresql.sql");
	}


	/* Database Dialect */

	public String getName()
	{
		return "PostgreSQL";
	}

	public Lob    createLob(InputStream i)
	{
		return new PgLob(i);
	}

	public long   readLob(Object b, OutputStream s)
	  throws SQLException, IOException
	{
		long x = 0L;

		//?: {no bytes}
		if(b == null)
			return 0L;

		//?: {binary stream}
		if(b instanceof InputStream)
			x = IO.pump((InputStream)b, s);
		//?: {direct bytes}
		else if(b instanceof byte[])
		{
			s.write((byte[]) b);
			x = ((byte[]) b).length;
		}
		//?: {large object}
		else if(b instanceof PgOID)
		{
			//~: large objects manager of native postgres connection
			LargeObjectManager lom = (((PgOID)b).co).getLargeObjectAPI();

			//~: open large object for read
			LargeObject lo = lom.open(((PgOID)b).oid, LargeObjectManager.READ);

			try //~: dump the bytes
			{
				byte[] bb = new byte[64 * 1024];

				for(int y;((y = lo.read(bb, 0, bb.length)) > 0);)
					s.write(bb, 0, y);
			}
			finally
			{
				lo.close();
			}
		}

		return x;
	}

	public Object result(ResultSet rs, ResultSetMetaData m, int c)
	  throws SQLException
	{
		//?: {is BYTEA type}
		if(m.getColumnType(c) == Types.BINARY)
			return rs.getBinaryStream(c);

		//?: {is large object}
		if("oid".equals(m.getColumnTypeName(c)))
			return new PgOID(rs.getLong(c),
			  pgco(rs.getStatement().getConnection()));

		return rs.getObject(c);
	}


	/* Postgres Large Objects */

	public static class PgLob implements Lob
	{
		public PgLob(InputStream i)
		{
			this.i = i;
		}

		public final InputStream i;


		/* Large Object Wrapper */

		public void set(PreparedStatement s, int j)
		  throws SQLException, IOException
		{
			if(i == null)
			{
				s.setNull(j, Types.BIGINT);
				return;
			}

			//~: large objects manager of native postgres connection
			LargeObjectManager lom = pgco(s.getConnection()).getLargeObjectAPI();

			//~: create large object
			long oid = lom.createLO();

			//~: open large object for write
			LargeObject lo = lom.open(oid, LargeObjectManager.WRITE);

			try //~: write the data
			{
				byte[] bb = new byte[64 * 1024];

				for(int x;((x = i.read(bb)) > 0);)
					lo.write(bb, 0, x);
			}
			finally
			{
				lo.close();
			}

			//~: set the oid
			s.setLong(j, oid);
		}

		public void close()
		  throws Exception
		{
			if(i != null)
				i.close();
		}
	}

	protected static PGConnection pgco(Connection co)
	  throws SQLException
	{
		return EX.assertn(
		  co.unwrap(PGConnection.class),
		  "PostgreSQL connection is unavailable!"
		);
	}

	public static class PgOID
	{
		public final long oid;

		public final PGConnection co;

		public PgOID(long oid, PGConnection co)
		{
			this.oid = oid;
			this.co = co;
		}
	}
}