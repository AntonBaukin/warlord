package net.java.web.warlord.db;

/* Java */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.sql.DataSource;

/* Spring Framework */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.io.IO;
import net.java.web.warlord.io.Streams.BytesStream;
import net.java.web.warlord.io.Streams.CharBytes;


/**
 * Implementation base of a data access strategy.
 * Warning! It does not set own transactional scopes.
 *
 * @author anton.baukin@gmail.com.
 */
public abstract class GetBase
{
	/* UUID */

	public boolean isUUID(String uuid)
	{
		if(uuid == null)
			return false;

		try
		{
			UUID.fromString(uuid);
			return true;
		}
		catch(IllegalArgumentException e)
		{
			return false;
		}
	}

	public String  newUUID()
	{
		return UUID.randomUUID().toString();
	}


	/* Connections & Statements */

	@Autowired
	protected DataSource dataSource;

	@Autowired
	protected ApplicationContext context;

	/**
	 * Provides connection only within
	 * {@code @Transactional} scopes.
	 *
	 * Warning! Do not close it!
	 */
	protected Connection        co()
	{
		//?: {has no data source wired}
		EX.assertn(dataSource, "DataSource is not @Autowired in [",
		  getClass().getName(), "] class bean!");

		//?: {not in transactional scopes}
		EX.assertx(TransactionSynchronizationManager.isSynchronizationActive(),
		  "Current thread is not nested in @Transactional scopes!");

		try
		{
			//~: get the connection
			Connection co = DataSourceUtils.
			  getConnection(dataSource);

			//?: {auto commit is set}
			EX.assertx(!co.getAutoCommit(),
			  "Connection has Auto Commit on!");

			return co;
		}
		catch(Throwable e)
		{
			throw EX.wrap(e, "Can't obtain database connection!");
		}
	}

	protected PreparedStatement prepare(boolean update, String sql)
	{
		try
		{
			return (!update)?(co().prepareStatement(sql)):
			  co().prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
			    ResultSet.CONCUR_UPDATABLE);
		}
		catch(SQLException e)
		{
			throw EX.wrap(e);
		}
	}


	/* Queries Caching */

	/**
	 * Returns query from the cache related to this strategy.
	 */
	protected String  q(String id)
	{
		if(qCache == null)
			qCache = QueryCache.cache(
			  this.getClass(), dialect().getName());

		return EX.asserts(qCache.q(id), "Not found query [",
		  id, "] in ", this.getClass().getSimpleName());
	}

	protected volatile QueryCache qCache;

	protected Dialect dialect()
	{
		return (Dialect) EX.assertn(
		  context.getBean("databaseDialect"),
		  "Bean 'databaseDialect' is not found!"
		);
	}


	/* Parameters */

	/**
	 * Parameter value to set NULL
	 * of specific SQL type.
	 */
	public static class SetNull
	{
		public SetNull(int sqlType)
		{
			this.sqlType = sqlType;
		}

		public void set(PreparedStatement s, int i)
		  throws SQLException
		{
			s.setNull(i, sqlType);
		}

		private int sqlType;
	}

	protected void        clear(PreparedStatement s)
	{
		try
		{
			s.clearParameters();
		}
		catch(SQLException e)
		{
			throw EX.wrap(e);
		}
	}

	protected Object[]    params(Object... ps)
	{
		return ps;
	}

	protected void        params(PreparedStatement s, Object... ps)
	{
		for(int i = 0;(i < ps.length);i++)
			param(s, i + 1, EX.assertn(ps[i]));
	}

	protected void        param(PreparedStatement s, int i, Object p)
	{
		try
		{
			Class cls = EX.assertn(p).getClass();

			//?: {set null}
			if(SetNull.class.isAssignableFrom(cls))
				((SetNull)p).set(s, i);
			//?: {date-time}
			else if(Date.class.isAssignableFrom(cls))
				s.setTimestamp(i, new Timestamp(((Date)p).getTime()));
			//?: {general string}
			else if(CharSequence.class.isAssignableFrom(cls))
				s.setString(i, p.toString());
			//?: {character}
			else if(Character.class.equals(cls))
				s.setString(i, p.toString());
			//?: {bytes stream}
			else if(BytesStream.class.isAssignableFrom(cls))
				s.setBinaryStream(i, ((BytesStream)p).inputStream(),
				  (int)((BytesStream)p).length());
			//?: {blob}
			else if(Lob.class.isAssignableFrom(cls))
				((Lob)p).set(s, i);
			else
				s.setObject(i, p);
		}
		catch(Exception e)
		{
			throw EX.wrap(e);
		}
	}

	protected Object      xnull(Class cls, Object p)
	{
		return (p == null)?(xnull(cls)):(p);
	}

	protected SetNull     xnull(Class cls)
	{
		int type;

		//?: {string}
		if(CharSequence.class.isAssignableFrom(cls))
			type = Types.VARCHAR;
		//?: {byte array}
		else if(byte[].class.equals(cls))
			type = Types.VARBINARY;
		//?: {big decimal}
		else if(BigDecimal.class.equals(cls))
			type = Types.DECIMAL;
		//?: {long}
		else if(Long.class.equals(cls))
			type = Types.BIGINT;
		//?: {date as timestamp}
		else if(Date.class.equals(cls))
			type = Types.TIMESTAMP;
		//?: {blob}
		else if(Blob.class.equals(cls))
			type = Types.BLOB;
		//?: {boolean}
		else if(Boolean.class.equals(cls))
			type = Types.BOOLEAN;
		//?: {character}
		else if(Character.class.equals(cls))
			type = Types.CHAR;
		else
			throw EX.ass("Unsupported type: ", cls.getName());

		return xnull(type);
	}

	protected SetNull     xnull(int sqlType)
	{
		return new SetNull(sqlType);
	}

	protected String      unzip(byte[] data)
	{
		return (data == null)?(null):unzip(new ByteArrayInputStream(data));
	}

	/**
	 * Reads Gun Zipped string (as JSON or XML) in UTF-8.
	 */
	protected String      unzip(InputStream is)
	{
		try
		(
		  BytesStream bs = new BytesStream();
		  GZIPInputStream gz = new GZIPInputStream(is)
		)
		{
			bs.write(gz);
			return new String(bs.bytes(), "UTF-8");
		}
		catch(Exception e)
		{
			throw EX.wrap(e);
		}
	}

	protected String      unzip(Object x)
	{
		if((x instanceof byte[]) || (x == null))
			return unzip((byte[]) x);

		if(x instanceof InputStream)
			return unzip((InputStream) x);

		throw EX.ass("Can't unzip from type [", x.getClass(), "]!");
	}

	protected String      unzip(Object[] row, int i)
	{
		return (row == null)?(null):unzip(row[i]);
	}

	/**
	 * Creates the bytes stream and fills it with
	 * UTF-8 encoded Gun Zipped string characters.
	 */
	protected Object      zip(String obj)
	{
		if(obj == null)
			return xnull(byte[].class);

		BytesStream os = new BytesStream().
		  setNotCloseNext(true);

		try(GZIPOutputStream gz = new GZIPOutputStream(os))
		{
			IO.pump(new CharBytes(obj), gz);
		}
		catch(Throwable e)
		{
			os.closeAlways();
			throw EX.wrap(e);
		}

		return os;
	}

	@SuppressWarnings("unchecked")
	protected <T> T       project(Class<T> cls, Object[] row, int i)
	{
		if((row == null) || (row[i] == null))
			return null;

		EX.assertx(cls.isAssignableFrom(row[i].getClass()),
		  "Selected record item [", i, "] has value of class [",
		  row[i].getClass().getName(), "] that is not a [", cls.getName(), "]!"
		);

		return (T) row[i];
	}

	/**
	 * Writes blob content into the stream
	 * and returns the number of bytes.
	 * Output stream is not closed.
	 */
	protected long        read(Object b, OutputStream s)
	{
		try
		{
			return dialect().readLob(b, s);
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}

	/**
	 * Writes the stream into the blob object.
	 * Input stream is closed!
	 */
	protected Lob         write(InputStream s)
	{
		return dialect().createLob(s);
	}


	/* Queries & Iteration */

	/**
	 * Callback on each next record
	 * of the result set.
	 */
	@FunctionalInterface
	public interface TakeResult
	{
		/**
		 * Return false to stop iteration.
		 */
		boolean take(ResultSet r)
		  throws Exception;
	}

	/**
	 * Invoked for each record.
	 * Warning! The callback array is the same!
	 */
	@FunctionalInterface
	public interface TakeRecord
	{
		/**
		 * Return false to stop iteration.
		 */
		boolean take(Object[] r)
		  throws Exception;
	}

	protected TakeResult result(TakeRecord r)
	{
		Result<Object[]> x = new Result<>();

		return rs -> {
			GetBase.this.result(rs, x);
			return r.take(x.result);
		};
	}

	/**
	 * Invoked for each record.
	 * Warning! The callback map is the same!
	 */
	@FunctionalInterface
	public interface TakeObject
	{
		/**
		 * Return false to stop iteration.
		 */
		boolean take(Map<String, Object> m)
		  throws Exception;
	}

	/**
	 * Iterates over the result set of the select query.
	 */
	protected void      select(
	  boolean close, PreparedStatement s, TakeResult v)
	{
		Throwable e = null;
		ResultSet r = null;

		try
		{
			r = s.executeQuery();

			while(r.next())
				v.take(r);
		}
		catch(Throwable x)
		{
			e = x;
		}
		finally
		{
			if(r != null) try
			{
				r.close();
			}
			catch(Throwable x)
			{
				if(e == null) e = x;
			}

			if(close) try
			{
				s.close();
			}
			catch(Throwable x)
			{
				if(e == null) e = x;
			}
		}

		if(e != null)
			throw EX.wrap(e);
	}

	/**
	 * General variant of executing prepared select statement.
	 */
	protected void      select(
	  boolean update, String sql, Object[] params, TakeResult v)
	{
		PreparedStatement s = prepare(update, sql);
		Throwable         e = null;

		//~: init the statement
		if(params != null) try
		{
			params(s, params);
		}
		catch(Throwable x)
		{
			e = x;
		}
		finally
		{
			if(e != null) try
			{
				s.close();
			}
			catch(Throwable ignore)
			{}
		}

		if(e != null)
			throw EX.wrap(e);

		//~: iterate over the result set
		select(true, s, v);
	}

	/**
	 * Read-only select iteration.
	 */
	protected void      select(
	  String sql, Object[] params, TakeResult v)
	{
		select(false, sql, params, v);
	}

	/**
	 * Simples rad-only select having no parameters.
	 */
	protected void      select(String sql, TakeResult v)
	{
		select(false, sql, null, v);
	}

	/**
	 * Returns the first record of the query.
	 */
	protected Object[]  first(String sql, Object... params)
	{
		final Result<Object[]> r = new Result<>();

		select(sql, params, rs ->
		{
			//?: {this is a second call} do break
			if(r.result != null)
				return false;

			result(rs, r);
			return true;
		});

		return r.result;
	}

	/**
	 * Select for update.
	 */
	protected void      update(
	  String sql, Object[] params, TakeResult v)
	{
		select(true, sql, params, v);
	}

	/**
	 * Select for update without parameters.
	 */
	protected void      update(String sql, TakeResult v)
	{
		select(true, sql, null, v);
	}

	/**
	 * Executes update on the prepared statement.
	 */
	protected int       update(boolean close, PreparedStatement s)
	{
		Throwable e = null;
		int       r = 0;

		try
		{
			r = s.executeUpdate();
		}
		catch(Throwable x)
		{
			e = x;
		}
		finally
		{
			//~: handle close
			if(close) try
			{
				s.close();
			}
			catch(Throwable x)
			{
				if(e == null) e = x;
			}
		}

		if(e != null)
			throw EX.wrap(e);

		return r;
	}

	protected int       update(String sql, Object[] params)
	{
		PreparedStatement s = prepare(false, sql);
		Throwable         e = null;

		//~: init the statement
		if(params != null) try
		{
			params(s, params);
		}
		catch(Throwable x)
		{
			e = x;
		}
		finally
		{
			if(e != null) try
			{
				s.close();
			}
			catch(Throwable ignore)
			{}
		}

		if(e != null)
			throw EX.wrap(e);

		//~: issue update
		int result = 0; try
		{
			result = update(true, s);
		}
		catch(Throwable x)
		{
			e = x;
		}
		finally
		{
			//~: close all input streams
			Throwable x = closeStreams(params);
			if((x != null) & (e == null)) e = x;
		}

		if(e != null)
			throw EX.wrap(e);

		return result;
	}


	/* Batch Updates */

	/**
	 * Callback interface for batch
	 * insert-update operation.
	 */
	@FunctionalInterface
	public interface Batch
	{
		/**
		 * Assigns the parameters in the given array
		 * (is fixed during the query processing).
		 *
		 * Returns false to break the batch instead
		 * of executing the next update.
		 */
		boolean next(Object[] params);
	}

	/**
	 * Executes the prepared statement with batch updates.
	 * The size of a batch is told by {@param size}.
	 */
	protected void      batch(String sql, int size, Batch batch)
	{
		batch(true, size, prepare(false, sql), batch);
	}

	protected void      batch
	  (boolean close, int size, PreparedStatement s, Batch batch)
	{
		List<Object> w = new ArrayList<>(); //<-- the streams collected

		EX.assertn(batch);
		EX.assertx(size > 0);

		try(PreparedStatement ps = s)
		{
			Object[] params = new Object[
			  s.getParameterMetaData().getParameterCount()];

			while(true)
			{
				boolean invoke = false;

				//c: fill the batch up to the size
				for(int i = 0;(i < size);i++)
				{
					//~: invoke the batch
					if(!batch.next(params))
						break;

					//~: assign the parameters
					try
					{
						s.clearParameters();
						params(s, params);

						//~: add the batch
						s.addBatch();
						invoke = true;
					}
					finally
					{
						//~: close all streams
						collectStreams(params, w);
					}
				}

				//?: {nothing is left}
				if(!invoke) break;

				//!: execute the batch
				s.executeBatch();

				try //~: close the streams
				{
					closeStreams(w);
				}
				finally
				{
					w.clear();
				}
			}
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
		finally
		{
			//~: close the streams
			closeStreams(w);
		}
	}


	/* Utilities */

	/**
	 * Result to assign from closures.
	 */
	public static class Result<T>
	{
		public T result;
	}

	/**
	 * Closes each stream within the parameters.
	 * Returns the last close error (if was).
	 */
	protected Throwable   closeStreams(Object[] params)
	{
		return closeStreams(Arrays.asList(params));
	}

	/**
	 * Closes each stream within the parameters.
	 * Returns the first close error (if was).
	 */
	protected Throwable   closeStreams(List<?> w)
	{
		Throwable error = null;

		for(Object p : w) try
		{
			if(p instanceof AutoCloseable)
				((AutoCloseable)p).close();
		}
		catch(Throwable e)
		{
			if(error != null)
				error.addSuppressed(e);
			else
				error = e;
		}

		return error;
	}

	protected void        collectStreams(
	  Object[] params, List<Object> streams)
	{
		for(Object p : params)
			if(p instanceof AutoCloseable)
				streams.add(p);
	}

	protected void        result(ResultSet rs, Result<Object[]> r)
	{
		try
		{
			ResultSetMetaData m = rs.getMetaData();
			Dialect d = dialect();
			int     c = m.getColumnCount();

			//?: {not allocated yet}
			Object[] x = r.result;
			if(x == null) r.result = x = new Object[c];

			for(int j = 1;(j <= c);j++)
				x[j - 1] = d.result(rs, m, j);
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}
}