package net.java.web.app.model;

/* Java */

import java.util.Map;

/* Spring Framework */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/* Warlord */

import net.java.web.warlord.EX;

/* Application */

import net.java.web.app.db.GetObjects;
import net.java.web.app.db.GetObjects.Remapper;


/**
 * Database operations on Departments.
 *
 * @author anton.baukin@gmail.com
 */
@Component
public class GetDepartments
{
	public Department get(String uuid)
	{
		return getObjects.entity(Department.class, uuid);
	}

	public Map<String, Department> list()
	{
		Remapper<Department> rm = new Remapper<>(Department.class);
		getObjects.eachType("Department", rm);
		return rm.entities;
	}

	public String save(Department d)
	{
		Map<String, Object> m = getObjects.saveMap(null, d);
		getObjects.save(m);
		return (String)m.get("uuid");
	}

	public void update(Department d)
	{
		getObjects.update(d);
	}

	@Autowired
	protected GetObjects getObjects;
}