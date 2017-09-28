package net.java.web.warlord.servlet.filter;

/**
 * A stage of Servlet Filters invocation.
 *
 * @author anton.baukin@gmail.com
 */
public enum FilterStage
{
	/**
	 * The stage of processing the outer
	 * HTTP request issued to the server.
	 */
	REQUEST,

	/**
	 * Internally included resource.
	 */
	INCLUDE,

	/**
	 * Internal forward redirection.
	 */
	FORWARD,

	/**
	 * Error response handling.
	 */
	ERROR
}