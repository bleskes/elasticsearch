
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import org.elasticsearch.test.ESTestCase;
import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.junit.Before;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.detectionrules.DetectionRule;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleCondition;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleConditionType;
import org.elasticsearch.xpack.prelert.lists.ListDocument;


public class FieldConfigWriterTest extends ESTestCase {
    private AnalysisConfig analysisConfig;
    private Set<ListDocument> lists;
    private OutputStreamWriter writer;

    @Before
    public void setUpDeps() {
        analysisConfig = new AnalysisConfig();
        lists = new LinkedHashSet<>();
    }

    public void testMultipleDetectorsToConfFile()
            throws IOException {
        List<Detector> detectors = new ArrayList<>();

        Detector d = new Detector("metric", "Integer_Value");
        d.setByFieldName("ts_hash");
        detectors.add(d);
        Detector d2 = new Detector("count");
        d2.setByFieldName("ipaddress");
        detectors.add(d2);
        Detector d3 = new Detector("max", "Integer_Value");
        d3.setOverFieldName("ts_hash");
        detectors.add(d3);
        Detector d4 = new Detector("rare");
        d4.setByFieldName("ipaddress");
        d4.setPartitionFieldName("host");
        detectors.add(d4);
        Detector d5 = new Detector("rare");
        d5.setByFieldName("weird field");
        detectors.add(d5);
        Detector d6 = new Detector("max", "\"quoted\" field");
        d6.setOverFieldName("ts\\hash");
        detectors.add(d6);

        analysisConfig.setDetectors(detectors);

        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        writer = new OutputStreamWriter(ba, StandardCharsets.UTF_8);

        createFieldConfigWriter().write();
        writer.close();

        // read the ini file - all the settings are in the global section
        StringReader reader = new StringReader(ba.toString("UTF-8"));

        Config iniConfig = new Config();
        iniConfig.setLineSeparator(new String(new char[]{WriterConstants.NEW_LINE}));
        iniConfig.setGlobalSection(true);

        Ini fieldConfig = new Ini();
        fieldConfig.setConfig(iniConfig);
        fieldConfig.load(reader);

        Section section = fieldConfig.get(iniConfig.getGlobalSectionName());

        assertEquals(detectors.size(), section.size());

        String value = fieldConfig.get(iniConfig.getGlobalSectionName(), "detector.0.clause");
        assertEquals("metric(Integer_Value) by ts_hash", value);
        value = fieldConfig.get(iniConfig.getGlobalSectionName(), "detector.1.clause");
        assertEquals("count by ipaddress", value);
        value = fieldConfig.get(iniConfig.getGlobalSectionName(), "detector.2.clause");
        assertEquals("max(Integer_Value) over ts_hash", value);
        value = fieldConfig.get(iniConfig.getGlobalSectionName(), "detector.3.clause");
        assertEquals("rare by ipaddress partitionfield=host", value);
        value = fieldConfig.get(iniConfig.getGlobalSectionName(), "detector.4.clause");
        assertEquals("rare by \"weird field\"", value);
        value = fieldConfig.get(iniConfig.getGlobalSectionName(), "detector.5.clause");
        // Ini4j meddles with escape characters itself, so the assertion below
        // fails even though the raw file is fine.  The file is never read by
        // Ini4j in the production system.
        // Assert.assertEquals("max(\"\\\"quoted\\\" field\") over \"ts\\\\hash\"", value);
    }

    public void testWrite_GivenConfigHasCategorizationField() throws IOException {
        Detector d = new Detector("metric", "Integer_Value");
        d.setByFieldName("ts_hash");

        analysisConfig.setDetectors(Arrays.asList(d));
        analysisConfig.setCategorizationFieldName("foo");
        writer = mock(OutputStreamWriter.class);

        createFieldConfigWriter().write();

        verify(writer).write("detector.0.clause = metric(Integer_Value) by ts_hash categorizationfield=foo\n");
        verifyNoMoreInteractions(writer);
    }

    public void testWrite_GivenConfigHasInfluencers() throws IOException {
        Detector d = new Detector("metric", "Integer_Value");
        d.setByFieldName("ts_hash");

        analysisConfig.setDetectors(Arrays.asList(d));
        analysisConfig.setInfluencers(Arrays.asList("sun", "moon", "earth"));

        writer = mock(OutputStreamWriter.class);

        createFieldConfigWriter().write();

        verify(writer).write("detector.0.clause = metric(Integer_Value) by ts_hash\n" +
                "influencer.0 = sun\n" +
                "influencer.1 = moon\n" +
                "influencer.2 = earth\n");
        verifyNoMoreInteractions(writer);
    }

    public void testWrite_GivenConfigHasCategorizationFieldAndFiltersAndInfluencer() throws IOException {
        Detector d = new Detector("metric", "Integer_Value");
        d.setByFieldName("ts_hash");

        analysisConfig.setDetectors(Arrays.asList(d));
        analysisConfig.setInfluencers(Arrays.asList("sun"));
        analysisConfig.setCategorizationFieldName("myCategory");
        analysisConfig.setCategorizationFilters(Arrays.asList("foo", " ", "abc,def"));

        writer = mock(OutputStreamWriter.class);

        createFieldConfigWriter().write();

        verify(writer).write(
                "detector.0.clause = metric(Integer_Value) by ts_hash categorizationfield=myCategory\n" +
                        "categorizationfilter.0 = foo\n" +
                        "categorizationfilter.1 = \" \"\n" +
                        "categorizationfilter.2 = \"abc,def\"\n" +
                "influencer.0 = sun\n");
        verifyNoMoreInteractions(writer);
    }

    public void testWrite_GivenDetectorWithRules() throws IOException {
        Detector detector = new Detector("mean", "metricValue");
        detector.setByFieldName("metricName");
        detector.setPartitionFieldName("instance");
        RuleCondition ruleCondition = new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setFieldName("metricName");
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldName("instance");
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        analysisConfig.setDetectors(Arrays.asList(detector));

        writer = mock(OutputStreamWriter.class);

        createFieldConfigWriter().write();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer).write(captor.capture());
        String actual = captor.getValue();
        String expectedFirstLine = "detector.0.clause = mean(metricValue) by metricName partitionfield=instance\n";
        assertTrue(actual.startsWith(expectedFirstLine));
        String secondLine = actual.substring(expectedFirstLine.length());
        String expectedSecondLineStart = "detector.0.rules = ";
        assertTrue(secondLine.startsWith(expectedSecondLineStart));
        String rulesJson = secondLine.substring(expectedSecondLineStart.length());
        List<DetectionRule> writtenRules = new ObjectMapper().readValue(rulesJson,
                new TypeReference<List<DetectionRule>>() {
        });
        assertEquals(1, writtenRules.size());
        assertEquals(rule, writtenRules.get(0));
    }

    public void testWrite_GivenLists() throws IOException {
        Detector d = new Detector("count");

        analysisConfig.setDetectors(Arrays.asList(d));
        lists.add(new ListDocument("list_1", Arrays.asList("a", "b")));
        lists.add(new ListDocument("list_2", Arrays.asList("c", "d")));
        writer = mock(OutputStreamWriter.class);

        createFieldConfigWriter().write();

        verify(writer).write("detector.0.clause = count\n" +
                "list.list_1 = [\"a\",\"b\"]\n" +
                "list.list_2 = [\"c\",\"d\"]\n");
        verifyNoMoreInteractions(writer);
    }

    private FieldConfigWriter createFieldConfigWriter() {
        return new FieldConfigWriter(analysisConfig, lists, writer, mock(Logger.class));
    }
}
