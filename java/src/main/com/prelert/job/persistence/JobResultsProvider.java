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

import java.io.Closeable;
import java.util.List;

import com.prelert.job.normalisation.InitialState;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

public interface JobResultsProvider extends Closeable
{
	/**
	 * Get a page of result buckets for the job id
	 * 
	 * @param jobId
	 * @param expand Include anomaly records
	 * @param skip Skip the first N Buckets. This parameter is for paging
	 * if not required set to 0.
	 * @param take Take only this number of Buckets
	 * @return
	 */
	public Pagination<Bucket> buckets(String jobId, 
			boolean expand, int skip, int take);
			
	
	/**
	 * Get the result buckets for the job id starting with bucket id = 
	 * <code>startBucket</code> up to <code>endBucket</code>. One of either
	 * <code>startBucket</code> or <code>endBucket</code> should be non-zero else
	 * it is more efficient to use {@linkplain #buckets(String, boolean, int, int)}
	 * 
	 * @param jobId
	 * @param expand Include anomaly records
	 * @param skip Skip the first N Buckets. This parameter is for paging
	 * if not required set to 0.
	 * @param take Take only this number of Buckets
	 * @param startBucket The start bucket id. A bucket with this Id will be 
	 * included in the results. If 0 all buckets up to <code>endBucket</code>
	 * are returned
	 * @param endBucket The end bucket id buckets up to but NOT including this
	 * are returned. If 0 all buckets from <code>startBucket</code>
	 * are returned
	 * @return
	 */
	public Pagination<Bucket> buckets(String jobId, 
			boolean expand, int skip, int take,
			long startBucket, long endBucket);
	
	
	/**
	 * Get the bucket by Id from the job. 
	 * 
	 * @param jobId
	 * @param bucketId
	 * @param expand Include anomaly records
	 * @return
	 */
	public SingleDocument<Bucket> bucket(String jobId, 
			String bucketId, boolean expand);
	
	
	/**
	 * Get the anomaly records for the bucket.
	 * The returned records will have the <code>parent</code> member 
	 * set to the parent bucket's id.
	 * 
	 * @param jobId
	 * @param bucketId 
	 * @param includeSimpleCount If true include the simple count records
	 * in the results 
	 * @param skip Skip the first N Jobs. This parameter is for paging
	 * results if not required set to 0.
	 * @param take Take only this number of Jobs
	 * @return
	 */
	public Pagination<AnomalyRecord> records(String jobId, 
			String bucketId, boolean includeSimpleCount, int skip, int take);
	
	/**
	 * Get the anomaly records for all buckets.
	 * The returned records will have the <code>parent</code> member 
	 * set to the parent bucket's id.
	 * 
	 * @param jobId
	 * @param includeSimpleCount If true include the simple count records
	 * in the results 
	 * @param skip Skip the first N records. This parameter is for paging
	 * if not required set to 0.
	 * @param take Take only this number of records
	 * @return
	 */
	public Pagination<AnomalyRecord> records(String jobId, 
			boolean includeSimpleCount, int skip, int take);
	
	/**
	 * Get the anomaly records for all buckets in the given 
	 * date (epoch time) range. The returned records will have the
	 * <code>parent</code> member set to the parent bucket's id.
	 * 
	 * @param jobId
	 * @param includeSimpleCount If true include the simple count records
	 * in the results 
	 * @param skip Skip the first N records. This parameter is for paging
	 * if not required set to 0.
	 * @param take Take only this number of records
	 * @param startBucket The start bucket id. A bucket with this Id will be 
	 * included in the results. If 0 all buckets up to <code>endBucket</code>
	 * are returned
	 * @param endBucket The end bucket id buckets up to but NOT including this
	 * are returned. If 0 all buckets from <code>startBucket</code> are returned
	 * @return
	 */
	public Pagination<AnomalyRecord> records(String jobId, 
			boolean includeSimpleCount, int skip, int take,
			long startBucket, long endBucket);
			
	
	/**
	 * Get the anomaly records in the list of buckets.	  
	 * The returned records will have the
	 * <code>parent</code> member set to the parent bucket's id.
	 * 
	 * @param jobId
	 * @param bucketIds The list of parent buckets
	 * @param includeSimpleCount If true include the simple count records
	 * in the results 
	 * @param skip Skip the first N records. This parameter is for paging
	 * if not required set to 0.
	 * @param take Take only this number of records
	 * @return
	 */
	public Pagination<AnomalyRecord> records(String jobId,
			List<String> bucketIds, boolean includeSimpleCount, int skip, int take);
			
	/**
	 * Return the initial state for normalisation by system change.
	 * This state contains bucket anomaly score and epoch pairs.
	 * @param jobId
	 * @return
	 */
	public InitialState getSystemChangeInitialiser(String jobId);
	
	/**
	 * Get the initial state for unusual behaviour normalisation
	 * 
	 * @param jobId
	 * @return
	 */
	public InitialState getUnusualBehaviourInitialiser(String jobId);
}
