package net.java.web.warlord.object;

/* Java */

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

/* Java Annotations */

import javax.annotation.PreDestroy;

/* Spring Framework */

import org.springframework.stereotype.Component;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.LU;


/**
 * This global singleton collects prototype
 * beans that are instantiated to also be
 * global singletons. @PreDestroy them.
 *
 * @author anton.baukin@gmail.com.
 */
@Component
public class BeanTracker
{
	public BeanTracker()
	{
		ReentrantReadWriteLock rwl =
		  new ReentrantReadWriteLock();

		this.readLock = rwl.readLock();
		this.writeLock = rwl.writeLock();
	}


	/* Bean Tracker */

	public boolean add(Object bean)
	{
		writeLock.lock();

		try
		{
			return beans.add(new Entry(bean));
		}
		finally
		{
			writeLock.unlock();
		}
	}

	public boolean remove(Object bean)
	{
		writeLock.lock();

		try
		{
			return beans.remove(new Entry(bean));
		}
		finally
		{
			writeLock.unlock();
		}
	}

	/**
	 * Iterates over the collection of tracked objects
	 * with the read lock held. Stops when predicate
	 * returns true and returns that object.
	 * Returns null when not stopped.
	 */
	public Object  iterate(Predicate<Object> i)
	{
		readLock.lock();

		try
		{
			for(Entry e : beans)
				if(i.test(e.bean))
					return e.bean;
		}
		finally
		{
			readLock.unlock();
		}

		return null;
	}


	/* protected: destruction */

	private final Object LOG = LU.logger(this.getClass());

	@PreDestroy
	protected void destroy()
	{
		HashSet<Entry> ds = null;

		while(true)
		{
			writeLock.lock();

			try
			{
				if(ds == null)
					ds = new HashSet<>(beans);
				else
				{
					ds.clear();
					ds.addAll(beans);
				}

				beans.clear();

				//?: {nothing added}
				if(ds.isEmpty())
					return;
			}
			finally
			{
				writeLock.unlock();
			}

			for(Entry e : ds) try
			{
				OU.methods(e.bean.getClass(), m ->
				{
					if(m.isAnnotationPresent(PreDestroy.class))
						try
						{
							//?: {is not public} access it
							if((m.getModifiers() & Modifier.PUBLIC) == 0)
								m.setAccessible(true);

							m.invoke(e.bean);
						}
						catch(Throwable x)
						{
							throw EX.wrap(x);
						}

					return false;
				});
			}
			catch(Throwable x)
			{
				LU.error(LOG, x, "Error destroying bean ", LU.sig(e.bean));
			}
		}
	}

	protected final Lock           readLock;
	protected final Lock           writeLock;
	protected final HashSet<Entry> beans = new HashSet<>();


	/* Entry */

	static final class Entry
	{
		public Entry(Object bean)
		{
			this.bean = bean;
		}

		public final Object bean;

		public int     hashCode()
		{
			return System.identityHashCode(bean);
		}

		public boolean equals(Object x)
		{
			return (x == this) ||
			  (x instanceof Entry) && (((Entry)x).bean == bean);
		}
	}
}