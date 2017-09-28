package net.java.web.warlord;

/* Java */

import java.util.Collection;


/**
 * Various string utilities.
 *
 * @author anton.baukin@gmail.com
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class SU
{
	/* Concatenations */

	public static String cat(Object... objs)
	{
		StringBuilder s = new StringBuilder(64);

		cat(s, objs);
		return s.toString();
	}

	private static void  cat(StringBuilder s, Collection objs)
	{
		for(Object o : objs)
			if(o instanceof Collection)
				cat(s, (Collection) o);
			else if(o instanceof Object[])
				cat(s, (Object[]) o);
			else if(o != null)
				s.append(o);
	}

	private static void  cat(StringBuilder s, Object[] objs)
	{
		for(Object o : objs)
			if(o instanceof Collection)
				cat(s, (Collection)o);
			else if(o instanceof Object[])
				cat(s, (Object[]) o);
			//else if(o instanceof Mapped)
			//	s.append(Json.o2s(((Mapped)o).map()));
			else if(o != null)
				s.append(o);
	}
}