package net.java.web.app;

/* Spring Framework */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.servlet.Req;
import net.java.web.warlord.servlet.filter.FilterTask;
import net.java.web.warlord.servlet.filter.PickedFilter;


/**
 * Filter that simply redirects
 * to the index page.
 *
 * @author anton.baukin@gmail.com.
 */
@Component
public class IndexFilter extends PickedFilter
{
	public void      openFilter(FilterTask task)
	{
		String p = task.getRequest().getRequestURI();
		String c = task.getRequest().getContextPath();

		if(p.endsWith("/")) //?: {trailing slash}
			p = p.substring(0, p.length() - 1);

		if(c.equals(p)) try
		{
			//?: {not a GET request}
			if(!Req.isGet(task.getRequest()))
			{
				task.getResponse().setStatus(400);
				task.doBreak();
				return;
			}

			//~: redirect & break
			sendRedirect(task);
			task.doBreak();
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}

	protected void   sendRedirect(FilterTask task)
	  throws Throwable
	{
		String p = EX.asserts(indexPage(task));
		String c = task.getRequest().getContextPath();

		//?: {has no context path}
		if(!p.startsWith(c))
			p = p.startsWith("/")?(c + p):(c + "/" + p);

		//~: do redirect
		task.getResponse().sendRedirect(p);
	}

	protected String indexPage(FilterTask task)
	{
		//…: somehow select the index page depending
		//   on the user logged in and so...

		return "/index";
	}

	@Autowired
	protected ApplicationContext context;
}