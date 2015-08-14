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

package com.prelert.job.persistence;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Persist the records sent the the API.
 * Only the analysis fields are written. Records are mapped by the
 * by, over, partition and metric fields.
 *
 * Concrete classes need to implement the {@link #persistRecord(long, String[])},
 * {@linkplain #deleteData()} and {@linkplain #flushRecords()} methods.
 */
public abstract class JobDataPersister
{
    public static final String FIELDS = "fields";
    public static final String BY_FIELDS = "byFields";
    public static final String OVER_FIELDS = "overFields";
    public static final String PARTITION_FIELDS = "partitionFields";


    protected String [] m_FieldNames;
    protected int [] m_FieldMappings;
    protected int [] m_ByFieldMappings;
    protected int [] m_OverFieldMappings;
    protected int [] m_PartitionFieldMappings;



	/**
	 * Find each of the lists of requried fields (by, over, etc)
	 * in <code>fieldMap</code> and save the indexes so the field mappings can
	 * be used in calls to {@linkplain #persistRecord(long, String[])}
	 *
	 * @param fields
	 * @param byFields
	 * @param overFields
	 * @param partitionFields
	 * @param fieldMap Field -> index map for the record passed in
	 * {@link #persistRecord())}
	 */
	public void setFieldMappings(List<String> fields,
			List<String> byFields, List<String> overFields,
			List<String> partitionFields, Map<String, Integer> fieldMap)
    {
        m_FieldNames = new String [fields.size()];
        m_FieldNames = fields.<String>toArray(m_FieldNames);
        m_FieldMappings = new int [fields.size()];
        m_ByFieldMappings = new int [byFields.size()];
        m_OverFieldMappings = new int [overFields.size()];
        m_PartitionFieldMappings = new int [partitionFields.size()];

        List<List<String>> allFieldTypes = Arrays.asList(fields, byFields,
                overFields, partitionFields);

        int [][] allFieldMappings = new int [][] {m_FieldMappings, m_ByFieldMappings,
                m_OverFieldMappings, m_PartitionFieldMappings};

        int i = 0;
        for (List<String> fieldType : allFieldTypes)
        {
            int j = 0;
            for (String f : fieldType)
            {
                Integer index = fieldMap.get(f);
                if (index != null)
                {
                    allFieldMappings[i][j] = index;
                }
                else
                {
                    // not found in header - so resize and delete from the array
                    int [] tmp = new int [allFieldMappings[i].length -1];
                    System.arraycopy(allFieldMappings[i], 0, tmp, 0, j);
                    System.arraycopy(allFieldMappings[i], j+1, tmp, j, tmp.length - j);

                    //allFieldMappings[i] = tmp;
                    switch (i)
                    {
                        case 0:
                            m_FieldMappings = tmp;
                            break;
                        case 1:
                            m_ByFieldMappings = tmp;
                            break;
                        case 2:
                            m_OverFieldMappings = tmp;
                            break;
                        case 3:
                            m_PartitionFieldMappings = tmp;
                            break;
                        default:
                            break;
                    }
                }

                j++;
            }
            i++;
        }
    }

	/**
	 * Save the record as per the field mappings
	 * set up in {@linkplain #setFieldMappings(List, List, List, List, String[])}
	 * setFieldMappings must have been called so this class knows where to
	 *
	 *
	 * @param epoch
	 * @param record
	 */
	public abstract void persistRecord(long epoch, String[] record);

	/**
	 * Delete all the persisted records
	 *
	 * @return
	 */
	public abstract boolean deleteData();

	/**
	 * Flush any records that may not have been persisted yet
	 */
	public abstract void flushRecords();
}