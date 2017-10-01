package net.java.web.app.model;

/* Warlord */

import net.java.web.warlord.object.Entity;


/**
 * Organization department.
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

	/**
	 * UUID of the person that is currently
	 * this department's head.
	 */
	public String getHead()
	{
		return head;
	}

	private String head;

	public void setHead(String head)
	{
		this.head = head;
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

		register(Department.class, "head", o -> o.head,
		  (o, v) -> o.head = (String)v);
	}
}