
package org.elasticsearch.xpack.prelert.job;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig.DataSource;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


public class SchedulerConfigTest extends ESTestCase {
    /**
     * Test parsing of the opaque {@link SchedulerConfig#getQuery()} object
     */

    public void testAnalysisConfigRequiredFields()
            throws IOException {
        BasicConfigurator.configure();
        Logger logger = Logger.getLogger(SchedulerConfigTest.class);

        String jobConfigStr =
                "{" +
                        "\"id\":\"farequote\"," +
                        "\"schedulerConfig\" : {" +
                        "\"dataSource\":\"ELASTICSEARCH\"," +
                        "\"baseUrl\":\"http://localhost:9200/\"," +
                        "\"indexes\":[\"farequote\"]," +
                        "\"types\":[\"farequote\"]," +
                        "\"query\":{\"match_all\":{} }" +
                        "}," +
                        "\"analysisConfig\" : {" +
                        "\"bucketSpan\":3600," +
                        "\"detectors\" :[{\"function\":\"metric\",\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]," +
                        "\"influencers\" :[\"airline\"]" +
                        "}," +
                        "\"dataDescription\" : {" +
                        "\"format\":\"ELASTICSEARCH\"," +
                        "\"timeField\":\"@timestamp\"," +
                        "\"timeFormat\":\"epoch_ms\"" +
                        "}" +
                        "}";

        ObjectReader objectReader = new ObjectMapper().readerFor(JobConfiguration.class);
        JobConfiguration jobConfig = objectReader.readValue(jobConfigStr);
        assertNotNull(jobConfig);

        SchedulerConfig schedulerConfig = jobConfig.getSchedulerConfig();
        assertNotNull(schedulerConfig);

        Map<String, Object> query = schedulerConfig.getQuery();
        assertNotNull(query);

        String queryAsJson = new ObjectMapper().writeValueAsString(query);
        logger.info("Round trip of query is: " + queryAsJson);
        assertTrue(query.containsKey("match_all"));
    }


    public void testBuildAggregatedFieldList_GivenNoAggregations() {
        assertTrue(new SchedulerConfig().buildAggregatedFieldList().isEmpty());
    }

    /**
     * Test parsing of the opaque {@link SchedulerConfig#getAggs()} object
     */

    public void testAggsParse()
            throws IOException {
        BasicConfigurator.configure();
        Logger logger = Logger.getLogger(SchedulerConfigTest.class);

        String jobConfigStr =
                "{" +
                        "\"id\":\"farequote\"," +
                        "\"schedulerConfig\" : {" +
                        "\"dataSource\":\"ELASTICSEARCH\"," +
                        "\"baseUrl\":\"http://localhost:9200/\"," +
                        "\"indexes\":[\"farequote\"]," +
                        "\"types\":[\"farequote\"]," +
                        "\"query\":{\"match_all\":{} }," +
                        "\"aggs\" : {" +
                        "\"top_level_must_be_time\" : {" +
                        "\"histogram\" : {" +
                        "\"field\" : \"@timestamp\"," +
                        "\"interval\" : 3600000" +
                        "}," +
                        "\"aggs\" : {" +
                        "\"by_field_in_the_middle\" : { " +
                        "\"terms\" : {" +
                        "\"field\" : \"airline\"," +
                        "\"size\" : 0" +
                        "}," +
                        "\"aggs\" : {" +
                        "\"stats_last\" : {" +
                        "\"avg\" : {" +
                        "\"field\" : \"responsetime\"" +
                        "}" +
                        "}" +
                        "} " +
                        "}" +
                        "}" +
                        "}" +
                        "}" +
                        "}," +
                        "\"analysisConfig\" : {" +
                        "\"bucketSpan\":3600," +
                        "\"detectors\" :[{\"function\":\"avg\",\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]," +
                        "\"influencers\" :[\"airline\"]" +
                        "}," +
                        "\"dataDescription\" : {" +
                        "\"format\":\"ELASTICSEARCH\"," +
                        "\"timeField\":\"@timestamp\"," +
                        "\"timeFormat\":\"epoch_ms\"" +
                        "}" +
                        "}";

        ObjectReader objectReader = new ObjectMapper().readerFor(JobConfiguration.class);
        JobConfiguration jobConfig = objectReader.readValue(jobConfigStr);
        assertNotNull(jobConfig);

        SchedulerConfig schedulerConfig = jobConfig.getSchedulerConfig();
        assertNotNull(schedulerConfig);

        Map<String, Object> aggs = schedulerConfig.getAggregationsOrAggs();
        assertNotNull(aggs);

        String aggsAsJson = new ObjectMapper().writeValueAsString(aggs);
        logger.info("Round trip of aggs is: " + aggsAsJson);
        assertTrue(aggs.containsKey("top_level_must_be_time"));

        List<String> aggregatedFieldList = schedulerConfig.buildAggregatedFieldList();
        assertEquals(3, aggregatedFieldList.size());
        assertEquals("@timestamp", aggregatedFieldList.get(0));
        assertEquals("airline", aggregatedFieldList.get(1));
        assertEquals("responsetime", aggregatedFieldList.get(2));
    }


    public void testFillDefaults_GivenDataSourceIsFile() {
        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.FILE);

        schedulerConfig.fillDefaults();

        SchedulerConfig expectedSchedulerConfig = new SchedulerConfig();
        expectedSchedulerConfig.setTailFile(false);

        assertEquals(expectedSchedulerConfig, schedulerConfig);
    }


    public void testFillDefaults_GivenDataSourceIsElasticsearchAndNothingToFill() {
        SchedulerConfig originalSchedulerConfig = new SchedulerConfig();
        originalSchedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        originalSchedulerConfig.setQuery(new HashMap<String, Object>());
        originalSchedulerConfig.setQueryDelay(30L);
        originalSchedulerConfig.setRetrieveWholeSource(true);
        originalSchedulerConfig.setScrollSize(2000);

        SchedulerConfig defaultedSchedulerConfig = new SchedulerConfig();
        defaultedSchedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        defaultedSchedulerConfig.setQuery(new HashMap<String, Object>());
        defaultedSchedulerConfig.setQueryDelay(30L);
        defaultedSchedulerConfig.setRetrieveWholeSource(true);
        defaultedSchedulerConfig.setScrollSize(2000);

        defaultedSchedulerConfig.fillDefaults();

        assertEquals(originalSchedulerConfig, defaultedSchedulerConfig);
    }


    public void testFillDefaults_GivenDataSourceIsElasticsearchAndDefaultsAreApplied() {
        SchedulerConfig expectedSchedulerConfig = new SchedulerConfig();
        expectedSchedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        Map<String, Object> defaultQuery = new HashMap<>();
        defaultQuery.put("match_all", new HashMap<String, Object>());
        expectedSchedulerConfig.setQuery(defaultQuery);
        expectedSchedulerConfig.setQueryDelay(60L);
        expectedSchedulerConfig.setRetrieveWholeSource(false);
        expectedSchedulerConfig.setScrollSize(1000);

        SchedulerConfig defaultedSchedulerConfig = new SchedulerConfig();
        defaultedSchedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        defaultedSchedulerConfig.setQuery(null);
        defaultedSchedulerConfig.setQueryDelay(null);

        defaultedSchedulerConfig.fillDefaults();

        assertEquals(expectedSchedulerConfig, defaultedSchedulerConfig);
    }


    public void testEquals_GivenDifferentClass() {
        assertFalse(new SchedulerConfig().equals("a string"));
    }


    public void testEquals_GivenSameRef() {
        SchedulerConfig schedulerConfig = new SchedulerConfig();
        assertTrue(schedulerConfig.equals(schedulerConfig));
    }


    public void testEquals_GivenEqual() {
        SchedulerConfig sc1 = createFullyPopulated();
        SchedulerConfig sc2 = createFullyPopulated();

        assertTrue(sc1.equals(sc2));
        assertTrue(sc2.equals(sc1));
        assertEquals(sc1.hashCode(), sc2.hashCode());
    }


    public void testEquals_GivenDifferentBaseUrl() {
        SchedulerConfig sc1 = createFullyPopulated();
        SchedulerConfig sc2 = createFullyPopulated();
        sc2.setBaseUrl("http://localhost:8081");

        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentDataSource() {
        SchedulerConfig sc1 = createFullyPopulated();
        SchedulerConfig sc2 = createFullyPopulated();
        sc2.setDataSource(DataSource.FILE);

        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentQueryDelay() {
        SchedulerConfig sc1 = createFullyPopulated();
        SchedulerConfig sc2 = createFullyPopulated();
        sc2.setQueryDelay(120L);

        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentScrollSize() {
        SchedulerConfig sc1 = createFullyPopulated();
        SchedulerConfig sc2 = createFullyPopulated();
        sc2.setScrollSize(1);

        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentFrequency() {
        SchedulerConfig sc1 = createFullyPopulated();
        SchedulerConfig sc2 = createFullyPopulated();
        sc2.setFrequency(120L);

        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentIndexes() {
        SchedulerConfig sc1 = createFullyPopulated();
        SchedulerConfig sc2 = createFullyPopulated();
        sc2.setIndexes(Arrays.asList("thisOtherCrazyIndex"));

        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentTypes() {
        SchedulerConfig sc1 = createFullyPopulated();
        SchedulerConfig sc2 = createFullyPopulated();
        sc2.setTypes(Arrays.asList("thisOtherCrazyType"));

        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentQuery() {
        SchedulerConfig sc1 = createFullyPopulated();
        SchedulerConfig sc2 = createFullyPopulated();
        Map<String, Object> emptyQuery = new HashMap<>();
        sc2.setQuery(emptyQuery);

        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentAggregations() {
        SchedulerConfig sc1 = createFullyPopulated();
        SchedulerConfig sc2 = createFullyPopulated();
        Map<String, Object> emptyAggs = new HashMap<>();
        sc2.setAggregations(emptyAggs);

        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentPath() {
        SchedulerConfig sc1 = createFullyPopulated();
        SchedulerConfig sc2 = createFullyPopulated();
        sc2.setFilePath("thisOtherCrazyPath");

        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentTail() {
        SchedulerConfig sc1 = createFullyPopulated();
        SchedulerConfig sc2 = createFullyPopulated();
        sc2.setTailFile(false);

        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }

    private static SchedulerConfig createFullyPopulated() {
        SchedulerConfig sc = new SchedulerConfig();
        sc.setBaseUrl("http://localhost:8080");
        sc.setDataSource(DataSource.ELASTICSEARCH);
        sc.setFrequency(60L);
        sc.setScrollSize(5000);
        sc.setIndexes(Arrays.asList("myIndex"));
        sc.setTypes(Arrays.asList("myType1", "myType2"));
        Map<String, Object> query = new HashMap<>();
        query.put("foo", new HashMap<>());
        sc.setQuery(query);
        Map<String, Object> aggs = new HashMap<>();
        aggs.put("bar", new HashMap<>());
        sc.setAggregations(aggs);
        sc.setQueryDelay(90L);
        sc.setFilePath("somePath");
        sc.setTailFile(true);
        return sc;
    }
}
