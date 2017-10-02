package net.java.web.app.model;

/* Java */

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/* Warlord */

import net.java.web.warlord.SU;


/**
 * Date and time utilities of the application.
 *
 * @author anton.baukin@gmail.com
 */
public class DU
{
	/* Formatting */

	public static final ZoneId Z_UTC =
	  ZoneId.of("UTC");

	/**
	 * Returns ISO 8601 formatted timestamp in UTC time zone.
	 */
	public static String ts(Date ts)
	{
		return (ts == null)?(null):
		  ZonedDateTime.ofInstant(ts.toInstant(), Z_UTC).
		  format(DateTimeFormatter.ISO_INSTANT);
	}

	/**
	 * Parses string to date, clears the time to zeros.
	 */
	public static Date s2d(String s)
	{
		if(SU.ises(s))
			return null;

		Calendar cl = GregorianCalendar.from(
		  ZonedDateTime.ofInstant(Instant.parse(s), Z_UTC));

		cl.set(Calendar.HOUR_OF_DAY,  0);
		cl.set(Calendar.MINUTE,       0);
		cl.set(Calendar.SECOND,       0);
		cl.set(Calendar.MILLISECOND,  0);

		return cl.getTime();
	}
}