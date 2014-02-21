package com.prelert.data;

import java.io.Serializable;
import java.util.ArrayList;

public class ViewMenuItem implements Serializable
{

	private String m_Name;
	private String m_ViewToOpen;
	
	private String m_FilterAttribute;
	private String m_FilterArg;
	
	
	public String getName()
	{
		return m_Name;
	}


	public void setName(String name)
	{
		m_Name = name;
	}


	public String getViewToOpen()
	{
		return m_ViewToOpen;
	}


	public void setViewToOpen(String viewToOpen)
	{
		m_ViewToOpen = viewToOpen;
	}


	public String getFilterAttribute()
	{
		return m_FilterAttribute;
	}


	public void setFilterAttribute(String queryFilter)
	{
		m_FilterAttribute = queryFilter;
	}


	public String getFilterArg()
	{
		return m_FilterArg;
	}


	public void setFilterArg(String filterArg)
	{
		m_FilterArg = filterArg;
	}
	

	@Override
    public String toString()
    {
		StringBuilder strRep = new StringBuilder("{");
		   
		strRep.append("Name=");
		strRep.append(m_Name);

		strRep.append(",Open View=");
		strRep.append(m_ViewToOpen);

		if (m_FilterAttribute != null)
		{
			strRep.append(",Filter Attribute=");
			strRep.append(m_FilterAttribute);
	
			strRep.append(",Filter Arg=");
			strRep.append(m_FilterArg);
		}

		strRep.append('}');

		return strRep.toString();
    }
}
