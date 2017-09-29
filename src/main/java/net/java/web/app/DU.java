package net.java.web.app;

/* Java */

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Date and time utilities of the application.
 *
 * @author anton.baukin@gmail.com
 */
public class DU
{
	/* Formatting */

	/**
	 * Returns ISO 8601 formatted timestamp in UTC time zone.
	 */
	public static String at(long ts)
	{
		return ZonedDateTime.ofInstant(
		  Instant.ofEpochMilli(ts),
		  ZoneId.systemDefault()).
		  format(DateTimeFormatter.ISO_INSTANT);
	}
}