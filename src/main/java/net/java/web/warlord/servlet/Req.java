package net.java.web.warlord.servlet;

/* Java Servlets */

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.io.IO;
import net.java.web.warlord.io.Streams.BytesStream;
import net.java.web.warlord.object.Json;


/**
 * Servlet Context shared reference and
 * request supporting routines.
 *
 * @author anton.baukin@gmail.com
 */
public class Req
{
	/**
	 * Public global reference to Servlet Context.
	 * Used within the inner layers, assigned on boot.
	 */
	public static volatile ServletContext context;

	public static ServletContext context()
	{
		return EX.assertn(context, "Servlet Context is not assigned!");
	}


	/* Request Helpers */

	public static boolean isGet(HttpServletRequest req)
	{
		return "GET".equalsIgnoreCase(req.getMethod());
	}


	/* Response Helpers */

	public static void noCache(HttpServletResponse res)
	{
		res.addHeader("Cache-Control", "no-cache, max-age=0");
		res.addHeader("Expires", "0");
	}

	/**
	 * Converts given object with {@link Json#o2s(Object)}
	 * writing it to the response with all the headers set.
	 * Response is not allowed to cache.
	 */
	public static void json(HttpServletResponse res, Object o)
	{
		try(BytesStream bs = new BytesStream())
		{
			//~: encode object to bytes stream
			Json.o2s(o, bs);

			noCache(res); //<-- forbid caching

			//~: json content type
			res.setContentType("application/json;charset=utf-8");
			res.setCharacterEncoding("UTF-8");

			//~: resulting length
			res.setContentLengthLong(bs.length());

			//~: pump the bytes to the socket
			IO.pump(bs.inputStream(), res.getOutputStream());
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}
}