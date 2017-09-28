package net.java.web.warlord.servlet.filter;

/* Java */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation to mark a Filter to be added
 * to the processing chain of the application.
 *
 * @author anton.baukin@gmail.com.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface PickFilter
{
	/**
	 * Tells the global order of invocation
	 * the filters within the same stage.
	 */
	int[] order();

	/**
	 * Tells the stage this filter processes.
	 */
	FilterStage[] stage() default { FilterStage.REQUEST };
}