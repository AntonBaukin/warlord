package net.java.web.app.model;

/* Java */

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
 * Database operations on Persons and Employees.
 *
 * @author anton.baukin@gmail.com
 */
@Component
public class GetEmployees
{
	public Person getPerson(String uuid)
	{
		return getObjects.entity(Person.class, uuid);
	}

	public Employee getEmployee(String uuid)
	{
		return getObjects.entity(Employee.class, uuid);
	}

	/**
	 * Returns document of Employee merged to Person.
	 */
	public Map<String, Object> get(String uuid)
	{
		Employee e = getEmployee(uuid);
		return (e == null)?(null):merge(getPerson(uuid), e);
	}

	public Map<String, Object> merge(Person p, Employee e)
	{
		EX.assertx(p != null && e != null);
		EX.asserts(p.getUuid()); //?: {no uuid}
		EX.assertx(p.getUuid().equals(e.getUuid()));

		//~: take the person as the root
		return new MapBuilder().put(p.map()).
		  nest("employee").put(e.map()).put("uuid", null).
		  up().map;
	}

	public Map<String, Person> listPersons()
	{
		Remapper<Person> rm = new Remapper<>(Person.class);
		getObjects.eachType("Person", rm);
		return rm.entities;
	}

	public Map<String, Employee> listEmployees()
	{
		Remapper<Employee> rm = new Remapper<>(Employee.class);
		getObjects.eachType("Employee", rm);
		return rm.entities;
	}

	/**
	 * Maps UUID to merged documents of Persons and Employees.
	 */
	public Map<String, Map<String, Object>> list()
	{
		//~: load both projections
		Map<String, Employee> es = listEmployees();
		Map<String, Person> ps = listPersons();

		Map<String, Map<String, Object>> ms =
		  new LinkedHashMap<>(es.size());

		for(String uuid : es.keySet())
			ms.put(uuid, merge(ps.get(uuid), es.get(uuid)));

		return ms;
	}

	public String save(Person p, Employee e)
	{
		EX.assertx(p != null && e != null);

		//?: {department is set}
		if(e.getDepartment() != null)
		{
			EX.assertu(e.getDepartment());

			//?: {the department record exists}
			EX.assertx(getObjects.exists(
			  e.getDepartment(), "Department"
			));
		}

		//~: map person first
		Map<String, Object> m = getObjects.
		  saveMap(null, EX.assertn(p));

		//~: save the person
		getObjects.save(m);

		//!: share uuid for employee
		e.setUuid((String) m.get("uuid"));

		//~: save the employee
		getObjects.save(getObjects.saveMap(
		  e.getDepartment(), e));

		return e.getUuid();
	}

	public void update(Person p, Employee e)
	{
		EX.assertx(p != null && e != null);
		EX.asserts(p.getUuid());

		if(e.getUuid() != null)
			EX.assertx(p.getUuid().equals(e.getUuid()));
		else
			e.setUuid(p.getUuid());

		getObjects.update(p);
		getObjects.update(e);
	}


	@Autowired
	protected GetObjects getObjects;
}