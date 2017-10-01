package net.java.web.warlord.servlet.filter;

/* Java Annotations */

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/* Spring Framework */

import org.springframework.beans.factory.annotation.Autowired;


/* Warlord */

import net.java.web.warlord.object.BeanTracker;


/**
 * Picked Filter (being a prototype bean) that
 * is globally tracked with {@link BeanTracker}.
 *
 * @author anton.baukin@gmail.com
 */
public abstract class PickedFilterTracked extends PickedFilter
{
	/* protected: bean tracking */

	@Autowired
	protected BeanTracker beanTracker;

	@PostConstruct
	protected void register()
	{
		beanTracker.add(this);
	}

	@PreDestroy
	protected void close()
	{
		beanTracker.remove(this);
	}
}