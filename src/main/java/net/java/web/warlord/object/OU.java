package net.java.web.warlord.object;

/* Java */

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/* Spring Framework */

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.servlet.Req;


/**
 * Various utility functions for objects.
 *
 * @author anton.baukin@gmail.com
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class OU
{
	/* Access Spring Beans */

	public static Object      bean(String name)
	{
		return springContext().getBean(EX.asserts(name));
	}

	/**
	 * The same as {@link #bean(String)}, but returns
	 * {@code null} if the bean is not registered.
	 */
	public static Object      beanOrNull(String name)
	{
		try
		{
			return springContext().getBean(EX.asserts(name));
		}
		catch(NoSuchBeanDefinitionException e)
		{
			return null;
		}
	}

	/**
	 * Returns the Spring bean registered by the name
	 * taken from the simple name of the class with
	 * the first letter lower-cased.
	 */
	public static <B> B       bean(Class<B> beanClass)
	{
		String        sn = beanClass.getSimpleName();
		StringBuilder sb = new StringBuilder(sn);

		sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
		return (B)bean(sb.toString());
	}

	/**
	 * The same as {@link #bean(Class)}, but returns
	 * {@code null} if the bean is not registered.
	 */
	public static <B> B       beanOrNull(Class<B> beanClass)
	{
		try
		{
			return bean(beanClass);
		}
		catch(NoSuchBeanDefinitionException e)
		{
			return null;
		}
	}

	public static ApplicationContext springContext()
	{
		return EX.assertn(WebApplicationContextUtils.
		  getWebApplicationContext(Req.context()),
		  "Spring Framework Context is not found!"
		);
	}


	/* Raw Structured Objects Access */

	/**
	 * Represents given raw list as a collection
	 * of raw structured objects.
	 */
	public static List<Map<String, Object>> list(Object o)
	{
		return (List<Map<String, Object>>) o;
	}

	/**
	 * Represents given raw list as a raw structured object.
	 */
	public static Map<String, Object> map(Object o)
	{
		return (Map<String, Object>) o;
	}

	/**
	 * With the given path, returns the nested value.
	 * Path item must be a string for objects, or integer
	 * for arrays. If intermediate element is not found,
	 * or index goes out of scope, returns null.
	 */
	public static Object get(Object o, Object... path)
	{
		if(o instanceof MapBuilder)
			o = ((MapBuilder)o).map;

		for(Object p : path)
		{
			//?: {got empty intermediate}
			if(o == null) return null;

			//?: {element is a map}
			if(o instanceof Map)
			{
				//?: {the key is not a string}
				EX.assertx(p instanceof String);
				o = ((Map)o).get(p);
			}

			//?: {element is a list}
			else if(o instanceof List)
			{
				//?: {the key is not an integer}
				if(!(p instanceof Integer))
					throw EX.ass();

				//?: {index out of scope}
				int k = (Integer)p;
				if(k < 0 || k >= ((List)o).size())
					return null;

				o = ((List)o).get(k);
			}

			//~: wrong element tyoe
			else throw EX.ass();
		}

		return o;
	}


	/* Raw Structured Objects Building */

	/**
	 * Shortcut to create Map Builder.
	 */
	public static MapBuilder mb()
	{
		return new MapBuilder();
	}

	/**
	 * Shortcut to create Map Builder initialized
	 * with decoded JSON object.
	 */
	public static MapBuilder mb(String json)
	{
		//?: {no model is provided}
		if(json == null || json.isEmpty())
			return null;

		return new MapBuilder().put(OU.map(Json.s2o(json)));
	}

	/**
	 * Shortcut to create Map Builder initialized
	 * with the given raw Map instance.
	 */
	public static MapBuilder mb(Object m)
	{
		return new MapBuilder(null, OU.map(m));
	}

	/**
	 * Iterates iver the collection of Mapped objects
	 * creating raw Maps from them. The resulting list
	 * may be encoded as JSON array (of that objects).
	 */
	public static List<Map<String, Object>> list(Iterable<? extends Mapped> ms)
	{
		ArrayList<Map<String, Object>> r = new ArrayList<>();

		for(Mapped m : ms)
			r.add(m.map());

		return r;
	}


	/* Classes and Hierarchy */

	/**
	 * Invokes the consumer for each class and interface
	 * up by the levels of declaration starting with the
	 * class given. Return true to stop traversing.
	 */
	public static void   up(Class<?> c, Predicate<Class<?>> f)
	{
		EX.assertn(c);
		EX.assertn(f);

		LinkedList<Class<?>> cs  = new LinkedList<>();
		HashSet<Class<?>>    ifs = new HashSet<>();

		//~: add classes up to Object (going the first)
		cs.addFirst(c);
		while(cs.getLast().getSuperclass() != null)
			cs.addLast(cs.getLast().getSuperclass());

		//~: add interfaces going from Object
		ListIterator<Class<?>> i =
		  cs.listIterator(cs.size());

		while(i.hasPrevious())
		{
			Class<?> x = i.previous();

			for(Class<?> ii : x.getInterfaces())
				if(ifs.add(ii))
				{
					i.next();
					i.add(ii);
					i.previous();
				}
		}

		//~: traverse the list
		for(Class<?> x : cs)
			if(f.test(x))
				return;
	}

	/**
	 * Returns public method, or private or protected
	 * making them accessible, or null.
	 */
	public static Method method(Class<?> c, String n, Class<?>... args)
	{
		EX.assertn(c);
		EX.asserts(n);

		try
		{
			return c.getMethod(n, args);
		}
		catch(NoSuchMethodException ignore)
		{
			while(c != null) try
			{
				Method x = c.getDeclaredMethod(n, args);
				x.setAccessible(true);
				return x;
			}
			catch(NoSuchMethodException ignore2)
			{
				c = c.getSuperclass();
			}
		}

		return null;
	}

	/**
	 * First, iterates over all public methods of the class.
	 * Then, goes over the protected ones including the inherited,
	 * but excluding overridden. Then, each private method (that
	 * is not made accessible) up to the superclasses.
	 *
	 * Stops iteration of predicate true and returns that method.
	 * If predicate returned false each time, return null.
	 */
	public static Method methods(Class<?> c, Predicate<Method> f)
	{
		//~: add all public methods
		List<Method> lst = new ArrayList<>(
		  Arrays.asList(c.getMethods()));

		//~: traverse the public list
		for(Method m : lst)
			if(f.test(m))
				return m;

		//~: protected and private
		Set<Sig> pro = new LinkedHashSet<>();
		lst.clear();

		//c: scan up by the super-classes
		while(c != null)
		{
			for(Method m : c.getDeclaredMethods())
				//?: {is private}
				if((m.getModifiers() & Modifier.PRIVATE) != 0)
					lst.add(m);
				//~: protected | package-visible
				else if((m.getModifiers() & Modifier.PUBLIC) == 0)
					pro.add(new Sig(m));

			//~: advance to the super-class
			c = c.getSuperclass();
		}

		//~: traverse the protected list
		for(Sig sm : pro)
			if(f.test(sm.m))
				return sm.m;

		//~: traverse the private list
		for(Method m : lst)
			if(f.test(m))
				return m;

		return null;
	}

	private static class Sig
	{
		public Sig(Method m)
		{
			this.m = m;
		}

		public final Method m;

		public int     hashCode()
		{
			int h = m.getName().hashCode();

			for(Class<?> c : m.getParameterTypes())
				h = 31*h + c.hashCode();

			return h;
		}

		public boolean equals(Object x)
		{
			if(x == this) return true;
			if(!(x instanceof Sig)) return false;

			Method xm = ((Sig)x).m;

			return m.getName().equals(xm.getName()) &&
			  Arrays.equals(m.getParameterTypes(), xm.getParameterTypes());
		}
	}
}