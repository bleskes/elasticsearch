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

package com.prelert.rs.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.prelert.rs.data.MultiDataPostResult;

/**
 * Simple program to upload a file to the Prelert Engine API.
 * <br>
 * The job should have been created already and the job's data endpoint known
 * as it is the first argument to this program, the second is the file to upload.
 */
public class StreamFile
{
	private StreamFile()
	{
	}

	/**
	 * The program expects 2 arguments a the Url of the job's data endpoint
	 * and the file to upload. The use the additional flag <code>--compressed</code>
	 * if the data file is gzip compresseed and <code>--close</code> if you wish
	 * to close the job once the upload is complete.
	 *
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args)
	throws IOException
	{
		List<String> argsList = Arrays.asList(args);

		if (argsList.size() < 2 || argsList.contains("--help"))
		{
			System.out.println("A filename and job id must be specified: ");
			System.out.println("Usage:");
			System.out.println("\tjava -cp '.:./*' com.prelert.rs.client.StreamFile data_endpoint data_file [--help --compressed --close]");
			System.out.println("Where data_endpoint is the full Url to a Engine API jobs");
			System.out.println("data endpoint e.g. http://localhost:8080:/engine/<version>/data/<job_id>");
			System.out.println("Options:");
			System.out.println("\t--compressed If the source file is gzip compressed");
			System.out.println("\t--close If the job should be closed after the file is uploaded");
			System.out.println("\t--help Show this help");
			return;
		}

		String url = args[0];
		String filename = args[1];
		boolean compressed = argsList.contains("--compressed");
		boolean close = argsList.contains("--close");

		// extract the job id from the url
		if (url.endsWith("/"))
		{
			url = url.substring(0, url.length() -1);
		}
		int lastIndex = url.lastIndexOf("/");
		String jobId = url.substring(lastIndex + 1);

		lastIndex = url.lastIndexOf("/data");
		String baseUrl = url.substring(0, lastIndex);


		FileInputStream fs = new FileInputStream(new File(filename));

		try (EngineApiClient engineApiClient = new EngineApiClient(baseUrl))
		{
			long start = System.currentTimeMillis();

			MultiDataPostResult uploaded = engineApiClient.streamingUpload(jobId, fs, compressed);

			if (close)
			{
				engineApiClient.closeJob(jobId);
			}

			long end = System.currentTimeMillis();

			if (uploaded.anErrorOccurred() == false)
			{
				System.out.println(String.format("%s uploaded in %dms",
						filename, end - start));
			}
		}
	}
}
