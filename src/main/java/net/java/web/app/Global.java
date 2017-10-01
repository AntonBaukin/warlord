package net.java.web.app;

/* Java */

import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.ServletRegistration;
import javax.sql.DataSource;

/* Spring Framework */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.db.DatabaseBean;
import net.java.web.warlord.db.Dialect;
import net.java.web.warlord.db.TxFilter;
import net.java.web.warlord.object.CallMe;
import net.java.web.warlord.object.spring.MappedMessageConverter;
import net.java.web.warlord.servlet.Req;
import net.java.web.warlord.servlet.filter.PickFilter;
import net.java.web.warlord.servlet.filter.FiltersGlobalPoint;
import net.java.web.warlord.servlet.filter.FiltersPoint;

/* This Application */

import net.java.web.app.db.HyperSQL;
import net.java.web.app.web.IndexFilter;


/**
 * Global point of the application.
 *
 * @author anton.baukin@gmail.com
 */
@Configuration @EnableWebMvc
public class Global extends WebMvcConfigurerAdapter
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
	@CallMe("setTxFilter")
	public TxFilter txFilter;

	private void setTxFilter(TxFilter tx)
	{
		tx.setContexts("/get/", "/update/", "/save/");
	}

	/**
	 * Register Spring Dispatcher in the container.
	 */
	@PostConstruct
	private void createDispatcherServlet()
	{
		//~: create dispatching servlet
		dispatcher = new DispatcherServlet((WebApplicationContext)context);

		//~: add it to the container
		ServletRegistration.Dynamic ds = Req.context().
		  addServlet("Spring Dispatcher Servlet", dispatcher);

		//~: map it under the root
		ds.setLoadOnStartup(1);
		ds.addMapping("/*");
	}

	@Autowired
	public ApplicationContext context;

	public DispatcherServlet dispatcher;


	/* Application Database */

	@Bean
	public DatabaseBean databaseBean()
	{
		return new DatabaseBean(new HyperSQL());
	}

	@Autowired
	public DatabaseBean dbBean;

	@Bean
	public Dialect databaseDialect()
	{
		return EX.assertn(dbBean).database;
	}

	@Bean
	public DataSource dataSource()
	{
		return EX.assertn(dbBean).getDataSource();
	}

	@Bean
	public PlatformTransactionManager transactionManager()
	{
		return new DataSourceTransactionManager(
		  EX.assertn(dataSource()));
	}


	/* Spring MVC Configuration */

	public void configureMessageConverters(List<HttpMessageConverter<?>> cs)
	{
		cs.clear(); //<-- don't use the default
		cs.add(new MappedMessageConverter());
	}
}