/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package demo.app.client.list;

import java.util.Comparator;

import com.extjs.gxt.ui.client.store.Store;
import com.extjs.gxt.ui.client.store.StoreSorter;

import demo.app.data.gxt.ProbableCauseModel;


/**
 * Custom sorter for a grouping store of {@link ProbableCauseModel} objects
 * which checks if the sort field is 'significance', to ensure the incoming
 * item of evidence is placed first.
 */
public class ProbableCauseStoreSorter extends StoreSorter<ProbableCauseModel>
{
	private Comparator<Object> 			m_Comparator;
	
	
	/**
	 * Creates a new probable cause store sorter.
	 */
	public ProbableCauseStoreSorter()
	{
		m_Comparator = StoreSorter.DEFAULT_COMPARATOR;
	}
	
	
	/**
	 * Compares two ProbableCauseModel objects by the specified property.
	 */
    public int compare(Store<ProbableCauseModel> store,
            ProbableCauseModel cause1, ProbableCauseModel cause2, String property)
    {	
    	if (property != null)
    	{
    		if (property.equals("significance") == true)
    		{
        		return compareBySignificance(cause1, cause2);
    			
    		}
    		else
    		{
    			Object prop1 = cause1.get(property);
        		Object prop2 = cause2.get(property);
        		
        		return StoreSorter.DEFAULT_COMPARATOR.compare(prop1, prop2);
    		}
    	}
    	else
    	{
    		return m_Comparator.compare(cause1, cause2);
    	}
    }
    
    
    /**
     * Compares two ProbableCauseModel objects by the value of their significance
     * property.
     */
    public int compareBySignificance(ProbableCauseModel cause1, ProbableCauseModel cause2)
    {
    	// A significance value of -1 is attached to the incoming
		// item of evidence. Place this first.
		int significance1 = cause1.getSignificance();
		int significance2 = cause2.getSignificance();
		
		if (significance1 == -1)
		{
			return 1;
		}
		else if (significance2 == -1)
		{
			return -1;
		}
		else
		{
			return StoreSorter.DEFAULT_COMPARATOR.compare(significance1, significance2);
		}
    }
}
