package net.java.web.warlord.servlet.filter;

/* Java */

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.LU;


/**
 * Implementation base of filters search strategy
 * that abstracts the method of collecting them.
 *
 * @author anton.baukin@gmail.com.
 */
public abstract class FiltersPointBase implements FiltersPoint
{
	/* Filters Point */

	public Filter[] getFilters(FilterStage stage)
	{
		synchronized(this.filters)
		{
			//?: {not processed yet}
			if(filters.isEmpty())
				findFilters();

			return filters.get(stage);
		}
	}


	/* protected: implementation base */

	protected abstract Filter[] collectFilters();

	protected void findFilters()
	{
		//~: pick up the filters
		Filter[] fs = collectFilters();

		//~: (stage -> order -> filter) mapping
		Map<FilterStage, Map<Integer, Filter>> filters = new HashMap<>();

		//c: collect the filters
		for(Filter f : fs)
		{
			//~: the annotation on instance-level
			PickFilter pf = f.pickFilter();

			//?: {class-level annotation}
			if(pf == null)
				pf = f.getClass().getAnnotation(PickFilter.class);

			//?: {not found}
			if(pf == null)
			{
				LU.warn(LOG, "skip filter ", LU.sig(f),
				  " as it's not marked with @PickFilter!");

				continue;
			}

			//?: {annotation is wrong}
			EX.assertx(pf.order().length != 0);
			EX.assertx(pf.order().length == pf.stage().length);

			//c: process the configured stages
			for(int i = 0;(i < pf.order().length);i++)
			{
				FilterStage s = pf.stage()[i];
				Integer     o = pf.order()[i];

				//~: take the stage-mapping
				Map<Integer, Filter> m = filters.get(s);
				if(m == null) filters.put(s, m = new TreeMap<>());

				//?: {got filter with the same order}
				EX.assertx(!m.containsKey(o), "Filter [", LU.logger(f),
				  "] is registered by the occupied order [",
				  o, "] at the stage ", s, "!");

				m.put(o, f);
			}
		}

		//~: resulting filters
		for(Map.Entry<FilterStage, Map<Integer, Filter>> e : filters.entrySet())
			this.filters.put(e.getKey(), e.getValue().values().
			  stream().toArray(Filter[]::new));

		logFilters(filters);
	}

	protected void logFilters(Map<FilterStage, Map<Integer, Filter>> m)
	{
		if(m.isEmpty())
		{
			LU.warn(LOG, LU.sig(this), " found no filters!");
			return;
		}

		StringBuilder s = new StringBuilder(128);

		for(Map.Entry<FilterStage, Map<Integer, Filter>> fs : m.entrySet())
			for(Map.Entry<Integer, Filter> f : fs.getValue().entrySet())
			{
				if(s.length() != 0) s.append("\n");

				s.append(fs.getKey());
				s.append(String.format("\t%5d", f.getKey()));
				s.append('\t').append(LU.sig(f.getValue()));
			}

		LU.debug(LOG, LU.sig(this), " filters found: \n", s);
	}

	/**
	 * The filters for each stage collected in the call-order.
	 */
	protected final Map<FilterStage, Filter[]> filters = new HashMap<>();

	protected final Object LOG = LU.logger(this);
}