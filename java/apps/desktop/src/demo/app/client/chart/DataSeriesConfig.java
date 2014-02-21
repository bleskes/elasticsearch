package demo.app.client.chart;

import com.extjs.gxt.ui.client.data.BaseModel;

public class DataSeriesConfig extends BaseModel
{
	
	public DataSeriesConfig(String metric, String source)
	{
		this(metric, source, null);
	}
	
	
	public DataSeriesConfig(String metric, String source, String user)
	{
		set("metric", metric);
		set("source", source);
		set("user", user);
	}
		
	
	public String getMetric()
	{
		return get("metric");
	}
	
	
	public String getSource()
	{
		return get("source");
	}
	
	
	public String getUser()
	{
		return get("user");
	}
	
	
	public void setLineColour(String lineColour)
	{
		set("lineColour", lineColour);
	}
	
	
	public String getLineColour()
	{
		return get("lineColour");
	}
}
