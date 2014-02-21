package demo.app.client;

import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.widget.*;
import com.extjs.gxt.ui.client.widget.button.*;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class App implements EntryPoint
{

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad()
	{
		/*
		ButtonBar bar = new ButtonBar();
		Button button = new Button("Click me please");


		button.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
	            MessageBox.confirm("Prelert Demo Ext GWT App", "This toolkit is cool!", null);
            }
			
		});
		
		bar.add(button);
		RootPanel.get().add(bar);
		*/
		
		 ContentPanel cp = new ContentPanel();
		 cp.setHeading("Folder Contents");
		 cp.setSize(250,140);
		 cp.setPosition(10, 10);
		 cp.setCollapsible(true);
		 cp.setFrame(true);
		 cp.setBodyStyle("backgroundColor: white;");
		 cp.getHeader().addTool(new ToolButton("x-tool-gear"));
		 cp.getHeader().addTool(new ToolButton("x-tool-close"));
		 cp.addText("Here is some text added by Pete");
		 cp.addButton(new Button("Ok"));
		 cp.setIconStyle("tree-folder-open");
		 RootPanel.get().add(cp);
		 cp.layout();
	}
}
