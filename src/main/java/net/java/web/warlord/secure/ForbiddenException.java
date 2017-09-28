package net.java.web.warlord.secure;

/**
 * Raised to enforce security on restricted areas.
 *
 * @author anton.baukin@gmail.com
 */
public class ForbiddenException extends RuntimeException
{
	public ForbiddenException()
	{}

	public ForbiddenException(String message)
	{
		super(message);
	}
}