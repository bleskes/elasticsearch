package com.prelert.client;

import com.extjs.gxt.ui.client.widget.Window;
import com.prelert.data.Tool;
import com.prelert.data.View;

/**
 * Abstract subclass of the Ext GWT Window, defining methods
 * to be implemented by Prelert Desktop View Windows.
 * @author Pete Harverson
 */
public abstract class ViewWindow extends Window
{
	/**
	 * Returns the View displayed in the Window.
	 * @return the view displayed in the Window.
	 */
	public abstract View getView();
	
	
	/**
	 * Loads the data in the window according to its current configuration.
	 */
	public abstract void load();
	
	
	/**
	 * Runs a tool on the view.
	 * @param tool the tool to run.
	 */
	public abstract void runTool(Tool tool);
}
