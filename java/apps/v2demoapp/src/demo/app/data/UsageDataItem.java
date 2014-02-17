package demo.app.data;

import java.io.Serializable;
import java.util.Date;


/**
 * Class encapsulating an item of usage data stored in the Prelert database.
 * @author Pete Harverson
 */
public class UsageDataItem implements Serializable
{
	private Date 	m_Time;
	private int 	m_Value;


	/**
	 * Creates a new empty item of usage data.
	 */
	public UsageDataItem()
	{

	}
	
	
	/**
	 * Creates a new item of usage data with the given time and value.
	 * @param time recorded time of usage data.
	 * @param value metric value of the data item.
	 */
	public UsageDataItem(Date time, int value)
	{
		m_Time = time;
		m_Value = value;
	}


	/**
	 * Returns the time of the usage data.
	 * @return recorded time of usage data.
	 */
	public Date getTime()
	{
		return m_Time;
	}


	/**
	 * Sets the time of the usage data.
	 * @param time recorded time of usage data.
	 */
	public void setTime(Date time)
	{
		m_Time = time;
	}


	/**
	 * Returns the value of the usage data item.
	 * @return metric value.
	 */
	public int getValue()
	{
		return m_Value;
	}


	/**
	 * Sets the value of the usage data item.
	 * @param value metric value of the data item.
	 */
	public void setValue(int value)
	{
		m_Value = value;
	}
	
	
	/**
	 * Returns a summary of this item of usage data.
	 * @return String representation of the Usage data item.
	 */
	public String toString()
	{
		StringBuilder strRep = new StringBuilder('{');
		strRep.append("time=");
		strRep.append(m_Time);
		strRep.append(", value=");
		strRep.append(m_Value);
		strRep.append('}');
		
		return strRep.toString();
	}

}
