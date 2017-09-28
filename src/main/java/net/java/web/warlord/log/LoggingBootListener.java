package net.java.web.warlord.log;

/* Java Servlet */

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/* Logging for Java */

import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.web.Log4jWebSupport;
import org.apache.logging.log4j.web.WebLoggerContextUtils;


/**
 * Register this listener as the first to start.
 * It initializes Log4j2 facility in the same
 * way as it's web context fragment does, but
 * turns it off after Spring and other.
 *
 *
 * @author anton.baukin@gmail.com
 */
public class      LoggingBootListener
       implements ServletContextListener
{
	/* public: ServletContextListener interface */

	public void contextInitialized(ServletContextEvent event)
	{
		//~: access logging framework
		this.logLifeCycle = WebLoggerContextUtils.
		  getWebLifeCycle(event.getServletContext());

		//~: start it & register
		this.logLifeCycle.start();
		((Log4jWebSupport) this.logLifeCycle).setLoggerContext();
	}

	protected LifeCycle logLifeCycle;

	public void contextDestroyed(ServletContextEvent event)
	{
		try //~: start logging framework
		{
			this.logLifeCycle.stop();
		}
		finally
		{
			this.logLifeCycle = null;
		}
	}
}