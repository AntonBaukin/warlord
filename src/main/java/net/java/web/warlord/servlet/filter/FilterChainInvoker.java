package net.java.web.warlord.servlet.filter;

/* Java Servlet */

import javax.servlet.FilterChain;

/* Warlord */

import net.java.web.warlord.EX;


/**
 * Terminal filter that invokes Servlet
 * filter chain thus continuing request
 * processing.
 *
 * @author anton.baukin@gmail.com
 */
public class FilterChainInvoker implements Filter
{
	/* public: constructor */

	public FilterChainInvoker(FilterChain chain)
	{
		this.chain = EX.assertn(chain);
	}


	/* public: Filter interface */

	public void openFilter(FilterTask task)
	{
		try
		{
			chain.doFilter(task.getRequest(), task.getResponse());
		}
		catch(Throwable e)
		{
			task.setError(e);
			task.doBreak();
		}
	}

	public void closeFilter(FilterTask task)
	{}


	/* protected: the chain */

	protected final FilterChain chain;
}