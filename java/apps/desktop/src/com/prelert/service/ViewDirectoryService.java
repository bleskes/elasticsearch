package com.prelert.service;

import com.google.gwt.user.client.rpc.RemoteService;
import com.prelert.data.DesktopViewConfig;
import com.prelert.data.HistoryView;
import com.prelert.data.ListView;
import com.prelert.data.View;

public interface ViewDirectoryService extends RemoteService
{
	/**
	 * Returns the Desktop View configuration that has been defined
	 * in the view configuration file.
	 */
	public DesktopViewConfig getDesktopViewConfig();
	
	
    /**
     * Creates and returns a new view, based on the view with the specified name,
     * and setting the supplied filter attribute and filter value on the new view.
     * @return new drill-down view based on the supplied view and filter.
     * @throws NullPointerException if there is no View in the View Directory with the
     * given name.
     */
	public View getDrillDownView(String viewName, String filterAttribute, String filterValue) 
		throws NullPointerException;
	
	
	/**
	 * Returns the Evidence History View to display the history of an item of evidence.
	 * @return the History View or <code>null</code> if no History View has been
	 * 			configured.
	 */
	public HistoryView getHistoryView();
	
	
	/**
	 * Returns the Probable Cause View to display the probable cause with the
	 * specified id.
	 * @param probableCauseId value of the id column for the Probable Cause to display.
	 * @return Probable Cause view for the specified id.
	 */
	public ListView getProbableCauseView(int probableCauseId);
}
