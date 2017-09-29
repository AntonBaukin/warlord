package net.java.web.app.db;

/* Java */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/* Spring Framework */

import org.springframework.stereotype.Component;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.db.GetBase;
import net.java.web.warlord.io.Streams.BytesStream;


/**
 * Data access strategy to process
 * 'Objects' database table.
 *
 * @author anton.baukin@gmail.com.
 */
@Component
public class GetObject extends GetBase
{
	/* Objects Search & Load */

	/**
	 * Tells whether object with the given UUID exists.
	 * Concrete projection type is not defined here.
	 */
	public boolean exists(String uuid)
	{
		//?: {not a valid uuid}
		EX.assertx(isUUID(uuid));

		return (null != first(q("exists"), uuid));
	}

	/**
	 * Checks whether an object with the given UUID
	 * and concrete projection type exists.
	 *
	 * A list of types may be defined with ' ' separator.
	 * In this case any of listed types match.
	 */
	public boolean exists(String uuid, String type)
	{
		//?: {not a valid uuid}
		EX.assertx(isUUID(uuid));
		EX.asserts(type);

		String q = q("exists-type" +
		  (type.contains(" ")?("-multi"):(""))
		);

		return (null != first(q, uuid, type));
	}

	/**
	 * Returns UUID of entity of the type given
	 * if only one entity having this prefix exists.
	 */
	public String  guess(String type, String uuidPrefix)
	{
		EX.asserts(type);
		EX.asserts(uuidPrefix);

		final Result<String> r = new Result<>();

		select(q("guess-type+prefix"), params(type, uuidPrefix), rs ->
		{
			if(r.result != null)
			{
				r.result = null;
				return false;
			}

			r.result = rs.getString(1);
			return true;
		});

		return r.result;
	}

	/**
	 * Loads the payload of the object.
	 * The projection type must be defined.
	 *
	 * A list of types may be defined with ' ' separator.
	 * In this case the unpredicted type match returned
	 * if the object has several types.
	 */
	public String  json(String uuid, String type)
	{
		//?: {not a valid uuid}
		EX.assertx(isUUID(uuid));
		EX.asserts(type);

		String q = q("json" +
		  (type.contains(" ")?("-multi"):(""))
		);

		Object[] r = first(q, uuid, type);
		return (r == null)?(null):unzip(r, 0);
	}

	public void    assign(Map<String, Object> o, Object[] r)
	{
		o.put("uuid", r[0]);

		if(r[1] != null)
			o.put("owner", r[1]);
		else
			o.remove("owner");

		o.put("ts",   ((Date)r[2]).getTime());
		o.put("type", r[3]);

		if(r[4] != null)
			o.put("text", r[4]);
		else
			o.remove("text");

		o.put("json", unzip(r, 5));
	}

	/**
	 * Loads into the map given all the fields
	 * of 'Objects' table excluding file BLOB.
	 * The timestamp is given as long integer.
	 *
	 * Returns false when not found.
	 * @see {@link #save(Map)}.
	 *
	 * A list of types may be defined with
	 * ' ' separator. See exists().
	 */
	public boolean load(String uuid, String type, Map<String, Object> o)
	{
		//?: {not a valid uuid}
		EX.assertx(isUUID(uuid));
		EX.asserts(type);

		String q = q("load" +
		  (type.contains(" ")?("-multi"):(""))
		);

		//~: load the record
		Object[] r = first(q, uuid, type);

		//?: {not found}
		if(r == null)
			return false;

		this.assign(o, r);
		return true;
	}

	/**
	 * Iterates over each object record having
	 * the type column of the value given.
	 * The results are ordered by time.
	 */
	public void    eachType(String type, TakeObject x)
	{
		EX.asserts(type);
		EX.assertn(x);

		select(q("each-type"), params(type), taker(x));
	}

	/**
	 * Iterates over each object record having
	 * the type column and the text of the values
	 * given. The results are ordered by time.
	 */
	public void    eachTypeText(String type, String text, TakeObject x)
	{
		EX.asserts(type);
		EX.asserts(text);
		EX.assertn(x);

		select(q("each-type+text"), params(type, text), taker(x));
	}

	public void    eachOwnerType(String owner, String type, TakeObject x)
	{
		EX.assertx(isUUID(owner));
		EX.asserts(type);
		EX.assertn(x);

		select(q("each-owner+type"), params(owner, type), taker(x));
	}

	public void    eachOwnerTypeText(String owner, String type, String text, TakeObject x)
	{
		EX.assertx(isUUID(owner));
		EX.asserts(type);
		EX.asserts(text);
		EX.assertn(x);

		select(q("each-owner+type+text"), params(owner, type, text), taker(x));
	}


	/* Objects Save & Update */

	/**
	 * Inserts new record into 'Objects' table.
	 * The map arguments looks like:
	 *
	 * · uuid   required UUID string;
	 * · owner  UUID string of the record owner;
	 * · type   string with the type of the record;
	 * · text   text value;
	 * · json   object payload (JSON document as a string);
	 * · ts     (optional) timestamp milliseconds.
	 */
	public void    save(Map<String, Object> o)
	{
		String uuid = (String) o.get("uuid");
		Object   ts = new java.sql.Timestamp(
		  o.containsKey("ts")?(((Number)o.get("ts")).longValue()):
		  System.currentTimeMillis());

		//?: {not a valid uuid}
		EX.assertx(isUUID(uuid));

		//?: {not a valid uuid of the owner}
		if(o.containsKey("owner"))
			EX.assertx(isUUID((String) o.get("owner")));

		update(q("save"), params(uuid,
		  xnull(String.class, o.get("owner")), ts,
		  xnull(String.class, o.get("type")),
		  xnull(String.class, o.get("text")),
		  zip((String)o.get("json"))
		));
	}

	/**
	 * Update analog of {@link #save(Map)}.
	 * Note that given timestamp is not applied!
	 */
	public void    update(Map<String, Object> o)
	{
		String uuid = (String) o.get("uuid");
		Object   ts = new java.sql.Timestamp(System.currentTimeMillis());

		//~: type given or the default
		String type = (String) o.get("type");
		if(type == null) type = "";

		//?: {not a valid uuid}
		EX.assertx(isUUID(uuid));

		//?: {not a valid uuid of the owner}
		if(o.containsKey("owner"))
			EX.assertx(isUUID((String) o.get("owner")));

		update(q("update"), params(xnull(String.class, o.get("owner")),
		  ts, type, xnull(String.class, o.get("text")),
		  zip((String)o.get("json")), uuid, type));
	}

	/**
	 * Updates the object (and the timestamp) only.
	 */
	public void    update(String uuid, String type, String json)
	{
		Object ts = new java.sql.Timestamp(System.currentTimeMillis());

		//?: {not a valid uuid}
		EX.assertx(isUUID(uuid));
		EX.asserts(type);

		update(q("update-json"), params(ts, zip(json), uuid, type));
	}

	public void    touch(String uuid)
	{
		Object ts = new java.sql.Timestamp(System.currentTimeMillis());

		//?: {not a valid uuid}
		EX.assertx(isUUID(uuid));

		update(q("touch"), params(ts, uuid));
	}


	/* File Objects */

	/**
	 * Loads the file given into the stream and
	 * returns true if anything was written
	 * (file object found).
	 */
	public boolean load(String uuid, String type, OutputStream s)
	{
		final Result<Boolean> r = new Result<>();

		//?: {not a valid uuid}
		EX.assertx(isUUID(uuid));
		EX.asserts(type);
		EX.assertn(s);

		String q = q("load-file" +
		  (type.contains(" ")?("-multi"):(""))
		);

		select(q, params(uuid, type), rs ->
		{
			r.result = (0L != read(dialect().result(rs, rs.getMetaData(), 1), s));
			return true;
		});

		return Boolean.TRUE.equals(r.result);
	}

	/**
	 * Loads binary object as bytes.
	 */
	public byte[]  bytes(String uuid, String type)
	{
		try(BytesStream b = new BytesStream())
		{
			if(!load(uuid, type, b))
				return null;

			return b.bytes();
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}

	/**
	 * Updates the file BLOB of the object
	 * previously saved. Ths input stream
	 * is closed at the end of this operation.
	 * Returns true if the file was updated.
	 */
	public boolean update(String uuid, String type, InputStream s)
	{
		//?: {not a valid uuid}
		EX.assertx(isUUID(uuid));
		EX.asserts(type);

		return 1 == update(q("update-file"),
		  params(write(s), uuid, type));
	}

	public boolean update(String uuid, String type, byte[] bytes)
	{
		EX.assertn(bytes);
		return update(uuid, type, new ByteArrayInputStream(bytes));
	}

	/**
	 * Erases blob of the database object.
	 */
	public boolean erase(String uuid, String type)
	{
		return update(uuid, type, (InputStream)null);
	}

	/**
	 * Caution! Removes object projections of all types.
	 */
	public boolean delete(String uuid)
	{
		//?: {not a valid uuid}
		EX.assertx(isUUID(uuid));

		return 0 < update(q("delete-by-uuid"), params(uuid));
	}

	/**
	 * Caution! Removes object projections with the given type.
	 */
	public boolean delete(String uuid, String type)
	{
		//?: {not a valid uuid}
		EX.assertx(isUUID(uuid));
		EX.asserts(type);

		return 0 < update(q("delete-by-uuid+type"), params(uuid, type));
	}


	/* Utilities */

	public TakeResult taker(TakeObject x)
	{
		final Map<String, Object> o = new LinkedHashMap<>();

		return result(r -> {
			assign(o, r);
			return x.take(o);
		});
	}
}