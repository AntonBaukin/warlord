package net.java.web.warlord.servlet.filter;

/* Warlord */

import net.java.web.warlord.EX;


/**
 * Implements a cycle of {@link Filter} invocations
 * that may be invoked recursively without repeating
 * the already passed filters, an not loosing a one.
 *
 * @author anton.baukin@gmail.com
 */
public class FilterCycle
{
	/* public: constructor */

	public FilterCycle(FilterTask task, Filter[] filters)
	{
		this.task    = task;
		this.filters = filters;
	}

	protected final FilterTask task;
	protected final Filter[]   filters;


	/* Filter Cycle */

	public void continueCycle()
	{
		//  Note that all the filters in the range
		//  [first; last] are invoked in this call,
		//  but the filters after 'last' (if any) are
		//  invoked when filter recursively continues
		//  the cycle.

		//0: open the filters
		Scope s = openFilters();

		//1: invoke the terminal
		invokeTerminal(s);

		//2: close the filters
		closeFilters(s);

		//?: {cycle is breaked with error}
		if(task.getError() != null)
			throw EX.wrap(task.getError());
	}

	protected int position;

	/**
	 * Terminal filter is invoked in the cycle after passing
	 * through all other filters. It is invoked if the cycle
	 * was not breaked, and there were no error.
	 *
	 * Note that a terminal filter is not in the list
	 * of the filters this cycle was created with.
	 */
	public Filter getTerminal()
	{
		return terminal;
	}

	protected Filter terminal;

	protected boolean terminalInvoked;

	public void setTerminal(Filter terminal)
	{
		this.terminal = terminal;
	}


	/* protected: details of processing */

	protected class Scope
	{
		public int first;
		public int last;
	}

	protected Scope openScope()
	{
		Scope s = new Scope();
		s.first = s.last = position;
		return s;
	}

	protected Scope openFilters()
	{
		Scope s = openScope();

		//~: open the filters left to call
		while(position < filters.length)
		{
			//?: {we need to break the cycle}
			if(task.isBreaked())
				break;

			//~: save the position of last invoked filter
			s.last = position++;

			try
			{
				filters[s.last].openFilter(task);
			}
			catch(Throwable e)
			{
				handleOpenError(s, e);
			}
		}

		return s;
	}

	protected void  handleOpenError(Scope s, Throwable e)
	{
		task.setError(EX.xrt(e));

		//!: break the cycle
		task.doBreak();
	}

	protected void  invokeTerminal(Scope s)
	{
		//?: {chain is broken}
		if(task.isBreaked())
			return;

		Filter terminal = getTerminal();

		//?: {have no terminal | done it}
		if(terminal == null || terminalInvoked)
			return;

		//~: invoke the terminal filter
		try
		{
			terminalInvoked = true;
			terminal.openFilter(task);
		}
		catch(Throwable e)
		{
			handleTerminalError(s, e, false);
		}
		finally
		{
			try
			{
				terminal.closeFilter(task);
			}
			catch(Throwable e)
			{
				handleTerminalError(s, e, true);
			}
		}
	}

	protected void  handleTerminalError(Scope s, Throwable e, boolean closing)
	{
		task.setError(e);
	}

	protected void  closeFilters(Scope s)
	{
		final int last = Math.min(s.last, filters.length - 1);

		//~: close the filters of our range [first; last]
		for(int i = last;(i >= s.first);i--) try
		{
			filters[i].closeFilter(task);
		}
		catch(Throwable e)
		{
			handleCloseError(s, e);
		}
	}

	protected void  handleCloseError(Scope s, Throwable e)
	{
		if(task.getError() == null)
			task.setError(e);
		else
			task.getError().addSuppressed(e);
	}
}