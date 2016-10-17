
package org.elasticsearch.xpack.prelert.job;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig.DataSource;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchedulerConfigTest extends AbstractSerializingTestCase<SchedulerConfig> {

    @Override
    protected SchedulerConfig createTestInstance() {
        DataSource dataSource = randomFrom(DataSource.values());
        SchedulerConfig.Builder builder = new SchedulerConfig.Builder(dataSource);
        switch (dataSource) {
            case FILE:
                builder.setFilePath(randomAsciiOfLength(10));
                builder.setTailFile(randomBoolean());
                break;
            case ELASTICSEARCH:
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return builder.build();
    }

    @Override
    protected Writeable.Reader<SchedulerConfig> instanceReader() {
        return SchedulerConfig::new;
    }

    @Override
    protected SchedulerConfig parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return SchedulerConfig.PARSER.apply(parser, () -> matcher).build();
    }

    /**
     * Test parsing of the opaque {@link SchedulerConfig#getQuery()} object
     */
    public void testAnalysisConfigRequiredFields()
            throws IOException {
        Logger logger = Loggers.getLogger(SchedulerConfigTest.class);

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

        SchedulerConfig.Builder schedulerConfig = jobConfig.getSchedulerConfig();
        assertNotNull(schedulerConfig);

        Map<String, Object> query = schedulerConfig.getQuery();
        assertNotNull(query);

        String queryAsJson = new ObjectMapper().writeValueAsString(query);
        logger.info("Round trip of query is: " + queryAsJson);
        assertTrue(query.containsKey("match_all"));
    }


    public void testBuildAggregatedFieldList_GivenNoAggregations() {
        assertTrue(new SchedulerConfig.Builder(DataSource.ELASTICSEARCH).build().buildAggregatedFieldList().isEmpty());
    }

    /**
     * Test parsing of the opaque {@link SchedulerConfig#getAggs()} object
     */

    public void testAggsParse()
            throws IOException {
        Logger logger = Loggers.getLogger(SchedulerConfigTest.class);

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

        SchedulerConfig schedulerConfig = jobConfig.getSchedulerConfig().build();
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
        SchedulerConfig.Builder schedulerConfig = new SchedulerConfig.Builder(DataSource.FILE);

        SchedulerConfig.Builder expectedSchedulerConfig = new SchedulerConfig.Builder(DataSource.FILE);
        expectedSchedulerConfig.setTailFile(false);

        assertEquals(expectedSchedulerConfig.build(), schedulerConfig.build());
    }


    public void testFillDefaults_GivenDataSourceIsElasticsearchAndNothingToFill() {
        SchedulerConfig.Builder originalSchedulerConfig = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        originalSchedulerConfig.setQuery(new HashMap<>());
        originalSchedulerConfig.setQueryDelay(30L);
        originalSchedulerConfig.setRetrieveWholeSource(true);
        originalSchedulerConfig.setScrollSize(2000);

        SchedulerConfig.Builder defaultedSchedulerConfig = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        defaultedSchedulerConfig.setQuery(new HashMap<>());
        defaultedSchedulerConfig.setQueryDelay(30L);
        defaultedSchedulerConfig.setRetrieveWholeSource(true);
        defaultedSchedulerConfig.setScrollSize(2000);

        assertEquals(originalSchedulerConfig.build(), defaultedSchedulerConfig.build());
    }


    public void testFillDefaults_GivenDataSourceIsElasticsearchAndDefaultsAreApplied() {
        SchedulerConfig.Builder expectedSchedulerConfig = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        Map<String, Object> defaultQuery = new HashMap<>();
        defaultQuery.put("match_all", new HashMap<String, Object>());
        expectedSchedulerConfig.setQuery(defaultQuery);
        expectedSchedulerConfig.setQueryDelay(60L);
        expectedSchedulerConfig.setRetrieveWholeSource(false);
        expectedSchedulerConfig.setScrollSize(1000);

        SchedulerConfig.Builder defaultedSchedulerConfig = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);

        assertEquals(expectedSchedulerConfig.build(), defaultedSchedulerConfig.build());
    }


    public void testEquals_GivenDifferentClass() {
        assertFalse(new SchedulerConfig.Builder(DataSource.FILE).build().equals("a string"));
    }


    public void testEquals_GivenSameRef() {
        SchedulerConfig schedulerConfig = new SchedulerConfig.Builder(DataSource.FILE).build();
        assertTrue(schedulerConfig.equals(schedulerConfig));
    }


    public void testEquals_GivenEqual() {
        SchedulerConfig.Builder b1 = createFullyPopulated();
        SchedulerConfig.Builder b2 = createFullyPopulated();

        SchedulerConfig sc1 = b1.build();
        SchedulerConfig sc2 = b2.build();
        assertTrue(sc1.equals(sc2));
        assertTrue(sc2.equals(sc1));
        assertEquals(sc1.hashCode(), sc2.hashCode());
    }


    public void testEquals_GivenDifferentBaseUrl() {
        SchedulerConfig.Builder b1 = createFullyPopulated();
        SchedulerConfig.Builder b2 = createFullyPopulated();
        b2.setBaseUrl("http://localhost:8081");

        SchedulerConfig sc1 = b1.build();
        SchedulerConfig sc2 = b2.build();
        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentQueryDelay() {
        SchedulerConfig.Builder b1 = createFullyPopulated();
        SchedulerConfig.Builder b2 = createFullyPopulated();
        b2.setQueryDelay(120L);

        SchedulerConfig sc1 = b1.build();
        SchedulerConfig sc2 = b2.build();
        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentScrollSize() {
        SchedulerConfig.Builder b1 = createFullyPopulated();
        SchedulerConfig.Builder b2 = createFullyPopulated();
        b2.setScrollSize(1);

        SchedulerConfig sc1 = b1.build();
        SchedulerConfig sc2 = b2.build();
        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentFrequency() {
        SchedulerConfig.Builder b1 = createFullyPopulated();
        SchedulerConfig.Builder b2 = createFullyPopulated();
        b2.setFrequency(120L);

        SchedulerConfig sc1 = b1.build();
        SchedulerConfig sc2 = b2.build();
        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentIndexes() {
        SchedulerConfig.Builder sc1 = createFullyPopulated();
        SchedulerConfig.Builder sc2 = createFullyPopulated();
        sc2.setIndexes(Arrays.asList("thisOtherCrazyIndex"));

        assertFalse(sc1.build().equals(sc2.build()));
        assertFalse(sc2.build().equals(sc1.build()));
    }


    public void testEquals_GivenDifferentTypes() {
        SchedulerConfig.Builder sc1 = createFullyPopulated();
        SchedulerConfig.Builder sc2 = createFullyPopulated();
        sc2.setTypes(Arrays.asList("thisOtherCrazyType"));

        assertFalse(sc1.build().equals(sc2.build()));
        assertFalse(sc2.build().equals(sc1.build()));
    }


    public void testEquals_GivenDifferentQuery() {
        SchedulerConfig.Builder b1 = createFullyPopulated();
        SchedulerConfig.Builder b2 = createFullyPopulated();
        Map<String, Object> emptyQuery = new HashMap<>();
        b2.setQuery(emptyQuery);

        SchedulerConfig sc1 = b1.build();
        SchedulerConfig sc2 = b2.build();
        assertFalse(sc1.equals(sc2));
        assertFalse(sc2.equals(sc1));
    }


    public void testEquals_GivenDifferentAggregations() {
        SchedulerConfig.Builder sc1 = createFullyPopulated();
        SchedulerConfig.Builder sc2 = createFullyPopulated();
        Map<String, Object> emptyAggs = new HashMap<>();
        sc2.setAggregations(emptyAggs);

        assertFalse(sc1.build().equals(sc2.build()));
        assertFalse(sc2.build().equals(sc1.build()));
    }


    public void testEquals_GivenDifferentPath() {
        SchedulerConfig.Builder sc1 = createFullyPopulated();
        SchedulerConfig.Builder sc2 = createFullyPopulated();
        sc2.setFilePath("thisOtherCrazyPath");

        assertFalse(sc1.build().equals(sc2.build()));
        assertFalse(sc2.build().equals(sc1.build()));
    }


    public void testEquals_GivenDifferentTail() {
        SchedulerConfig.Builder sc1 = createFullyPopulated();
        SchedulerConfig.Builder sc2 = createFullyPopulated();
        sc2.setTailFile(false);

        assertFalse(sc1.build().equals(sc2.build()));
        assertFalse(sc2.build().equals(sc1.build()));
    }

    private static SchedulerConfig.Builder createFullyPopulated() {
        SchedulerConfig.Builder sc = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        sc.setBaseUrl("http://localhost:8080");
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
