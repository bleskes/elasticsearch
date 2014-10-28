/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2011     *
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

package com.prelert.dev.config;

import java.io.IOException;


/**
 * Interface that defines the methods to be implemented by an object which 
 * provides access to configuration properties. A properties file is one which contains 
 * a list of key / value pairs in a simple line-oriented format, with the key 
 * and value separated by  <code>'='</code>, <code>':'</code> or any white space 
 * character. Examples:
 * <pre>
 *  key1 = value1
 *  key2 : value2
 *  key3   value3</pre>
 *
 * @author Pete Harverson
 */
public interface ConfigurationProvider
{
	/**
	 * Returns the String value associated with the given key.
	 * @param key the property key.
	 * @return the associated value, as a <code>String</code>, or <code>null</code>
	 * 	if there is no entry for the specified key.
	 */
	public String getString(String key);
	
	
	/**
	 * Sets a property, replacing any previously set value. If the property did not
	 * exist beforehand, a new property with the specified key is added.
	 * @param key the key of the property to change.
	 * @param value the new value.
	 */
	public void setProperty(String key, Object value);
	
	
	/**
	 * Saves the configuration back to the same source file.
	 * @throws IOException if an error occurs during the save operation.
	 */
	public void save() throws IOException;
	
	
	/**
	 * Saves the properties to the specified file. This does not change the source 
	 * of the properties for this provider. 
	 * @param fileName name of the file to save the properties to.
	 * @throws IOException if an error occurs during the save operation.
	 */
	public void save(String fileName) throws IOException;
}
