package demo.app.data;

import java.io.Serializable;
import java.util.HashMap;

/**
 * View subclass for a History View. It defines configuration properties such
 * as the properties of the evidence views that are displayed for each time frame.
 * @author Pete Harverson
 */
public class HistoryView extends View implements Serializable
{
	private HashMap<String, EvidenceView>		m_EvidenceViews;
	
	
	/**
	 * Creates a new History View.
	 */
	public HistoryView()
	{
		m_EvidenceViews = new HashMap<String, EvidenceView>();
	}
	
	
	/**
	 * Returns the evidence view that should be displayed for the specified time frame.
	 * @param timeFrame time frame for which to obtain the Evidence View
	 * e.g. DAY, HOUR, MINUTE or SECOND.
	 * @return the Evidence View for the specified time frame.
	 */
	public EvidenceView getEvidenceView(TimeFrame timeFrame)
	{
		return m_EvidenceViews.get(timeFrame.toString());
	}
	
	
	/**
	 * Sets the map of evidence views (keyed on the time frame as a String).
	 * @param evidenceViews the evidence views for this History View.
	 */
	public void setEvidenceViews(HashMap<String, EvidenceView> evidenceViews)
	{
		m_EvidenceViews = evidenceViews;
	}
	
	
	/**
	 * Returns the map of evidence views (keyed on the time frame as a String).
	 * @return the evidence views for this History View (keyed on the time 
	 * 	frame as a String).
	 */
	public HashMap<String, EvidenceView> getEvidenceViews()
	{
		return m_EvidenceViews;
	}
	
	
	/**
	 * Adds an Evidence View to this History View which is added to the internal
	 * map of evidence views (keyed on the time frame as a String).
	 * @param view Evidence View to add.
	 */
	public void addEvidenceView(EvidenceView view)
	{
		m_EvidenceViews.put(view.getTimeFrame().toString(), view);
	}
	

	/**
	 * Creates a new History View based on the properties of this view.
	 * @param filterAttribute
	 * @param filterValue
	 * @return a new History View which has the same properties as this view.
	 */
	public View createCopyAndAppendFilter(String filterAttribute,
	        String filterValue)
	{
		HistoryView newView = new HistoryView();
		
		newView.setName(new String(getName()));
		newView.setDataType(getDataType());
		newView.setStyleId(new String(getStyleId()));
		newView.setEvidenceViews(m_EvidenceViews);
		
		return newView;
	}

	
	/**
	 * Returns a String summarising the properties of this View.
	 * @return a String displaying the properties of the View.
	 */
    public String toString()
    {
	   StringBuilder strRep = new StringBuilder("{");
	   
	   strRep.append("HistoryView Name=");
	   strRep.append(getName());
	   
	   strRep.append(",Data Type=");
	   strRep.append(getDataType());
	   
	   strRep.append(",Style=");
	   strRep.append(getStyleId());
	   
	   strRep.append(",Time Frames={");
	   strRep.append(m_EvidenceViews.keySet());
	   strRep.append('}');
	   
	   strRep.append('}');
	   
	   return strRep.toString();
    }
}
