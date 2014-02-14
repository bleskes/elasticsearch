package demo.app.client;

import java.util.ArrayList;
import java.util.HashMap;

import com.extjs.gxt.ui.client.Events;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.util.Point;
import com.extjs.gxt.ui.client.widget.Window;
import com.google.gwt.core.client.GWT;


public class DesktopWindowManager
{
	private ArrayList<Window>	m_Windows;
	private HashMap<Window, Point> m_RestorePositions; // Stores positions of hidden windows.
	private HashMap<Window, Listener<WindowEvent>> m_CloseListeners;
	private HashMap<Window, Listener<WindowEvent>> m_HideListeners;
	
	private int		m_XStart;
	private int		m_YStart;
	private int		m_Offset;
	
	
	public DesktopWindowManager(int xStart, int yStart, int offset)
	{
		m_Windows = new ArrayList<Window>();
		m_RestorePositions = new HashMap<Window, Point>();
		m_CloseListeners = new HashMap<Window, Listener<WindowEvent>>();
		m_HideListeners = new HashMap<Window, Listener<WindowEvent>>();
		
		m_XStart = xStart;
		m_YStart = yStart;
		m_Offset = offset;
	}
	
	
	public void addWindow(Window window)
	{
		positionWindow(window);
		
		m_Windows.add(window);
		
		Listener<WindowEvent> closeListener = new Listener<WindowEvent>(){

            public void handleEvent(WindowEvent be)
            {
            	removeWindow(be.window);
            }
			
		};
		
		Listener<WindowEvent> hideListener = new Listener<WindowEvent>(){

            public void handleEvent(WindowEvent be)
            {
            	m_RestorePositions.put(be.window, be.window.getPosition(true));
            }
			
		};

		window.addListener(Events.BeforeClose, closeListener);
		window.addListener(Events.BeforeHide, hideListener);
		m_CloseListeners.put(window, closeListener);
	}
	
	
	public void removeWindow(Window window)
	{
		window.removeListener(Events.BeforeClose, m_CloseListeners.get(window));
		window.removeListener(Events.BeforeHide, m_HideListeners.get(window));
		m_RestorePositions.remove(window);
		m_Windows.remove(window);
	}
	
	
	public void positionWindow(Window window)
	{
		// Loop through the windows to find the first available position,
		// adding multiples of the specified offset.
		int numberOfWindows = m_Windows.size();
		
		int offset = numberOfWindows*m_Offset;
		int xPos = m_XStart; 
		int yPos = m_YStart; 
		boolean located = false;
		
		for (int i = 0; i <= numberOfWindows; i++)
		{
			xPos = m_XStart + i*offset;
			yPos = m_YStart + i*offset;
			GWT.log("Trying xPos: " + xPos, null);
			
			for (Window win : m_Windows)
			{
				if (win.isVisible() == true)
				{
					if (win.getPosition(true).x == xPos)
					{
						break;
					}
				}
				else
				{
					Point restorePos = m_RestorePositions.get(win);
					if (restorePos.x == xPos)
					{
						break;
					}
				}
				located = true;
			}
			
			if (located == true)
			{
				break;
			}
		}
		
		window.setPosition(xPos, yPos);
	}
}
