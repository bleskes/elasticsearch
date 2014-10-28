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

import java.rmi.RemoteException;
import java.util.List;

import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFault;

/**
 * Class defines static methods for querying the property values of 
 * vSphere managed objects. 
 */
public class VSphereObjectProperties 
{
	/**
	 * Returns the managed objects property values for the specified 
	 * property names.
	 * 
	 * If <code>propNames</code> is empty or <code>null</code> then 
	 * the object referenced by <code>objectRef</code> is returned. 
	 * 
	 * @param objectRef
	 * @param propNames
	 * @return
	 * @throws InvalidProperty
	 * @throws RuntimeFault
	 * @throws RemoteException
	 */
	static public RetrieveResult queryManagedObjectProperties(
											VSphereConnection connection,
											ManagedObjectReference objectRef,
											String[] propNames) 
	throws InvalidProperty, RuntimeFault, RemoteException
	{
		PropertySpec propSpec = new PropertySpec();
		propSpec.setType(objectRef.getType());
		
		if (propNames == null || propNames.length == 0)
		{
			propSpec.setAll(true);
		}
		else
		{
			propSpec.setPathSet(propNames);
		}

		ObjectSpec objectSpec = new ObjectSpec();
		objectSpec.setObj(objectRef);

		PropertyFilterSpec filterSpec = new PropertyFilterSpec();
		filterSpec.setPropSet(new PropertySpec[] {propSpec});
		filterSpec.setObjectSet(new ObjectSpec[] {objectSpec});

		RetrieveOptions retrieveOptions = new RetrieveOptions();

		RetrieveResult props = connection.getService().retrievePropertiesEx(
										connection.getServiceContent().getPropertyCollector(), 
										new PropertyFilterSpec[] {filterSpec},
										retrieveOptions);

		return props;
	}
	
	/**
	 * Returns the managed objects property values for the specified 
	 * property names. Each object in the list objectRefs will be queried
	 * for the values.
	 * 
	 * If <code>propNames</code> is empty or <code>null</code> then 
	 * the object referenced by <code>objectRef</code> is returned. 
	 * 
	 * @param connection vSphere connection object
	 * @param objectRefs list must contain at least 1 element else an 
	 * 					 <code>IllegalArgumentException</code> is thrown.	
	 * @param propNames
	 * @return
	 * @throws InvalidProperty
	 * @throws RuntimeFault
	 * @throws RemoteException
	 * @throws IllegalArgumentException
	 */
	static public RetrieveResult queryManagedObjectProperties(
												VSphereConnection connection,
												List<ManagedObjectReference> objectRefs,
												String[] propNames) 
	throws InvalidProperty, RuntimeFault, RemoteException
	{
		if (objectRefs.size() < 1)
		{
			throw new IllegalArgumentException("getManagedObjectProperties():" +
									" At least one object ref must be supplied.");
		}
		
		PropertySpec propSpec = new PropertySpec();
		propSpec.setType(objectRefs.get(0).getType());
		
		if (propNames == null || propNames.length == 0)
		{
			propSpec.setAll(true);
		}
		else
		{
			propSpec.setPathSet(propNames);
		}

		ObjectSpec[] objectSpecs = new ObjectSpec[objectRefs.size()];
		for (int i = 0; i < objectSpecs.length; i++)
		{
			ObjectSpec objectSpec = new ObjectSpec();
			objectSpec.setObj(objectRefs.get(i));
			
			objectSpecs[i] = objectSpec;
		}

		PropertyFilterSpec filterSpec = new PropertyFilterSpec();
		filterSpec.setPropSet(new PropertySpec[] {propSpec});
		filterSpec.setObjectSet(objectSpecs);

		RetrieveOptions retrieveOptions = new RetrieveOptions();

		RetrieveResult props = connection.getService().retrievePropertiesEx(
										connection.getServiceContent().getPropertyCollector(), 
										new PropertyFilterSpec[] {filterSpec},
										retrieveOptions);

		return props;
	}
}
