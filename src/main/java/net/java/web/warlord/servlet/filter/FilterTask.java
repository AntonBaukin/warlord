package net.java.web.warlord.servlet.filter;

/* Java Servlet */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Aggregates all the parameters of {@link Filter}
 * invocation. Also has internal reference to the
 * algorithm of filters invocation cycle.
 *
 * @author anton.baukin@gmail.com
 */
public interface FilterTask
{
	/* Filter Task */

	FilterStage getFilterStage();

	/**
	 * Call this method to continue the cycle from
	 * {@link Filter#openFilter(FilterTask)} without
	 * exiting the method.
	 *
	 * Allows to nest invocation context to create
	 * transaction scopes and else needs.
	 */
	void        continueCycle();

	/**
	 * Tells whether the cycle was breaked. Breaking
	 * may not be cancelled.
	 */
	boolean     isBreaked();

	/**
	 * Breaks the cycle. Has meaning only in
	 * {@link Filter#openFilter(FilterTask)} method.
	 *
	 * If the filter sets or raises an exception,
	 * the cycle is automatically breaked.
	 */
	void        doBreak();

	/**
	 * Returns the exception saved (or raised) in the task
	 * by one of the filters of the cycle.
	 */
	Throwable   getError();

	void        setError(Throwable error);


	/* Filter Task (access the request) */

	HttpServletRequest  getRequest();

	HttpServletResponse getResponse();
}