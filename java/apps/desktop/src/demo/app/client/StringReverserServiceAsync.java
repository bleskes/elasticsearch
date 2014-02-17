package demo.app.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * StringReverserServiceAsync
 * 
 * Asynchronous interface for String Reverser Service
 * 
 * @author Masud Idris
 * @version 1.0
 * @since Dec 6, 2007, 11:35:32 AM
 * 
 */
public interface StringReverserServiceAsync
{
	public void reverseString(String stringToReverse, AsyncCallback async);
}
