package net.java.web.app;

/* Spring Framework */

import org.springframework.beans.factory.annotation.Autowired;

/* Warlord */

import net.java.web.warlord.servlet.filter.FilterStage;
import net.java.web.warlord.servlet.filter.PickFilter;



/**
 * Global point of the application.
 *
 * @author anton.baukin@gmail.com
 */
public class Global
{
	/* Application Level Web Filters */

	/**
	 * This filter is used to redirect requests
	 * to the index page that depends on the user
	 * settings, access rights and other stuff.
	 * It reacts only on GET '/' root page.
	 */
	@Autowired @PickFilter(order = 10, stage = FilterStage.REQUEST)
	public IndexFilter indexFilter;
}