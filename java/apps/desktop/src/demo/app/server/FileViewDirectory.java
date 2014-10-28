/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package demo.app.server;

import java.io.InputStream;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import demo.app.data.EvidenceView;
import demo.app.data.UsageView;


/**
 * Extension of the ViewDirectory base class that reads in views that are defined 
 * in an external XML configuration file.
 * @author Pete Harverson
 */
public class FileViewDirectory extends ViewDirectory
{
	static Logger logger = Logger.getLogger(FileViewDirectory.class);
	
	private String					m_ConfigFile;
	
	
	/**
	 * Initialises the View Directory, reading in the views which have been
	 * configured in an external XML configuration file.
	 */
	public void init()
	{
		super.init();
		
		try
		{
			// Read in the views defined in the view configuration file.
			parseViewFile(m_ConfigFile);
		}
		catch (Exception e)
		{
			logger.error("Error loading views from configuration file: ", e);
		}
	}
	
	
	/**
	 * Returns the name of the view configuration file being used by the directory.
	 * @return the view configuration file, which will be loaded from the classpath.
	 */
	public String getConfigFile()
    {
    	return m_ConfigFile;
    }


	/**
	 * Sets the name of the view configuration file being used by the directory.
	 * @param configFile the view configuration file, which will be loaded 
	 * from the classpath.
	 */
	public void setConfigFile(String configFile)
    {
    	m_ConfigFile = configFile;
    }


	/**
	 * Parses the specified view configuration file into a list of Prelert Views.
	 * @param viewFile the file to parse, which will be loaded from the classpath.
	 * @return the list of views.
	 */
	protected void parseViewFile(String viewFile)
	{
		logger.info("Parsing view config file " + viewFile);
		
		InputStream stream = getClass().getClassLoader().getResourceAsStream(viewFile);
		
		Digester digester = new Digester();
		digester.setValidating(false);
		
		// Set the ViewDirectory object itself as the top object in the stack.
		digester.push(this);
		
		// Read in the Prelert-defined desktop views.
		digester.addRuleSet(new EvidenceViewDigesterRuleSet("prelertViews/evidenceView"));
		digester.addRuleSet(new ExceptionViewDigesterRuleSet("prelertViews/exceptionView"));
		digester.addRuleSet(new CausalityViewDigesterRuleSet("prelertViews/causalityView"));
		digester.addRuleSet(new HistoryViewDigesterRuleSet("prelertViews/historyView"));
		digester.addRuleSet(new UsageViewDigesterRuleSet("prelertViews/usageView"));
		
		digester.addSetNext("prelertViews/evidenceView", "addView", "demo.app.data.EvidenceView");
		digester.addSetNext("prelertViews/exceptionView", "setExceptionView", "demo.app.data.ExceptionView");
		digester.addSetNext("prelertViews/causalityView", "setCausalityView", "demo.app.data.CausalityView");
		digester.addSetNext("prelertViews/historyView", "setHistoryView", "demo.app.data.HistoryView");
		digester.addSetNext("prelertViews/usageView", "addView", "demo.app.data.UsageView");
		
		
		// Read in any user-defined list or usage views.
		digester.addRuleSet(new ListViewDigesterRuleSet("prelertViews/userViews/listView"));
		digester.addRuleSet(new EvidenceViewDigesterRuleSet("prelertViews/userViews/evidenceView"));
		
		digester.addSetNext("prelertViews/userViews/listView", "addUserDefinedView", "demo.app.data.ListView");
		digester.addSetNext("prelertViews/userViews/evidenceView", "addUserDefinedView", "demo.app.data.EvidenceView");
		
		try
		{
			digester.parse(stream);
			
			if (getEvidenceViews() != null)
			{
				for (EvidenceView evidenceView : getEvidenceViews())
				{
					logger.info("Loaded Evidence View: " + evidenceView);
				}
			}
			logger.info("Loaded Exception View: " + getExceptionView());
			logger.info("Loaded Causality View: " + getCausalityView());
			logger.info("Loaded History View: " + getHistoryView());
			if (getTimeSeriesViews() != null)
			{
				for (UsageView usageView : getTimeSeriesViews())
				{
					logger.info("Loaded Time Series View: " + usageView);
				}
			}
	
			logger.info("Total number of views loaded into directory: " + getViews().size());
		}
		catch (Exception e)
		{
			logger.error("Error parsing " + viewFile, e);
		}
	}
	
	
	/**
	 * main() method for standalone testing.
	 */
	public static void main(String[] args)
	{
		// Configure the log4j logging properties.
		PropertyConfigurator.configure("C:/eclipse/workspace/Ext GWT/config/log4j.properties");
		
		FileViewDirectory viewDirectory = new FileViewDirectory();
		viewDirectory.setConfigFile("config/views.xml");
		viewDirectory.init();
	}

}
