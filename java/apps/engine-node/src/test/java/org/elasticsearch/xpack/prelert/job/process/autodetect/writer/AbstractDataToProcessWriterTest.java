
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

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

import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectProcess;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MissingFieldException;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.AbstractDataToProcessWriter.InputOutputMap;
import org.elasticsearch.xpack.prelert.job.status.HighProportionOfBadTimestampsException;
import org.elasticsearch.xpack.prelert.job.status.OutOfOrderRecordsException;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfigs;
import org.elasticsearch.xpack.prelert.job.transform.TransformType;
import org.elasticsearch.xpack.prelert.transforms.Concat;
import org.elasticsearch.xpack.prelert.transforms.HighestRegisteredDomain;
import org.elasticsearch.xpack.prelert.transforms.RegexSplit;
import org.elasticsearch.xpack.prelert.transforms.StringTransform;
import org.elasticsearch.xpack.prelert.transforms.Transform;
import org.elasticsearch.xpack.prelert.transforms.Transform.TransformIndex;
import org.mockito.MockitoAnnotations;

/**
 * Testing methods of AbstractDataToProcessWriter but uses the concrete instances.
 * <p>
 * Asserts that the transforms have the right input and outputs.
 */
public class AbstractDataToProcessWriterTest extends ESTestCase {
    @Mock
    private AutodetectProcess autodetectProcess;
    @Mock
    private StatusReporter statusReporter;
    @Mock
    private Logger jobLogger;

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);
    }

    public void testInputFields_MulitpleInputsSingleOutput() throws MissingFieldException, IOException {

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


        AbstractDataToProcessWriter writer = new CsvDataToProcessWriter(true, autodetectProcess
                , dd, ac, transforms, statusReporter, jobLogger);

        Set<String> inputFields = new HashSet<>(writer.inputFields());
        assertEquals(4, inputFields.size());
        assertTrue(inputFields.contains("timeField"));
        assertTrue(inputFields.contains("value"));
        assertTrue(inputFields.contains("host"));
        assertTrue(inputFields.contains("metric"));


        String[] header = {"timeField", "metric", "host", "value"};
        writer.buildTransformsAndWriteHeader(header);
        List<Transform> trs = writer.postDateTransforms;
        assertEquals(1, trs.size());
        Transform tr = trs.get(0);

        List<TransformIndex> readIndexes = tr.getReadIndexes();
        assertEquals(readIndexes.get(0), new TransformIndex(0, 2));
        assertEquals(readIndexes.get(1), new TransformIndex(0, 1));

        List<TransformIndex> writeIndexes = tr.getWriteIndexes();
        assertEquals(writeIndexes.get(0), new TransformIndex(2, 1));


        Map<String, Integer> inputIndexes = writer.getInputFieldIndexes();
        assertEquals(4, inputIndexes.size());
        Assert.assertEquals(new Integer(0), inputIndexes.get("timeField"));
        Assert.assertEquals(new Integer(1), inputIndexes.get("metric"));
        Assert.assertEquals(new Integer(2), inputIndexes.get("host"));
        Assert.assertEquals(new Integer(3), inputIndexes.get("value"));

        Map<String, Integer> outputIndexes = writer.getOutputFieldIndexes();
        assertEquals(4, outputIndexes.size());
        Assert.assertEquals(new Integer(0), outputIndexes.get("timeField"));
        Assert.assertEquals(new Integer(1), outputIndexes.get("host-metric"));
        Assert.assertEquals(new Integer(2), outputIndexes.get("value"));
        Assert.assertEquals(new Integer(3), outputIndexes.get(LengthEncodedWriter.CONTROL_FIELD_NAME));


        List<InputOutputMap> inOutMaps = writer.getInputOutputMap();
        assertEquals(1, inOutMaps.size());
        assertEquals(inOutMaps.get(0).inputIndex, 3);
        assertEquals(inOutMaps.get(0).outputIndex, 2);
    }

    
    public void testInputFields_SingleInputMulitpleOutputs() throws MissingFieldException, IOException {

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


        AbstractDataToProcessWriter writer = new CsvDataToProcessWriter(true, autodetectProcess
                , dd, ac, transforms, statusReporter, jobLogger);

        Set<String> inputFields = new HashSet<>(writer.inputFields());

        assertEquals(3, inputFields.size());
        assertTrue(inputFields.contains("timeField"));
        assertTrue(inputFields.contains("value"));
        assertTrue(inputFields.contains("domain"));

        String[] header = {"timeField", "domain", "value"};
        writer.buildTransformsAndWriteHeader(header);
        List<Transform> trs = writer.postDateTransforms;
        assertEquals(1, trs.size());

        Map<String, Integer> inputIndexes = writer.getInputFieldIndexes();
        assertEquals(3, inputIndexes.size());
        Assert.assertEquals(new Integer(0), inputIndexes.get("timeField"));
        Assert.assertEquals(new Integer(1), inputIndexes.get("domain"));
        Assert.assertEquals(new Integer(2), inputIndexes.get("value"));

        Map<String, Integer> outputIndexes = writer.getOutputFieldIndexes();

        List<String> allOutputs = new ArrayList<>(TransformType.DOMAIN_SPLIT.defaultOutputNames());
        allOutputs.add("value");
        Collections.sort(allOutputs);  // outputs are in alphabetical order

        assertEquals(5, outputIndexes.size()); // time + control field + outputs
        Assert.assertEquals(new Integer(0), outputIndexes.get("timeField"));

        int count = 1;
        for (String f : allOutputs) {
            Assert.assertEquals(new Integer(count++), outputIndexes.get(f));
        }
        Assert.assertEquals(new Integer(allOutputs.size() + 1),
                outputIndexes.get(LengthEncodedWriter.CONTROL_FIELD_NAME));


        List<InputOutputMap> inOutMaps = writer.getInputOutputMap();
        assertEquals(1, inOutMaps.size());
        assertEquals(inOutMaps.get(0).inputIndex, 2);
        assertEquals(inOutMaps.get(0).outputIndex, allOutputs.indexOf("value") + 1);

        Transform tr = trs.get(0);
        assertEquals(tr.getReadIndexes().get(0), new TransformIndex(0, 1));

        List<TransformIndex> writeIndexes = new ArrayList<>();
        int[] outIndexes = new int[TransformType.DOMAIN_SPLIT.defaultOutputNames().size()];
        for (int i = 0; i < outIndexes.length; i++) {
            writeIndexes.add(new TransformIndex(2,
                    allOutputs.indexOf(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(i)) + 1));
        }
        assertEquals(writeIndexes, tr.getWriteIndexes());
    }


    /**
     * Only one output of the transform is used
     *
     * @throws MissingFieldException
     * @throws IOException
     */
    
    public void testInputFields_SingleInputMulitpleOutputs_OnlyOneOutputUsed()
            throws MissingFieldException, IOException {

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
        
        AbstractDataToProcessWriter writer = new CsvDataToProcessWriter(true, autodetectProcess, dd, ac, transforms, statusReporter, jobLogger);

        Set<String> inputFields = new HashSet<>(writer.inputFields());

        assertEquals(3, inputFields.size());
        assertTrue(inputFields.contains("timeField"));
        assertTrue(inputFields.contains("value"));
        assertTrue(inputFields.contains("domain"));

        String[] header = {"timeField", "domain", "value"};
        writer.buildTransformsAndWriteHeader(header);
        List<Transform> trs = writer.postDateTransforms;
        assertEquals(1, trs.size());

        Map<String, Integer> inputIndexes = writer.getInputFieldIndexes();
        assertEquals(3, inputIndexes.size());
        Assert.assertEquals(new Integer(0), inputIndexes.get("timeField"));
        Assert.assertEquals(new Integer(1), inputIndexes.get("domain"));
        Assert.assertEquals(new Integer(2), inputIndexes.get("value"));

        Map<String, Integer> outputIndexes = writer.getOutputFieldIndexes();

        List<String> allOutputs = new ArrayList<>();
        allOutputs.add(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        allOutputs.add("value");
        Collections.sort(allOutputs);  // outputs are in alphabetical order

        assertEquals(4, outputIndexes.size()); // time + control field + outputs
        Assert.assertEquals(new Integer(0), outputIndexes.get("timeField"));

        int count = 1;
        for (String f : allOutputs) {
            Assert.assertEquals(new Integer(count++), outputIndexes.get(f));
        }
        Assert.assertEquals(new Integer(allOutputs.size() + 1),
                outputIndexes.get(LengthEncodedWriter.CONTROL_FIELD_NAME));


        List<InputOutputMap> inOutMaps = writer.getInputOutputMap();
        assertEquals(1, inOutMaps.size());
        assertEquals(inOutMaps.get(0).inputIndex, 2);
        assertEquals(inOutMaps.get(0).outputIndex, allOutputs.indexOf("value") + 1);

        Transform tr = trs.get(0);
        assertEquals(tr.getReadIndexes().get(0), new TransformIndex(0, 1));

        TransformIndex ti = new TransformIndex(2,
                allOutputs.indexOf(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0)) + 1);
        assertEquals(tr.getWriteIndexes().get(0), ti);
    }


    /**
     * Only one output of the transform is used
     *
     * @throws MissingFieldException
     * @throws IOException
     */
    
    public void testBuildTransforms_ChainedTransforms()
            throws MissingFieldException, IOException {

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


        AbstractDataToProcessWriter writer = new CsvDataToProcessWriter(true, autodetectProcess
                , dd, ac, transforms, statusReporter, jobLogger);

        Set<String> inputFields = new HashSet<>(writer.inputFields());

        assertEquals(4, inputFields.size());
        assertTrue(inputFields.contains("date"));
        assertTrue(inputFields.contains("time"));
        assertTrue(inputFields.contains("value"));
        assertTrue(inputFields.contains("domain"));

        String[] header = {"date", "time", "domain", "value"};

        writer.buildTransformsAndWriteHeader(header);
        List<Transform> trs = writer.dateInputTransforms;
        assertEquals(1, trs.size());
        assertTrue(trs.get(0) instanceof Concat);

        trs = writer.postDateTransforms;
        assertEquals(1, trs.size());
        assertTrue(trs.get(0) instanceof HighestRegisteredDomain);

        Map<String, Integer> inputIndexes = writer.getInputFieldIndexes();
        assertEquals(4, inputIndexes.size());
        Assert.assertEquals(new Integer(0), inputIndexes.get("date"));
        Assert.assertEquals(new Integer(1), inputIndexes.get("time"));
        Assert.assertEquals(new Integer(2), inputIndexes.get("domain"));
        Assert.assertEquals(new Integer(3), inputIndexes.get("value"));
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
    
    public void testApplyTransforms_transformReturnsExclude()
            throws MissingFieldException, IOException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException {
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

        AbstractDataToProcessWriter writer = new CsvDataToProcessWriter(true, autodetectProcess
                , dd, ac, transforms, statusReporter, jobLogger);

        String[] header = {"datetime", "metric", "value"};

        writer.buildTransformsAndWriteHeader(header);

        // metricA is excluded
        String[] input = {"1", "metricA", "0"};
        String[] output = new String[3];


        assertFalse(writer.applyTransformsAndWrite(input, output, 3));

        verify(autodetectProcess, never()).writeRecord(output);
        verify(statusReporter, never()).reportRecordWritten(anyLong(), anyLong());
        verify(statusReporter, times(1)).reportExcludedRecord(3);

        // reset the call counts etc.
        Mockito.reset(statusReporter);

        // this is ok
        input = new String[]{"2", "metricB", "0"};
        String[] expectedOutput = {"2", null, null};
        assertTrue(writer.applyTransformsAndWrite(input, output, 3));


        verify(autodetectProcess, times(1)).writeRecord(expectedOutput);
        verify(statusReporter, times(1)).reportRecordWritten(3, 2000);
        verify(statusReporter, never()).reportExcludedRecord(anyLong());
    }


    
    public void testBuildTransforms_DateTransformsAreSorted() throws MissingFieldException, IOException {

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

        AbstractDataToProcessWriter writer = new CsvDataToProcessWriter(true, autodetectProcess,
                dd, ac, transforms, statusReporter, jobLogger);


        String[] header = {"date-somethingelse", "time", "type", "value"};

        writer.buildTransformsAndWriteHeader(header);

        // the date input transforms should be in this order
        List<Transform> trs = writer.dateInputTransforms;
        assertEquals(3, trs.size());
        assertTrue(trs.get(0) instanceof RegexSplit);
        assertTrue(trs.get(1) instanceof StringTransform);
        assertTrue(trs.get(2) instanceof Concat);
    }
}
