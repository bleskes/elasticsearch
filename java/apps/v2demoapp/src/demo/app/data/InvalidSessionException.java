package demo.app.data;

import java.io.IOException;




public class InvalidSessionException extends IOException
{
	public InvalidSessionException()
	{
		super();
	}
	
	
	public InvalidSessionException(String message)
	{
		super(message);
	}
}
