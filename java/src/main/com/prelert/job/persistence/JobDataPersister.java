/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

package com.prelert.job.persistence;

import java.util.List;

/**
 * Persist the records sent the the API.
 * Records are mapped by the by, over, partition and metric fields. 
 */
public interface JobDataPersister 
{
	/**
	 * Find each of the lists of requried fields (by, over, etc)
	 * in the header and save the indexes so the field mappings can
	 * be used in calls to {@linkplain #persistRecord(long, String[])}
	 * @param fields
	 * @param byFields
	 * @param overFields
	 * @param partitionFields
	 * @param header
	 */
	public abstract void setFieldMappings(List<String> fields,
			List<String> byFields, List<String> overFields,
			List<String> partitionFields, String[] header);

	/**
	 * Save the record as per the field mappings 
	 * set up in {@linkplain #setFieldMappings(List, List, List, List, String[])}
	 * 
	 * @param epoch 
	 * @param record
	 */
	public abstract void persistRecord(long epoch, String[] record);
}