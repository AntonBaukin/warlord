package net.java.web.app.model;

/* Java */

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/* Spring Framework */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.object.MapBuilder;

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
	public Department getDep(String uuid)
	{
		return getObjects.entity(Department.class, uuid);
	}

	public DepartmentHead getHead(String uuid)
	{
		return getObjects.entity(DepartmentHead.class, uuid);
	}

	/**
	 * Returns document of Department merged to the Head.
	 */
	public Map<String, Object> get(String uuid)
	{
		Department d = getDep(uuid);
		return (d == null)?(null):merge(d, getHead(uuid));
	}

	/**
	 * Returns document of Department merged with it's head.
	 */
	public Map<String, Object> merge(Department d, DepartmentHead h)
	{
		EX.assertx(d != null);
		EX.asserts(d.getUuid()); //?: {no uuid}
		EX.assertx(h == null || d.getUuid().equals(h.getUuid()));

		//~: take the department as the root
		MapBuilder m = new MapBuilder().put(d.map());

		if(h != null) //~: add the head sub
			m.nest("head").put(h.map()).
			  put("uuid", null);

		return m.map;
	}

	public Map<String, Department> listDeps()
	{
		Remapper<Department> rm = new Remapper<>(Department.class);
		getObjects.eachType("Department", rm);
		return rm.entities;
	}

	public Map<String, DepartmentHead> listHeads()
	{
		Remapper<DepartmentHead> rm = new Remapper<>(DepartmentHead.class);
		getObjects.eachType("DepartmentHead", rm);
		return rm.entities;
	}

	/**
	 * Maps UUID to merged documents of Department and the Head.
	 */
	public Map<String, Map<String, Object>> list()
	{
		//~: load both projections
		Map<String, Department> ds = listDeps();
		Map<String, DepartmentHead> hs = listHeads();

		Map<String, Map<String, Object>> ms =
		  new LinkedHashMap<>(ds.size());

		for(String uuid : ds.keySet())
			ms.put(uuid, merge(ds.get(uuid), hs.get(uuid)));

		return ms;
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

	/**
	 * Makes existing Employee to be the head of
	 * the Department. Employee is not required to
	 * work in this Department.
	 */
	public void setHead(String dep, String emp, Date since)
	{
		EX.assertu(dep);

		if(emp == null) //?: {erase the head}
		{
			getObjects.delete(dep, "DepartmentHead");
			return;
		}

		//?: {real employee is given}
		EX.assertx(getObjects.exists(emp, "Employee"));

		//~: create new relation
		DepartmentHead h = new DepartmentHead();

		//=: uuid of the projection
		h.setUuid(dep);

		//=: head employee
		h.setEmployee(emp);

		//=: since this date
		if(since == null) since = new Date();
		h.setSince(since);

		//~: create new object record
		Map<String, Object> m = getObjects.saveMap(emp, h);

		//?: {relation exists} update
		if(getObjects.exists(dep, "DepartmentHead"))
			getObjects.update(m);
		else
			getObjects.save(m);
	}

	@Autowired
	protected GetObjects getObjects;

	@Autowired
	protected GetEmployees getEmps;
}