/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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
 ***********************************************************/

package com.prelert.proxy.plugin.vsphere;

/**
 * Class for defining which DataCenters and Virtual Machines
 * should be queried for performance data. 
 */
public class VSphereResourceSelection 
{
	private final String m_DataCenterRegex;
	private final String m_HostSystemRegex;
	private final String m_VmRegex;
	
	public VSphereResourceSelection(String dataCenterRegex, String hostSystemRegex,
										String vmRegex)
	{
		m_DataCenterRegex = dataCenterRegex;
		m_HostSystemRegex = hostSystemRegex;
		m_VmRegex = vmRegex;		
	}
	
	public VSphereResourceSelection()
	{
		m_DataCenterRegex = ".*";
		m_HostSystemRegex = ".*";
		m_VmRegex = ".*";		
	}
	
	public String getDataCenterRegEx()
	{
		return m_DataCenterRegex;
	}	
	
	public String getHostSystemRegex()
	{
		return m_HostSystemRegex;
	}
	
	public String getVmRegex()
	{
		return m_VmRegex;
	}
}



