package net.java.web.app.model;

/* Java */

import java.util.Date;

/* Warlord */

import net.java.web.warlord.object.Entity;


/**
 * Additional projection of Department that relates
 * it with Employee being the head of it.
 *
 * @author anton.baukin@gmail.com
 */
public class DepartmentHead extends Entity
{
	/**
	 * UUID of Employee that is currently
	 * this department's head.
	 */
	public String getEmployee()
	{
		return employee;
	}

	private String employee;

	public void setEmployee(String employee)
	{
		this.employee = employee;
	}

	/**
	 * Date when the employee headed the department.
	 */
	public Date getSince()
	{
		return since;
	}

	private Date since;

	public void setSince(Date since)
	{
		this.since = since;
	}


	/* Department Head Mappings */

	static
	{
		register(DepartmentHead.class, "employee", o -> o.employee,
		  (o, v) -> o.employee = (String)v);

		register(DepartmentHead.class, "since", o -> DU.ts(o.since),
		  (o, v) -> o.since = DU.s2d((String)v));
	}
}