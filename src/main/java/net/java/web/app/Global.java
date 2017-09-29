package net.java.web.app;

/* Spring Framework */

import net.java.web.warlord.servlet.filter.FiltersGlobalPoint;
import net.java.web.warlord.servlet.filter.FiltersPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/* Warlord */

import net.java.web.warlord.servlet.filter.FilterStage;
import net.java.web.warlord.servlet.filter.PickFilter;


/**
 * Global point of the application.
 *
 * @author anton.baukin@gmail.com
 */
@Configuration
public class Global
{
	/* Application Level Web Filters */

	@Bean
	public FiltersPoint filtersPoint()
	{
		return new FiltersGlobalPoint();
	}

	/**
	 * This filter is used to redirect requests
	 * to the index page that depends on the user
	 * settings, access rights and other stuff.
	 * It reacts only on GET '/' root page.
	 */
	@Autowired @PickFilter(order = 10)
	public IndexFilter indexFilter;

	@Autowired @PickFilter(order = 20)
	public HelloFilter helloFilter;
}