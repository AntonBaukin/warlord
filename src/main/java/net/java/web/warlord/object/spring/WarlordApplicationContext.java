package net.java.web.warlord.object.spring;

/* Spring Framework */

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.context.support.XmlWebApplicationContext;


/**
 * Spring web application Context that installs
 * {@link WarlordBeanFactory} instead of the default.
 *
 * @author anton.baukin@gmail.com
 */
public class WarlordApplicationContext extends XmlWebApplicationContext
{
	/* protected: Refreshable Application Context */

	protected DefaultListableBeanFactory createBeanFactory()
	{
		return new WarlordBeanFactory(getInternalParentBeanFactory());
	}
}