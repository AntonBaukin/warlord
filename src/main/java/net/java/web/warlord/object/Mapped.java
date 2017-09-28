package net.java.web.warlord.object;

/* Java */

import java.util.Map;


/**
 * Tells that class instance provides mapping
 * of it's named properties to object values.
 * The returned map is treated as read-only.
 *
 * @author anton.baukin@gmail.com
 */
public interface Mapped
{
	/* Mapped */

	Map<String, Object> map();
}