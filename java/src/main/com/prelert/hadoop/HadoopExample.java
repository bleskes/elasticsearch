/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.hadoop;

import org.apache.log4j.Logger;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.mapred.JobConf;

public class HadoopExample 
{
	private static Logger s_Logger = Logger.getLogger(HadoopExample.class);

	public static void main(String[] args) 
	{
		s_Logger.info("Hadoop example main");

        System.out.println("Hadoop example system");
		
		JobConf conf = new JobConf();
		DistributedCache.createSymlink(conf); 
//		DistributedCache.addCacheFile("hdfs://host:port/user/dkyle/prelert/bi/libPreApi.dylib#libPreApi.dylib", conf);
//		DistributedCache.addCacheFile("hdfs://host:port/user/dkyle/prelert/bi/libPreApi.dylib#libPreApi.dylib", conf);
//		DistributedCache.addCacheFile("hdfs://host:port/user/dkyle/prelert/bi/libPreApi.dylib#libPreApi.dylib", conf);


	}

}
