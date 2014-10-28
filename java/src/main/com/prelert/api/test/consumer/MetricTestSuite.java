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

package com.prelert.api.test.consumer;

/**
 * The test suite invokes the Prelert Metric API integration tests.
 * All these tests modify the database and should be run with caution, only run 
 * against a temporary database where you do not wish to keep the contents. 
 * 
 * <ul>
 * 	<li>{@link MetricConfigTest}</li>  
 * 	<li>{@link MetricUploadTest}</li>  
 * 	<li>{@link ConcurrentUploadTest}</li>  
 * </ul>
 * 
 * <p/>
 * This test requires the <a href=https://code.google.com/p/odata4j/>Odata4J</a> and 
 * <a href=https://code.google.com/p/json-simple/>Json-Simple</a> libraries (Apache Licence 2.0).
 */
public class MetricTestSuite 
{

    /**
     * Main entry point, runs the metric upload, concurrent metric upload 
     * and metric configuration tests.
     * 
     * @param args 1 optional argument is expected which is the service URI.
     * If not set the default <code>http://localhost:8080/prelertApi/prelert.svc</code> is used.
     */
	public static void main(String[] args) 
	{
        String serviceUri = "http://localhost:8080/prelertApi/prelert.svc";
        if (args.length > 0)
        {
            serviceUri = args[0];
        }
        
        boolean testsSuccessful = true;
        
		MetricUploadTest uploadTest = new MetricUploadTest();
		testsSuccessful = uploadTest.runTests(serviceUri);    
		
		System.out.println();

		MetricConfigTest configTest = new MetricConfigTest();
		testsSuccessful = configTest.runTests(serviceUri) && testsSuccessful;
		
		System.out.println();

		ConcurrentUploadTest concurrentTest = new ConcurrentUploadTest();
		testsSuccessful = concurrentTest.runTests(serviceUri) && testsSuccessful;
		
		System.out.println();

		if (testsSuccessful)
        {
        	System.out.println("TESTS SUCCESSFUL");
        }
        else
        {
        	System.out.println("TESTS FAILED");
        }
	}

}
