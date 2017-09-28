package net.java.web.warlord.servlet.filter;

/**
 * Strategy to access filters.
 *
 * @author anton.baukin@gmail.com.
 */
public interface FiltersPoint
{
	/* Filters Point */

	public Filter[] getFilters(FilterStage stage);
}