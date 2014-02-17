package demo.app.client;

import java.io.Serializable;

import com.extjs.gxt.ui.client.data.BaseModel;

public class EvidenceLog extends BaseModel implements Serializable
{
	
	public EvidenceLog()
	{
		
	}
	
	
	public EvidenceLog(String source, String description, String severity)
	{
		setSource(source);
		setDescription(description);
		setSeverity(severity);
	}
	
	
	public int getId()
	{
		return ((Integer)get("id")).intValue();
	}
	
	
	public void setId(int id)
	{
		set("id", new Integer(id));
	}


	public String getSource()
    {
    	return get("source");
    }


	public void setSource(String source)
    {
    	set("source", source);
    }


	public String getDescription()
    {
    	return get("description");
    }


	public void setDescription(String description)
    {
    	set("description", description);
    }


	public String getSeverity()
    {
    	return get("severity");
    }


	public void setSeverity(String severity)
    {
    	set("severity", severity);
    }


	/**
	 * Returns a summary of this EvidenceLog record.
	 * @return String representation of the EvidenceLog, showing all fields
	 * and values.
	 */
    public String toString()
    {
		StringBuilder strRep = new StringBuilder();
		strRep.append(getProperties());
		
		return strRep.toString();
    }

}
