package demo.app.client;

import java.io.Serializable;

import com.extjs.gxt.ui.client.data.BaseModel;

public class GridRowInfo extends BaseModel implements Serializable
{
	public GridRowInfo()
	{
		
	}
	
	
	public GridRowInfo(String columnName, String columnValue)
	{
		setColumnName(columnName);
		setColumnValue(columnValue);
	}
	
	
	public String getColumnName()
    {
    	return get("columnName");
    }


	public void setColumnName(String columnName)
    {
    	set("columnName", columnName);
    }


	public String getColumnValue()
    {
    	return get("columnValue");
    }


	public void setColumnValue(String columnValue)
    {
    	set("columnValue", columnValue);
    }
}
