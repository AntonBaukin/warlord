package net.java.web.app.model;

/* Java */

import java.util.Date;

/* Warlord */

import net.java.web.warlord.object.Entity;


/**
 * Information on a human person.
 *
 * @author anton.baukin@gmail.com
 */
public class Person extends Entity
{
	public String getLastName()
	{
		return lastName;
	}

	private String lastName;

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	public String getFirstName()
	{
		return firstName;
	}

	private String firstName;

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	public String getMiddleName()
	{
		return middleName;
	}

	private String middleName;

	public void setMiddleName(String middleName)
	{
		this.middleName = middleName;
	}

	/**
	 * If defined, 'M' means male, 'F' female.
	 */
	public Character getSex()
	{
		return sex;
	}

	private Character sex;

	public void setSex(Character sex)
	{
		this.sex = sex;
	}

	/**
	 * Date of birth.
	 */
	public Date getDob()
	{
		return dob;
	}

	private Date dob;

	public void setDob(Date dob)
	{
		this.dob = dob;
	}

	/**
	 * Personal (mobile) phone.
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
	 * Personal e-mail.
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
	 * Address of living.
	 */
	public String getAddress()
	{
		return address;
	}

	private String address;

	public void setAddress(String address)
	{
		this.address = address;
	}


	/* Person Mappings */

	static
	{
		register(Person.class, "lastName", o -> o.lastName,
		  (o, v) -> o.lastName = (String)v);

		register(Person.class, "firstName", o -> o.firstName,
		  (o, v) -> o.firstName = (String)v);

		register(Person.class, "middleName", o -> o.middleName,
		  (o, v) -> o.middleName = (String)v);

		register(Person.class, "sex",
		  o -> (o.sex == null)?(null):(String.valueOf(o.sex)),
		  (o, v) -> o.sex = "F".equals(v)?('F'):"M".equals(v)?('M'):(null));

		register(Person.class, "dob", o -> DU.ts(o.dob),
		  (o, v) -> o.dob = DU.s2d((String)v));

		register(Person.class, "phone", o -> o.phone,
		  (o, v) -> o.phone = (String)v);
		
		register(Person.class, "email", o -> o.email,
		  (o, v) -> o.email = (String)v);
		
		register(Person.class, "address", o -> o.address,
		  (o, v) -> o.address = (String)v);
	}
}