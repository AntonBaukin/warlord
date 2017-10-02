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

import net.java.web.app.model.Department;
import net.java.web.app.model.GetDepartments;


/**
 * Spring MVC controller dealing with Departments.
 *
 * @author anton.baukin@gmail.com
 */
@Controller
public class WebDepartments
{
	/**
	 * Returns single Department.
	 */
	@ResponseBody
	@RequestMapping(value = "/get/department",
	  method = RequestMethod.GET)
	public Object get(@RequestParam String uuid)
	{
		return getDeps.get(uuid);
	}

	/**
	 * Returns (UUID -> Department) mapping of all
	 * the departments. Treat their number as few.
	 */
	@ResponseBody
	@RequestMapping(value = "/get/departments",
	  method = RequestMethod.GET)
	public Object list()
	{
		return getDeps.list();
	}

	/**
	 * Saves new Department and returns it back.
	 */
	@ResponseBody
	@RequestMapping(value = "/save/department",
	  method = RequestMethod.POST)
	public Object save(@RequestBody Map<String, Object> m)
	{
		String uuid = getDeps.save(OU.unmap(Department.class, m));
		return this.get(uuid);
	}

	/**
	 * Updates existing Department and returns it back.
	 */
	@ResponseBody
	@RequestMapping(value = "/update/department",
	  method = RequestMethod.POST)
	public Object update(@RequestBody Map<String, Object> m)
	{
		getDeps.update(OU.unmap(Department.class, m));
		return this.get((String) m.get("uuid"));
	}

	@Autowired
	protected GetDepartments getDeps;
}