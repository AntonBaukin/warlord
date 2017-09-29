package net.java.web.warlord.object;

/* Java */

import java.util.ArrayList;
import java.util.List;

/* Warlord */

import net.java.web.warlord.EX;


/**
 * Simplifies building JSON arrays
 * (stored in Java as Lists).
 *
 * @author anton.baukin@gmail.com
 */
public class ListBuilder
{
	/**
	 * Resulting list.
	 */
	public final List<Object> list;

	/**
	 * Parent Map builder.
	 */
	public final MapBuilder parent;

	public ListBuilder()
	{
		this.parent = null;
		this.list = new ArrayList<>();
	}

	public ListBuilder(MapBuilder parent, List<Object> list)
	{
		this.parent = parent;
		this.list = EX.assertn(list);
	}


	/* List Builder */

	/**
	 * Returns the parent builder, or raises error
	 * if it has no parent.
	 */
	public MapBuilder  up()
	{
		return EX.assertn(parent);
	}

	public ListBuilder add(Object o)
	{
		list.add(o);
		return this;
	}

	public ListBuilder add(MapBuilder o)
	{
		list.add(EX.assertn(o).map);
		return this;
	}

	public String     toString()
	{
		return Json.o2s(list);
	}
}