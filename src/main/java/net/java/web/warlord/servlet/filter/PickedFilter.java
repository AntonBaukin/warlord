package net.java.web.warlord.servlet.filter;

/* Java */

import java.lang.annotation.Annotation;

/* Warlord */

import net.java.web.warlord.object.AutoAwire;


/**
 * Filter base that is {@link AutoAwire}
 * with dynamic {@link PickFilter}.
 *
 * @author anton.baukin@gmail.com.
 */
public abstract class PickedFilter implements Filter, AutoAwire
{
	/* Picked Filter */

	public PickFilter pickFilter()
	{
		return pickFilter;
	}

	protected PickFilter pickFilter;


	/* Autowire Aware */

	public void autowiredAnnotations(Object injector, Annotation[] ans)
	{
		this.callMe(injector, ans);

		for(Annotation a : ans)
			if(a instanceof PickFilter)
				this.pickFilter = (PickFilter) a;
	}
}