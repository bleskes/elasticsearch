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

package com.prelert.proxy.configuration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.encryption.PasswordEncryption;


/**
 * Class manages the configuration objects. 
 *
 * This class manages template and full configs which should be stored in 
 * separate directories and are loaded using the {@link #loadConfigurations(File)}
 * and {@link #loadTemplateConfigurations(File)} methods.
 * 
 * All functions that return template DataTypeConfig objects return copies
 * so the originals cannot be copied.
 * 
 * The {@link #saveConfigurations()} method only saves the fully configured 
 * data types, use {@link #addDataTypeConfig(DataTypeConfig)} to add a new
 * fully configured datatype.
 * 
 * The configurations are saved in a file with the name of the datatype and 
 * the .xml extension e.g. 'sampledatatype.xml'.
 */
public class ConfigurationManager 
{
	static private final String DEFALULT_FILE_EXTENSION = ".xml";
	
	static private Logger s_Logger = Logger.getLogger(ConfigurationManager.class);
	
	private Map<String, DataTypeConfig> m_FullConfigsByType;
	private Map<String, DataTypeConfig> m_TemplateConfigsByType;
	
	private File m_ConfigDir;
	private File m_TemplateConfigDir;

	
	public ConfigurationManager()
	{
		m_FullConfigsByType = new HashMap<String, DataTypeConfig>();
		m_TemplateConfigsByType = new HashMap<String, DataTypeConfig>();
	}

	
	/**
	 * Return a list of all the stored data type names.
	 * @return
	 */
	public List<String> getDataTypeNames()
	{
		return Arrays.asList(m_FullConfigsByType.keySet().toArray(new String[0]));
	}
	
	/**
	 * Returns the configuration object for the <code>datatype</code>
	 * or <code>null</code> if there is no associated config.
	 * 
	 * @param datatype
	 * @return DataTypeConfig or <code>null</code>
	 */
	public DataTypeConfig getDataTypeConfig(String datatype)
	{
		return m_FullConfigsByType.get(datatype);
	}
	
	/**
	 * Return a list of all the stored data type configurations.
	 * @return
	 */
	public List<DataTypeConfig> getDataTypeConfigs()
	{
		return new ArrayList<DataTypeConfig>(m_FullConfigsByType.values());
	}
	

	/**
	 * Return a list of all the template data type names.
	 * @return
	 */
	public List<String> getTemplateDataTypeNames()
	{
		return Arrays.asList(m_TemplateConfigsByType.keySet().toArray(new String[0]));
	}
	
	/**
	 * Returns a copy of the template config object for the <code>datatype</code>
	 * or <code>null</code> if there is no associated config. A copy is returned
	 * so the original cannot be modified.
	 * 
	 * @param datatype
	 * @return DataTypeConfig or <code>null</code>
	 */
	public DataTypeConfig getTemplateDataTypeConfig(String datatype)
	{
		DataTypeConfig conf = m_TemplateConfigsByType.get(datatype);
		if (conf != null)
		{
			conf = new DataTypeConfig(conf);
		}
		
		return conf;
	}
	
	/**
	 * Return a list of copies of all the stored template configurations.
	 * Copies are returned so the originals cannot be modified.
	 * 
	 * @return
	 */
	public List<DataTypeConfig> getTemplateDataTypeConfigs()
	{
		List<DataTypeConfig> confs = new ArrayList<DataTypeConfig>();
		for (DataTypeConfig value : m_TemplateConfigsByType.values())
		{
			confs.add(new DataTypeConfig(value));
		}
		
		return confs;
	}
	
	
	/**
	 * Adds the config to the set of configured data type configurations.
	 * No validation is performed on the config and any previous config
	 * with the same datatype will be overwritten.
	 * 
	 * @param value The new config to add.
	 * @return true always
	 */
	public boolean addDataTypeConfig(DataTypeConfig value)
	{
		if (m_FullConfigsByType.containsKey(value.getDataType()))
		{
			s_Logger.warn("The configuration manager already has a configuration for type "
						+ value.getDataType() +
						". Overwriting the existing config. ");
		}
		
		 m_FullConfigsByType.put(value.getDataType(), value);
		 
		 return true;
	}
	
	
	/**
	 * Remove the datatype config from this object and deletes 
	 * the config Xml file if it exists.
	 * 
	 * @param datatype - Type to remove
	 * @return True if the datatype is present and the file is deleted.
	 */
	public boolean removeDataTypeConfig(String datatype)
	{
		if (m_FullConfigsByType.containsKey(datatype) == false)
		{
			s_Logger.error("No data type config of type " + datatype + " to remove");
			return false;
		}
		
		m_FullConfigsByType.remove(datatype);
		
		
		if (m_ConfigDir == null)
		{
			s_Logger.error("Cannot delete data type config file as config directory is not set.");
			return false;
		}
		
		
		// delete the config file.
		File configFile = new File(m_ConfigDir, datatype + DEFALULT_FILE_EXTENSION);
		if (configFile.exists())
		{
			return configFile.delete();
		}
		
		return false;
	}
	
	
	/**
	 * Lists all the Xml files from <code>configDir</code> and tries to load
	 * instances of DataSourceConfig from the Xml. 
	 * 
	 * @param configDirectory The directory the Data Source Configuration files
	 * are kept.
	 * @return True is any configs were successfully loaded.
	 */
	public boolean loadConfigurations(File configDirectory)
	{
		m_ConfigDir = configDirectory;
		return loadConfigurations(configDirectory, m_FullConfigsByType);
	}
	
	public boolean loadTemplateConfigurations(File configDirectory)
	{
		m_TemplateConfigDir = configDirectory;
		return loadConfigurations(configDirectory, m_TemplateConfigsByType);
	}
	
	
	/**
	 * Wipes the cached configuration are reloads them
	 * from the directories previous used in {@link #loadConfigurations(File)}
	 * and {@link #loadTemplateConfigurations(File)} method calls. 
	 * If neither of those methods have been called before then
	 * nothing will be re-loaded but the current contents will
	 * be wiped.
	 * 
	 * @return true.
	 */
	public boolean reload()
	{
		m_FullConfigsByType.clear();
		m_TemplateConfigsByType.clear();
		
		if (m_ConfigDir != null)
		{
			loadConfigurations(m_ConfigDir);
		}
		
		if (m_TemplateConfigDir != null)
		{
			loadTemplateConfigurations(m_TemplateConfigDir);
		}
		
		return true;
	}
	
	
	/**	
	 * Lists all the Xml files from <code>configDir</code> and tries to load
	 * instances of DataSourceConfig from the Xml. 
	 * The extra parameter in this overloaded method specifies the 
	 * map into which the configs should be loaded. The templated and
	 * full configs are stored separately internally.
	 * 
	 * @param configDirectory The directory the Data Source Configuration files
	 * are kept.
	 * @param configMap The map to load the configs into.
	 * @return True is any configs were successfully loaded.
	 */
	private boolean loadConfigurations(File configDirectory, Map<String, DataTypeConfig> configMap) 
	{
		if (configDirectory.isDirectory() == false)
		{
			s_Logger.error("loadConfigurations " + configDirectory + " is not a directory");
			return false;
		}
		
		
		File[] files = configDirectory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name)
			{
				return name.endsWith(DEFALULT_FILE_EXTENSION);
			}
		});
		
		if (files.length == 0)
		{
			boolean isTemplate = configMap == m_TemplateConfigsByType;
			if (isTemplate)
			{
				s_Logger.warn("No data type template configurations defined in " + configDirectory);
			}
			else
			{
				s_Logger.warn("No data type configurations defined in " + configDirectory);
			}
		}
		
		
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
		try 
		{
			docBuilder = docBuilderFactory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) 
		{
			s_Logger.error("Cannot create DocumentBuilder", e);
			return false;
		}
		
		
		for (File xml : files)
		{
			Document fromDoc;
			try 
			{
				fromDoc = docBuilder.parse(xml);
				DataTypeConfig config = DataTypeConfig.fromXml(fromDoc);
				
				// Decrypt the password
				try
				{
					String encrypted = config.getSourceConnectionConfig().getPassword();
					String decrypted = PasswordEncryption.decryptPassword(encrypted);
					config.getSourceConnectionConfig().setPassword(decrypted);
				}
				catch(Exception e)
				{
				}
				
				if (config != null)
				{
					configMap.put(config.getDataType(), config);

					boolean isTemplate = configMap == m_TemplateConfigsByType;
					if (isTemplate)
					{
						s_Logger.info("Loaded template configuration for " + config.getDataType());
					}
					else
					{
						s_Logger.info("Loaded configuration for " + config.getDataType());
					}
				}
				else
				{
					s_Logger.error("Cannot parse configuration file " + xml);
				}
			}
			catch (SAXException e) 
			{
				String msg = "Cannot parse configuration file " + xml;
				s_Logger.error(msg, e);
			} 
			catch (IOException e) 
			{
				String msg = "Cannot parse configuration file " + xml;
				s_Logger.error(msg, e);
			}
			catch (ParseException e) 
			{
				String msg = "Cannot parse configuration file " + xml;
				s_Logger.error(msg, e);
			}
		}
		
		return configMap.size() > 0;
	}
	
	
	/**
	 * Saves the configuration object as Xml files.
	 * Each file has the name of the config object's DataType with 
	 * '.xml' appended to it.
	 * 
	 * The configurations will be saved to the directory they were last
	 * loaded from i.e. the argument to the last call to {@link #loadConfigurations(File)}
	 *  
	 * @return True if a least one config is written out to configDirectory.
	 */
	public boolean saveConfigurations()
	{
		return saveConfigurations(m_ConfigDir);
	}
	
	
	/**
	 * Saves the configuration object as Xml files in the given directory.
	 * Each file has the name of the config object's DataType with 
	 * '.xml' appended to it.
	 * 
	 * @param configDir The directory to save the config to.
	 * @return
	 */
	public boolean saveConfigurations(File configDir)
	{
		if (configDir == null)
		{
			s_Logger.error("saveConfigurations: " +
					"Cannot save configurations as the config directory == null");
			return false;
		}
				
		if (configDir.exists() == false)
		{
			if (configDir.mkdir() == false)
			{
				s_Logger.error("saveConfigurations cannot create the configuration directory.");
				return false;
			}
		}
		
		if (configDir.isDirectory() == false)
		{
			s_Logger.error("saveConfigurations " + configDir + " is not a directory");
			return false;
		}
		
		// Write the DOM document to the file
		Transformer xformer;
		try 
		{
			xformer = TransformerFactory.newInstance().newTransformer();
		}
		catch (TransformerConfigurationException e) 
		{
			s_Logger.error("saveConfigurations() Cannot create Document Transformer", e);
			return false;
		}
		catch (TransformerFactoryConfigurationError e) 
		{
			s_Logger.error("saveConfigurations() Cannot configure Document Transformer", e);
			return false;
		}
		
		// Pretty print
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		
		
		boolean aConfigWritten = false;
		for (DataTypeConfig config : m_FullConfigsByType.values())
		{
			// Make a copy as we are going to encrypt the password.
			DataTypeConfig copy = new DataTypeConfig(config);
			try 
			{
				String encrypted = PasswordEncryption.encryptPassword(copy.getSourceConnectionConfig().getPassword());
				copy.getSourceConnectionConfig().setPassword(encrypted);
			} 
			catch (Exception e1) 
			{
			}
			
			Document doc;
			try 
			{
				doc = copy.toXml();
				Source domSource = new DOMSource(doc);
				
				String filename = copy.getDataType() + DEFALULT_FILE_EXTENSION;
				File file = new File(configDir, filename);
				Result result = new StreamResult(file);
				
				xformer.transform(domSource, result);
				
				aConfigWritten = true;
			} 
			catch (ParserConfigurationException e) 
			{
				s_Logger.error("saveConfigurations parse exception", e);
			} 
			catch (TransformerException e) 
			{
				s_Logger.error("saveConfigurations transform exception", e);
			}
		}
		
		return aConfigWritten;
	}
}
