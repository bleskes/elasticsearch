package demo.app.data;

import java.io.Serializable;
import java.util.Date;

import com.extjs.gxt.ui.client.data.BaseModelData;


public class UsageRecord extends BaseModelData implements Serializable
{
	public UsageRecord()
	{
		
	}
	
	public double getValue()
	{
		double valueInt = get("value", -1D);
		return valueInt;
	}
	
	
	public Date getTime()
	{
		Date timeStamp = get("time");
		return timeStamp;
	}
	
	
	@Override
    public String toString()
    {
		StringBuilder strRep = new StringBuilder();
		strRep.append(getProperties());
		
		return strRep.toString();
    }
}
