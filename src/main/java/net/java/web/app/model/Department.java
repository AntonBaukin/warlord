package net.java.web.app.model;

/* Warlord */

import net.java.web.warlord.object.Entity;


/**
 * Organization department.
 *
 * Department has optional projection with type
 * "Department Head" that has empty object and
 * refers via the owner an Employee.
 *
 * @author anton.baukin@gmail.com
 */
public class Department extends Entity
{
	public String getName()
	{
		return name;
	}

	private String name;

	public void setName(String name)
	{
		this.name = name;
	}

	public String getOffice()
	{
		return office;
	}

	private String office;

	public void setOffice(String office)
	{
		this.office = office;
	}

	public String getPhone()
	{
		return phone;
	}

	private String phone;

	public void setPhone(String phone)
	{
		this.phone = phone;
	}


	/* Department Mappings */

	static
	{
		register(Department.class, "name", o -> o.name,
		  (o, v) -> o.name = (String)v);

		register(Department.class, "office", o -> o.office,
		  (o, v) -> o.office = (String)v);

		register(Department.class, "phone", o -> o.phone,
		  (o, v) -> o.phone = (String)v);
	}
}