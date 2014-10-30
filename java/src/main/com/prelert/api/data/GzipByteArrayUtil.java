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

package com.prelert.api.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;


/**
 * Gzip compress/uncompress utilities. 
 */
public class GzipByteArrayUtil 
{
	private static final Logger s_Logger = Logger.getLogger(GzipByteArrayUtil.class);
	
	/**
	 * GZip compress the input byte array.
	 * 
	 * @param data
	 * @return
	 */
	public static byte[] compress(byte[] data)
	{
		ByteArrayOutputStream oBuf = new ByteArrayOutputStream();
		GZIPOutputStream gBuf;
		try 
		{
			gBuf = new GZIPOutputStream(oBuf);
			gBuf.write(data);
			gBuf.close();
			oBuf.close();
			
			return oBuf.toByteArray();
		}
		catch (IOException e) 
		{
			s_Logger.error("gzip compression failed!");
		}

		return null;
	}

	
	/**
	 * Uncompress the Gzipped input byte array.
	 * 
	 * @param cData
	 * @return
	 */
	public static byte[] uncompress(byte[] cData) 
	{
		try 
		{
			byte[] buffer = new byte[5120];
			ByteArrayOutputStream oBuf = new ByteArrayOutputStream();
			ByteArrayInputStream iBuf = new ByteArrayInputStream(cData);
			
			GZIPInputStream gIn = new GZIPInputStream(iBuf);
			for (int len = gIn.read(buffer); len != -1; len = gIn.read(buffer)) 
			{
				oBuf.write(buffer, 0, len);
			}
			
			gIn.close();
			iBuf.close();
			oBuf.close();
			
			return oBuf.toByteArray();
		} 
		catch (Exception e) 
		{
			s_Logger.error("gzip decompress failed!");
		}
		return null;
	}
}
