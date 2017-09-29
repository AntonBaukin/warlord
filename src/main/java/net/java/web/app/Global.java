package net.java.web.app;

/* Spring Framework */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/* Warlord */

import net.java.web.warlord.db.DatabaseBean;
import net.java.web.warlord.db.TxFilter;
import net.java.web.warlord.object.CallMe;
import net.java.web.warlord.servlet.filter.PickFilter;
import net.java.web.warlord.servlet.filter.FiltersGlobalPoint;
import net.java.web.warlord.servlet.filter.FiltersPoint;

/* This Application */

import net.java.web.app.db.HyperSQL;


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

	@Autowired @PickFilter(order = 15)
	public HelloFilter helloFilter;

	@Autowired @PickFilter(order = 20)
	@CallMe("setTxFilter")
	public TxFilter txFilter;

	private void setTxFilter(TxFilter tx)
	{
		tx.setContexts("/get", "/set");
	}


	/* Application Database */

	@Bean
	public DatabaseBean database()
	{
		return new DatabaseBean(new HyperSQL());
	}
}