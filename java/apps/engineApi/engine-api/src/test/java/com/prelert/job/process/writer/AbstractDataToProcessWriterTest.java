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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.persistence.DummyJobDataPersister;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.writer.AbstractDataToProcessWriter.InputOutputMap;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.Condition;
import com.prelert.job.transform.Operator;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigs;
import com.prelert.job.transform.TransformType;
import com.prelert.transforms.Concat;
import com.prelert.transforms.HighestRegisteredDomain;
import com.prelert.transforms.RegexSplit;
import com.prelert.transforms.StringTransform;
import com.prelert.transforms.Transform;
import com.prelert.transforms.Transform.TransformIndex;

/**
 * Testing methods of AbstractDataToProcessWriter but uses the concrete instances.
 *
 * Asserts that the transforms have the right input and outputs.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractDataToProcessWriterTest
{
    @Mock private LengthEncodedWriter m_LengthEncodedWriter;
    @Mock private StatusReporter m_StatusReporter;
    @Mock private JobDataPersister m_DataPersister;
    @Mock private Logger m_Logger;


	@Test
	public void testInputFields_MulitpleInputsSingleOutput() throws MissingFieldException, IOException
	{
	    DummyJobDataPersister persister = new DummyJobDataPersister();

	    DataDescription dd = new DataDescription();
		dd.setTimeField("timeField");

		AnalysisConfig ac = new AnalysisConfig();
        Detector detector = new Detector();
        detector.setFieldName("value");
        detector.setByFieldName("host-metric");
        ac.setDetectors(Arrays.asList(detector));

        TransformConfig tc = new TransformConfig();
        tc.setInputs(Arrays.asList("host", "metric"));
        tc.setOutputs(Arrays.asList("host-metric"));
        tc.setTransform(TransformType.Names.CONCAT_NAME);

        TransformConfigs transforms = new TransformConfigs(Arrays.asList(tc));


		AbstractDataToProcessWriter writer = new CsvDataToProcessWriter(m_LengthEncodedWriter
				, dd, ac, transforms, m_StatusReporter, persister, m_Logger);

		Set<String> inputFields = new HashSet<>(writer.inputFields());
		assertEquals(4, inputFields.size());
		assertTrue(inputFields.contains("timeField"));
		assertTrue(inputFields.contains("value"));
		assertTrue(inputFields.contains("host"));
		assertTrue(inputFields.contains("metric"));


		String [] header = {"timeField", "metric", "host", "value"};
		writer.buildTransformsAndWriteHeader(header);
		List<Transform> trs = writer.m_PostDateTransforms;
		assertEquals(1, trs.size());
		Transform tr = trs.get(0);

        List<TransformIndex> readIndicies = tr.getReadIndicies();
        assertEquals(readIndicies.get(0), new TransformIndex(0, 2));
        assertEquals(readIndicies.get(1), new TransformIndex(0, 1));

        List<TransformIndex> writeIndicies = tr.getWriteIndicies();
        assertEquals(writeIndicies.get(0), new TransformIndex(2, 1));


		Map<String, Integer> inputIndicies = writer.getInputFieldIndicies();
		assertEquals(4, inputIndicies.size());
		Assert.assertEquals(new Integer(0), inputIndicies.get("timeField"));
		Assert.assertEquals(new Integer(1), inputIndicies.get("metric"));
		Assert.assertEquals(new Integer(2), inputIndicies.get("host"));
		Assert.assertEquals(new Integer(3), inputIndicies.get("value"));

		Map<String, Integer> outputIndicies = writer.getOutputFieldIndicies();
		assertEquals(4, outputIndicies.size());
		Assert.assertEquals(new Integer(0), outputIndicies.get("timeField"));
		Assert.assertEquals(new Integer(1), outputIndicies.get("host-metric"));
		Assert.assertEquals(new Integer(2), outputIndicies.get("value"));
		Assert.assertEquals(new Integer(3), outputIndicies.get(LengthEncodedWriter.CONTROL_FIELD_NAME));


		List<InputOutputMap> inOutMaps = writer.getInputOutputMap();
		assertEquals(1, inOutMaps.size());
		assertEquals(inOutMaps.get(0).m_Input, 3);
		assertEquals(inOutMaps.get(0).m_Output, 2);

		// The persister's field mappings are the same as the output indicies
        assertArrayEquals(new String[] {"value"}, persister.getFieldNames());
        assertArrayEquals(new int [] {2}, persister.getFieldMappings());
        assertArrayEquals(new int [] {1}, persister.getByFieldMappings());
        assertArrayEquals(new int [0], persister.getOverFieldMappings());
        assertArrayEquals(new int [0],  persister.getPartitionFieldMappings());
	}

	@Test
	public void testInputFields_SingleInputMulitpleOutputs() throws MissingFieldException, IOException
	{
	    DummyJobDataPersister persister = new DummyJobDataPersister();

		DataDescription dd = new DataDescription();
		dd.setTimeField("timeField");

		AnalysisConfig ac = new AnalysisConfig();
        Detector detector = new Detector();
        detector.setFieldName("value");
        detector.setByFieldName(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        detector.setOverFieldName(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(1));
        ac.setDetectors(Arrays.asList(detector));

        TransformConfig tc = new TransformConfig();
        tc.setInputs(Arrays.asList("domain"));
        tc.setTransform(TransformType.Names.DOMAIN_SPLIT_NAME);

        TransformConfigs transforms = new TransformConfigs(Arrays.asList(tc));


		AbstractDataToProcessWriter writer = new CsvDataToProcessWriter(m_LengthEncodedWriter
				, dd, ac, transforms, m_StatusReporter, persister, m_Logger);

		Set<String> inputFields = new HashSet<>(writer.inputFields());

		assertEquals(3, inputFields.size());
		assertTrue(inputFields.contains("timeField"));
		assertTrue(inputFields.contains("value"));
		assertTrue(inputFields.contains("domain"));

		String [] header = {"timeField", "domain", "value"};
		writer.buildTransformsAndWriteHeader(header);
		List<Transform> trs = writer.m_PostDateTransforms;
		assertEquals(1, trs.size());

		Map<String, Integer> inputIndicies = writer.getInputFieldIndicies();
		assertEquals(3, inputIndicies.size());
		Assert.assertEquals(new Integer(0), inputIndicies.get("timeField"));
		Assert.assertEquals(new Integer(1), inputIndicies.get("domain"));
		Assert.assertEquals(new Integer(2), inputIndicies.get("value"));

		Map<String, Integer> outputIndicies = writer.getOutputFieldIndicies();

		List<String> allOutputs = new ArrayList<>(TransformType.DOMAIN_SPLIT.defaultOutputNames());
		allOutputs.add("value");
		Collections.sort(allOutputs);  // outputs are in alphabetical order

		assertEquals(5, outputIndicies.size()); // time + control field + outputs
		Assert.assertEquals(new Integer(0), outputIndicies.get("timeField"));

		int count = 1;
		for (String f : allOutputs)
		{
			Assert.assertEquals(new Integer(count++), outputIndicies.get(f));
		}
		Assert.assertEquals(new Integer(allOutputs.size() + 1),
						outputIndicies.get(LengthEncodedWriter.CONTROL_FIELD_NAME));


		List<InputOutputMap> inOutMaps = writer.getInputOutputMap();
		assertEquals(1, inOutMaps.size());
		assertEquals(inOutMaps.get(0).m_Input, 2);
		assertEquals(inOutMaps.get(0).m_Output, allOutputs.indexOf("value") + 1);

		Transform tr = trs.get(0);
        assertEquals(tr.getReadIndicies().get(0), new TransformIndex(0, 1));

        List<TransformIndex> writeIndicies = new ArrayList<>();
		int [] outIndices = new int [TransformType.DOMAIN_SPLIT.defaultOutputNames().size()];
		for (int i = 0; i < outIndices.length; i++)
		{
			writeIndicies.add(new TransformIndex(2,
			                allOutputs.indexOf(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(i)) + 1));
		}
		assertEquals(writeIndicies, tr.getWriteIndicies());


        // The persister's field mappings are the same as the output indicies
        assertArrayEquals(new String[] {"value"}, persister.getFieldNames());
        assertArrayEquals(new int [] {outputIndicies.get("value")}, persister.getFieldMappings());
        assertArrayEquals(new int [] {outputIndicies.get(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0))},
                                    persister.getByFieldMappings());
        assertArrayEquals(new int [] {outputIndicies.get(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(1))},
                                    persister.getOverFieldMappings());
        assertArrayEquals(new int [0],  persister.getPartitionFieldMappings());
	}


	/**
	 * Only one output of the transform is used
	 * @throws MissingFieldException
	 * @throws IOException
	 */
	@Test
	public void testInputFields_SingleInputMulitpleOutputs_OnlyOneOutputUsed()
	throws MissingFieldException, IOException
	{
	    DummyJobDataPersister persister = new DummyJobDataPersister();

		DataDescription dd = new DataDescription();
		dd.setTimeField("timeField");

		AnalysisConfig ac = new AnalysisConfig();
        Detector detector = new Detector();
        detector.setFieldName("value");
        detector.setByFieldName(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        ac.setDetectors(Arrays.asList(detector));

        TransformConfig tc = new TransformConfig();
        tc.setInputs(Arrays.asList("domain"));
        tc.setTransform(TransformType.Names.DOMAIN_SPLIT_NAME);

        TransformConfigs transforms = new TransformConfigs(Arrays.asList(tc));


		AbstractDataToProcessWriter writer = new CsvDataToProcessWriter(m_LengthEncodedWriter
				, dd, ac, transforms, m_StatusReporter, persister, m_Logger);

		Set<String> inputFields = new HashSet<>(writer.inputFields());

		assertEquals(3, inputFields.size());
		assertTrue(inputFields.contains("timeField"));
		assertTrue(inputFields.contains("value"));
		assertTrue(inputFields.contains("domain"));

		String [] header = {"timeField", "domain", "value"};
		writer.buildTransformsAndWriteHeader(header);
		List<Transform> trs = writer.m_PostDateTransforms;
		assertEquals(1, trs.size());

		Map<String, Integer> inputIndicies = writer.getInputFieldIndicies();
		assertEquals(3, inputIndicies.size());
		Assert.assertEquals(new Integer(0), inputIndicies.get("timeField"));
		Assert.assertEquals(new Integer(1), inputIndicies.get("domain"));
		Assert.assertEquals(new Integer(2), inputIndicies.get("value"));

		Map<String, Integer> outputIndicies = writer.getOutputFieldIndicies();

		List<String> allOutputs = new ArrayList<>();
		allOutputs.add(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
		allOutputs.add("value");
		Collections.sort(allOutputs);  // outputs are in alphabetical order

		assertEquals(4, outputIndicies.size()); // time + control field + outputs
		Assert.assertEquals(new Integer(0), outputIndicies.get("timeField"));

		int count = 1;
		for (String f : allOutputs)
		{
			Assert.assertEquals(new Integer(count++), outputIndicies.get(f));
		}
		Assert.assertEquals(new Integer(allOutputs.size() + 1),
						outputIndicies.get(LengthEncodedWriter.CONTROL_FIELD_NAME));


		List<InputOutputMap> inOutMaps = writer.getInputOutputMap();
		assertEquals(1, inOutMaps.size());
		assertEquals(inOutMaps.get(0).m_Input, 2);
		assertEquals(inOutMaps.get(0).m_Output, allOutputs.indexOf("value") + 1);

		Transform tr = trs.get(0);
        assertEquals(tr.getReadIndicies().get(0), new TransformIndex(0, 1));

		TransformIndex ti = new TransformIndex(2,
		                allOutputs.indexOf(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0)) + 1);
		assertEquals(tr.getWriteIndicies().get(0), ti);


        // The persister's field mappings are the same as the output indicies
        assertArrayEquals(new String[] {"value"}, persister.getFieldNames());
        assertArrayEquals(new int [] {outputIndicies.get("value")}, persister.getFieldMappings());
        assertArrayEquals(new int [] {outputIndicies.get(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0))},
                persister.getByFieldMappings());
        assertArrayEquals(new int [0], persister.getOverFieldMappings());
        assertArrayEquals(new int [0],  persister.getPartitionFieldMappings());
	}



    /**
     * Only one output of the transform is used
     * @throws MissingFieldException
     * @throws IOException
     */
    @Test
    public void testBuildTransforms_ChainedTransforms()
    throws MissingFieldException, IOException
    {
        DummyJobDataPersister persister = new DummyJobDataPersister();

        DataDescription dd = new DataDescription();
        dd.setTimeField("datetime");

        AnalysisConfig ac = new AnalysisConfig();
        Detector detector = new Detector();
        detector.setFieldName("value");
        detector.setByFieldName(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        ac.setDetectors(Arrays.asList(detector));

        TransformConfig concatTc = new TransformConfig();
        concatTc.setInputs(Arrays.asList("date", "time"));
        concatTc.setOutputs(Arrays.asList("datetime"));
        concatTc.setTransform(TransformType.Names.CONCAT_NAME);

        TransformConfig hrdTc = new TransformConfig();
        hrdTc.setInputs(Arrays.asList("domain"));
        hrdTc.setTransform(TransformType.Names.DOMAIN_SPLIT_NAME);

        TransformConfigs transforms = new TransformConfigs(Arrays.asList(concatTc, hrdTc));


        AbstractDataToProcessWriter writer = new CsvDataToProcessWriter(m_LengthEncodedWriter
                , dd, ac, transforms, m_StatusReporter, persister, m_Logger);

        Set<String> inputFields = new HashSet<>(writer.inputFields());

        assertEquals(4, inputFields.size());
        assertTrue(inputFields.contains("date"));
        assertTrue(inputFields.contains("time"));
        assertTrue(inputFields.contains("value"));
        assertTrue(inputFields.contains("domain"));

        String [] header = {"date", "time", "domain", "value"};

        writer.buildTransformsAndWriteHeader(header);
        List<Transform> trs = writer.m_DateInputTransforms;
        assertEquals(1, trs.size());
        assertTrue(trs.get(0) instanceof Concat);

        trs = writer.m_PostDateTransforms;
        assertEquals(1, trs.size());
        assertTrue(trs.get(0) instanceof HighestRegisteredDomain);

        Map<String, Integer> inputIndicies = writer.getInputFieldIndicies();
        assertEquals(4, inputIndicies.size());
        Assert.assertEquals(new Integer(0), inputIndicies.get("date"));
        Assert.assertEquals(new Integer(1), inputIndicies.get("time"));
        Assert.assertEquals(new Integer(2), inputIndicies.get("domain"));
        Assert.assertEquals(new Integer(3), inputIndicies.get("value"));
    }


    /**
     * The exclude transform returns fail fatal meaning the record
     * shouldn't be processed.
     *
     * @throws MissingFieldException
     * @throws IOException
     * @throws OutOfOrderRecordsException
     * @throws HighProportionOfBadTimestampsException
     */
    @Test
    public void testApplyTransforms_transformReturnsExclude()
    throws MissingFieldException, IOException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        DataDescription dd = new DataDescription();
        dd.setTimeField("datetime");

        AnalysisConfig ac = new AnalysisConfig();
        Detector detector = new Detector();
        detector.setFieldName("value");
        detector.setByFieldName("metric");
        ac.setDetectors(Arrays.asList(detector));

        TransformConfig excludeConfig = new TransformConfig();
        excludeConfig.setInputs(Arrays.asList("metric"));
        excludeConfig.setCondition(new Condition(Operator.MATCH, "metricA"));
        excludeConfig.setTransform(TransformType.EXCLUDE.prettyName());

        TransformConfigs transforms = new TransformConfigs(Arrays.asList(excludeConfig));

        AbstractDataToProcessWriter writer = new CsvDataToProcessWriter(m_LengthEncodedWriter
                , dd, ac, transforms, m_StatusReporter, m_DataPersister, m_Logger);

        String [] header = {"datetime", "metric", "value"};

        writer.buildTransformsAndWriteHeader(header);

        // metricA is excluded
        String [] input = {"1", "metricA", "0"};
        String [] output = new String[3];


        assertFalse(writer.applyTransformsAndWrite(input, output, 3));

        verify(m_LengthEncodedWriter, never()).writeRecord(output);
        verify(m_StatusReporter, never()).reportRecordWritten(anyLong(), anyLong());
        verify(m_StatusReporter, times(1)).reportExcludedRecord(3);
        verify(m_DataPersister, never()).persistRecord(1, output);

        // reset the call counts etc.
        Mockito.reset(m_StatusReporter);

        // this is ok
        input = new String [] {"2", "metricB", "0"};
        String [] expectedOutput = {"2", null, null};
        assertTrue(writer.applyTransformsAndWrite(input, output, 3));


        verify(m_LengthEncodedWriter, times(1)).writeRecord(expectedOutput);
        verify(m_StatusReporter, times(1)).reportRecordWritten(3, 2000);
        verify(m_StatusReporter, never()).reportExcludedRecord(anyLong());
        verify(m_DataPersister, times(1)).persistRecord(2, expectedOutput);
    }


    @Test
    public void testBuildTransforms_DateTransformsAreSorted()
    throws MissingFieldException, IOException
    {
        DummyJobDataPersister persister = new DummyJobDataPersister();

        DataDescription dd = new DataDescription();
        dd.setTimeField("datetime");

        AnalysisConfig ac = new AnalysisConfig();
        Detector detector = new Detector();
        detector.setFieldName("value");
        detector.setByFieldName("type");
        ac.setDetectors(Arrays.asList(detector));

        TransformConfig concatTc = new TransformConfig();
        concatTc.setInputs(Arrays.asList("DATE", "time"));
        concatTc.setOutputs(Arrays.asList("datetime"));
        concatTc.setTransform(TransformType.Names.CONCAT_NAME);

        TransformConfig upperTc = new TransformConfig();
        upperTc.setInputs(Arrays.asList("date"));
        upperTc.setOutputs(Arrays.asList("DATE"));
        upperTc.setTransform(TransformType.Names.UPPERCASE_NAME);

        TransformConfig splitTc = new TransformConfig();
        splitTc.setInputs(Arrays.asList("date-somethingelse"));
        splitTc.setOutputs(Arrays.asList("date"));
        splitTc.setArguments(Arrays.asList("-"));
        splitTc.setTransform(TransformType.Names.SPLIT_NAME);


        TransformConfigs transforms = new TransformConfigs(Arrays.asList(upperTc, concatTc, splitTc));

        AbstractDataToProcessWriter writer = new CsvDataToProcessWriter(m_LengthEncodedWriter,
                   dd, ac, transforms, m_StatusReporter, persister, m_Logger);


        String [] header = {"date-somethingelse", "time", "type", "value"};

        writer.buildTransformsAndWriteHeader(header);

        // the date input transforms should be in this order
        List<Transform> trs = writer.m_DateInputTransforms;
        assertEquals(3, trs.size());
        assertTrue(trs.get(0) instanceof RegexSplit);
        assertTrue(trs.get(1) instanceof StringTransform);
        assertTrue(trs.get(2) instanceof Concat);
    }
}
