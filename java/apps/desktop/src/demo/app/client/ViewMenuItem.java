package demo.app.client;

import com.extjs.gxt.ui.client.widget.menu.MenuItem;

import demo.app.data.*;

/**
 * Extension of the Ext GWT MenuItem widget to hold a reference to a tool to be
 * run when the menu item is selected.
 * @author Pete Harverson
 */
public class ViewMenuItem extends MenuItem
{
	private Tool	m_Tool;
	
	
	/**
	 * Creates a new, empty View MenuItem.
	 */
	public ViewMenuItem()
	{
		
	}
	
	
	/**
	 * Creates a new menu item to run the specified tool when selected.
	 * @param tool tool to run when the menu item is selected.
	 */
	public ViewMenuItem(Tool tool)
	{
		m_Tool = tool;
		if(m_Tool != null)
		{
			if (m_Tool.getClass() == CausalityViewTool.class)
			{
				setText(ClientUtil.CLIENT_CONSTANTS.showProbableCause());
			}
			else if (m_Tool.getClass() == ListViewTool.class ||
					m_Tool.getClass() == UsageViewTool.class)
			{
				ViewTool viewTool = (ViewTool)m_Tool;
				setText(ClientUtil.CLIENT_CONSTANTS.show() + " " + 
						viewTool.getViewToOpen());
			}
			else
			{
				setText(m_Tool.getName());
			}
		}
	}
	
	
	/**
	 * Returns the tool to be run when this menu item is selected.
	 * @return the tool to run.
	 */
	public Tool getTool()
    {
    	return m_Tool;
    }

	
	/**
	 * Sets the tool to be run when this menu item is selected.
	 * @param viewTool the tool to run.
	 */
	public void setTool(Tool viewTool)
    {
		m_Tool = viewTool;
    }


}
