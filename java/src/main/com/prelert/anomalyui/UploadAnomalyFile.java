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

package com.prelert.anomalyui;

import org.springframework.web.multipart.commons.CommonsMultipartFile;


/**
 * Class encapsulating an uploaded file of anomaly data, held as a Spring
 * <code>CommonsMultipartFile</code> implementation of the Apache Commons FileUpload.
 * @author Pete Harverson
 */
public class UploadAnomalyFile
{

	private String m_Name;
	private CommonsMultipartFile m_FileData;


	/**
	 * Returns the file name of the uploaded file.
	 * @return the uploaded file name.
	 */
	public String getName()
	{
		return m_Name;
	}


	/**
	 * Sets the file name of the uploaded file. 
	 * @param name the uploaded file name.
	 */
	public void setName(String name)
	{
		m_Name = name;
	}


	/**
	 * Returns the uploaded file as a Spring <code>CommonsMultipartFile</code> object.
	 * @return the uploaded file data.
	 */
	public CommonsMultipartFile getFileData()
	{
		return m_FileData;
	}


	/**
	 * Sets the uploaded file as a Spring <code>CommonsMultipartFile</code> object. 
	 * @param fileData the uploaded file data.
	 */
	public void setFileData(CommonsMultipartFile fileData)
	{
		m_FileData = fileData;
	}

}
