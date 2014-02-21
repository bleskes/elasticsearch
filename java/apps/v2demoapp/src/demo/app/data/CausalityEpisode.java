package demo.app.data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import demo.app.client.ClientUtil;
import demo.app.data.gxt.EvidenceModel;

/**
 * Class representing a causality episode - a series of linked evidence constituting
 * a probable cause of an event. It has a probability and a list of one or more 
 * evidence items.
 * @author Pete Harverson.
 */
public class CausalityEpisode implements Serializable
{
	private int m_Id;
	private int m_Probability;
	private List<EvidenceModel> m_EvidenceList;
	
	/** 
	 * Name of column in episode_evidence table holding contiguous value (true/false).
	 */
	public static final String CONTIGUOUS_EVIDENCE_COLUMN = "contiguous";


	/**
	 * Returns the value of the <code>id</code> column for this episode.
	 * @return the <code>id</code> of the episode. An id of 0 indicates that there
	 * is no sequence of evidence associated with this episode.
	 */
	public int getId()
	{
		return m_Id;
	}


	/**
	 * Sets the id for this episode.
	 * @param id the id of the episode.
	 */
	public void setId(int id)
	{
		m_Id = id;
	}


	/**
	 * Returns the probability of this episode.
	 * @return the probability, as an integer in the range 0 to 100.
	 */
	public int getProbability()
	{
		return m_Probability;
	}


	/**
	 * Sets the probability of this episode.
	 * @param probability the probability, as an integer in the range 0 to 100.
	 */
	public void setProbability(int probability)
	{
		m_Probability = probability;
	}


	/**
	 * Returns the list of related evidence that make up this causality episode.
	 * @return the list of evidence as <code>EventRecord</code> objects.
	 */
	public List<EvidenceModel> getEvidenceList()
	{
		return m_EvidenceList;
	}


	/**
	 * Sets the list of related evidence that make up this causality episode.
	 * @param evidenceList the list of evidence as <code>EventRecord</code> objects.
	 */
	public void setEvidenceList(List<EvidenceModel> evidenceList)
	{
		m_EvidenceList = evidenceList;
	}
	
	
	/**
	 * Returns the start time of the episode, corresponding to the time of the
	 * first item of evidence in the episode.
	 * @return the start time of the episode.
	 */
	public Date getStartTime()
	{
		Date startTime = null;
		
		if (m_EvidenceList != null && m_EvidenceList.size() > 0)
		{
			startTime = ClientUtil.parseTimeField(
					m_EvidenceList.get(m_EvidenceList.size() - 1), TimeFrame.SECOND);
		}
		
		
		return startTime;
	}

	
	/**
	 * Returns the end time of the episode, corresponding to the time of the
	 * last item of evidence in the episode.
	 * @return the end time of the episode.
	 */
	public Date getEndTime()
	{
		Date endTime = null;
		
		if (m_EvidenceList != null && m_EvidenceList.size() > 0)
		{
			endTime = ClientUtil.parseTimeField(
					m_EvidenceList.get(0), TimeFrame.SECOND);
		}
		
		return endTime;
	}
	
	
	/**
	 * Returns whether an item of evidence from a Causality Episode is
	 * contiguous i.e. if it falls in a sequence of similar evidence items.
	 * @param evidence item of evidence to check whether it is contiguous.
	 * @return <code>true</code> if it is contiguous, <code>false</code> otherwise.
	 */
	public static boolean isContiguousEvidence(EvidenceModel evidence)
	{
		boolean isContiguous = false;
		try
		{
			isContiguous = (Boolean)(evidence.get(CONTIGUOUS_EVIDENCE_COLUMN));
		}
		catch (Exception e)
		{
			isContiguous = false;
		}
		
		return isContiguous;
	}
	

	/**
	 * Returns a summary of this causality episode.
	 * 
	 * @return String representation of the episode, showing all fields and values.
	 */
	public String toString()
	{
		StringBuilder strRep = new StringBuilder('{');
		
		strRep.append("id=");
		strRep.append(m_Id);
		strRep.append(",probability=");
		strRep.append(m_Probability);
		strRep.append(",evidence=");
		strRep.append(m_EvidenceList);
		strRep.append('}');

		return strRep.toString();
	}

}
