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

package com.prelert.proxy.plugin.introscope;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;



public class CLWorkstationCmdFactory
{
	private static Logger s_Logger = Logger.getLogger(CLWorkstationCmdFactory.class);
	
	private static String s_ClwJarFileVersion = null;
	
	/**
	 * Create a new IntroscopeWorkstationConnection for the specific version 
	 * CLWorkstation.jar file.
	 * 
	 * A new instance of class CLWorkstation9Cmd or CLWorkstation8Cmd will
	 * be created for the version string. The class is loaded from the 
	 * class loader to gets round build issues with the different APIs
	 * in CLWorkstation 8 & 9.
	 * 
	 * @param version If the version is unrecognised it defaults to 9.0.
	 * 				  Valid values are: 8.x, 9.x
	 * @return
	 */
	static public IntroscopeConnection newWorkstationConnection(String version)
	{	
		// Default is 9.0
		String className = "com.prelert.proxy.plugin.introscope.clworkstation.CLWorkstation9Cmd";
		if (version.startsWith("8."))
		{
			className = "com.prelert.proxy.plugin.introscope.clworkstation.CLWorkstation8Cmd";
		}
		
		
		Class<?> clwClass;
		try 
		{
			clwClass = CLWorkstationCmdFactory.class.getClassLoader().loadClass(className);
			return (IntroscopeConnection)clwClass.newInstance();
		}
		catch (ClassNotFoundException e) 
		{
			s_Logger.error("newWorkstationConnection " + version, e);	
		 }
		catch (InstantiationException e) 
		{
			s_Logger.error("newWorkstationConnection " + version, e);	
		}
		catch (IllegalAccessException e)
		{
			s_Logger.error("newWorkstationConnection " + version, e);	
		}	
		
		return null;
	}
	
	
	/**
	 * Get the version number of the CLWorkstation.jar file.
	 * 
	 * Expects the jar file to exist on the classpath then reads
	 * the 'com-wily-Release' property from the manifest file and 
	 * returns that value.
	 * 
	 * Once the version has been read it is cached in static variable. 
	 * This should be ok as the jar file shouldn't change once running.
	 * 
	 * Version should be formatted as 4 digits like 8.0.1.0
	 * 
	 * @return The string or <code>null</code> if the jar file cannot
	 *  	   be found or the version can't be read.
	 */
	static public String getCLWjarFileVersion()
	{
		if (s_ClwJarFileVersion != null)
		{
			return s_ClwJarFileVersion;
		}
		
		String filePath = null;
		
		String classpath = System.getProperties().getProperty("java.class.path");
		if (classpath != null)
		{
			String[] jars = classpath.split(File.pathSeparator);
			
			for (String jar : jars)
			{
				if (jar.contains("CLWorkstation.jar"))
				{
					filePath = jar;
					break;
				}
			}
		}
		
		String version = "Unknown";
		
		if (filePath != null)
		{
			try 
			{
				JarFile jarFile = new JarFile(filePath);
				Manifest manifest = jarFile.getManifest();

				Attributes attrs = manifest.getMainAttributes();

				if (attrs.getValue("com-wily-Release") != null)
				{
					version = attrs.getValue("com-wily-Release");
				}

				s_Logger.info("Using version " + version + " of CLWorkstation.jar");
			}
			catch (IOException e) 
			{
				s_Logger.error("Could not read version number of CLWorkstation.jar", e);
			}
		}
		
		
		s_ClwJarFileVersion = version;
		return s_ClwJarFileVersion;
	}

}
