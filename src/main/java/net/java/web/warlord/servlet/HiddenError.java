package net.java.web.warlord.servlet;

/**
 * Mark exception class with this interface
 * to indicate the the error must not be reported.
 *
 * @author anton.baukin@gmail.com
 */
public interface HiddenError
{
	/**
	 * Tells that after processing on the upper
	 * level of the application stack this error
	 * must not be further reported.
	 */
	default boolean isTransparent()
	{
		return false;
	}
}