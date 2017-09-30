package net.java.web.warlord.object;

/* Java */

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* Spring Framework */

import org.springframework.web.bind.annotation.ResponseBody;

/* Warlord */

import net.java.web.warlord.EX;


/**
 * Simplifies building Maps for JSON objects.
 *
 * @author anton.baukin@gmail.com
 */
public class MapBuilder implements Mapped
{
	/**
	 * Resulting mb.
	 */
	public final Map<String, Object> map;

	/**
	 * Parent map builder.
	 */
	public final MapBuilder parent;

	public MapBuilder()
	{
		this.parent = null;
		this.map = new LinkedHashMap<>();
	}

	public MapBuilder(MapBuilder parent, Map<String, Object> map)
	{
		this.parent = parent;
		this.map = EX.assertn(map);
	}


		/* Mapped */

	public Map<String, Object> map()
	{
		return this.map;
	}


		/* Map Builder */

	/**
	 * Assigns a property.
	 */
	public MapBuilder put(String k, Object v)
	{
		//?: {mapped object}
		if(v instanceof Mapped)
			v = ((Mapped)v).map();

		map.put(EX.asserts(k), v);
		return this;
	}

	/**
	 * Assigns all the properties removing nulls.
	 */
	public MapBuilder put(Map<String, Object> ext)
	{
		for(Map.Entry<String, Object> e : ext.entrySet())
		{
			EX.asserts(e.getKey());

			if(e.getValue() == null)
				map.remove(e.getKey());
			else
				this.put(e.getKey(), e.getValue());
		}

		return this;
	}

	/**
	 * Nests the object if it doesn't exist
	 * and returns the builder for it.
	 */
	public MapBuilder nest(String k)
	{
		Object m = map.get(k);

		//?: {object is defined}
		if(m != null)
			EX.assertx(m instanceof Map);
		else
			this.put(k, m = new LinkedHashMap());

		return new MapBuilder(this, OU.map(m));
	}

	/**
	 * Nests list by the key. or takes the existing
	 * one, and returns the list builder.
	 */
	public ListBuilder list(String k)
	{
		Object l = map.get(k);

		//?: {object is defined}
		if(l != null)
			EX.assertx(l instanceof List);
		else
			this.put(k, l = new ArrayList());

		return new ListBuilder(this, (List<Object>) l);
	}

	/**
	 * Returns the parent builder, or raises error
	 * if it has no parent.
	 */
	public MapBuilder up()
	{
		return EX.assertn(parent);
	}

	/**
	 * Assigns all the properties removing nulls.
	 * For objects (i.e., tha maps), goes deeply.
	 */
	public MapBuilder deep(Map<String, Object> ext)
	{
		deep(this.map, ext, null);
		return this;
	}

	private void      deep(Map obj, Map ext, Object key)
	{
		//?: {assign each key recursively}
		if(key == null)
		{
			for(Object k : ext.keySet())
				deep(obj, ext, k);
			return;
		}

		Object o = obj.get(key);
		Object e = ext.get(key);

		//?: {extending object is mapped}
		if(e instanceof Mapped)
			e = ((Mapped)e).map();

		//?: {deeply extend the maps}
		if((o instanceof Map) && (e instanceof Map))
			deep((Map) o, (Map) e, null);
			//?: {remove the key}
		else if(e == null)
			obj.remove(key);
			//~: plain assign
		else
			obj.put(key, e);
	}

	public String     toString()
	{
		return Json.o2s(map);
	}
}