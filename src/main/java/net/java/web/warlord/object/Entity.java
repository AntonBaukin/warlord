package net.java.web.warlord.object;

/* Java */

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.object.Mapper.Get;
import net.java.web.warlord.object.Mapper.Set;


/**
 * Abstract mapped entity with UUID key.
 *
 * @author anton.baukin@gmail.com
 */
public abstract class Entity implements Unmapped
{
	public String getUuid()
	{
		return uuid;
	}

	private String uuid;

	public void setUuid(String uuid)
	{
		this.uuid = EX.assertu(uuid);
	}


	/* Object Interface */

	public int     hashCode()
	{
		return EX.assertn(uuid).hashCode();
	}

	public boolean equals(Object o)
	{
		return (this == o) || (
		  o.getClass().equals(this.getClass()) &&
		  EX.assertn(uuid).equals(EX.assertn(((Entity)o).uuid))
		);
	}


	/* Mapped & Unmapped */

	/**
	 * Maps all get-methods registered in {@link #MAPPERS}
	 * for each class and interface up the hierarchy, see
	 * {@link OU#up(Class, Predicate)}, adding the values
	 * to the linked map without the duplicates if field
	 * is already saved by a lower-class.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> map()
	{
		LinkedHashMap<String, Object> vs =
		  new LinkedHashMap<>();

		//c: for each class-interface up the hierarchy
		OU.up(this.getClass(), c ->
		{
			Mapper<Entity> m = MAPPERS.get(c);

			if(m != null) //?: {this mapper is registered}
				m.get(this, vs);

			return false;
		});

		return vs;
	}

	/**
	 * Reverse operation to {@link #map()}.
	 * Also goes up the hierarchy.
	 */
	public void unmap(Map<String, Object> vs)
	{
		if(vs == null) //?: {got nothing}
			return;

		//c: for each class-interface up the hierarchy
		OU.up(this.getClass(), c ->
		{
			Mapper<Entity> m = MAPPERS.get(c);

			if(m != null) //?: {this mapper is registered}
				m.set(this, vs);

			return false;
		});
	}


	/* Class Shared Mappers */

	/**
	 * Registers (adds) get-set method objects to the given
	 * entity class. Invoked from the class' static section.
	 */
	@SuppressWarnings("unchecked")
	public static <O extends Entity> void register(
	  Class<O> c, String f, Get<O> g, Set<O> s)
	{
		//~: get or create this class mapper
		((Mapper<O>) MAPPERS.computeIfAbsent(EX.assertn(c),
		  x -> new Mapper<>())).map(f, g, s);
	}

	/**
	 * Mappers registry for all hierarchy of Entities.
	 */
	protected static final Map<Class<? extends Entity>, Mapper<Entity>>
	  MAPPERS = new ConcurrentHashMap<>();


	/* Entity Mappers */

	static
	{
		register(Entity.class, "uuid", o -> o.uuid,
		  (o, v) -> o.uuid = (String)v);
	}
}