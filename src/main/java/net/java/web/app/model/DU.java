package net.java.web.app.model;

/* Java */

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

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

	public static final TimeZone TZ_UTC =
	  TimeZone.getTimeZone(Z_UTC);

	public static final DateTimeFormatter ISO_FMT =
		new DateTimeFormatterBuilder().
		  appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").
		  toFormatter();

	/**
	 * Returns ISO 8601 formatted timestamp in UTC time zone.
	 */
	public static String ts(Date ts)
	{
		return (ts == null)?(null):
		  ZonedDateTime.ofInstant(ts.toInstant(), Z_UTC).
		  format(ISO_FMT);
	}

	/**
	 * Parses string to date, clears the time to zeros.
	 */
	public static Date s2d(String s)
	{
		if(SU.ises(s)) //?: {date is not given}
			return null;

		//~: create the calendar
		Calendar c = GregorianCalendar.getInstance(TZ_UTC);
		c.setTime(Date.from(Instant.parse(s)));

		//~: clear the time
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		return c.getTime();
	}
}