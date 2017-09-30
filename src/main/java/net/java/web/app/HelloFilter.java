package net.java.web.app;

/* Spring Framework */

import org.springframework.stereotype.Component;

/* Warlord */

import net.java.web.warlord.object.OU;
import net.java.web.warlord.servlet.Req;
import net.java.web.warlord.servlet.filter.FilterTask;
import net.java.web.warlord.servlet.filter.PickedFilter;


/**
 * Sample "Hello, World!" filter.
 *
 * @author anton.baukin@gmail.com
 */
@Component
public class HelloFilter extends PickedFilter
{
	public void openFilter(FilterTask task)
	{
		String c = task.getRequest().getContextPath();
		String u = task.getRequest().getRequestURI();

		//?: {not the hello page}
		if(!u.equals(c + "/hello"))
			return;

		//?: {not a GET request}
		if(!Req.isGet(task.getRequest()))
		{
			task.getResponse().setStatus(400);
			task.doBreak();
			return;
		}

		task.doBreak(); //!: notify the end

		//--> writes: { "message": { "text": "Hello..." }}
		Req.json(task.getResponse(), OU.mb().
		  nest("message").put("text", "Hello, World!").
		  up()
		);
	}
}