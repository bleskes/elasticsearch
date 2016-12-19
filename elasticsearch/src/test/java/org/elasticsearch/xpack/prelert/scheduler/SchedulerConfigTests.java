/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchedulerConfigTests extends AbstractSerializingTestCase<SchedulerConfig> {

    @Override
    protected SchedulerConfig createTestInstance() {
        SchedulerConfig.Builder builder = new SchedulerConfig.Builder(randomValidSchedulerId(), randomAsciiOfLength(10));
        builder.setIndexes(randomStringList(1, 10));
        builder.setTypes(randomStringList(1, 10));
        if (randomBoolean()) {
            builder.setQuery(Collections.singletonMap(randomAsciiOfLength(10), randomAsciiOfLength(10)));
        }
        boolean retrieveWholeSource = randomBoolean();
        if (retrieveWholeSource) {
            builder.setRetrieveWholeSource(randomBoolean());
        } else if (randomBoolean()) {
            builder.setScriptFields(Collections.singletonMap(randomAsciiOfLength(10), randomAsciiOfLength(10)));
        }
        if (randomBoolean()) {
            builder.setScrollSize(randomIntBetween(0, Integer.MAX_VALUE));
        }
        if (randomBoolean()) {
            builder.setAggregations(Collections.singletonMap(randomAsciiOfLength(10), randomAsciiOfLength(10)));
        }
        if (randomBoolean()) {
            builder.setFrequency(randomPositiveLong());
        }
        if (randomBoolean()) {
            builder.setQueryDelay(randomPositiveLong());
        }
        return builder.build();
    }

    private static List<String> randomStringList(int min, int max) {
        int size = scaledRandomIntBetween(min, max);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(randomAsciiOfLength(10));
        }
        return list;
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
    public void testQueryParsing() throws IOException {
        Logger logger = Loggers.getLogger(SchedulerConfigTests.class);

        String schedulerConfigStr = "{" + "\"scheduler_id\":\"scheduler1\"," + "\"job_id\":\"job1\"," + "\"indexes\":[\"farequote\"],"
                + "\"types\":[\"farequote\"]," + "\"query\":{\"match_all\":{} }" + "}";

        XContentParser parser = XContentFactory.xContent(schedulerConfigStr).createParser(schedulerConfigStr);
        SchedulerConfig schedulerConfig = SchedulerConfig.PARSER.apply(parser, () -> ParseFieldMatcher.STRICT).build();
        assertNotNull(schedulerConfig);

        Map<String, Object> query = schedulerConfig.getQuery();
        assertNotNull(query);

        String queryAsJson = XContentFactory.jsonBuilder().map(query).string();
        logger.info("Round trip of query is: " + queryAsJson);
        assertTrue(query.containsKey("match_all"));
    }

    public void testBuildAggregatedFieldList_GivenNoAggregations() {
        SchedulerConfig.Builder builder = new SchedulerConfig.Builder("scheduler1", "job1");
        builder.setIndexes(Arrays.asList("index"));
        builder.setTypes(Arrays.asList("type"));
        assertTrue(builder.build().buildAggregatedFieldList().isEmpty());
    }

    public void testAggsParse() throws IOException {
        Logger logger = Loggers.getLogger(SchedulerConfigTests.class);

        String aggregationsConfig = "{" + "\"scheduler_id\":\"scheduler1\"," + "\"job_id\":\"job1\"," + "\"indexes\":[\"farequote\"],"
                + "\"types\":[\"farequote\"]," + "\"query\":{\"match_all\":{} }," + "\"aggs\" : {" + "\"top_level_must_be_time\" : {"
                + "\"histogram\" : {" + "\"field\" : \"@timestamp\"," + "\"interval\" : 3600000" + "}," + "\"aggs\" : {"
                + "\"by_field_in_the_middle\" : { " + "\"terms\" : {" + "\"field\" : \"airline\"," + "\"size\" : 0" + "}," + "\"aggs\" : {"
                + "\"stats_last\" : {" + "\"avg\" : {" + "\"field\" : \"responsetime\"" + "}" + "}" + "} " + "}" + "}" + "}" + "}" + "}"
                + "}";

        String aggsConfig = "{" + "\"scheduler_id\":\"scheduler1\"," + "\"job_id\":\"job1\"," + "\"indexes\":[\"farequote\"],"
                + "\"types\":[\"farequote\"]," + "\"query\":{\"match_all\":{} }," + "\"aggs\" : {" + "\"top_level_must_be_time\" : {"
                + "\"histogram\" : {" + "\"field\" : \"@timestamp\"," + "\"interval\" : 3600000" + "}," + "\"aggs\" : {"
                + "\"by_field_in_the_middle\" : { " + "\"terms\" : {" + "\"field\" : \"airline\"," + "\"size\" : 0" + "}," + "\"aggs\" : {"
                + "\"stats_last\" : {" + "\"avg\" : {" + "\"field\" : \"responsetime\"" + "}" + "}" + "} " + "}" + "}" + "}" + "}" + "}"
                + "}";

        XContentParser parser = XContentFactory.xContent(aggregationsConfig).createParser(aggregationsConfig);
        SchedulerConfig aggregationsSchedulerConfig = SchedulerConfig.PARSER.apply(parser, () -> ParseFieldMatcher.STRICT).build();
        parser = XContentFactory.xContent(aggsConfig).createParser(aggsConfig);
        SchedulerConfig aggsSchedulerConfig = SchedulerConfig.PARSER.apply(parser, () -> ParseFieldMatcher.STRICT).build();
        assertNotNull(aggregationsSchedulerConfig);
        assertNotNull(aggsSchedulerConfig);
        assertEquals(aggregationsSchedulerConfig, aggsSchedulerConfig);

        Map<String, Object> aggs = aggsSchedulerConfig.getAggregations();
        assertNotNull(aggs);

        String aggsAsJson = XContentFactory.jsonBuilder().map(aggs).string();
        logger.info("Round trip of aggs is: " + aggsAsJson);
        assertTrue(aggs.containsKey("top_level_must_be_time"));

        List<String> aggregatedFieldList = aggsSchedulerConfig.buildAggregatedFieldList();
        assertEquals(3, aggregatedFieldList.size());
        assertEquals("@timestamp", aggregatedFieldList.get(0));
        assertEquals("airline", aggregatedFieldList.get(1));
        assertEquals("responsetime", aggregatedFieldList.get(2));
    }

    public void testFillDefaults() {
        SchedulerConfig.Builder expectedSchedulerConfig = new SchedulerConfig.Builder("scheduler1", "job1");
        expectedSchedulerConfig.setIndexes(Arrays.asList("index"));
        expectedSchedulerConfig.setTypes(Arrays.asList("type"));
        Map<String, Object> defaultQuery = new HashMap<>();
        defaultQuery.put("match_all", new HashMap<String, Object>());
        expectedSchedulerConfig.setQuery(defaultQuery);
        expectedSchedulerConfig.setQueryDelay(60L);
        expectedSchedulerConfig.setRetrieveWholeSource(false);
        expectedSchedulerConfig.setScrollSize(1000);
        SchedulerConfig.Builder defaultedSchedulerConfig = new SchedulerConfig.Builder("scheduler1", "job1");
        defaultedSchedulerConfig.setIndexes(Arrays.asList("index"));
        defaultedSchedulerConfig.setTypes(Arrays.asList("type"));

        assertEquals(expectedSchedulerConfig.build(), defaultedSchedulerConfig.build());
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
        sc2.setIndexes(Arrays.asList("blah", "di", "blah"));

        assertFalse(sc1.build().equals(sc2.build()));
        assertFalse(sc2.build().equals(sc1.build()));
    }

    public void testEquals_GivenDifferentTypes() {
        SchedulerConfig.Builder sc1 = createFullyPopulated();
        SchedulerConfig.Builder sc2 = createFullyPopulated();
        sc2.setTypes(Arrays.asList("blah", "di", "blah"));

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

    private static SchedulerConfig.Builder createFullyPopulated() {
        SchedulerConfig.Builder sc = new SchedulerConfig.Builder("scheduler1", "job1");
        sc.setIndexes(Arrays.asList("myIndex"));
        sc.setTypes(Arrays.asList("myType1", "myType2"));
        sc.setFrequency(60L);
        sc.setScrollSize(5000);
        Map<String, Object> query = new HashMap<>();
        query.put("foo", new HashMap<>());
        sc.setQuery(query);
        Map<String, Object> aggs = new HashMap<>();
        aggs.put("bar", new HashMap<>());
        sc.setAggregations(aggs);
        sc.setQueryDelay(90L);
        return sc;
    }

    public void testCheckValid_AllOk() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder("scheduler1", "job1");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setQueryDelay(90L);
        String json = "{ \"match_all\" : {} }";
        XContentParser parser = XContentFactory.xContent(json).createParser(json);
        conf.setQuery(parser.map());
        conf.setScrollSize(2000);
        conf.build();
    }

    public void testCheckValid_NoQuery() {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder("scheduler1", "job1");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        assertEquals(Collections.singletonMap("match_all", new HashMap<>()), conf.build().getQuery());
    }

    public void testCheckValid_GivenScriptFieldsNotWholeSource() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder("scheduler1", "job1");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        String json = "{ \"twiceresponsetime\" : { \"script\" : { \"lang\" : \"expression\", "
                + "\"inline\" : \"doc['responsetime'].value * 2\" } } }";
        XContentParser parser = XContentFactory.xContent(json).createParser(json);
        conf.setScriptFields(parser.map());
        conf.setRetrieveWholeSource(false);
        assertEquals(1, conf.build().getScriptFields().size());
    }

    public void testCheckValid_GivenScriptFieldsAndWholeSource() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder("scheduler1", "job1");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        String json = "{ \"twiceresponsetime\" : { \"script\" : { \"lang\" : \"expression\", "
                + "\"inline\" : \"doc['responsetime'].value * 2\" } } }";
        XContentParser parser = XContentFactory.xContent(json).createParser(json);
        conf.setScriptFields(parser.map());
        conf.setRetrieveWholeSource(true);
        expectThrows(IllegalArgumentException.class, conf::build);
    }

    public void testCheckValid_GivenNullIndexes() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder("scheduler1", "job1");
        expectThrows(IllegalArgumentException.class, () -> conf.setIndexes(null));
    }

    public void testCheckValid_GivenEmptyIndexes() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder("scheduler1", "job1");
        conf.setIndexes(Collections.emptyList());
        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class, conf::build);
        assertEquals(Messages.getMessage(Messages.SCHEDULER_CONFIG_INVALID_OPTION_VALUE, "indexes", "[]"), e.getMessage());
    }

    public void testCheckValid_GivenIndexesContainsOnlyNulls() throws IOException {
        List<String> indexes = new ArrayList<>();
        indexes.add(null);
        indexes.add(null);
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder("scheduler1", "job1");
        conf.setIndexes(indexes);
        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class, conf::build);
        assertEquals(Messages.getMessage(Messages.SCHEDULER_CONFIG_INVALID_OPTION_VALUE, "indexes", "[null, null]"), e.getMessage());
    }

    public void testCheckValid_GivenIndexesContainsOnlyEmptyStrings() throws IOException {
        List<String> indexes = new ArrayList<>();
        indexes.add("");
        indexes.add("");
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder("scheduler1", "job1");
        conf.setIndexes(indexes);
        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class, conf::build);
        assertEquals(Messages.getMessage(Messages.SCHEDULER_CONFIG_INVALID_OPTION_VALUE, "indexes", "[, ]"), e.getMessage());
    }

    public void testCheckValid_GivenNegativeQueryDelay() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder("scheduler1", "job1");
        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class, () -> conf.setQueryDelay(-10L));
        assertEquals(Messages.getMessage(Messages.SCHEDULER_CONFIG_INVALID_OPTION_VALUE, "query_delay", -10L), e.getMessage());
    }

    public void testCheckValid_GivenZeroFrequency() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder("scheduler1", "job1");
        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class, () -> conf.setFrequency(0L));
        assertEquals(Messages.getMessage(Messages.SCHEDULER_CONFIG_INVALID_OPTION_VALUE, "frequency", 0L), e.getMessage());
    }

    public void testCheckValid_GivenNegativeFrequency() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder("scheduler1", "job1");
        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class, () -> conf.setFrequency(-600L));
        assertEquals(Messages.getMessage(Messages.SCHEDULER_CONFIG_INVALID_OPTION_VALUE, "frequency", -600L), e.getMessage());
    }

    public void testCheckValid_GivenNegativeScrollSize() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder("scheduler1", "job1");
        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class, () -> conf.setScrollSize(-1000));
        assertEquals(Messages.getMessage(Messages.SCHEDULER_CONFIG_INVALID_OPTION_VALUE, "scroll_size", -1000L), e.getMessage());
    }

    public static String randomValidSchedulerId() {
        CodepointSetGenerator generator =  new CodepointSetGenerator("abcdefghijklmnopqrstuvwxyz".toCharArray());
        return generator.ofCodePointsLength(random(), 10, 10);
    }
}
