package demo.app.splash.gxt;

import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.SplitButton;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.user.client.ui.Widget;

import demo.app.client.ViewWidget;
import demo.app.data.Tool;
import demo.app.data.UsageView;
import demo.app.data.View;

public class AutoSizeViewWidget extends ContentPanel implements ViewWidget
{
	private UsageView m_TimeSeriesView;

	public AutoSizeViewWidget(UsageView timeSeriesView)
	{
		m_TimeSeriesView = timeSeriesView;
		

		setSize(650, 525);
		setLayout(new FitLayout());
		setHeaderVisible(false);
	  
	    Label label1 = new Label("Autosizing " + timeSeriesView.getDataType());   
	    add(label1);   
	    
	    createToolbar();

	}
	
	
	protected void createToolbar()
	{
	    ToolBar toolBar = new ToolBar();   
	    
	    Button item1 = new Button("Button 1");   

	  
	    toolBar.add(item1);   
	  
	    toolBar.add(new SeparatorToolItem());   
	  
	    SplitButton splitItem = new SplitButton("Split Button");   
	  
	    Menu menu = new Menu();   
	    menu.add(new MenuItem("<b>Bold</b>"));   
	    menu.add(new MenuItem("<i>Italic</i>"));   
	    menu.add(new MenuItem("<u>Underline</u>"));   
	    splitItem.setMenu(menu);   
	  
	    toolBar.add(splitItem);   
	  
	    toolBar.add(new SeparatorToolItem());   
	  
	    toolBar.add(new FillToolItem());   

	    toolBar.add(new Button("Button 2"));  
	    
	    setTopComponent(toolBar);
	}
	
	

	/**
	 * Returns the user interface Widget sub-class itself.
	 * @return the Widget which is added in the user interface.
	 */
	public Widget getWidget()
	{
		return this;
	}
	
	
	/**
	 * Returns the View displayed in the Widget.
	 * @return the view displayed in the Widget.
	 */
	public View getView()
	{
		return m_TimeSeriesView;
	}
	
	
	/**
	 * Loads the data in the widget according to its current configuration.
	 */
	public void load()
	{
		// TO DO.
	}
	
	
	/**
	 * Runs a tool on the view in the widget.
	 * @param tool the tool to run.
	 */
	public void runTool(Tool tool)
	{
		// TO DO.
	}

}
