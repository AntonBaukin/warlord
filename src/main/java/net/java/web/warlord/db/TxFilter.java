package net.java.web.warlord.db;

/* Spring Framework */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.servlet.filter.FilterTask;
import net.java.web.warlord.servlet.filter.PickedFilterTracked;


/**
 * Filter that opens transactional scopes
 * for certain incoming requests.
 *
 * @author anton.baukin@gmail.com.
 */
@Component @Scope("prototype")
public class TxFilter extends PickedFilterTracked
{
	/* Filter */

	public void openFilter(FilterTask task)
	{
		EX.assertn(task);

		if(!isTxRequest(task))
			return;

		//~: nest the cycle in the tx-scopes
		context.getBean(TxBean.class).
		  run(task::continueCycle);
	}

	/**
	 * Each context substring starting with '/'
	 * is a prefix of request path; else it's
	 * a suffix. Sample: '/get/' works for all
	 * requests starting with, '.jsx' works
	 * for all JsX requests.
	 */
	public void setContexts(String... contexts)
	{
		for(String s : contexts) EX.asserts(s);
		this.contexts = contexts;
	}

	protected String[] contexts = new String[0];


	/* protected: filtering */

	@Autowired
	protected ApplicationContext context;

	protected boolean isTxRequest(FilterTask task)
	{
		String u = task.getRequest().getRequestURI();
		String c = task.getRequest().getContextPath();

		if(u.startsWith(c)) //?: {in-context request}
			u = u.substring(c.length());

		//c: search for the path within the context
		for(String ctx : contexts)
			//?: {context is a prefix}
			if(ctx.charAt(0) == '/')
			{
				//?: {request starts with this prefix}
				if(u.startsWith(ctx))
					return true;
			}
			//?: {request ends with the context suffix}
			else if(u.endsWith(ctx))
				return true;

		return false;
	}
}