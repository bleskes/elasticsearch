/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

package com.prelert.job.process.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.TransformConfig;
import com.prelert.job.TransformConfigs;
import com.prelert.job.TransformConfigurationException;
import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;
import com.prelert.transforms.Transform;
import com.prelert.transforms.TransformException;
import com.prelert.transforms.TransformFactory;
import com.prelert.transforms.date.DateFormatTransform;
import com.prelert.transforms.date.DateTransform;
import com.prelert.transforms.date.DoubleDateTransform;

public abstract class AbstractDataToProcessWriter implements DataToProcessWriter
{
	protected static int TIME_FIELD_OUT_INDEX = 0;

    protected final LengthEncodedWriter m_LengthEncodedWriter;
    protected final DataDescription m_DataDescription;
    protected final AnalysisConfig m_AnalysisConfig;
    protected final StatusReporter m_StatusReporter;
    protected final JobDataPersister m_JobDataPersister;
    protected final Logger m_Logger;
    protected final TransformConfigs m_TransformConfigs;

    protected DateTransform m_DateTransform;

    protected Map<String, Integer> m_InFieldIndexes;
    protected Map<String, Integer> m_OutFieldIndexes;
    protected List<InputOutputMap> m_InputOutputMap;

    private long m_LatestEpoch;


    protected AbstractDataToProcessWriter(LengthEncodedWriter lengthEncodedWriter,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            TransformConfigs transformConfigs, StatusReporter statusReporter,
            JobDataPersister jobDataPersister, Logger logger)
    {
        m_LengthEncodedWriter = Objects.requireNonNull(lengthEncodedWriter);
        m_DataDescription = Objects.requireNonNull(dataDescription);
        m_AnalysisConfig = Objects.requireNonNull(analysisConfig);
        m_StatusReporter = Objects.requireNonNull(statusReporter);
        m_JobDataPersister = Objects.requireNonNull(jobDataPersister);
        m_Logger = Objects.requireNonNull(logger);
        m_TransformConfigs = Objects.requireNonNull(transformConfigs);

        m_LatestEpoch = 0;
    }


    /**
     * Create the transforms. This must be called before any of the write
     * functions even if no transforms are configured as it creates the
     * date transform and sets up the field mappings.<br>
     *
     * Finds the required input indicies in the <code>header</code>
     * and sets the mappings for the transforms so they know where
     * to read their inputs.
     *
     * @param header
     * @return
     * @throws MissingFieldException
     */
    public List<Transform> buildTransforms(String [] header)
    throws MissingFieldException
    {
    	List<Transform> transforms = new ArrayList<>();

    	Collection<String> inputFields = inputFields();
    	m_InFieldIndexes = inputFieldIndicies(header, inputFields);



    	checkForMissingFields(inputFields, m_InFieldIndexes, header);

    	m_OutFieldIndexes = outputFieldIndicies();

    	m_InputOutputMap = createInputOutputMap();

        m_StatusReporter.setAnalysedFieldsPerRecord(m_AnalysisConfig.analysisFields().size());

        /* TODO
        m_JobDataPersister.setFieldMappings(m_AnalysisConfig.fields(),
                m_AnalysisConfig.byFields(), m_AnalysisConfig.overFields(),
                m_AnalysisConfig.partitionFields(), record);
        */

        boolean isDateFromatString = m_DataDescription.isTransformTime()
        									&& !m_DataDescription.isEpochMs();
        if (isDateFromatString)
        {
        	m_DateTransform = new DateFormatTransform(m_DataDescription.getTimeFormat(),
					new int[] {m_InFieldIndexes.get(m_DataDescription.getTimeField())},
					new int [] {m_OutFieldIndexes.get(m_DataDescription.getTimeField())});
        }
        else
        {
        	m_DateTransform = new DoubleDateTransform(m_DataDescription.isEpochMs(),
					new int[] {m_InFieldIndexes.get(m_DataDescription.getTimeField())},
					new int [] {m_OutFieldIndexes.get(m_DataDescription.getTimeField())});
        }

    	for (TransformConfig config : m_TransformConfigs.getTransforms())
    	{
    		try
    		{
				Transform tr = TransformFactory.create(config, m_InFieldIndexes,
													m_OutFieldIndexes, m_Logger);
				transforms.add(tr);
			}
    		catch (TransformConfigurationException e)
    		{
				m_Logger.error("Error creating transform " + config, e);
			}
    	}

    	return transforms;
    }

    /**
     * Transform the input data and write to length encoded writer.<br>
     *
     * Fields that aren't transformed i.e. those in m_InputOutputMap must be
     * copied from input to output before this function is called.
     *
     * @param transforms See {@linkplain #buildTransforms(String[])}
     * @param input The record the transforms should read their input from. The contents should
     * align with the header paramter passed to {@linkplain #buildTransforms(String[])}
     * @param output The record that will be written to the length encoded writer.
     * This should be the same size as the number of output (analysis fields) i.e.
     * the size of the map returned by {@linkplain #outputFieldIndicies()}
     * @param numberOfFieldsRead The total number read not just those included in the analysis
     *
     * @return
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws IOException
     */
    protected boolean applyTransformsAndWrite(List<Transform> transforms,
    									String [] input, String [] output,
    									long numberOfFieldsRead)
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException
    {

    	try
    	{
			m_DateTransform.transform(input, output);
		}
    	catch (TransformException e)
    	{
            m_StatusReporter.reportDateParseError(m_InFieldIndexes.size());
            m_Logger.error(e.getMessage());
    		return false;
		}

    	long epoch = m_DateTransform.epoch();
        if (epoch < m_LatestEpoch - m_AnalysisConfig.getLatency())
        {
            // out of order
            m_StatusReporter.reportOutOfOrderRecord(m_InFieldIndexes.size());
            return false;
        }
        m_LatestEpoch = Math.max(m_LatestEpoch, epoch);


        for (Transform tr : transforms)
        {
        	try
        	{
        		tr.transform(input, output);
        	}
        	catch (TransformException e)
        	{
        		m_Logger.warn("e");
        	}
        }

        m_LengthEncodedWriter.writeRecord(output);
        m_JobDataPersister.persistRecord(epoch, output);
        m_StatusReporter.reportRecordWritten(numberOfFieldsRead);

        return true;
    }


    /**
     * Write the header.
     * The header is created from the list of analysis input fields,
     * the time field and the control field
     *
     * @throws IOException
     */
    protected void writeHeader() throws IOException
    {
        //  header is all the analysis input fields + the time field + control field
        int numFields = m_OutFieldIndexes.size();
        String[] record = new String[numFields];

        Iterator<Map.Entry<String, Integer>> itr = m_OutFieldIndexes.entrySet().iterator();
        while (itr.hasNext())
        {
        	Map.Entry<String, Integer> entry = itr.next();
        	record[entry.getValue()] = entry.getKey();
        }

        // Write the header
        m_LengthEncodedWriter.writeRecord(record);
    }


    /**
     * Get all the expected input fields
     * = time field +  transform input fields + analysis fields that aren't a transform output
     *
     * @return
     */
    protected Collection<String> inputFields()
    {
    	Set<String> requiredFields = new HashSet<>(m_AnalysisConfig.analysisFields());

    	requiredFields.removeAll(m_TransformConfigs.outputFieldNames()); // inputs not in a transform
    	requiredFields.addAll(m_TransformConfigs.inputFieldNames());

    	requiredFields.add(m_DataDescription.getTimeField());

    	return requiredFields;
    }

    /**
     * Find the indicies of the input fields from the header
     * @param header
     * @param inputFields The required input fields
     * @return
     */
    protected Map<String, Integer> inputFieldIndicies(String[] header, Collection<String> inputFields)
    {
        List<String> headerList = Arrays.asList(header);  // TODO header could be empty

        Map<String, Integer> fieldIndexes = new HashMap<String, Integer>();

        for (String field : inputFields)
        {
        	int index = headerList.indexOf(field);
        	if (index >= 0)
        	{
        		fieldIndexes.put(field, index);
        		m_Logger.info("Index of field " + field + " is " + index);
        	}
        }

        return fieldIndexes;
    }

    /**
     * Create indicies of the output fields.
     * This is the time field and all the fields configured for analysis
     * and the control field.
     * Time is the first field and the last is the control field
     *
     * @param header
     * @param inputFields
     * @return
     */
    protected Map<String, Integer> outputFieldIndicies()
    {
    	Map<String, Integer> fieldIndexes = new HashMap<String, Integer>();

    	// time field
    	fieldIndexes.put(m_DataDescription.getTimeField(), TIME_FIELD_OUT_INDEX);

    	int index = 1;
    	List<String> analysisFields = m_AnalysisConfig.analysisFields();
    	Collections.sort(analysisFields);

    	for (String field : m_AnalysisConfig.analysisFields())
    	{
    		fieldIndexes.put(field, index++);
    	}

    	// control field
    	fieldIndexes.put(LengthEncodedWriter.CONTROL_FIELD_NAME, index);

    	return fieldIndexes;
    }

    /**
     * For inputs that aren't transformed create a map of input index
     * to output index
     * @return
     */
    protected List<InputOutputMap> createInputOutputMap()
    {
    	// where no transform
    	List<InputOutputMap> inputOutputMap = new ArrayList<>();

    	int outIndex = TIME_FIELD_OUT_INDEX + 1;
    	for (String field : m_AnalysisConfig.analysisFields())
    	{
    		Integer inIndex = m_InFieldIndexes.get(field);
    		if (inIndex != null)
    		{
    			inputOutputMap.add(new InputOutputMap(inIndex, outIndex));
    		}

    		++outIndex;
    	}

    	return inputOutputMap;
    }


    /**
     * Check that all the fields are present in the header.
     * Either return true or throw a MissingFieldException
     *
     * Every input field should have an entry in <code>inputFieldIndicies</code>
     * otherwise the field cannnot be found.
     *
     * @param inputFields
     * @param inputFieldIndicies
     * @param header
     * @return True
     * @throws MissingFieldException
     */
    protected abstract boolean checkForMissingFields(Collection<String> inputFields,
    										Map<String, Integer> inputFieldIndicies,
    										String [] header)
    throws MissingFieldException;



    /**
     * Input and output array indexes map
     */
    protected class InputOutputMap
    {
    	int m_Input;
    	int m_Output;

    	public InputOutputMap(int in, int out)
    	{
    		m_Input = in;
    		m_Output = out;
    	}
    }

}
