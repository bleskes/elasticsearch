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
import java.util.Date;
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
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigs;
import com.prelert.transforms.DependencySorter;
import com.prelert.transforms.Transform;
import com.prelert.transforms.Transform.TransformIndex;
import com.prelert.transforms.Transform.TransformResult;
import com.prelert.transforms.TransformException;
import com.prelert.transforms.TransformFactory;
import com.prelert.transforms.date.DateFormatTransform;
import com.prelert.transforms.date.DateTransform;
import com.prelert.transforms.date.DoubleDateTransform;

public abstract class AbstractDataToProcessWriter implements DataToProcessWriter
{
    protected static final int TIME_FIELD_OUT_INDEX = 0;
    private static final int MS_IN_SECOND = 1000;

    protected final RecordWriter m_RecordWriter;
    protected final DataDescription m_DataDescription;
    protected final AnalysisConfig m_AnalysisConfig;
    protected final StatusReporter m_StatusReporter;
    protected final JobDataPersister m_JobDataPersister;
    protected final Logger m_Logger;
    protected final TransformConfigs m_TransformConfigs;

    protected List<Transform> m_DateInputTransforms;
    protected DateTransform m_DateTransform;
    protected List<Transform> m_PostDateTransforms;

    protected Map<String, Integer> m_InFieldIndexes;
    protected List<InputOutputMap> m_InputOutputMap;

    private String [] m_ScratchArea;
    private String [][] m_ReadWriteArea;

    // epoch in seconds
    private long m_LatestEpochMs;
    private long m_LatestEpochMsThisUpload;


    protected AbstractDataToProcessWriter(RecordWriter recordWriter,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            TransformConfigs transformConfigs, StatusReporter statusReporter,
            JobDataPersister jobDataPersister, Logger logger)
    {
        m_RecordWriter = Objects.requireNonNull(recordWriter);
        m_DataDescription = Objects.requireNonNull(dataDescription);
        m_AnalysisConfig = Objects.requireNonNull(analysisConfig);
        m_StatusReporter = Objects.requireNonNull(statusReporter);
        m_JobDataPersister = Objects.requireNonNull(jobDataPersister);
        m_Logger = Objects.requireNonNull(logger);
        m_TransformConfigs = Objects.requireNonNull(transformConfigs);

        m_PostDateTransforms = new ArrayList<>();
        m_DateInputTransforms = new ArrayList<>();
        Date date = statusReporter.getLatestRecordTime();
        m_LatestEpochMsThisUpload = 0;
        m_LatestEpochMs = 0;
        if (date != null)
        {
            m_LatestEpochMs  = date.getTime();
        }

        m_ReadWriteArea = new String[3][];
    }


    /**
     * Create the transforms. This must be called before {@linkplain #write(java.io.InputStream)}
     * even if no transforms are configured as it creates the
     * date transform and sets up the field mappings.<br>
     *
     * Finds the required input indicies in the <code>header</code>
     * and sets the mappings for the transforms so they know where
     * to read their inputs and write outputs.
     *
     * Transforms can be chained so some write their outputs to
     * a scratch area which is input to another transform
     *
     * Writes the header.
     *
     * @param header
     * @throws MissingFieldException
     * @throws IOException
     */
    public void buildTransformsAndWriteHeader(String [] header)
    throws MissingFieldException, IOException
    {
        Collection<String> inputFields = inputFields();
        m_InFieldIndexes = inputFieldIndicies(header, inputFields);
        checkForMissingFields(inputFields, m_InFieldIndexes, header);

        Map<String, Integer> outFieldIndexes = outputFieldIndicies();
        m_InputOutputMap = createInputOutputMap(m_InFieldIndexes);
        m_StatusReporter.setAnalysedFieldsPerRecord(m_AnalysisConfig.analysisFields().size());

        Map<String, Integer> scratchAreaIndicies = scratchAreaIndicies(inputFields, outputFields(),
                                                    m_DataDescription.getTimeField());
        m_ScratchArea = new String[scratchAreaIndicies.size()];
        m_ReadWriteArea[TransformFactory.SCRATCH_ARRAY_INDEX] = m_ScratchArea;


        m_JobDataPersister.setFieldMappings(m_AnalysisConfig.fields(),
                m_AnalysisConfig.byFields(), m_AnalysisConfig.overFields(),
                m_AnalysisConfig.partitionFields(), outFieldIndexes);


        buildDateTransform(scratchAreaIndicies, outFieldIndexes);

        List<TransformConfig> dateInputTransforms = DependencySorter.findDependencies(
                                    m_DataDescription.getTimeField(), m_TransformConfigs.getTransforms());


        TransformFactory transformFactory = new TransformFactory();
        for (TransformConfig config : dateInputTransforms)
        {
            Transform tr = transformFactory.create(config, m_InFieldIndexes, scratchAreaIndicies,
                    outFieldIndexes, m_Logger);
            m_DateInputTransforms.add(tr);
        }

        // get the transforms that don't input into the date
        List<TransformConfig> postDateTransforms = new ArrayList<>();
        for (TransformConfig tc : m_TransformConfigs.getTransforms())
        {
            if (dateInputTransforms.contains(tc) == false)
            {
                postDateTransforms.add(tc);
            }
        }

        postDateTransforms = DependencySorter.sortByDependency(postDateTransforms);
        for (TransformConfig config : postDateTransforms)
        {
            Transform tr = transformFactory.create(config, m_InFieldIndexes, scratchAreaIndicies,
                    outFieldIndexes, m_Logger);
            m_PostDateTransforms.add(tr);
        }

        writeHeader(outFieldIndexes);
    }

    protected void buildDateTransform(Map<String, Integer> scratchAreaIndicies,
            Map<String, Integer> outFieldIndexes)
    {
        boolean isDateFormatString = m_DataDescription.isTransformTime()
                && !m_DataDescription.isEpochMs();

        List<TransformIndex> readIndicies = new ArrayList<>();

        Integer index = m_InFieldIndexes.get(m_DataDescription.getTimeField());
        if (index != null)
        {
            readIndicies.add(new TransformIndex(TransformFactory.INPUT_ARRAY_INDEX, index));
        }
        else
        {
            index = outFieldIndexes.get(m_DataDescription.getTimeField());
            if (index != null)
            {
                // date field could also be an output field
                readIndicies.add(new TransformIndex(TransformFactory.OUTPUT_ARRAY_INDEX, index));
            }
            else if (scratchAreaIndicies.containsKey(m_DataDescription.getTimeField()))
            {
                index = scratchAreaIndicies.get(m_DataDescription.getTimeField());
                readIndicies.add(new TransformIndex(TransformFactory.SCRATCH_ARRAY_INDEX, index));
            }
            else
            {
                throw new IllegalStateException(
                        String.format("Transform input date field '%s' not found",
                                m_DataDescription.getTimeField()));
            }
        }


        List<TransformIndex> writeIndicies = new ArrayList<>();
        writeIndicies.add(new TransformIndex(TransformFactory.OUTPUT_ARRAY_INDEX,
                outFieldIndexes.get(m_DataDescription.getTimeField())));

        if (isDateFormatString)
        {
            m_DateTransform = new DateFormatTransform(m_DataDescription.getTimeFormat(),
                    readIndicies, writeIndicies, m_Logger);
        }
        else
        {
            m_DateTransform = new DoubleDateTransform(m_DataDescription.isEpochMs(),
                    readIndicies, writeIndicies, m_Logger);
        }

    }

    /**
     * Transform the input data and write to length encoded writer.<br>
     *
     * Fields that aren't transformed i.e. those in m_InputOutputMap must be
     * copied from input to output before this function is called.
     *
     * First all the transforms whose outputs the Date transform relies
     * on are executed then the date transform then the remaining transforms.
     *
     * @param input The record the transforms should read their input from. The contents should
     * align with the header parameter passed to {@linkplain #buildTransformsAndWriteHeader(String[])}
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
    protected boolean applyTransformsAndWrite(String [] input, String [] output,
                                        long numberOfFieldsRead)
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException, IOException
    {
        m_ReadWriteArea[TransformFactory.INPUT_ARRAY_INDEX] = input;
        m_ReadWriteArea[TransformFactory.OUTPUT_ARRAY_INDEX] = output;
        Arrays.fill(m_ReadWriteArea[TransformFactory.SCRATCH_ARRAY_INDEX], "");

        if (!applyTransforms(m_DateInputTransforms, numberOfFieldsRead))
        {
            return false;
        }

        try
        {
            m_DateTransform.transform(m_ReadWriteArea);
        }
        catch (TransformException e)
        {
            m_StatusReporter.reportDateParseError(numberOfFieldsRead);
            m_Logger.error(e.getMessage());
            return false;
        }

        long epochMs = m_DateTransform.epochMs();

        // Records have epoch seconds timestamp so compare for out of order in seconds
        if (epochMs / MS_IN_SECOND < m_LatestEpochMs / MS_IN_SECOND - m_AnalysisConfig.getLatency())
        {
            // out of order
            m_StatusReporter.reportOutOfOrderRecord(m_InFieldIndexes.size());

            if (epochMs > m_LatestEpochMsThisUpload)
            {
                // record this timestamp even if the record won't be processed
                m_LatestEpochMsThisUpload = epochMs;
                m_StatusReporter.reportLatestTimeIncrementalStats(m_LatestEpochMsThisUpload);
            }
            return false;
        }

        // Now do the rest of the transforms
        if (!applyTransforms(m_PostDateTransforms, numberOfFieldsRead))
        {
            return false;
        }

        m_LatestEpochMs = Math.max(m_LatestEpochMs, epochMs);
        m_LatestEpochMsThisUpload = m_LatestEpochMs;

        m_RecordWriter.writeRecord(output);
        m_JobDataPersister.persistRecord(epochMs / MS_IN_SECOND, output);
        m_StatusReporter.reportRecordWritten(numberOfFieldsRead, m_LatestEpochMs);

        return true;
    }

    /**
     * If false then the transform is excluded
     * @param transforms
     * @return
     */
    private boolean applyTransforms(List<Transform> transforms, long inputFieldCount)
    {
        for (Transform tr : transforms)
        {
            try
            {
                TransformResult result = tr.transform(m_ReadWriteArea);
                if (result == TransformResult.FAIL)
                {
                    m_StatusReporter.reportFailedTransform();
                }
                else if (result == TransformResult.EXCLUDE)
                {
                    m_StatusReporter.reportExcludedRecord(inputFieldCount);
                    return false;
                }
            }
            catch (TransformException e)
            {
                m_Logger.warn(e);
            }
        }

        return true;
    }


    /**
     * Write the header.
     * The header is created from the list of analysis input fields,
     * the time field and the control field
     *
     * @throws IOException
     */
    protected void writeHeader(Map<String, Integer> outFieldIndexes) throws IOException
    {
        //  header is all the analysis input fields + the time field + control field
        int numFields = outFieldIndexes.size();
        String[] record = new String[numFields];

        Iterator<Map.Entry<String, Integer>> itr = outFieldIndexes.entrySet().iterator();
        while (itr.hasNext())
        {
            Map.Entry<String, Integer> entry = itr.next();
            record[entry.getValue()] = entry.getKey();
        }

        // Write the header
        m_RecordWriter.writeRecord(record);
    }


    /**
     * Get all the expected input fields i.e. all the fields we
     * must see in the csv header.
     * = transform input fields + analysis fields that aren't a transform output
     * + the date field - the transform output field names
     *
     * @return
     */
    public final Collection<String> inputFields()
    {
        Set<String> requiredFields = new HashSet<>(m_AnalysisConfig.analysisFields());
        requiredFields.add(m_DataDescription.getTimeField());
        requiredFields.addAll(m_TransformConfigs.inputFieldNames());

        requiredFields.removeAll(m_TransformConfigs.outputFieldNames()); // inputs not in a transform

        return requiredFields;
    }

    /**
     * Find the indicies of the input fields from the header
     * @param header
     * @param inputFields The required input fields
     * @return
     */
    protected final Map<String, Integer> inputFieldIndicies(String[] header, Collection<String> inputFields)
    {
        List<String> headerList = Arrays.asList(header);  // TODO header could be empty

        Map<String, Integer> fieldIndexes = new HashMap<String, Integer>();

        for (String field : inputFields)
        {
            int index = headerList.indexOf(field);
            if (index >= 0)
            {
                fieldIndexes.put(field, index);
            }
        }

        return fieldIndexes;
    }

    public Map<String, Integer> getInputFieldIndicies()
    {
        return m_InFieldIndexes;
    }

    /**
     * This output fields are the time field and all the fields
     * configured for analysis
     * @return
     */
    public final Collection<String> outputFields()
    {
        List<String> outputFields = new ArrayList<>(m_AnalysisConfig.analysisFields());
        outputFields.add(m_DataDescription.getTimeField());

        return outputFields;
    }

    /**
     * Create indicies of the output fields.
     * This is the time field and all the fields configured for analysis
     * and the control field.
     * Time is the first field and the last is the control field
     *
     * @return
     */
    protected final Map<String, Integer> outputFieldIndicies()
    {
        Map<String, Integer> fieldIndexes = new HashMap<String, Integer>();

        // time field
        fieldIndexes.put(m_DataDescription.getTimeField(), TIME_FIELD_OUT_INDEX);

        int index = TIME_FIELD_OUT_INDEX + 1;
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
     * The number of fields used in the analysis +
     * the time and control fields
     * @return
     */
    public int outputFieldCount()
    {
        return m_AnalysisConfig.analysisFields().size() + 2;
    }

    protected Map<String, Integer> getOutputFieldIndicies()
    {
        return outputFieldIndicies();
    }


    /**
     * Find all the scratch area fields. These are those
     * that are input to a transform but are not written to the
     * output or read from input. i.e. for the case where a transforms
     * output is used exclusively by another transform
     *
     * @param inputFields Fields we expect in the header
     * @param outputFields Fields that are written to the analytics
     * @param dateTimeField
     * @return
     */
    protected final Map<String, Integer> scratchAreaIndicies(Collection<String> inputFields,
                                            Collection<String> outputFields, String dateTimeField)
    {
        Set<String> requiredFields = new HashSet<>(m_TransformConfigs.outputFieldNames());
        boolean dateTimeFieldIsTransformOutput = requiredFields.contains(dateTimeField);

        requiredFields.addAll(m_TransformConfigs.inputFieldNames());

        requiredFields.removeAll(inputFields);
        requiredFields.removeAll(outputFields);

        // date time is a output of a transform AND the input to the date time transform
        // so add it back into the scratch area
        if (dateTimeFieldIsTransformOutput)
        {
            requiredFields.add(dateTimeField);
        }

        int index = 0;
        Map<String, Integer> result = new HashMap<String, Integer>();
        for (String field : requiredFields)
        {
            result.put(field, new Integer(index++));
        }

        return result;
    }


    /**
     * For inputs that aren't transformed create a map of input index
     * to output index. This does not include the time or control fields
     * @param inFieldIndexes Map of field name -> index in the input array
     * @return
     */
    protected final List<InputOutputMap> createInputOutputMap(Map<String, Integer> inFieldIndexes)
    {
        // where no transform
        List<InputOutputMap> inputOutputMap = new ArrayList<>();

        int outIndex = TIME_FIELD_OUT_INDEX + 1;
        for (String field : m_AnalysisConfig.analysisFields())
        {
            Integer inIndex = inFieldIndexes.get(field);
            if (inIndex != null)
            {
                inputOutputMap.add(new InputOutputMap(inIndex, outIndex));
            }

            ++outIndex;
        }

        return inputOutputMap;
    }

    protected List<InputOutputMap> getInputOutputMap()
    {
        return m_InputOutputMap;
    }



    protected void getWideInputArray()
    {

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
