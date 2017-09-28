package net.java.web.warlord.object;

/* Java */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation tells to invoke the specified
 * method of the injecting bean with argument:
 * the bean is being injected. Used for classes
 * that are {@link AutoAwire}.
 *
 * @author anton.baukin@gmail.com.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface CallMe
{
	/**
	 * Tells the method name.
	 */
	String value() default "";
}