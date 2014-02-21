package com.prelert.service;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.*;

/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the View Directory service.
 * @author Pete Harverson
 */
public interface ViewDirectoryServiceAsync
{
	public void getDesktopViewConfig(AsyncCallback<DesktopViewConfig> callback);
	
	public void getDrillDownView(String viewName, String filterAttribute, 
			String filterValue, AsyncCallback<View> callback);
	
	public void getHistoryView(AsyncCallback<HistoryView> callback);
	
	public void getProbableCauseView(int probableCauseId, 
			AsyncCallback<ListView> callback);
}
