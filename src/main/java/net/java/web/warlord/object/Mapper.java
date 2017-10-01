package net.java.web.warlord.object;

/* Java */

import java.util.ArrayList;
import java.util.Map;

/* Warlord */

import net.java.web.warlord.EX;


/**
 * Collects mapping and un-mapping methods
 * for single entity.
 *
 * @author anton.baukin@gmail.com
 */
public class Mapper<O extends Unmapped>
{
	/**
	 * Reads single field.
	 */
	@FunctionalInterface
	public interface Get<O>
	{
		Object get(O o);
	}

	/**
	 * Writes single field.
	 */
	@FunctionalInterface
	public interface Set<O>
	{
		void set(O o, Object v);
	}

	public static class Entry<O>
	{
		public final String f;

		public final Get<O> g;

		public final Set<O> s;

		public Entry(String f, Get<O> g, Set<O> s)
		{
			this.f = EX.asserts(f);
			this.g = EX.assertn(g);
			this.s = EX.assertn(s);
		}

		public boolean equals(Object o)
		{
			return (this == o) || (
			  (o instanceof Entry) && ((Entry)o).f.equals(f)
			);
		}
	}

	/**
	 * Gets object's fields into the mapping.
	 * If fields is already in the map, the
	 * call by this field is skipped.
	 */
	public void get(O o, Map<String, Object> m)
	{
		//c: invoke the entries
		for(Entry<O> e : entries)
			if(!m.containsKey(e.f))
			{
				Object v = e.g.get(o);

				if(v != null) //?: {has value deined}
					m.put(e.f, v);
			}
	}

	/**
	 * Assigns fields from the mapping to the object.
	 */
	public void set(O o, Map<String, Object> m)
	{
		//c: invoke the entries
		for(Entry<O> e : entries)
			if(m.containsKey(e.f))
				e.s.set(o, m.get(e.f));
	}

	/**
	 * Adds field get and set strategies.
	 * This call is not thread-safe, but at the
	 * most cases it's done from entity class
	 * static initialization section that is safe.
	 */
	public void map(String field, Get<O> g, Set<O> s)
	{
		//~: ensure this field is not in the list
		final Entry<O> e = new Entry<>(field, g, s);
		EX.assertx(!entries.contains(e));

		//~: append this entry
		entries.add(e);
	}

	protected final ArrayList<Entry<O>> entries =
	  new ArrayList<>(8);
}