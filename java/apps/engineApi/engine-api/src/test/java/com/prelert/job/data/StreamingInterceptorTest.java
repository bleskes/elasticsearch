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

package com.prelert.job.data;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import org.junit.Assert;
import org.junit.Test;

import com.prelert.job.data.StreamingInterceptor;

public class StreamingInterceptorTest 
{
	public static final String LOG_CONTENTS = 
		"2014-02-03 16:47:24,665 GMT DEBUG [3506] CLogger.cc@385 Logger re-initialised using properties file /Source/prelert_home/config/log4cxx.properties\n"
		+ "2014-02-03 16:47:24,666 GMT DEBUG [3506] CLogger.cc@389 uname -a : Darwin DKyles-MacBook-Pro 12.5.0 Darwin Kernel Version 12.5.0: Sun Sep 29 13:33:47 PDT 2013; root:xnu-2050.48.12~1/RELEASE_X86_64 x86_64\n"
		+ "2014-02-03 16:47:24,666 GMT INFO  [3506] Main.cc@127 prelert_autodetect_api (64 bit): Version based on 4.2.1 (Build DEVELOPMENT BUILD by dkyle) Copyright (c) Prelert Ltd 2006-2014\n"
		+ "2014-02-03 16:47:24,666 GMT DEBUG [3506] CLicense.cc@1133 License expires at 2014-07-08T13:08:10+0100 : 1404821290|any|prelert|68a6bcad50afee56d91f160a485b2636f96f83bd\n"
		+ "2014-02-03 16:47:24,667 GMT DEBUG [3506] CFieldConfig.cc@415 responsetime\n"
		+ "2014-02-03 16:47:24,667 GMT DEBUG [3506] CFieldConfig.cc@415 by\n"
		+ "2014-02-03 16:47:24,667 GMT DEBUG [3506] CFieldConfig.cc@415 airline\n"
		+ "2014-02-03 16:47:24,671 GMT DEBUG [3506] CCsvInputParser.cc@410 Parse field names\n"
		+ "2014-02-03 16:47:24,671 GMT INFO  [3506] CCsvInputParser.cc@155 Using time in field '_time'\n"
		+ "2014-02-03 16:47:24,671 GMT DEBUG [3506] CAnomalyDetector.cc@752 Creating new detector for key individual count//count///\n"
		+ "2014-02-03 16:47:24,672 GMT DEBUG [3506] CAnomalyDetector.cc@753 Detector count 1\n"
		+ "2014-02-03 16:47:24,672 GMT DEBUG [3506] CFirstTimeStore.cc@67 Set first time for partition / to 1350824400\n"
		+ "2014-02-03 16:47:24,672 GMT DEBUG [3506] CAnomalyDetector.cc@752 Creating new detector for key individual metric/responsetime/airline///\n"
		+ "2014-02-03 16:47:24,672 GMT DEBUG [3506] CAnomalyDetector.cc@753 Detector count 2\n"
		+ "2014-02-03 16:47:24,944 GMT DEBUG [3506] CMetricDataGatherer.cc@645 Setting sample count to 3086 for person DJA\n"
		+ "2014-02-03 16:47:24,944 GMT DEBUG [3506] CMetricDataGatherer.cc@645 Setting sample count to 3086 for person JQA\n"
		+ "2014-02-03 16:47:24,944 GMT DEBUG [3506] CMetricDataGatherer.cc@645 Setting sample count to 3105 for person GAL\n"
		+ "2014-02-03 16:47:25,773 GMT INFO  [3506] CCmdSkeleton.cc@215 Handled 175909 records\n"
		+ "2014-02-03 16:47:25,775 GMT DEBUG [3506] CAnomalyDetector.cc@684 Persisted state for key 'individual count//count///'\n"
		+ "2014-02-03 16:47:25,784 GMT DEBUG [3506] CAnomalyDetector.cc@684 Persisted state for key 'individual metric/responsetime/airline///'\n";
	
	@Test
	public void streamingTest() 
	throws IOException, InterruptedException
	{
		Path tmpFile = Files.createTempFile("tmp", ".gz");
		
		final StreamingInterceptor si = new StreamingInterceptor(tmpFile);
		InputStream input = si.createStream();
		
		final ByteArrayInputStream bais = new ByteArrayInputStream(LOG_CONTENTS.getBytes(Charset.forName("UTF-8")));
		
		// start the pump
		Thread th = new Thread() {
			@Override
			public void run()
			{
				si.pump(bais);
			}
		};
		
		th.start();

		
		try (BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(input)))
		{
			StringBuilder sb = new StringBuilder();
			String line = bufferedReader.readLine();
			while(line != null)
			{
				sb.append(line).append('\n');
				line = bufferedReader.readLine();
			}
			
			Assert.assertEquals(LOG_CONTENTS, sb.toString());
		}
		
        
		// now test file contents are the same
		try (InputStream fs = new GZIPInputStream(new FileInputStream(tmpFile.toString()));
			BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(fs)))
		{
			StringBuilder sb = new StringBuilder();
			String line = bufferedReader.readLine();
			while(line != null)
			{
				sb.append(line).append('\n');
				line = bufferedReader.readLine();
			}	

			Assert.assertEquals(LOG_CONTENTS, sb.toString());
		}

		// If the test has passed then clean up
		Files.delete(tmpFile);
	}

}
