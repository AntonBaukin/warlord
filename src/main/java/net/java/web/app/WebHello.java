package net.java.web.app;

/* Spring Framework */

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/* Warlord */

import net.java.web.warlord.object.OU;


/**
 * Sample Spring MVC controller.
 *
 * @author anton.baukin@gmail.com
 */
@Controller
public class WebHello
{
	@ResponseBody
	@RequestMapping(value = "/index", method = RequestMethod.GET)
	public Object index()
	{
		return OU.mb().
		  nest("message").put("text", "Hello, World!").
		  up();
	}
}