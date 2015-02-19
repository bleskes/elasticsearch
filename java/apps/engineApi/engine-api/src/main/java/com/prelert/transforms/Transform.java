/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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
 ************************************************************/

package com.prelert.transforms;

public abstract class Transform
{
	protected final int [] m_InputIndicies;
	protected final int [] m_OutputIndicies;

	/**
	 *
	 * @param inputIndicies Input indexes into the <code>record</code>
	 * @param outputIndicies Transform results go into these indexes
	 */
	public Transform(int [] inputIndicies, int [] outputIndicies)
	{
		m_InputIndicies = inputIndicies;
		m_OutputIndicies = outputIndicies;
	}

	/**
	 * Return a copy of the array.
	 * This function is only really here for testing purposes
	 * @return
	 */
	public int [] inputIndicies()
	{
		int [] tmp = new int[m_InputIndicies.length];
		System.arraycopy(m_InputIndicies, 0, tmp, 0, tmp.length);
		return tmp;
	}

	/**
	 * Return a copy of the array.
	 * This function is only really here for testing purposes
	 * @return
	 */
	public int [] outputIndicies()
	{
		int [] tmp = new int[m_OutputIndicies.length];
		System.arraycopy(m_OutputIndicies, 0, tmp, 0, tmp.length);
		return tmp;
	}

	/**
	 * Transform function
	 *
	 * @param inputRecord
	 * @param outputRecord
	 * @return
	 * @throws TransformException
	 */
	public abstract boolean transform(String [] inputRecord,
									String [] outputRecord)
				throws TransformException;
}
