package com.prelert.data;

import java.io.Serializable;
import java.util.Date;

import com.extjs.gxt.ui.client.data.BaseModelData;


/**
 * Class encapsulates a record of usage data stored in the Prelert database.
 * @author Pete Harverson
 */
public class UsageRecord extends BaseModelData implements Serializable
{
	/**
	 * Creates a new Usage record.
	 */
	public UsageRecord()
	{
		
	}
	
	
	/**
	 * Returns the usage record metric value.
	 * @return metric value.
	 */
	public int getValue()
	{
		int valueInt = get("value", -1);
		return valueInt;
	}
	
	
	/**
	 * Returns the time of the usage record.
	 * @return recorded time of usage data.
	 */
	public Date getTime()
	{
		Date timeStamp = get("time");
		return timeStamp;
	}
	
	
	/**
	 * Returns a summary of this Usage Record.
	 * @return String representation of the Usage Record.
	 */
    public String toString()
    {
		StringBuilder strRep = new StringBuilder();
		strRep.append(getProperties());
		
		return strRep.toString();
    }
}

