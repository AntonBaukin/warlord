package net.java.web.warlord.servlet.filter;

/* Java Annotations */

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/* Java Servlet */

import javax.servlet.ServletContext;

/* Spring Framework */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/* Warlord */

import net.java.web.warlord.object.BeanTracker;


/**
 * Picked Filter that wraps else filter that
 * is dynamically assigned and revoked. Is a
 * placeholder in the global filters chain.
 *
 * @author anton.baukin@gmail.com.
 */
@Component @Scope("prototype")
public class ProxyFilter extends PickedFilter
{
	public void openFilter(FilterTask task)
	{
		final Filter f = this.filter;
		if(f != null)
			f.openFilter(task);
	}

	public void closeFilter(FilterTask task)
	{
		final Filter f = this.filter;
		if(f != null)
			f.closeFilter(task);
	}

	public void setServletContext(ServletContext ctx)
	{
		final Filter f = this.filter;
		
		this.ctx = ctx;

		//?: {the filter is assigned}
		if(f != null)
			f.setServletContext(ctx);

		//?: {unbind the filter when context is destroyed}
		if((f != null) && (ctx == null))
			this.filter = null;
	}

	protected volatile ServletContext ctx;


	/* protected: initialization */

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


	/* Filter Proxy */

	public void setFilter(Filter f)
	{
		final ServletContext ctx = this.ctx;

		this.filter = f;

		if((f != null) && (ctx != null))
			f.setServletContext(ctx);
	}

	protected volatile Filter filter;
}