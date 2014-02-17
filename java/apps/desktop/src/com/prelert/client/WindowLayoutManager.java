package com.prelert.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.extjs.gxt.ui.client.Events;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.util.Point;
import com.extjs.gxt.ui.client.widget.Window;
import com.google.gwt.core.client.GWT;


/**
 * Class which manages the layout of windows as they are added to the Prelert
 * desktop. It uses a simple algorithm to cascade new windows when they are
 * first added to the desktop.
 * @author Pete Harverson
 */
public class WindowLayoutManager
{
	private ArrayList<Window>		m_Windows;
	private HashMap<Window, Point> 	m_RestorePositions; // Stores positions of hidden windows.
	private HashMap<Window, Listener<WindowEvent>> m_CloseListeners;
	private HashMap<Window, Listener<WindowEvent>> m_HideListeners;
	
	private int		m_XStart;
	private int		m_YStart;
	private int		m_Offset;
	
	
	/**
	 * Creates a new window layout manager to manage the positioning of new
	 * windows which are added to the desktop.
	 * @param xStart top left corner x coordinate for the first window to be added.
	 * @param yStart top left y coordinate for the first window to be added.
	 * @param offset offset for x and y coordinates of subsequent windows.
	 */
	public WindowLayoutManager(int xStart, int yStart, int offset)
	{
		m_Windows = new ArrayList<Window>();
		m_RestorePositions = new HashMap<Window, Point>();
		m_CloseListeners = new HashMap<Window, Listener<WindowEvent>>();
		m_HideListeners = new HashMap<Window, Listener<WindowEvent>>();
		
		m_XStart = xStart;
		m_YStart = yStart;
		m_Offset = offset;
	}
	
	
	/**
	 * Adds a new window to the window manager, and sets the initial position of
	 * the window on the desktop.
	 * @param window the window to be added.
	 */
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
	
	
	/**
	 * Removes a window from the window manager.
	 * @param window the desktop window to remove.
	 */
	public void removeWindow(Window window)
	{
		window.removeListener(Events.BeforeClose, m_CloseListeners.get(window));
		window.removeListener(Events.BeforeHide, m_HideListeners.get(window));
		m_RestorePositions.remove(window);
		m_Windows.remove(window);
	}
	
	
	/**
	 * Positions the window by setting the coordinates for the top left corner
	 * of the window.
	 * @param window the desktop window to position.
	 */
	public void positionWindow(Window window)
	{
		// Loop through the windows to find the first available position,
		// adding multiples of the specified offset.
		int numberOfWindows = m_Windows.size();
		
		//int offset = numberOfWindows*m_Offset;
		int xPos = m_XStart; 
		int yPos = m_YStart; 
		boolean located = false;
		boolean foundClash = false;
		
		for (int i = 0; i <= numberOfWindows; i++)
		{
			xPos = m_XStart + i*m_Offset;
			yPos = m_YStart + i*m_Offset;
			
			foundClash = false;
			
			for (Window win : m_Windows)
			{
				if (win.isVisible() == true)
				{
					if (win.getPosition(true).x == xPos)
					{
						foundClash = true;
						break;
					}
				}
				else
				{
					Point restorePos = m_RestorePositions.get(win);
					if (restorePos.x == xPos)
					{
						foundClash = true;
						break;
					}
				}
				located = true;
			}
			
			if (foundClash == false)
			{
				break;
			}
		}
		
		window.setPosition(xPos, yPos);
	}
	
	
	/**
	 * Returns a list of the windows that have been added to this layout manager.
	 * @return the windows added to this layout manager.
	 */
	public List<Window> getWindows()
	{
		return m_Windows;
	}
}

