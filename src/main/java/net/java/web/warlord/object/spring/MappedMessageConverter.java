package net.java.web.warlord.object.spring;

/* Java */

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/* Spring Framework */

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.io.IO;
import net.java.web.warlord.io.Streams.BytesStream;
import net.java.web.warlord.object.Json;
import net.java.web.warlord.object.Mapped;
import net.java.web.warlord.object.OU;
import net.java.web.warlord.object.Unmapped;


/**
 * Spring HTTP message converter that Supports
 * {@link Mapped} and {@link Unmapped} objects
 * along as regular Java Maps. It makes JSON.
 *
 * @author anton.baukin@gmail.com
 */
public class MappedMessageConverter
  implements HttpMessageConverter<Object>
{
	/**
	 * The only supported media type.
	 */
	public static final MediaType JS =
	  MediaType.APPLICATION_JSON_UTF8;


	/* HTTP Message Converter */

	public boolean canRead(Class<?> c, MediaType mt)
	{
		if(mt != null) //?: {media type is defined}
			//?: {not a json type}
			if(!JS.equals(mt))
				return false;

		return Map.class.isAssignableFrom(c) ||
		  Unmapped.class.isAssignableFrom(c);
	}

	public boolean canWrite(Class<?> c, MediaType mt)
	{
		//?: {media type is defined & not all}
		if(mt != null && !MediaType.ALL.equals(mt))
			//?: {json type is not compatible}
			if(!JS.isCompatibleWith(mt))
				return false;

		return Map.class.isAssignableFrom(c) ||
		  Mapped.class.isAssignableFrom(c)   ||
		  List.class.isAssignableFrom(c); //<-- arrays too
	}

	public List<MediaType> getSupportedMediaTypes()
	{
		return Collections.singletonList(JS);
	}

	public Object  read(Class<?> c, HttpInputMessage i)
	  throws IOException, HttpMessageNotReadableException
	{
		try
		{
			//~: json-decode the body
			Object o = Json.s2o(i.getBody());

			//?: {it's not a map}
			EX.assertx(o instanceof Map, "Can't JSON",
			  "-read not a map type: ", c.getName());

			//?: {a map is requested}
			if(Map.class.isAssignableFrom(c))
				//?: {it's that map}
				if(c.isInterface() || c.equals(o.getClass()))
					return o;
				else
				{
					//~: requested map instance
					Object x = c.newInstance();

					//~: put all the fields
					OU.map(x).putAll(OU.map(o));
					return x; //<-- give it
				}

			//?: {not an Unmapped requested}
			EX.assertx(Unmapped.class.isAssignableFrom(c));

			//~: create unmapped & fill it
			Unmapped u = (Unmapped)c.newInstance();
			u.unmap(OU.map(o));

			return u;
		}
		catch(Throwable e)
		{
			Throwable x = EX.xrt(e); //<-- unwrap the error

			//?: {io exception}
			if(x instanceof IOException)
				throw (IOException)x;
			else
				throw new HttpMessageNotReadableException(EX.e2en(e), x);
		}
	}

	public void    write(Object obj, MediaType ct, HttpOutputMessage o)
	  throws IOException, HttpMessageNotWritableException
	{
		try(BytesStream bs = new BytesStream())
		{
			//~: json-encode the object to bytes
			Json.o2s(obj, bs);

			//=: content type
			o.getHeaders().setContentType(JS);

			//=: content length
			o.getHeaders().setContentLength(bs.length());

			//~: expires now & no cache
			o.getHeaders().setExpires(0L);
			o.getHeaders().setCacheControl("no-cache, max-age=0");

			//~: pump the bytes to the body
			IO.pump(bs.inputStream(), o.getBody());
		}
		catch(Throwable e)
		{
			Throwable x = EX.xrt(e); //<-- unwrap the error

			//?: {io exception}
			if(x instanceof IOException)
				throw (IOException)x;
			else
				throw new HttpMessageNotWritableException(EX.e2en(e), x);
		}
	}
}