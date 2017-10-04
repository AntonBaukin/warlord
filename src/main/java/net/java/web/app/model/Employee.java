package net.java.web.app.model;

/* Java */

import java.util.Date;

/* Warlord */

import net.java.web.warlord.object.Entity;


/**
 * Additional projection of a Person: employee.
 * Both document objects share the same UUID,
 * but have distinct types.
 *
 * Note: that Person is a required projection,
 * even if the document is empty!
 *
 *
 * @author anton.baukin@gmail.com
 */
public class Employee extends Entity
{
	/**
	 * UUID of the department where the employee works.
	 * The department is the owner (of the object record).
	 */
	public String getDepartment()
	{
		return department;
	}

	private String department;

	public void setDepartment(String department)
	{
		this.department = department;
	}

	/**
	 * Work phone..
	 */
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
	 * Work e-mail.
	 */
	public String getEmail()
	{
		return email;
	}

	private String email;

	public void setEmail(String email)
	{
		this.email = email;
	}

	/**
	 * Date when the employee started the work.
	 */
	public Date getHired()
	{
		return hired;
	}

	private Date hired;

	public void setHired(Date hired)
	{
		this.hired = hired;
	}


	/* Employee Mappings */

	static
	{
		register(Employee.class, "department", o -> o.department,
		  (o, v) -> o.department = (String)v);

		register(Employee.class, "phone", o -> o.phone,
		  (o, v) -> o.phone = (String)v);

		register(Employee.class, "email", o -> o.email,
		  (o, v) -> o.email = (String)v);

		register(Employee.class, "hired", o -> DU.ts(o.hired),
		  (o, v) -> o.hired = DU.s2d((String)v));
	}
}