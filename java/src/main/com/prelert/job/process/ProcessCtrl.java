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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.Detector;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.DataDescription;
import com.prelert.job.DetectorState;
import com.prelert.job.JobDetails;
import com.prelert.job.normalisation.InitialState;


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
	 * The normalisation native program name
	 */
	static final public String NORMALIZE_API = "prelert_normalize_api";	
	/**
	 * The location of Prelert Home. Equivalent to $PRELERT_HOME
	 */
	static final public String PRELERT_HOME;
	/**
	 * The base log file directory. Equivalent to $PRELERT_HOME/logs
	 */
	static final public String LOG_DIR;	
	/**
	 * The config directory. Equivalent to $PRELERT_HOME/config
	 */
	static final public String CONFIG_DIR;	
	/**
	 * The full to the autodetect program 
	 */
	static final public String AUTODETECT_PATH;
	/**
	 *  The full to the normalisation program
	 */
	static final public String NORMALIZE_PATH; 
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
	static final public String WIN_LIB_PATH_ENV = "Path"; 
	/**
	 * Solaris library path variable
	 */	
	static final public String SOLARIS_LIB_PATH_ENV = "LD_LIBRARY_PATH_64"; 
	
	/*
	 * General arguments
	 */
	static final public String FIELD_CONFIG_ARG = "--fieldconfig=";
	static final public String MODEL_CONFIG_ARG = "--modelconfig=";
	static final public String LIMIT_CONFIG_ARG = "--limitconfig=";
	static final public String BUCKET_SPAN_ARG = "--bucketspan=";
	static final public String LOG_ID_ARG = "--logid=";
	static final public String LENGTH_ENCODED_INPUT_ARG = "--lengthEncodedInput";

	/*
	 * Autodetect arguments 
	 */
	static final public String BATCH_SPAN_ARG = "--batchspan=";
	static final public String PERIOD_ARG = "--period=";
	static final public String PARTITION_FIELD_ARG = "--partitionfield=";
	static final public String USE_NULL_ARG = "--usenull=";
	static final public String DELIMITER_ARG = "--delimiter=";
	static final public String TIME_FIELD_ARG = "--timefield=";
	static final public String TIME_FORMAT_ARG = "--timeformat=";
	static final public String RESTORE_STATE_ARG = "--restoreState=";
	static final public String DELETE_STATE_FILES_ARG = "--deleteStateFiles";
	static final public String PERSIST_STATE_ARG = "--persistState";
	static final public String VERSION_ARG = "--version";
	static final public String INFO_ARG = "--info";
	static final public String MAX_ANOMALY_RECORDS = "--maxAnomalyRecords=";
	
	/*
	 * Normalize_api args
	 */
	static final public String SYS_STATE_CHANGE_ARG = "--sysChangeState=";
	static final public String UNUSUAL_STATE_ARG = "--unusualState=";
	
	/**
	 * The types of normalisation the the normaliser will do. 
	 */
	public enum NormalisationType {	SYS_STATE_CHANGE, UNUSUAL_STATE};
	
	/**
	 * Name of the model config file
	 */
	static final public String PRELERT_MODEL_CONF = "prelertmodel.conf";
	
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
	
	/*
	 * The standard file extension for the temporary  
	 * model state files. 
	 */
	static final public String BASE_STATE_FILE_EXTENSION = ".xml";
			
	/*
	 * command line args
	 */
	static final public String BY_ARG = "by";
	static final public String OVER_ARG = "over";
	
	/*
	 * Field config file strings
	 */
	static final public String DOT_IS_ENABLED = ".isEnabled";
	static final public String DOT_USE_NULL = ".useNull";
	static final public String DOT_BY = ".by";
	static final public String DOT_OVER = ".over";
	static final public String DOT_PARTITION = ".partition";
	static final public char NEW_LINE = '\n';
	
	/*
	 * The configuration fields used in limits.conf
	 */
	static final public String MAX_FIELD_VALUES_CONFIG_STR = "maxfieldvalues";
	static final public String MAX_TIME_BUCKETS_CONFIG_STR = "maxtimebuckets";	
	
	/*
	 * Normalisation init state csv headers
	 */
	static final public String SYS_CHANGE_STATE_HEADER[] = { "t", "a" };
	static final public String UNUSUAL_STATE_HEADER[] = { "t", "p", "d" };
	
	/*
	 * Normalisation input fields
	 */
	static final public String PROBABILITY = "probability";
	static final public String RAW_ANOMALY_SCORE = "anomalyScore";
	
	
	
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
		
		executable = new File(new File(PRELERT_HOME, "bin"), NORMALIZE_API);	
		NORMALIZE_PATH = executable.getPath();
		
		File logDir = new File(PRELERT_HOME, "logs");	
		LOG_DIR = logDir.toString();
		
		File configDir = new File(PRELERT_HOME, "config");	
		CONFIG_DIR = configDir.toString();
				
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


	/**
	 * Set up an environment containing the PRELERT_HOME and LD_LIBRARY_PATH
	 * (or equivalent) environment variables.
	 */
	static private void buildEnvironment(ProcessBuilder pb)
	{
		// Always clear inherited environment variables
		pb.environment().clear();

		s_Logger.info(String.format("%s=%s", PRELERT_HOME_ENV, PRELERT_HOME));
		pb.environment().put(PRELERT_HOME_ENV, PRELERT_HOME);

		s_Logger.info(String.format("%s=%s", LIB_PATH_ENV, LIB_PATH));
		pb.environment().put(LIB_PATH_ENV, LIB_PATH);
	}


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
		buildEnvironment(pb);

		try
		{
			Process proc = pb.start();
			try
			{
				int exitValue = proc.waitFor();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(proc.getErrorStream(), 
								StandardCharsets.UTF_8));
				
				String output = reader.readLine();
				s_Logger.debug("autodetect version output = " + output);

				if (exitValue >= 0) 
				{
					if (output == null)
					{
						return UNKNOWN_VERSION;						
					}
					
					s_AnalyticsVersion = output;
					return s_AnalyticsVersion;
				}
				else
				{
					return String.format("Error autodetect returned %d. \nError Output = '%s'.\n%s",
							exitValue, output, UNKNOWN_VERSION);
				}
			}
			catch (InterruptedException ie)
			{
				s_Logger.error("Interrupted reading analytics version number", ie);
				return UNKNOWN_VERSION;
			}
			
		}
		catch (IOException e)
		{
			s_Logger.error("Error reading analytics version number", e);
			return UNKNOWN_VERSION;
		}
	}


	/**
	 * Get the C++ process to print a JSON document containing some of the usage
	 * and license info
	 */
	synchronized public String getInfo()
	{
		List<String> command = new ArrayList<>();
		command.add(AUTODETECT_PATH);
		command.add(INFO_ARG);

		s_Logger.info("Getting info from " + command);

		// Build the process
		ProcessBuilder pb = new ProcessBuilder(command);
		buildEnvironment(pb);

		try
		{
			Process proc = pb.start();
			try
			{
				int exitValue = proc.waitFor();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(proc.getInputStream(),
								StandardCharsets.UTF_8));

				String output = reader.readLine();
				s_Logger.debug("autodetect info output = " + output);

				if (exitValue >= 0 && output != null)
				{
					return output;
				}
			}
			catch (InterruptedException ie)
			{
				s_Logger.error("Interrupted reading autodetect info", ie);
			}
		}
		catch (IOException e)
		{
			s_Logger.error("Error reading autodetect info", e);
		}

		// On error return an empty JSON document
		return "{}";
	}


	/**
	 * Calls {@link #buildProcess(String, JobDetails, DetectorState)} with 
	 * detectorState set to <code>null</code>.
	 * 
	 * @param processName The name of program to execute this should exist in the 
	 * directory PRELERT_HOME/bin/ 
	 * @param logger The job's logger
	 * @return A Java Process object
	 * @throws IOException
	 */
	static public Process buildAutoDetect(String processName, JobDetails job, Logger logger)
	throws IOException	
	{
		return buildAutoDetect(processName, job, null, logger);
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
	 * @param logger The job's logger
	 * 
	 * @return A Java Process object
	 * @throws IOException 
	 */
	static public Process buildAutoDetect(String processName, JobDetails job,
			DetectorState detectorState, Logger logger)
	throws IOException
	{
		logger.info("PRELERT_HOME is set to " + PRELERT_HOME);
		
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
		}
		
		if (job.getAnalysisLimits() != null)
		{			
			File limitConfigFile = File.createTempFile("limitconfig", ".conf");
			writeLimits(job.getAnalysisLimits(), limitConfigFile);		
			String limitConfig = LIMIT_CONFIG_ARG + limitConfigFile.toString();
			command.add(limitConfig);
		}
				
		if (modelConfigFilePresent())
		{
			String modelConfigFile = new File(CONFIG_DIR, PRELERT_MODEL_CONF).toString();
			command.add(MODEL_CONFIG_ARG + modelConfigFile);
		}
		
		// Input is always length encoded
		command.add(LENGTH_ENCODED_INPUT_ARG);
		
		
		// TODO limit on the number of anomaly records?
		String recordCountArg = MAX_ANOMALY_RECORDS + "0";
		command.add(recordCountArg);
		
		String timeField = DataDescription.DEFAULT_TIME_FIELD;

		DataDescription dataDescription = job.getDataDescription();
		if (dataDescription != null)
		{
			if (DataDescription.DEFAULT_DELIMITER != dataDescription.getFieldDelimiter())
			{
				String delimiterArg = DELIMITER_ARG
						+  dataDescription.getFieldDelimiter();
				command.add(delimiterArg);
			}
			
			if (dataDescription.getTimeField() != null &&
					dataDescription.getTimeField().isEmpty() == false)
			{
				timeField = dataDescription.getTimeField();
			}

		}
		// always set the time field
		String timeFieldArg = TIME_FIELD_ARG + timeField;
		command.add(timeFieldArg);
				
		// Restoring the model state
		if (detectorState != null && detectorState.getDetectorKeys().size() > 0)
		{
			logger.info("Restoring models for job '" + job.getId() +"'");

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
						StandardCharsets.UTF_8))
				{
					writeFieldConfig(job.getAnalysisConfig(), osw, logger);
				}
				
				String fieldConfig = FIELD_CONFIG_ARG + fieldConfigFile.toString();
				command.add(fieldConfig);	
			}
		}
		
		// Build the process
		logger.info("Starting autodetect process with command: " +  command);
		ProcessBuilder pb = new ProcessBuilder(command);
		buildEnvironment(pb);

		return pb.start();		
	}
	
	
	/**
	 * Write the Prelert autodetect model options to <code>emptyConfFile</code>.
	 * 
	 * @param emptyConfFile
	 * @throws IOException
	 */
	static private void writeLimits(AnalysisLimits options, File emptyConfFile) 
	throws IOException	
	{
		StringBuilder contents = new StringBuilder("[anomaly]").append(NEW_LINE);
		if (options.getMaxFieldValues() > 0)
		{
			contents.append(MAX_FIELD_VALUES_CONFIG_STR + " = ")
					.append(options.getMaxFieldValues()).append(NEW_LINE);
		}
		if (options.getMaxTimeBuckets() > 0)
		{
			contents.append(MAX_TIME_BUCKETS_CONFIG_STR + " = ")
					.append(options.getMaxTimeBuckets()).append(NEW_LINE);
		}

		try (OutputStreamWriter osw = new OutputStreamWriter(
				new FileOutputStream(emptyConfFile),
				StandardCharsets.UTF_8))
		{
			osw.write(contents.toString());
		}		
	}
	
	
	/**
	 * Return true if there is a file PRELERT_HOME/config/prelertmodel.conf
	 * @return
	 */
	static private boolean modelConfigFilePresent()
	{
		File f = new File(CONFIG_DIR, PRELERT_MODEL_CONF);
		
		return f.exists() && !f.isDirectory();
	}
	
	
	/**
	 * Interpret the detector object as a list of strings in the format
	 * expected by autodetect api to configure t 
	 * 
	 * @param detector
	 * @return
	 */
	static public List<String> detectorConfigToCommandLinArgs(Detector detector)
	{
		List<String> commandLineArgs = new ArrayList<>();
		
		if (detector.isUseNull() != null)
		{
			String usenull = USE_NULL_ARG + detector.isUseNull();
			commandLineArgs.add(usenull);
		}

		if (isNotNullOrEmpty(detector.getFunction()))
		{
			if (isNotNullOrEmpty(detector.getFieldName() ))
			{
				commandLineArgs.add(detector.getFunction() + "(" + detector.getFieldName() + ")");
			}
			else
			{
				commandLineArgs.add(detector.getFunction());
			}
		}
		else if (isNotNullOrEmpty(detector.getFieldName()))
		{
			commandLineArgs.add(detector.getFieldName());
		}

		if (isNotNullOrEmpty(detector.getByFieldName()))
		{
			commandLineArgs.add(BY_ARG);
			commandLineArgs.add(detector.getByFieldName());
		}
		if (isNotNullOrEmpty(detector.getOverFieldName()))
		{
			commandLineArgs.add(OVER_ARG);
			commandLineArgs.add(detector.getOverFieldName());
		}
		if (isNotNullOrEmpty(detector.getPartitionFieldName()))
		{
			String partition = PARTITION_FIELD_ARG + detector.getPartitionFieldName();
			commandLineArgs.add(partition);
		}	
		
		return commandLineArgs;
	}
	
	
	/**
	 * Write the Prelert autodetect field options to the output stream.
	 *
	 * @param config The configuration to write
	 * @param osw Stream to write to
	 * @param logger
	 * @throws IOException
	 */
	static public void writeFieldConfig(AnalysisConfig config, OutputStreamWriter osw,
			Logger logger)
	throws IOException
	{
		StringBuilder contents = new StringBuilder();

		Set<String> detectorKeys = new HashSet<>();
		for (Detector detector : config.getDetectors())
		{
			StringBuilder keyBuilder = new StringBuilder();
			if (isNotNullOrEmpty(detector.getFunction()))
			{
				keyBuilder.append(detector.getFunction());
				if (detector.getFieldName() != null)
				{
					keyBuilder.append("(")
							.append(detector.getFieldName())
							.append(")");
				}
			}
			else if (isNotNullOrEmpty(detector.getFieldName()))
			{
				keyBuilder.append(detector.getFieldName());
			}

			if (isNotNullOrEmpty(detector.getByFieldName()))
			{
				keyBuilder.append("-").append(detector.getByFieldName());
			}
			if (isNotNullOrEmpty(detector.getOverFieldName()))
			{
				keyBuilder.append("-").append(detector.getOverFieldName());
			}
			if (isNotNullOrEmpty(detector.getPartitionFieldName()))
			{
				keyBuilder.append("-").append(detector.getPartitionFieldName());
			}

			String key = keyBuilder.toString();
			if (detectorKeys.contains(key))
			{
				logger.warn(String.format(
						"Duplicate detector key '%s' ignorning this detector", key));
				continue;
			}
			detectorKeys.add(key);
			
			// .isEnabled is only necessary if nothing else is going to be added
			// for this key
			if (detector.isUseNull() == null &&
					isNullOrEmpty(detector.getByFieldName()) &&
					isNullOrEmpty(detector.getOverFieldName()) && 
					isNullOrEmpty(detector.getPartitionFieldName()))
			{
				contents.append(key).append(DOT_IS_ENABLED).append(" = true").append(NEW_LINE);
			}

			if (detector.isUseNull() != null)
			{
				contents.append(key).append(DOT_USE_NULL)
					.append((detector.isUseNull() ? " = true" : " = false"))
					.append(NEW_LINE);
			}

			if (isNotNullOrEmpty(detector.getByFieldName()))
			{
				contents.append(key).append(DOT_BY).append(" = ").append(detector.getByFieldName()).append(NEW_LINE);
			}
			if (isNotNullOrEmpty(detector.getOverFieldName()))
			{
				contents.append(key).append(DOT_OVER).append(" = ").append(detector.getOverFieldName()).append(NEW_LINE);
			}
			if (isNotNullOrEmpty(detector.getPartitionFieldName()))
			{
				contents.append(key).append(DOT_PARTITION).append(" = ").append(detector.getPartitionFieldName()).append(NEW_LINE);
			}
		}

		logger.debug("FieldConfig: \n" + contents.toString());	

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
	static private void writeModelState(String state, File file)
	throws IOException
	{
		try (OutputStreamWriter osw = new OutputStreamWriter(
				new FileOutputStream(file),
				StandardCharsets.UTF_8))
		{
			osw.write(state);
			osw.write('\n');
		}
	}
	

	/**
	 * The process can be initialised with both sysChangeState and 
	 * unusualBehaviourState if either is <code>null</code> then is 
	 * is not used. 
	 * 
	 * 
	 * @param jobId
	 * @param sysChangeState Set to <code>null</code> to be ignored
	 * @param unusualBehaviourState Set to <code>null</code> to be ignored
	 * @param bucketSpan If <code>null</code> then use the program default
	 * @param logger
	 * @return
	 * @throws IOException
	 */
	static public Process buildNormaliser(String jobId, 
			InitialState sysChangeState, InitialState unusualBehaviourState,
			Integer bucketSpan, Logger logger)
	throws IOException
	{
		logger.info("PRELERT_HOME is set to " + PRELERT_HOME);
		
		List<String> command = new ArrayList<>();
		command.add(NORMALIZE_PATH);
		
		if (sysChangeState != null)
		{
			Path sysChangeStateFilePath = writeNormaliserInitState(jobId, 
					NormalisationType.SYS_STATE_CHANGE, sysChangeState);

			String stateFileArg = SYS_STATE_CHANGE_ARG + sysChangeStateFilePath;
			command.add(stateFileArg);
		}

		if (unusualBehaviourState != null)
		{
			Path unusualStateFilePath = writeNormaliserInitState(jobId, 
					NormalisationType.UNUSUAL_STATE, unusualBehaviourState);
			
			String stateFileArg = UNUSUAL_STATE_ARG + unusualStateFilePath;
			command.add(stateFileArg);
		}
		
		if (bucketSpan != null)
		{
			String bucketSpanArg = BUCKET_SPAN_ARG + bucketSpan.toString();
			command.add(bucketSpanArg);
		}
		
		// TODO Log everything to the default normalize_api dir
//		String logId = LOG_ID_ARG + jobId;
//		command.add(logId);
		
		command.add(LENGTH_ENCODED_INPUT_ARG);
		
		if (modelConfigFilePresent())
		{
			String modelConfigFile = new File(CONFIG_DIR, PRELERT_MODEL_CONF).toString();
			command.add(MODEL_CONFIG_ARG + modelConfigFile);
		}
		
		// Build the process
		logger.info("Starting normaliser process with command: " +  command);
		ProcessBuilder pb = new ProcessBuilder(command);
		buildEnvironment(pb);

		return pb.start();		
	}
	
	
	/**
	 * Write the normaliser init state to file.
	 * 
	 * @param jobId
	 * @param type
	 * @param state
	 * @return The state file path
	 * @throws IOException
	 */
	static private Path writeNormaliserInitState(String jobId, NormalisationType type,
			InitialState state)
	throws IOException
	{
		Path stateFile = Files.createTempFile(jobId + "_state", "csv");
		
		try (CsvListWriter csvWriter = new CsvListWriter(new OutputStreamWriter(
				new FileOutputStream(stateFile.toString()),
				StandardCharsets.UTF_8), CsvPreference.EXCEL_PREFERENCE))
		{
			switch (type)
			{
			case SYS_STATE_CHANGE:
				csvWriter.writeHeader(SYS_CHANGE_STATE_HEADER);
				for (InitialState.InitialStateRecord record : state)
				{
					csvWriter.write(record.toSysChangeArray());
				}
				break;
			case UNUSUAL_STATE:
				csvWriter.writeHeader(UNUSUAL_STATE_HEADER);
				for (InitialState.InitialStateRecord record : state)
				{
					csvWriter.write(record.toUnusualArray());
				} 				
				break;
			}
		}
		
		return stateFile;
	}
	
	/**
	 * Returns true if the string arg is not null and not empty
	 * i.e. it is a valid string
	 * @param arg
	 */
	private static boolean isNotNullOrEmpty(String arg)
	{
		return (arg != null && arg.isEmpty() == false);
	}
	
	/**
	 * Returns true if the string arg is either null or empty
	 * i.e. it is NOT a valid string
	 * @param arg
	 */
	private static boolean isNullOrEmpty(String arg)
	{
		return (arg == null || arg.isEmpty());
	}
	
}
