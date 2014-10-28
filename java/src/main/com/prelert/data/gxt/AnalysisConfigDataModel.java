/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ***********************************************************/

package com.prelert.data.gxt;

import java.util.Date;

import com.extjs.gxt.ui.client.data.BaseModelData;


/**
 * Extension of the GXT BaseModelData class for the properties of the data to be
 * analysed by Prelert.
 * @author Pete Harverson
 */
public class AnalysisConfigDataModel extends BaseModelData
{
    private static final long serialVersionUID = 6633430196402403346L;
    

    /**
     * Returns the start date of the valid data collection period.
     * @return the earliest date of data that is available for analysis, 
     * 	or January 1, 1970, 00:00:00 GMT if no start date has been set.
     */
     public Date getValidDataStartTime()
     {
     	return get("dataStartTime", new Date(0));
     }
     
     
     /**
      * Sets the start date of the valid data collection period.
      * @param startTime the earliest date of data that is available for analysis.
      */
     public void setValidDataStartTime(Date startTime)
     {
     	set("dataStartTime", startTime);
     }
     
     
     /**
      * Returns the end date of the valid data collection period.
      * @return the latest date of data that is available for analysis, 
      * 	or the current time if no end date has been set.
      */
      public Date getValidDataEndTime()
      {
      	return get("dataEndTime", new Date());
      }
      
      
      /**
       * Sets the end date of the valid data collection period.
       * @param endTime the latest date of data that is available for analysis.
       */
      public void setValidDataEndTime(Date endTime)
      {
      	set("dataEndTime", endTime);
      }
      
      
  	/**
  	 * Returns the time of data that has been set for analysis.
  	 * @return the time being analysed.
  	 */
  	public Date getTimeToAnalyze()
  	{
  		return get("analysisTime");
  	}
  	
  	
  	/**
  	 * Sets the time of data to analyse.
  	 * @param time the date/time to analyse.
  	 */
  	public void setTimeToAnalyze(Date time)
  	{
  		set("analysisTime", time);
  	}
 	
 	
 	/**
 	 * Returns the length of queries used for the analysis.
 	 * @return query length
 	 */
 	public int getQueryLength()
 	{
 		return get("queryLength", -1);
 	}
 	
      
 	/**
 	 * Sets the length of queries used for the analysis.
 	 * @param length - query length
 	 */
 	public void setQueryLength(int length)
 	{
 		set("queryLength", length);
 	}

 	
 	/**
 	 * Return the interval at which the queries 
 	 * should ask for data points. This should be 
 	 * a multiple of 15.
 	 * @return The point interval or -1 if not set.
 	 */
 	public int getDataPointInterval()
 	{
 		return get("dataPointInterval", -1);
 	}
 	
 	/**
 	 * Set the interval at which the queries 
 	 * should ask for data points. <code>interval</code> 
 	 * should be a multiple of 15.
 	 */
 	public void setDataPointInterval(int interval)
 	{
 		set("dataPointInterval", interval);
 	}
 	
 	
      /**
  	 * Returns a summary of the properties of the Introscope data that is being analysed.
  	 * @return <code>String</code> representation of the data analysis properties.
  	 */
  	public String toString()
  	{
  		return getProperties().toString();
  	}
    
}
