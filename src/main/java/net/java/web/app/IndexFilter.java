package net.java.web.app;

/* Spring Framework */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/* Warlord */

import net.java.web.warlord.EX;
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

		if(p.isEmpty() || "/".equals(p)) try
		{
			//?: {not a GET request}
			if(!task.isGet())
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
		//~: do redirect
		task.getResponse().sendRedirect(
		  EX.asserts(indexPage(task)));
	}

	protected String indexPage(FilterTask task)
	{
		//…: somehow select the index page depending
		//   on the user logged in and so...

		return "/index.html";
	}

	@Autowired
	protected ApplicationContext context;
}