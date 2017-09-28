package net.java.web.warlord.servlet;

/* Java Servlets */

import javax.servlet.ServletContext;

/* Warlord */

import net.java.web.warlord.EX;


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
}