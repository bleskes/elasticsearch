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

import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.TraversalSpec;

/**
 * Class declares some useful <code>TraversalSpec</code>s for traversing 
 * the VCentre hierarchy.
 */
public class VSphereTraversalSpecs 
{
	/**
	 * Traverse from a Data Centre node to a VM Folder node. 
	 */
	public static final TraversalSpec DATACENTRE_TO_VMFOLDER = new TraversalSpec(
										null, null,
										"datacenterToVmFolder",
										"Datacenter",
										"vmFolder",
										false,
										new SelectionSpec[] {
												new SelectionSpec(null, null, "folderToChild") 
										});
	
	/**
	 * Traverse from a Data Centre node to a Host Folder node. 
	 */
	public static final TraversalSpec DATACENTRE_TO_HOSTFOLDER = new TraversalSpec(
			null, null,
			"datacenterToHostFolder",
			"Datacenter",
			"hostFolder",
			false,
			new SelectionSpec[] {
					new SelectionSpec(null, null, "folderToChild") 
			});
	
	/**
	 * Traverse from a Compute Resource node to a Host node. 
	 */
	public static final TraversalSpec COMPUTERESOURCE_TO_HOST = new TraversalSpec(
			null, null,
			"computeResourceToHost",
			"ComputeResource",
			"host",
			false,
			new SelectionSpec[] {
					new SelectionSpec(null, null, "folderToChild") 
			});
	
	/**
	 * Traverse from a Host System node to a Virtual Machine node. 
	 */
	public static final TraversalSpec HOSTSYSTEM_TO_VM = new TraversalSpec(
			null, null,
			"hostSystemToVM",
			"HostSystem",
			"vm",
			false,
			new SelectionSpec[] {
					new SelectionSpec(null, null, "folderToChild") 
			});
	

	/**
	 * Recursive <code>TraversalSpec</code>.
	 */
	public static final TraversalSpec FOLDER_TO_CHILD = new TraversalSpec(
										null, null, "folderToChild",
										"Folder", 
										"childEntity", 
										Boolean.FALSE, 
										new SelectionSpec[] {
												new SelectionSpec(null, null, "folderToChild"),
												new SelectionSpec(null, null, "datacenterToHostFolder"),
												new SelectionSpec(null, null, "computeResourceToHost"),
												new SelectionSpec(null, null, "hostSystemToVM")
										});
}
