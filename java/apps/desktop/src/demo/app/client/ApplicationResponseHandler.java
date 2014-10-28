/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ***********************************************************/

package demo.app.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import static demo.app.service.ApplicationResponseMessages.*;


/**
 * Main application response handler implementing the GWT RPC AsyncCallback
 * interface. The class checks the received error against a number of known
 * cases when an asynchronous call fails to complete normally e.g. HttpSession
 * timeout.
 * <p>
 * Client-side code should use this handler to receive responses from
 * remote procedure calls, providing implementations of the
 * {@link #uponSuccess(T)} and {@link #uponFailure(Throwable)} methods. 
 * @author Pete Harverson
 *
 * @param <T> the return type of the correlated synchronous method.
 */
public abstract class ApplicationResponseHandler<T> implements AsyncCallback<T>
{
	public ApplicationResponseHandler()
	{

	}


	/**
	 * Called when an asynchronous call fails to complete normally.
	 * The type of failure is checked against a number of known cases and
	 * the appropriate action taken e.g. redirect to login page if the user's
	 * HttpSession has timed out. If an unexpected error has occurred then
	 * the error is forwarded on to the {@link #uponFailure(Throwable)}
	 * method defined in the concrete sub-class.
	 * @param caught failure encountered while executing a remote procedure call.
	 */
	public final void onFailure(Throwable caught)
	{
		String messageKey = caught.getMessage();
		
		// If the request has gone through the ValidSessionFilter, then the 
		// exception will be a com.google.gwt.user.client.rpc.StatusCodeException,
		// whose message should refer to the InvalidSessionException.
		if (messageKey.indexOf(SESSION_TIMEOUT) >= 0)
		{
			Window.Location.reload();
			return;
		}
		
		uponFailure(caught); 
	}


	/**
	 * Called when an asynchronous call completes successfully. The
	 * result is forwarded onto the {@link #uponSuccess(T)} method defined in the
	 * concrete sub-class.
	 * @param result the return value of the remote procedure call.
	 */
	public final void onSuccess(T result)
	{
		uponSuccess(result); 
	}


	/** 
	 * Called when the asynchronous call completes successfully. 
	 * @param result the return value of the remote procedure call.
	 */
	public abstract void uponSuccess(T result);


	/**
	 * Called when an asynchronous call fails to complete normally and the 
	 * error is not one of the cases known by this handler.
	 * @param caught failure encountered while executing a remote procedure call.
	 */
	public abstract void uponFailure(Throwable problem);
}
