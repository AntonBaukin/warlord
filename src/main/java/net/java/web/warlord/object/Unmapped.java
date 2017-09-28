package net.java.web.warlord.object;

import java.util.Map;

/**
 * Denotes Mapped objects that also support
 * back-reading the mapped properties.
 *
 * @author anton.baukin@gmail.com
 */
public interface Unmapped extends Mapped
{
	/* Unmapped */

	/**
	 * Reads properties from the map given
	 * not modifying it.
	 */
	void unmap(Map<String, Object> m);
}