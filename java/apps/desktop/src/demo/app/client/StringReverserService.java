package demo.app.client;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * StringReverserService
 * 
 * This is the interface used to define the String Reverser Service
 * 
 * @author Masud Idris
 * @version 1.0
 * @since Dec 6, 2007, 11:35:32 AM
 * 
 */

public interface StringReverserService extends RemoteService
{
	public String reverseString(String stringToReverse);
}
