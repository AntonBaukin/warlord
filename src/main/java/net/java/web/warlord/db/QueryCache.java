package net.java.web.warlord.db;

/* Java */

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/* SAX */

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;

/* Warlord */

import net.java.web.warlord.EX;


/**
 * Supports loading and storing database queries
 * from XML files names as the Get-strategies
 * with '.q.xml' suffix added.
 *
 * Cache supports hierarchy of Get-strategies
 * and look up in the parent when doesn't find
 * the query id requested.
 *
 *
 * @author anton.baukin@gmail.com.
 */
public class QueryCache
{
	/* Cache Registry */

	/**
	 * Returns cache entry for the Get-class given
	 * (data access strategy).
	 */
	public static QueryCache cache(Class<?> get, String dialect)
	{
		EX.assertn(get);

		//?: {query base class} has it no
		if(GetBase.class.equals(get))
			return null;

		//?: {object base class} has it no
		if(Object.class.equals(get))
			return null;

		//~: get with create on first demand
		return CACHES.computeIfAbsent(new Key(get, dialect), QueryCache::new);
	}

	private static final ConcurrentMap<Key, QueryCache>
	  CACHES = new ConcurrentHashMap<>(17);

	protected QueryCache(Key key)
	{
		this.key     = key;
		this.parent  = QueryCache.cache(key.get.getSuperclass(), key.dialect);
		this.files   = getQueryFiles();

		ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
		this.readLock  = rwl.readLock();
		this.writeLock = rwl.writeLock();
	}


	/* Query Cache */

	public String q(String id)
	{
		EX.asserts(id);
		readLock.lock();

		try
		{
			//?: {no loaded yet} do load
			if(queries == null)
			{
				readLock.unlock();
				writeLock.lock();

				try
				{
					if(queries == null)
						this.load();
				}
				finally
				{
					writeLock.unlock();
					readLock.lock();
				}
			}

			//~: lookup in the mapping
			String q = queries.get(id);
			return (q != null)?(q):(parent == null)?(null):parent.q(id);
		}
		finally
		{
			readLock.unlock();
		}
	}

	public void   load()
	{
		writeLock.lock();

		try
		{
			//?: {do reload}
			if(queries != null)
			{
				queries = null;

				if(parent != null)
					parent.load();
			}

			queries = new HashMap<>(17);

			//?: {has no file} do nothing
			if(files == null) return;

			//~: invoke the reader
			for(URL file : files) try
			{
				loadQueries(file, queries);
			}
			catch(Throwable e)
			{
				throw EX.wrap(e, "Error while processing ",
				  "queries file [", file, "]!");
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}


	/* Cache Key */

	public static class Key
	{
		public final Class<?> get;
		public final String   dialect;

		public Key(Class<?> get, String dialect)
		{
			this.get     = EX.assertn(get);
			this.dialect = dialect;
		}

		public boolean equals(Object o)
		{
			return (this == o) || (o != null) &&
			  get.equals(((Key)o).get) && EX.eq(dialect, ((Key)o).dialect);
		}

		public int     hashCode()
		{
			return 31 * get.hashCode() +
			  (dialect != null ? dialect.hashCode() : 0);
		}
	}

	public final Key key;


	/* protected: loading the queries */

	protected URL[] getQueryFiles()
	{
		ArrayList<URL> files = new ArrayList<>(4);

		for(int i = 0;;i++)
		{
			URL url = key.get.getResource(
			  key.get.getSimpleName() + '.' + i + ".xml");

			if(url == null)
				break;
			else
				files.add(url);
		}

		return files.toArray(new URL[files.size()]);
	}

	protected void  loadQueries(URL file, Map<String, String> queries)
	  throws Throwable
	{
		synchronized(QueryCache.class)
		{
			if(parserFactory == null)
				parserFactory = SAXParserFactory.newInstance();
		}

		parserFactory.newSAXParser().parse(
		  file.toString(), new QueriesReader(key.dialect, queries));
	}

	protected static volatile SAXParserFactory parserFactory;


	/* Queries Reader */

	public static class QueriesReader extends DefaultHandler
	{
		public QueriesReader(String dialect, Map<String, String> queries)
		{
			this.dialect = dialect;
			this.queries = EX.assertn(queries);
		}

		public final String dialect;
		public final Map<String, String> queries;


		/* Content Handler */

		public void startElement(String u, String n, String q, Attributes a)
		{
			//?: {does dialect match}
			if("queries".equals(q))
			{
				String d = a.getValue("dialect");
				match = (d == null) || d.equals(dialect);
			}

			//?: {read the query tag}
			if(match && "query".equals(q))
			{
				sb.delete(0, sb.length());

				id = EX.asserts(a.getValue("id"),
				  "Query id is undefined!");
			}
		}

		public void endElement(String u, String n, String q)
		{
			if(match && "query".equals(q))
			{
				String qq = sb.toString().trim();
				sb.delete(0, sb.length());

				EX.assertn(id);
				EX.asserts(qq, "Query by id [", id, "] is empty!");

				queries.put(id, qq);
				id = null;
			}
		}

		public void characters(char[] ch, int start, int length)
		{
			if(id != null)
				sb.append(ch, start, length);
		}

		protected boolean       match;
		protected String        id;
		protected StringBuilder sb = new StringBuilder(128);
	}


	/* private: the state of the cache */

	private final QueryCache     parent;
	private final URL[]          files;
	private Map<String, String>  queries;
	private final Lock           readLock;
	private final Lock           writeLock;
}