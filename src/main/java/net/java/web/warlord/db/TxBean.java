package net.java.web.warlord.db;

/* Spring Framework */

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.object.Callable;


/**
 * Wraps execution into transaction scopes.
 *
 * @author anton.baukin@gmail.com.
 */
@Component @Scope("prototype")
public class TxBean
{
	/* Transactional Bean */

	@SuppressWarnings("unchecked")
	public <T> T  invoke(Callable<T> task)
	{
		try
		{
			if(newTx)
				return (T) invokeNewTx(task);
			else
				return (T) invokeTx(task);
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}

	public void   run(Runnable task)
	{
		try
		{
			if(newTx)
				invokeNewTx(Callable.wrap(task));
			else
				invokeTx(Callable.wrap(task));
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}

	public TxBean setNew(boolean newTx)
	{
		this.newTx = newTx;
		return this;
	}

	protected boolean newTx;


	/* protected: execution variants */

	@Transactional(rollbackFor = Throwable.class,
	  propagation = Propagation.REQUIRED)
	protected Object invokeTx(Callable task)
	  throws Throwable
	{
		return task.call();
	}

	@Transactional(rollbackFor = Throwable.class,
	  propagation = Propagation.REQUIRES_NEW)
	protected Object invokeNewTx(Callable task)
	  throws Throwable
	{
		return task.call();
	}
}