package net.java.web.app.web;

/* Java */

import java.util.Map;

/* Spring Framework */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/* Warlord */

import net.java.web.warlord.object.OU;

/* Application */

import net.java.web.app.model.Employee;
import net.java.web.app.model.Person;
import net.java.web.app.model.GetEmployees;


/**
 * Spring MVC controller dealing with
 * Persons and Employees.
 *
 * @author anton.baukin@gmail.com
 */
@Controller
public class WebEmployees
{
	/**
	 * Returns merged Employee and Person.
	 */
	@ResponseBody
	@RequestMapping(value = "/get/employee",
	  method = RequestMethod.GET)
	public Object get(@RequestParam String uuid)
	{
		return getEmps.get(uuid);
	}

	/**
	 * Returns (UUID -> Person & Employee) documents
	 * for all employees.
	 */
	@ResponseBody
	@RequestMapping(value = "/get/employees",
	  method = RequestMethod.GET)
	public Object list()
	{
		return getEmps.list();
	}

	/**
	 * Saves new Person and Employee and returns it back.
	 */
	@ResponseBody
	@RequestMapping(value = "/save/employee",
	  method = RequestMethod.POST)
	public Object save(@RequestBody Map<String, Object> m)
	{
		Person   p = new Person();
		Employee e = new Employee();

		p.unmap(m);
		e.unmap(OU.map(m.get("employee")));

		getEmps.save(p, e);
		return this.get(e.getUuid());
	}

	/**
	 * Updates Person and Employee and returns it back.
	 */
	@ResponseBody
	@RequestMapping(value = "/update/employee",
	  method = RequestMethod.POST)
	public Object update(@RequestBody Map<String, Object> m)
	{
		Person   p = new Person();
		Employee e = new Employee();

		p.unmap(m);
		e.unmap(OU.map(m.get("employee")));

		getEmps.update(p, e);
		return this.get(e.getUuid());
	}

	@Autowired
	protected GetEmployees getEmps;
}