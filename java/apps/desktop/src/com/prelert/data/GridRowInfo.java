package com.prelert.data;

import java.io.Serializable;

import com.extjs.gxt.ui.client.data.BaseModel;

/**
 * An extension of the Ext GWT BaseModel class encapsulating an item of data
 * for display in a grid row. It represents a single column and value from a
 * row of data from a Prelert database table e.g. description = 'Received service list'.
 * @author Pete Harverson
 */
public class GridRowInfo extends BaseModel implements Serializable
{
	/**
	 * Creates a new, empty GridRowInfo object.
	 */
	public GridRowInfo()
	{
		
	}
	
	
	/**
	 * Creates a GridRowInfo object for the specified column name and value.
	 * @param columnName the column name.
	 * @param columnValue the column value.
	 */
	public GridRowInfo(String columnName, String columnValue)
	{
		setColumnName(columnName);
		setColumnValue(columnValue);
	}
	
	
	/**
	 * Returns the name of the column for this GridRowInfo object.
	 * @return the column name.
	 */
	public String getColumnName()
    {
    	return get("columnName");
    }


	/**
	 * Sets the name of the column for this GridRowInfo object.
	 * @param columnName the column name.
	 */
	public void setColumnName(String columnName)
    {
    	set("columnName", columnName);
    }


	/**
	 * Returns the value of the column for this GridRowInfo object.
	 * @return the column value.
	 */
	public String getColumnValue()
    {
    	return get("columnValue");
    }


	/**
	 * Sets the value of the column for this GridRowInfo object.
	 * @param columnValue the column value.
	 */
	public void setColumnValue(String columnValue)
    {
    	set("columnValue", columnValue);
    }
	
	

}
