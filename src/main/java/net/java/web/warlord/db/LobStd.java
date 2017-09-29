package net.java.web.warlord.db;

/* Java */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/* Warlord */

import net.java.web.warlord.EX;


/**
 * Large object of BLOB.
 *
 * @author anton.baukin@gmail.com.
 */
public class LobStd implements Lob
{
	public LobStd(InputStream stream)
	{
		this.stream = stream;
	}


	/* Large Object Wrapper */

	public void set(PreparedStatement s, int c)
	  throws SQLException, IOException
	{
		//?: {null content}
		if(stream == null)
		{
			s.setNull(c, Types.BLOB);
			return;
		}

		//~: write the stream
		try(InputStream i = stream)
		{
			//~: create the blob
			Blob b = s.getConnection().createBlob();

			//~: set binary stream
			try(OutputStream o = b.setBinaryStream(1L))
			{
				byte x[] = new byte[512];
				for(int w;(w = i.read(x)) > 0;)
					o.write(x, 0, w);
			}

			//~: assign the parameter
			s.setBlob(1, b);
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}

	protected final InputStream stream;

	public void close()
	  throws Exception
	{
		if(stream != null)
			stream.close();
	}
}