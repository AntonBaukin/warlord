package net.java.web.warlord.io;

/* Java */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.io.Streams.BytesStream;
import net.java.web.warlord.io.Streams.CharBytes;


/**
 * Collection of input-output routines and helpers.
 *
 * @author anton.baukin@gmail.com
 */
public class IO
{
	/* Sink */

	@FunctionalInterface
	public interface Sink
	{
		void write(byte[] b, int o, int l)
			throws IOException;
	}


	/* Streaming */

	public static long pump(InputStream i, OutputStream o)
	  throws IOException
	{
		return pump(i, o::write);
	}

	public static long pump(InputStream i, Sink o)
	  throws IOException
	{
		byte[] b = Streams.BUFFERS.get();
		long   s = 0L;

		try
		{
			for(int x;((x = i.read(b)) > 0);s += x)
				o.write(b, 0, x);

			return s;
		}
		finally
		{
			Streams.BUFFERS.free(b);
		}
	}


	/* Base64 Decoding */

	/**
	 * Decodes given Base64 encoded string to the bytes.
	 */
	public static byte[] base64(String b64)
	  throws IOException
	{
		if(b64 == null || b64.isEmpty())
			return new byte[0];

		//~: decode the characters into buffer stream
		try(BytesStream b = new BytesStream())
		{
			pump(new Streams.Base64Decoder(new CharBytes(b64)), b);
			return b.bytes();
		}
	}

	public static String base64(byte[] b)
	{
		if(b == null || b.length == 0)
			return "";

		//~: encode the characters into single string
		try(BytesStream bs = new BytesStream())
		{
			Streams.Base64Encoder e =
			  new Streams.Base64Encoder(bs, 0, 128);

			//~: encode the bytes
			pump(new ByteArrayInputStream(b), e);

			//~: finish the stream
			bs.setNotCloseNext(true);
			e.close();

			//~: returns the bytes
			return new String(bs.bytes(), "ASCII");
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
 	}
}