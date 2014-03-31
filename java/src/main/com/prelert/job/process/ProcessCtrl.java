/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

package com.prelert.job.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.Detector;
import com.prelert.job.AnalysisOptions;
import com.prelert.job.DataDescription;
import com.prelert.job.DetectorState;
import com.prelert.job.JobDetails;

/**
 * Utility class for running a Prelert process<br/>
 * The environment variables PRELERT_HOME and LIB_PATH (or platform variants) 
 * are set for the environment of the launched process, any inherited values 
 * of LIB_PATH or PRELERT_HOME are overwritten.
 * 
 * This class first needs to know where PRELERT_HOME is so it checks for 
 * the system property <b>prelert.home</b> and failing that looks for the 
 * PRELERT_HOME env var. If neither exist prelert home is set to an empty string.
 */
public class ProcessCtrl 
{
	/**
	 * Autodetect API native program name
	 */
	static final public String AUTODETECT_API = "prelert_autodetect_api";
	/**
	 * The location of Prelert Home. Equivalent to $PRELERT_HOME
	 */
	static final public String PRELERT_HOME;
	/**
	 * The base log file directory. Equivalant to $PRELERT_HOME/logs
	 */
	static final public String LOG_DIR;	
	/**
	 * The full to the autodetect program 
	 */
	static final public String AUTODETECT_PATH;
	/**
	 * Equivalent to $PRELERT_HOME/lib + $PRELERT_HOME/cots/lib
	 * (or $PRELERT_HOME/cots/bin on Windows)
	 */
	static final public String LIB_PATH;
	/**
	 * The name of the platform path environment variable.
	 * See {@linkplain #OSX_LIB_PATH_ENV}, {@linkplain #LINUX_LIB_PATH_ENV}
	 * and {@linkplain #WIN_LIB_PATH_ENV}, {@linkplain #SOLARIS_LIB_PATH_ENV}
	 */
	static final public String LIB_PATH_ENV;	
	/**
	 * Name of the System property containing the value of Prelert Home
	 */
	static final public String PRELERT_HOME_PROPERTY = "prelert.home";	
	/**
	 * Name of the environment variable PRELERT_HOME 
	 */
	static final public String PRELERT_HOME_ENV = "PRELERT_HOME";		
	/**
	 * OSX library path variable
	 */
	static final public String OSX_LIB_PATH_ENV = "DYLD_LIBRARY_PATH"; 
	/**
	 * Linux library path variable
	 */	
	static final public String LINUX_LIB_PATH_ENV = "LD_LIBRARY_PATH"; 
	/**
	 * Windows library path variable
	 */	
	static final public String WIN_LIB_PATH_ENV = "PATH"; 
	/**
	 * Solaris library path variable
	 */	
	static final public String SOLARIS_LIB_PATH_ENV = "LD_LIBRARY_PATH_64"; 
	
	/**
	 * Program arguments 
	 */
	static final public String BUCKET_SPAN_ARG = "--bucketspan=";
	static final public String FIELD_CONFIG_ARG = "--fieldconfig=";
	static final public String MODEL_CONFIG_ARG = "--modelconfig=";
	static final public String LIMIT_CONFIG_ARG = "--limitconfig=";
	static final public String BATCH_SPAN_ARG = "--batchspan=";
	static final public String PERIOD_ARG = "--period=";
	static final public String PARTITION_FIELD_ARG = "--partitionfield=";
	static final public String USE_NULL_ARG = "--usenull=";
	static final public String LOG_ID_ARG = "--logid=";
	static final public String DELIMITER_ARG = "--delimiter=";
	static final public String LENGTH_ENCODED_INPUT_ARG = "--lengthEncodedInput";
	static final public String TIME_FIELD_ARG = "--timefield=";
	static final public String TIME_FORMAT_ARG = "--timeformat=";
	static final public String RESTORE_STATE_ARG = "--restoreState=";
	static final public String DELETE_STATE_FILES_ARG = "--deleteStateFiles";
	static final public String PERSIST_STATE_ARG = "--persistState";
	static final public String VERSION_ARG = "--version";
	
	/**
	 * The unknown analytics version number string returned when the version 
	 * cannot be read 
	 */
	static final public String UNKNOWN_VERSION = "Unknown version of the analytics";
	
	/**
	 * Persisted model state is written to disk so it can be read 
	 * by the autodetect program. All model state files have this 
	 * base name followed by a unique number and 
	 * {@linkplain #BASE_STATE_FILE_EXTENSION}
	 */
	static final public String BASE_STATE_FILE_NAME = "model_state";
	
	/**
	 * The standard file extension for the temporary  
	 * model state files. 
	 */
	static final public String BASE_STATE_FILE_EXTENSION = ".xml";

	/**
	 * By default autodetect expects the timestamp in a field with this name
	 */
	static final public String DEFAULT_TIME_FIELD = "_time";
			
	/**
	 * command line args
	 */
	static final private String BY_ARG = "by";
	static final private String OVER_ARG = "over";
	
	/**
	 * Field config file strings
	 */
	static final private String DOT_IS_ENABLED = ".isEnabled";
	static final private String DOT_USE_NULL = ".useNull";
	static final private String DOT_BY = ".by";
	static final private String DOT_OVER = ".over";
	static final private char NEW_LINE = '\n';
	
	
	/**
	 * Static initialisation finds Prelert home and the path to the binaries,
	 * sets lib path to PRELERT_HOME/lib + PRELERT_HOME/cots/(lib|bin)
	 */
	static 
	{	
		String prelertHome = "";
		if (System.getProperty(PRELERT_HOME_PROPERTY) != null)
		{
			prelertHome = System.getProperty(PRELERT_HOME_PROPERTY);
		}
		else if (System.getenv().containsKey(PRELERT_HOME_ENV))
		{
			prelertHome = System.getenv().get(PRELERT_HOME_ENV);
		}
		
				
		PRELERT_HOME = prelertHome; 
		File executable = new File(new File(PRELERT_HOME, "bin"), AUTODETECT_API);		
		AUTODETECT_PATH = executable.getPath();
		
		File logDir = new File(PRELERT_HOME, "logs");	
		LOG_DIR = logDir.toString();
				
		String libSubDirectory = "lib";
		if (SystemUtils.IS_OS_MAC_OSX)
		{
			LIB_PATH_ENV = OSX_LIB_PATH_ENV;
		}
		else if (SystemUtils.IS_OS_WINDOWS)
		{
			LIB_PATH_ENV = WIN_LIB_PATH_ENV;
			libSubDirectory = "bin";
		}
		else if (SystemUtils.IS_OS_LINUX)
		{
			LIB_PATH_ENV = LINUX_LIB_PATH_ENV; 
		}
		else if (SystemUtils.IS_OS_SUN_OS)
		{
			LIB_PATH_ENV = SOLARIS_LIB_PATH_ENV; 
		}
		else 
		{
			throw new UnsupportedOperationException("Unsupported platform " + SystemUtils.OS_NAME);
		}
		
		File libDir = new File(PRELERT_HOME, libSubDirectory);
		File cotsDir = new File(new File(PRELERT_HOME, "cots"), libSubDirectory);
		LIB_PATH = libDir.getPath() + File.pathSeparatorChar + cotsDir.getPath();
	}
	
	static final private Logger s_Logger = Logger.getLogger(ProcessCtrl.class);
	
	static String s_AnalyticsVersion;
	
	synchronized public String getAnalyticsVersion()
	{
		if (s_AnalyticsVersion != null) 
		{
			return s_AnalyticsVersion;
		}
		
		List<String> command = new ArrayList<>();
		command.add(AUTODETECT_PATH);
		command.add(VERSION_ARG);
		
		s_Logger.info("Getting version number from " + command);
		
		// Build the process
		ProcessBuilder pb = new ProcessBuilder(command); 		
		

		s_Logger.info(String.format("%s=%s", PRELERT_HOME_ENV, PRELERT_HOME));
		pb.environment().put(PRELERT_HOME_ENV, PRELERT_HOME);
		
		s_Logger.info(String.format("%s=%s", LIB_PATH_ENV, LIB_PATH));
		pb.environment().put(LIB_PATH_ENV, LIB_PATH);
		
		try
		{
			Process proc = pb.start();	
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(proc.getErrorStream()));
			
			String output = reader.readLine();
			s_Logger.debug("autodetect version output = " + output);
			
			s_AnalyticsVersion = output;			
		}
		catch (IOException e)
		{
			s_Logger.error("Error reading analytics version number", e);
			return UNKNOWN_VERSION;
		}
		
		return s_AnalyticsVersion;
	}
	
	/**
	 * Calls {@link #buildProcess(String, JobDetails, DetectorState)} with 
	 * detectorState set to <code>null</code>.
	 * 
	 * @param processName The name of program to execute this should exist in the 
	 * directory PRELERT_HOME/bin/ 
	 * @return A Java Process object
	 * @throws IOException
	 */
	public Process buildProcess(String processName, JobDetails job)
	throws IOException	
	{
		return buildProcess(processName, job, null);
	}
	
	/**
	 * Sets the environment variables PRELERT_HOME and LIB_PATH (or platform
	 * variants) and starts the process in that environment. Any inherited value 
	 * of LIB_PATH or PRELERT_HOME is overwritten. 
	 * <code>processName</code> is not the full path it is the relative path of the
	 * program from the PRELERT_HOME/bin directory.
	 * 
	 * @param processName The name of program to execute this should exist in the 
	 * directory PRELERT_HOME/bin/ 
	 * @param job The job configuration
	 * @param detectorState if <code>null</code> this parameter is 
	 * ignored else the models' state is restored from this object 
	 * 
	 * @return A Java Process object
	 * @throws IOException 
	 */
	public Process buildProcess(String processName, JobDetails job,
			DetectorState detectorState)
	throws IOException
	{
		// TODO unit test for this build command stuff
		s_Logger.info("PRELERT_HOME is set to " + PRELERT_HOME);
		
		List<String> command = new ArrayList<>();
		command.add(AUTODETECT_PATH);

		if (job.getAnalysisConfig() != null)
		{
			if (job.getAnalysisConfig().getBucketSpan() != null)
			{
				String bucketspan = BUCKET_SPAN_ARG + job.getAnalysisConfig().getBucketSpan();
				command.add(bucketspan);
			}		
			if (job.getAnalysisConfig().getBatchSpan() != null)
			{
				String batchspan = BATCH_SPAN_ARG + job.getAnalysisConfig().getBatchSpan();
				command.add(batchspan);
			}
			if (job.getAnalysisConfig().getPeriod() != null)
			{
				String period = PERIOD_ARG + job.getAnalysisConfig().getPeriod();
				command.add(period);
			}	
			if (job.getAnalysisConfig().getPartitionField() != null)
			{
				String partition = PARTITION_FIELD_ARG + job.getAnalysisConfig().getPartitionField();
				command.add(partition);
			}		
		}
		
		if (job.getAnalysisOptions() != null)
		{			
			File modelConfigFile = File.createTempFile("modelconfig", ".conf");
			writeModelOptions(job.getAnalysisOptions(), modelConfigFile);		
			String modelConfig = MODEL_CONFIG_ARG + modelConfigFile.toString();
			command.add(modelConfig);
		}
		
		// Input is always length encoded
		command.add(LENGTH_ENCODED_INPUT_ARG);
		
		DataDescription dataDescription = job.getDataDescription();
		if (dataDescription != null)
		{
			if (dataDescription.getFieldDelimiter() != null)
			{
				String delimiterArg = DELIMITER_ARG
						+  dataDescription.getFieldDelimiter();
				command.add(delimiterArg);
			}
			if (dataDescription.getTimeField() != null)
			{
				String timeFieldArg = TIME_FIELD_ARG
						+  dataDescription.getTimeField();
				command.add(timeFieldArg);
			}
		}
				
		// Restoring the model state
		if (detectorState != null && detectorState.getDetectorKeys().size() > 0)
		{
			s_Logger.info("Restoring models for job '" + job.getId() +"'");

			Path tempDir = Files.createTempDirectory(null);
			String tempDirStr = tempDir.toString();
			
			int fileNumber = 1;
			for (String key : detectorState.getDetectorKeys())
			{
				File modelStateFile = new File(tempDirStr, BASE_STATE_FILE_NAME +
						fileNumber + BASE_STATE_FILE_EXTENSION);
				
				fileNumber++;
				
				writeModelState(detectorState.getDetectorState(key), modelStateFile);
			}
						
			String restoreArg = RESTORE_STATE_ARG + tempDirStr +
					File.separator + BASE_STATE_FILE_NAME;
			command.add(restoreArg);
			
			// tell autodetect to delete the temporary state files
			command.add(DELETE_STATE_FILES_ARG);				
		}		
		
		// Always persist the models when finished. 
		command.add(PERSIST_STATE_ARG);
		
		// the logging id is the job id
		String logId = LOG_ID_ARG + job.getId();
		command.add(logId);

		// now the actual field args
		if (job.getAnalysisConfig() != null)
		{
			if (job.getAnalysisConfig().getDetectors().size() == 1)
			{
				// Only one set of field args so use the command line options
				List<String> args = detectorConfigToCommandLinArgs(job.getAnalysisConfig().getDetectors().get(0));
				command.addAll(args);			
			}
			else
			{
				// write to a temporary field config file
				File fieldConfigFile = File.createTempFile("fieldconfig", ".conf");
				try (OutputStreamWriter osw = new OutputStreamWriter(
						new FileOutputStream(fieldConfigFile),
						Charset.forName("UTF-8")))
				{
					writeFieldConfig(job.getAnalysisConfig(), osw);
				}
				
				String modelConfig = FIELD_CONFIG_ARG + fieldConfigFile.toString();
				command.add(modelConfig);	
			}
		}
		
		// Build the process
		s_Logger.info("Starting native process with command: " +  command);
		ProcessBuilder pb = new ProcessBuilder(command); 		
		
		s_Logger.info(String.format("%s=%s", PRELERT_HOME_ENV, PRELERT_HOME));
		pb.environment().put(PRELERT_HOME_ENV, PRELERT_HOME);
		
		s_Logger.info(String.format("%s=%s", LIB_PATH_ENV, LIB_PATH));
		pb.environment().put(LIB_PATH_ENV, LIB_PATH);
		
		return pb.start();		
	}
	
	
	/**
	 * Write the Prelert autodetect model options to <code>emptyConfFile</code>.
	 * 
	 * @param emptyConfFile
	 * @throws IOException
	 */
	private void writeModelOptions(AnalysisOptions options, File emptyConfFile) 
	throws IOException	
	{
		StringBuilder contents = new StringBuilder("[anomaly]").append(NEW_LINE);
		if (options.getMaxFieldValues() > 0)
		{
			contents.append(AnalysisOptions.MAX_FIELD_VALUES + " = ")
					.append(options.getMaxFieldValues()).append(NEW_LINE);
		}
		if (options.getMaxTimeBuckets() > 0)
		{
			contents.append(AnalysisOptions.MAX_TIME_BUCKETS + " = ")
					.append(options.getMaxTimeBuckets()).append(NEW_LINE);
		}

		try (OutputStreamWriter osw = new OutputStreamWriter(
				new FileOutputStream(emptyConfFile),
				Charset.forName("UTF-8")))
		{
			osw.write(contents.toString());
		}		
	}
	
	
	/**
	 * Interpret the detector object as a list of strings in the format
	 * expected by autodetect api to configure t 
	 * 
	 * @param detector
	 * @return
	 */
	public List<String> detectorConfigToCommandLinArgs(Detector detector)
	{
		List<String> commandLineArgs = new ArrayList<>();
		
		if (detector.isUseNull() != null)
		{
			String usenull = USE_NULL_ARG + detector.isUseNull();
			commandLineArgs.add(usenull);
		}

		if (detector.getFunction() != null)
		{
			if (detector.getFieldName() != null)
			{
				commandLineArgs.add(detector.getFunction() + "(" + detector.getFieldName() + ")");
			}
			else
			{
				commandLineArgs.add(detector.getFunction());
			}
		}
		else if (detector.getFieldName() != null)
		{
			commandLineArgs.add(detector.getFieldName());
		}
		else
		{
			// TODO maybe return error instead?
			commandLineArgs.add("count");
		}

		if (detector.getByFieldName() != null)
		{
			commandLineArgs.add(BY_ARG);
			commandLineArgs.add(detector.getByFieldName());
		}
		if (detector.getOverFieldName() != null)
		{
			commandLineArgs.add(OVER_ARG);
			commandLineArgs.add(detector.getOverFieldName());
		}
		
		return commandLineArgs;
	}
	
	
	/**
	 * Write the Prelert autodetect field options to the output stream.
	 *
	 * @param config The configuration to write
	 * @param osw Stream to write to
	 * @throws IOException
	 */
	public void writeFieldConfig(AnalysisConfig config, OutputStreamWriter osw)
	throws IOException
	{
		StringBuilder contents = new StringBuilder();

		Set<String> detectorKeys = new HashSet<>();
		for (Detector detector : config.getDetectors())
		{
			StringBuilder keyBuilder = new StringBuilder();
			if (detector.getFunction() != null)
			{
				keyBuilder.append(detector.getFunction());
				if (detector.getFieldName() != null)
				{
					keyBuilder.append("(")
							.append(detector.getFieldName())
							.append(")");
				}
			}
			else if (detector.getFieldName() != null)
			{
				keyBuilder.append(detector.getFieldName());
			}
			else
			{
				// TODO maybe return error instead?
				keyBuilder.append("count");
			}

			if (detector.getByFieldName() != null)
			{
				keyBuilder.append("-").append(detector.getByFieldName());
			}
			if (detector.getOverFieldName() != null)
			{
				keyBuilder.append("-").append(detector.getOverFieldName());
			}

			String key = keyBuilder.toString();
			if (detectorKeys.contains(key))
			{
				s_Logger.warn(String.format(
						"Duplicate detector key '%s' ignorning this detector", key));
				continue;
			}
			detectorKeys.add(key);

			// .isEnabled is only necessary if nothing else is going to be added
			// for this key
			if (detector.isUseNull() == null &&
				detector.getByFieldName() == null &&
				detector.getOverFieldName() == null)
			{
				contents.append(key).append(DOT_IS_ENABLED).append(" = true").append(NEW_LINE);
			}

			if (detector.isUseNull() != null)
			{
				contents.append(key).append(DOT_USE_NULL)
					.append((detector.isUseNull() ? " = true" : " = false"))
					.append(NEW_LINE);
			}

			if (detector.getByFieldName() != null)
			{
				contents.append(key).append(DOT_BY).append(" = ").append(detector.getByFieldName()).append(NEW_LINE);
			}
			if (detector.getOverFieldName() != null)
			{
				contents.append(key).append(DOT_OVER).append(" = ").append(detector.getOverFieldName()).append(NEW_LINE);
			}
		}

		s_Logger.debug("FieldConfig = " + contents.toString());	

		osw.write(contents.toString());
	}
	
	/**
	 * Write the string <code>state</code> to <code>file</code>
	 * followed by a newline character.
	 * 
	 * @param state
	 * @param file
	 * @throws IOException
	 */
	private void writeModelState(String state, File file)
	throws IOException
	{
		try (OutputStreamWriter osw = new OutputStreamWriter(
				new FileOutputStream(file),
				Charset.forName("UTF-8")))
		{
			osw.write(state);
			osw.write('\n');
		}
	}
	
}
