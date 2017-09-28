package net.java.web.warlord.object.spring;

/* Java */

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/* Spring Framework */

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;


/* Warlord */

import net.java.web.warlord.EX;
import net.java.web.warlord.LU;
import net.java.web.warlord.object.AutoAwire;



/**
 * Spring Bean Factory that enhances @Autowire abilities
 * with fields of generic types hooked by {@link AutoAwire}.
 *
 * @author anton.baukin@gmail.com.
 */
public class WarlordBeanFactory extends DefaultListableBeanFactory
{
	public WarlordBeanFactory(BeanFactory parentBeanFactory)
	{
		super(parentBeanFactory);
	}


	/* Default Bean Factory */

	public Object     applyBeanPostProcessorsBeforeInitialization(
	  Object bean, String beanName)
	  throws BeansException
	{
		initBean(bean, beanName);
		return super.applyBeanPostProcessorsBeforeInitialization(bean, beanName);
	}

	protected Object  doCreateBean(String name, RootBeanDefinition mbd, Object[] args)
	{
		//~: push get-record
		GetBean get = pushGet(name);

		if(get != null)
			get.mbd = mbd;

		try
		{
			return super.doCreateBean(name, mbd, args);
		}
		finally
		{
			if(get != null)
				popGet(get);
		}
	}

	protected Object  initializeBean(String name, Object bean, RootBeanDefinition mbd)
	{
		//~: push get-record
		GetBean get = pushGet(name);

		if(get != null)
			get.mbd = mbd;

		try
		{
			return super.initializeBean(name, bean, mbd);
		}
		finally
		{
			if(get != null)
				popGet(get);
		}
	}

	protected GetBean pushGet(String beanName)
	{
		//~: ensure get-list
		LinkedList<GetBean> gets = this.gets.get();
		if(gets == null) this.gets.set(
		  gets = new LinkedList<>());

		//HINT: we assume here that it's not possible
		//  to resolve a prototype bean immediately while
		//  resolving the same bean, i.e., that prototype
		//  bean auto-wires itself.

		//?: {has the same bean on the top}
		if(!gets.isEmpty() && (gets.getFirst().beanName.equals(beanName)))
			return null;

		//~: create get-record
		GetBean get = new GetBean(beanName);
		gets.addFirst(get); //<-- push it

		return get;
	}

	protected void    popGet(GetBean get)
	{
		LinkedList<GetBean> gets = this.gets.get();
		EX.assertn(gets);

		EX.assertx(!gets.isEmpty());
		GetBean x = gets.removeFirst();
		EX.assertx(get == x);

		if(gets.isEmpty())
			this.gets.remove();
	}

	protected final ThreadLocal<LinkedList<GetBean>>
	  gets = new ThreadLocal<>();

	public Object     doResolveDependency (
	  DependencyDescriptor d, String bean,
	  Set<String> names, TypeConverter tc
	)
	  throws BeansException
	{
		GetBean get = EX.assertn(this.gets.get()).getFirst();

		DependencyDescriptor prev = get.depDescr;
		get.depDescr = d;

		try
		{
			return super.doResolveDependency(d, bean, names, tc);
		}
		finally
		{
			get.depDescr = prev;
		}
	}

	/* protected: specials */

	protected static final Object LOG =
	  LU.logger(WarlordBeanFactory.class);

	protected void initBean(Object bean, String beanName)
	{
		GetBean get = EX.assertn(this.gets.get()).getFirst();
		String  oldAutoAwireName = null;

		if(bean instanceof AutoAwire) try
		{
			oldAutoAwireName  = get.autoAwireName;
			get.autoAwireName = beanName;

			initAutoAwire((AutoAwire)bean);
		}
		finally
		{
			get.autoAwireName = oldAutoAwireName;
		}

		//?: {has requests for annotations}
		if(get.initAnses != null)
			initAnnses(bean, get.initAnses);
	}

	protected void initAnnses(Object bean, List<InitAns> anses)
	{
		for(InitAns ans : anses)
			ans.bean.autowiredAnnotations(bean, ans.ans);
	}

	@SuppressWarnings("unchecked")
	protected void initAutoAwire(AutoAwire bean)
	{
		LinkedList<GetBean> gets = this.gets.get();

		//?: {has no enough items}
		if(gets.size() == 1)
		{
			LU.warn(LOG, "initAutoAwire(): bean of class [",
			  bean.getClass().getName(), "] is top-accessed! ",
			  "Is it not injected as @Autowire?");

			return;
		}

		//~: the previous get
		GetBean get = gets.get(1); //<-- hint: stack
		if(get.depDescr == null)
		{
			LU.warn(LOG, "initAutoAwire(): bean of class [",
			  bean.getClass().getName(), "] is not injected as @Autowired?");

			return;
		}

		Type         gt  = null;
		Class<?>     tc  = null;
		Annotation[] ans = null;

		//?: {has field}
		if(get.depDescr.getField() != null)
		{
			gt  = get.depDescr.getField().getGenericType();
			tc  = get.depDescr.getField().getType();
			ans = get.depDescr.getField().getAnnotations();
		}

		//?: {has method | constructor}
		else if(get.depDescr.getMethodParameter() != null)
		{
			gt  = get.depDescr.getMethodParameter().getGenericParameterType();
			tc  = get.depDescr.getMethodParameter().getParameterType();

			//~: collect both annotation groups
			Annotation[] x = get.depDescr.getMethodParameter().getMethodAnnotations();
			Annotation[] y = get.depDescr.getMethodParameter().getParameterAnnotations();
			ans = new Annotation[x.length + y.length];
			System.arraycopy(x, 0, ans, 0, x.length);
			System.arraycopy(y, 0, ans, x.length, y.length);
		}

		//!: double check the type of the bean
		EX.assertx(EX.assertn(tc).isAssignableFrom(bean.getClass()));

		//?: {declaration has type parameters}
		Class[] cs = null;
		if(gt instanceof ParameterizedType)
		{
			Type[] gs = ((ParameterizedType) gt).getActualTypeArguments();

			if((gs != null) && (gs.length != 0))
			{
				ArrayList<Class> a = new ArrayList<>(gs.length);

				for(Type g : gs)
					if(g instanceof Class)
						a.add((Class) g);

				cs = a.toArray(new Class[a.size()]);
			}
		}

		//?: {has generic arguments}
		if((cs != null) && (cs.length != 0))
			bean.autowiredTypes(cs);

		//~: remove @Autowired (as redundant)
		List<Annotation> xans = new ArrayList<>(Arrays.asList(ans));
		for(Iterator<Annotation> i = xans.iterator();(i.hasNext());)
			if(i.next() instanceof Autowired)
				i.remove();

		//?: {has any left}
		if(!xans.isEmpty())
		{
			InitAns ia = new InitAns();
			ia.bean = bean;
			ia.ans  = xans.toArray(new Annotation[xans.size()]);

			if(get.initAnses == null)
				get.initAnses = new ArrayList<>(2);
			get.initAnses.add(ia);
		}
	}

	protected static class GetBean
	{
		public GetBean(String beanName)
		{
			this.beanName = beanName;
		}

		public String               beanName;
		public RootBeanDefinition mbd;

		/**
		 * DependencyDescriptor of pending AutoAwire request.
		 * Hint: this descriptor is not of the bean is being
		 * created (it's @Autowire annotations are processed).
		 * It is descriptor of the field of that bean.
		 */
		public DependencyDescriptor depDescr;
		public String               autoAwireName;

		/**
		 * Collection of requests to init the
		 * annotations of injected beans.
		 */
		public List<InitAns>        initAnses;
	}

	protected static class InitAns
	{
		public AutoAwire    bean;
		public Annotation[] ans;
	}
}