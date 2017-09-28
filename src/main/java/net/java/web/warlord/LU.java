package net.java.web.warlord;

/* Logging for Java */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Wrapper around logging framework.
 *
 * @author anton.baukin@gmail.com.
 */
public class LU
{
	public static String sig(Object obj)
	{
		return (obj == null)?("NULL"):SU.cat(obj.getClass().getSimpleName(), '@',
		  Integer.toUnsignedString(System.identityHashCode(obj), 16).toUpperCase());
	}

	public static Object logger(String name)
	{
		return LogManager.getLogger(name);
	}

	public static Object logger(Class cls)
	{
		return LogManager.getLogger(cls);
	}

	public static Object logger(Object obj)
	{
		return logger((obj == null)?(null):(obj.getClass()));
	}

	public static void   debug(Object logger, Object... msg)
	{
		((Logger) logger).debug(SU.cat(msg));
	}

	public static void   info(Object logger, Object... msg)
	{
		((Logger) logger).info(SU.cat(msg));
	}

	public static void   warn(Object logger, Object... msg)
	{
		((Logger) logger).warn(SU.cat(msg));
	}

	public static void   error(Object logger, Object... msg)
	{
		((Logger) logger).error(SU.cat(msg));
	}

	public static <E extends Throwable> E
	                     error(Object logger, E e, Object... msg)
	{
		((Logger) logger).error(SU.cat(msg), e);
		return e;
	}
}