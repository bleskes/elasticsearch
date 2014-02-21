package com.prelert.data;

import java.io.Serializable;

public class SortInformation implements Serializable
{
	public enum SortDirection { NONE, ASC, DESC }
	
	private String m_ColumnName;
	private SortDirection m_SortDir;
	
	public SortInformation()
	{
		
	}
	
	
	public SortInformation(String columnName, SortDirection sortDirection)
	{
		m_ColumnName = columnName;
		m_SortDir = sortDirection;
	}


	public String getColumnName()
    {
    	return m_ColumnName;
    }


	public void setColumnName(String columnName)
    {
    	m_ColumnName = columnName;
    }


	public SortDirection getSortDirection()
    {
    	return m_SortDir;
    }


	public void setSortDirection(SortDirection sortDir)
    {
    	m_SortDir = sortDir;
    }
	
	
	public void setSortDirection(String sortDir)
	{
		m_SortDir = SortDirection.valueOf(sortDir);
	}


	@Override
    public String toString()
    {
		StringBuilder strRep = new StringBuilder("{Column Name=");
		strRep.append(m_ColumnName);
		strRep.append(",Direction=");
		strRep.append(m_SortDir);
		strRep.append('}');
		
		return strRep.toString();
    }
	
	
	
	
}
