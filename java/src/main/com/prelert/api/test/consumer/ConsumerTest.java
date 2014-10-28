/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.api.test.consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.core4j.Enumerable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.ODataConsumers;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OObject;
import org.odata4j.core.OSimpleObject;
import org.odata4j.format.FormatType;
import org.odata4j.core.OLink;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmSimpleType;


/**
 * Integration and performance tests for the Prelert ODATA Query API. 
 * These tests are designed to run against the 'prelert_api_demo' database and will fail 
 * otherwise.
 *  </p>
 * By default the host URI is set to <code>http://localhost:8080/prelertApi/prelert.svc</code>
 * but if any command line argument is passed that value is used instead.
 * </p>
 * The following requirements are tested:
 * <table border=1>
 * <tr><th>Requirement</th><th>Test Case</th></tr>
 * <tr><td>Ability to query all activities in a given time interval</td>
 *      <td>{@link #testGetActivities(ODataConsumer)}, {@link #testGetActivitiesFilterByPeakEvidenceTime(ODataConsumer)},
 *      	{@link #testGetActivitiesFilterByFirstLastTimes(ODataConsumer)}, {@link #testGetActivitiesFilterUpdateTime(ODataConsumer)}</td></tr>
 * <tr><td>Ability to query activities related to a specific metric path in a given time interval</td>
 *      <td>{@link #testGetActvitiesByMetricPath(ODataConsumer)}</td></tr>
 * <tr><td>Each query shall ask for results to be delivered in a shallow or full level</td>
 *      <td>{@link #testGetActivities(ODataConsumer)}, {@link #testActivityRelatedMetricsInlineExpansion(ODataConsumer)},
 *      {@link #testActivitiesRelatedMetricsInlineExpansion(ODataConsumer)}</td></tr>
 * <tr><td>Count of the number of records the query would return</td>
 *      <td>{@link #testGetActivitiesCount(ODataConsumer)}</td></tr>
 * <tr><td>Sparse: return a subset of the full activity.</td>
 *      <td>{@link #testGetActivities(ODataConsumer)}, {@link #testGetActivity(ODataConsumer)}</td></tr>
 * <tr><td>The set of fields returned via the shallow queries should be configurable</td>
 *      <td>{@link #testSelectQueries(ODataConsumer)}</td></tr>      
 * <tr><td>Full: Return Activity Id's, Confidence Score, and full metric details that are part of each Activity for the query (heavier weight)</td>
 *      <td>{@link #testActivityToRelatedMetricsNavigation(ODataConsumer)}, {@link #testActivityRelatedMetricsInlineExpansion(ODataConsumer)},
 *      {@link #testActivitiesRelatedMetricsInlineExpansion(ODataConsumer)}</td></tr>
 * <tr><td>Queries will support Slice Views, for example give me the first 50 records for a given query.  The number of records in a slice should be configurable. The slices apply to either the Sparse or Full queries</td>
 *      <td>{@link #testGetActivities(ODataConsumer)}</td></tr>
 * <tr><td>Ability to pull full details on any given ActivityIDs</td>
 *      <td>{@link #testGetActivity(ODataConsumer)}, {@link #testGetRelatedMetric(ODataConsumer)}</td></tr>
 * 
 * </table>
 * <p/>
 * This test requires the <a href=https://code.google.com/p/odata4j/>Odata4J</a> library (Apache Licence 2.0).
 */
public class ConsumerTest 
{   
    /**
     * The Prelert namespace
     */
    public final static String NAMESPACE = "Prelert";
    
    /**
     * The Activities Entity Set name
     */
    public final static String ACTIVITIES_SET = "Activities";

    /**
     * The RelatedMetrics Entity Set name
     */
    public final static String RELATEDMETRICS_SET = "RelatedMetrics";
        
    /**
     * The Activity Entity type name
     */
    public final static String ACTIVITY_ENTITY = "Activity";
    
    /**
     * The RelatedMetric Entity type name
     */
    public final static String RELATEDMETRICS_ENTITY = "RelatedMetric";
    
    
    /**
     * The Activity -> RelatedMetrics Navigation property
     */
    public final static String ACTIVITYMETRICS_NAV = "ActivityMetrics";
    
    /**
     * The RelatedMetric -> Activity Navigation property
     */
    public final static String METRIC_ACTIVITY_NAV = "MetricActivity";
    
    /**
     * Earliest Activity time function name
     */
    public final static String EARLIEST_ACTIVITY_FUNC = "EarliestActivityTime"; 
    
    /**
     * Latest Activity time function name
     */
    public final static String LATEST_ACTIVITY_FUNC = "LatestActivityTime"; 
    
    
    private boolean m_TestFailed = false;
    
    /**
     * Main entry point, runs the test cases and performance test.
     * 
     * @param args 1 optional argument is expected which is the service URI.
     * If not set the default <code>http://localhost:8080/prelertApi/prelert.svc</code> is used.
     */
    public static void main(String[] args)
    {
        String serviceUri = "http://localhost:8080/prelertApi/prelert.svc";
        if (args.length > 0)
        {
            serviceUri = args[0];
        }
       
        
        ConsumerTest consumerTest = new ConsumerTest();
        consumerTest.runTests(serviceUri);
    }
    
    
    /**
     * Run the tests.
     * 
     * @param serviceUri
     * @return true if the tests ran successfully 
     */
    public boolean runTests(String serviceUri)
    {
        System.out.println("Running the Query API tests.");
        System.out.println("Using host " + serviceUri);
        
    	/* 
         * There is a bug in Odata4j where date objects are always parsed as
         * Xml even if the format type is Json. This only effects objs returned
         * by function calls (EarliestDateTime, LatestDateTime) not date properties
         * of entities. Set format type to ATOM to work around this bug or don't
         * use the EarliestDateTime/LatestDateTime functions.
         */
        ODataConsumer consumer = ODataConsumers.newBuilder(serviceUri).setFormatType(FormatType.ATOM).build();

        // Read and validate metadata
        testValidateMetaData(consumer.getMetadata());

        // Get single entity
        testGetActivity(consumer);
        testGetRelatedMetric(consumer);
        
        // Get entities
        testGetActivities(consumer);
        testGetActivitiesFilterByAnomalyScore(consumer);
        testGetActivitiesFilterByPeakEvidenceTime(consumer);
        testGetActivitiesFilterByFirstLastTimes(consumer);
        testGetActivitiesFilterUpdateTime(consumer);
        
        // Query Activities with RelatedMetics on a specified metric path 
        testGetActvitiesByMetricPath(consumer);
        testGetActvitiesLikeMetricPath(consumer);

        // count
        testGetActivitiesCount(consumer);

        // Navigation and inline expansion
        testActivityToRelatedMetricsNavigation(consumer);     
        testActivityRelatedMetricsInlineExpansion(consumer);
        testActivitiesRelatedMetricsInlineExpansion(consumer);
        
        testMetricToActivityNavigation(consumer); 

        // test queries containing combinations of and/or expressions
        testComplicatedFilters(consumer);
        
        // Select queries
        testSelectQueries(consumer);
        
        // Functions
        // These calls only work if the ODataConsumer format type = ATOM
        testEarliestTime(consumer);
        testLatestTime(consumer);
        
        // Timezone test
        testTimezones(consumer);

        // Run the performance test.
        performanceTest(consumer);

        if (m_TestFailed)
        {
        	System.out.println("TEST FAILED");
        }
        else
        {
        	System.out.println("TEST SUCCESSFUL");
        }
        
        return !m_TestFailed;
    }
    
    
    /**
     * Request the Entity Data Model and validate.
     * <p/>
     * Equivalent URI:
     * <ul>
     *      <li>http://localhost:8080/prelertApi/prelert.svc/$metadata</li>
     * </ul>
     * @param metaData
     */
    public void testValidateMetaData(EdmDataServices metaData)
    {
        System.out.println("Testing Metadata");
        
        testCondition(metaData != null, "No metadata returned");
        
        // Sets
        EdmEntitySet set = metaData.getEdmEntitySet(ACTIVITIES_SET);
        testCondition(set != null, "Missing " + ACTIVITIES_SET + " entity set.");
        set = metaData.getEdmEntitySet(RELATEDMETRICS_SET);
        testCondition(set != null, "Missing " + RELATEDMETRICS_SET + " entity set.");
        
        int count = 0;
        for (@SuppressWarnings("unused") EdmEntitySet s : metaData.getEntitySets())
        {
            count++;
        }
        testCondition(count == 4, "Wrong number of entity sets = " + count + ". Should be 4");
        
        // Entity types
        count = 0;
        boolean gotActivityType = false;
        boolean gotRelatedMetricType = false;
        for (EdmEntityType type : metaData.getEntityTypes())
        {
            if (type.getName().equals(ACTIVITY_ENTITY))
            {
                gotActivityType = true;
                
                for (EdmNavigationProperty navProp : type.getNavigationProperties())
                {
                    // Only one navigation for this type
                    if (navProp.getName().equals(ACTIVITYMETRICS_NAV) == false)
                    {
                        testCondition(false, "Unknown navigation " + navProp.getName());
                    }
                }
            }
            else if (type.getName().equals(RELATEDMETRICS_ENTITY))
            {
                gotRelatedMetricType = true;
                
                for (EdmNavigationProperty navProp : type.getNavigationProperties())
                {
                    // Only one navigation for this type
                    if (navProp.getName().equals(METRIC_ACTIVITY_NAV) == false)
                    {
                        testCondition(false, "Unknown navigation " + navProp.getName());
                    }
                }
            }
            
            count++;
        }
        
        testCondition(gotActivityType, "Missing Entity type " + ACTIVITY_ENTITY);
        testCondition(gotRelatedMetricType, "Missing Entity type " + RELATEDMETRICS_ENTITY);
        testCondition(count == 4, "Wrong number of entity types = " + count + ". Should be 4");
        
        // Functions
        EdmFunctionImport earliestFunc = metaData.findEdmFunctionImport(EARLIEST_ACTIVITY_FUNC);
        testCondition(earliestFunc != null, "Missing function " +  EARLIEST_ACTIVITY_FUNC);
        earliestFunc.getReturnType().equals(EdmSimpleType.DATETIME);
        
        EdmFunctionImport latestFunc = metaData.findEdmFunctionImport(LATEST_ACTIVITY_FUNC);
        testCondition(latestFunc != null, "Missing function " +  LATEST_ACTIVITY_FUNC);
        latestFunc.getReturnType().equals(EdmSimpleType.DATETIME);
    }
    
    
    /**
     * Test get a single Activity by its key.
     * <p/> 
     * Equivalent URI:
     * <ul>
     *      <li>http://localhost:8080/prelertApi/prelert.svc/Activities(26)</li>
     * </ul>
     * @param consumer
     */
    public void testGetActivity(ODataConsumer consumer)
    {
        System.out.println("Testing get Activity by key");
        
        OEntity ent = consumer.getEntity(ACTIVITIES_SET, OEntityKey.parse("26")).execute();
        
        verifyActivity26(ent);
    }
    
    
    private void verifyActivity26(OEntity ent)
    {
        DateTime clientTime = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T07:18:45+00:00");
        DateTime serverTime = (DateTime)ent.getProperty("PeakEvidenceTime").getValue();
        
        testCondition(clientTime.isEqual(serverTime), String.format("Incorrect PeakEvidenceTime client = %s, server = %s", clientTime, serverTime));
        
        // demo database doesn't have the correct values for first/last time -just check not null.
        DateTime firstTime = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T07:17:30+00:00");
        DateTime lastTime = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T07:19:00+00:00");
        DateTime updateTime = ISODateTimeFormat.dateTimeParser().parseDateTime("2013-03-18T14:01:44.970+00:00");
        
        DateTime testFirstTime = (DateTime)ent.getProperty("FirstEvidenceTime").getValue();
        DateTime testLastTime = (DateTime)ent.getProperty("LastEvidenceTime").getValue();
        DateTime testUpdateTime = (DateTime)ent.getProperty("UpdateTime").getValue();
        
        testCondition(testFirstTime.isEqual(firstTime), "Incorrect FirstEvidenceTime property") ;
        testCondition(testLastTime.isEqual(lastTime), "Incorrect LastEvidenceTime property") ;
        testCondition(testUpdateTime.isEqual(updateTime), "Incorrect UpdateTime property") ;
        
        testCondition(((Integer)ent.getProperty("Id").getValue()).equals(26), "Incorrect id") ;
        testCondition(((Integer)ent.getProperty("AnomalyScore").getValue()).equals(13), "Incorrect anomalyScore");
        testCondition(((String)ent.getProperty("SourceType").getValue()).equals("CA-APM"), "Incorrect sourceType");
        testCondition(((Integer)ent.getProperty("RelatedMetricCount").getValue()).equals(4), "Incorrect relatedMetricCount");
        testCondition(((Integer)ent.getProperty("HostCount").getValue()).equals(1), "Incorrect hostCount");
        testCondition(((String)ent.getProperty("SharedMetricPath").getValue())
                .equals("middleware|WebSphere|WebSphere_MQ|Backends|qx21 on server18 (DB2 DB)|SQL|Dynamic|Query"), "Incorrect sharedMetricPath");
        testCondition(((String)ent.getProperty("Description").getValue())
                .equals("4 anomalies on 1 source, 1 data type (CA-APM) with commonalities WebSphere_MQ, WebSphere, Backends, qx21 on server18 (DB2 DB), SQL, Dynamic, Query.  Most common ResourcePath5 values are SELECT CURRENT SQLID FROM SYSIBM.SYSDUMMY1 (x2), SELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1 (x2).  Source is middleware (x4)."),
        "Incorrect description");
    }
    
    
    /**
     * Get the number of Activities in the entity set.
     * <p/>
     * Equivalent URI:</p>
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities/$count</li>
     * </ul>
     * @param consumer
     */
    public void testGetActivitiesCount(ODataConsumer consumer)
    {
        System.out.println("Testing get Activities Count");
        
        int count = consumer.getEntitiesCount(ACTIVITIES_SET).execute();
        testCondition(count == 39, "Incorrect Activities entity count returned");
    }
    
    
    /**
     * Get Activities and test the <code>$orderby</code> and <code>$filter</code>
     * query options.
     * <ul>
     *  <li>Order by anomalyScore</li>  
     *  <li>Order by PeakEvidenceTime</li>  
     *  <li>Filter between 2 dates, order by anomalyScore</li>  
     *  <li>Top N results after a start date, order by PeakEvidenceTime</li>  
     *  <li>Top and Skip N results after a start date, order by PeakEvidenceTime</li>  
     * </ul>
     * <p/>
     * Equivalent URIs:
     * <em>Note the '+' character must be Url encoded as %2B</em>
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$orderby=anomalyScore</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$orderby=anomalyScore desc</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$orderby=PeakEvidenceTime</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$orderby=PeakEvidenceTime desc</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45%2B00:00') and (PeakEvidenceTime le datetimeoffset'2012-02-29T09:40:15%2B00:00')&$orderby=anomalyScore</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45%2B00:00'&$top=10&$orderby=PeakEvidenceTime</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45%2B00:00'&$top=10&$skip=10&$orderby=peakEvidenceTime</li>
     * </ul>
     * 
     * @param consumer
     */
    public void testGetActivities(ODataConsumer consumer)
    {
        System.out.println("Testing get Activities");
        
        int count = consumer.getEntitiesCount(ACTIVITIES_SET).execute();
        testCondition(count == 39, "Incorrect Activities entity count returned");
        
        Enumerable<OEntity> ents = consumer.getEntities(ACTIVITIES_SET).execute();
        testCondition(ents.count() == 39, "Incorrect number of entities returned");

        // order by
        ents = consumer.getEntities(ACTIVITIES_SET).orderBy("AnomalyScore").execute();
        testCondition(ents.count() == 39, "Incorrect number of entities returned");
        Integer previousScore = (Integer)ents.first().getProperty("AnomalyScore").getValue();
        for (OEntity ent : ents)
        {
            Integer score = (Integer)ent.getProperty("AnomalyScore").getValue();
            testCondition(score >= previousScore, "Activity anomalyScore order incorrect");
            previousScore = score;
        }
        
        ents = consumer.getEntities(ACTIVITIES_SET).orderBy("anomalyScore desc").execute();
        testCondition(ents.count() == 39, "Incorrect number of entities returned");
        previousScore = (Integer)ents.first().getProperty("AnomalyScore").getValue();
        for (OEntity ent : ents)
        {
            Integer score = (Integer)ent.getProperty("AnomalyScore").getValue();
            testCondition(score <= previousScore, "Activity anomalyScore descending order incorrect");  
            previousScore = score;
        }
        
        ents = consumer.getEntities(ACTIVITIES_SET).orderBy("PeakEvidenceTime").execute();
        testCondition(ents.count() == 39, "Incorrect number of entities returned");
        DateTime previousDate = (DateTime)ents.first().getProperty("PeakEvidenceTime").getValue();
        for (OEntity ent : ents)
        {
            DateTime date = (DateTime)ent.getProperty("PeakEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activity dates order incorrect");      
            previousDate = date;
        }
        
        ents = consumer.getEntities(ACTIVITIES_SET).orderBy("PeakEvidenceTime desc").execute();
        testCondition(ents.count() == 39, "Incorrect number of entities returned");
        previousDate = (DateTime)ents.first().getProperty("PeakEvidenceTime").getValue();
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("PeakEvidenceTime").getValue();
            testCondition(date.isBefore(previousDate) || date.isEqual(previousDate), "Activity PeakEvidenceTime descending order incorrect");      
            previousDate = date;
        }
        
        // filter by date and order by anomaly score
        ents = consumer.getEntities(ACTIVITIES_SET).filter("(PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45+00:00') and (PeakEvidenceTime le datetimeoffset'2012-02-29T09:40:15+00:00')").orderBy("anomalyScore").execute();
        testCondition(ents.count() == 9, "Incorrect number of entities returned (filter earliest/latest date). Got " + ents.count());
        previousScore = (Integer)ents.first().getProperty("AnomalyScore").getValue();
        for (OEntity ent : ents)
        {
            Integer score = (Integer)ent.getProperty("AnomalyScore").getValue();
            testCondition(score >= previousScore, "Activity anomalyScore order between 2 dates incorrect");
            previousScore = score;
        }
        
        
        // skip and top
        ents = consumer.getEntities(ACTIVITIES_SET).filter("PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45+00:00'").orderBy("PeakEvidenceTime").top(10).execute();
        testCondition(ents.count() == 10, "Incorrect number of entities returned (top 10). Got " + ents.count());
        previousDate = (DateTime)ents.first().getProperty("PeakEvidenceTime").getValue();
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("PeakEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activity dates between 2 dates order incorrect");      
            previousDate = date;
        }

        // get the next 10 Date and compare dates
        ents = consumer.getEntities(ACTIVITIES_SET).filter("PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45+00:00'").orderBy("PeakEvidenceTime").skip(10).top(10).execute();
        testCondition(ents.count() == 10, "Incorrect number of entities returned (skip 10, top 10). Got " + ents.count());
        
        DateTime firstDate = (DateTime)ents.first().getProperty("PeakEvidenceTime").getValue();
        DateTime sampleDate = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T10:15:45+00:00");
        testCondition(firstDate.isEqual(sampleDate), "Incorrect PeakEvidenceTime for first activity in second page");
        
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("PeakEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activity PeakEvidenceTime between 2 dates order incorrect");      
            previousDate = date;
        }
    }
    
    
    /**
     * Get Activities and test the 'AnomalyScore' filter option.
     * <p/>
     * Equivalent URIs:
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=AnomalyScore ge 50</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=AnomalyScore gt 22&$orderby=AnomalyScore</li>
     * </ul>
     * 
     * @param consumer
     */
    public void testGetActivitiesFilterByAnomalyScore(ODataConsumer consumer)
    {
    	System.out.println("Testing get Activities filter by AnomalyScore");
    	
    	Enumerable<OEntity> ents = consumer.getEntities(ACTIVITIES_SET).filter("AnomalyScore ge 50").execute(); 
    	testCondition(ents.count() > 0, "Filter by anomalyScore returned 0 Activities");
        for (OEntity ent : ents)
        {
            Integer score = (Integer)ent.getProperty("AnomalyScore").getValue();
            testCondition(score >= 50, "Filter Activity by anomalyScore is not >= 50");
        }
        
        // test >. There is an activity with score 22 in the dataset this shouldn't be returned.
    	ents = consumer.getEntities(ACTIVITIES_SET).filter("AnomalyScore gt 22").orderBy("AnomalyScore").execute(); 
    	testCondition(ents.count() > 0, "Filter by anomalyScore returned 0 Activities");
        for (OEntity ent : ents)
        {
            Integer score = (Integer)ent.getProperty("AnomalyScore").getValue();
            testCondition(score > 22, "Filter Activity by anomalyScore is not > 22");
        }
    }
    
    
    /**
     * Get Activities and test the PeakEvidenceTime filter option.
     * <p/>
     * Equivalent URIs:
     * <em>Note the '+' character must be Url encoded as %2B</em>
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45%2B00:00') and (PeakEvidenceTime le datetimeoffset'2012-02-29T09:40:15%2B00:00')</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(PeakEvidenceTime gt datetimeoffset'2012-02-29T07:18:45%2B00:00') and (PeakEvidenceTime le datetimeoffset'2012-02-29T09:40:15%2B00:00')</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(PeakEvidenceTime gt datetimeoffset'2012-02-29T07:18:45%2B00:00') and (PeakEvidenceTime lt datetimeoffset'2012-02-29T09:40:15%2B00:00')</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45%2B00:00') and (PeakEvidenceTime lt datetimeoffset'2012-02-29T09:40:15%2B00:00')</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=PeakEvidenceTime eq datetimeoffset'2012-02-29T07:18:45%2B00:00'</li>
     * </ul>
     * 
     * @param consumer
     */
    public void testGetActivitiesFilterByPeakEvidenceTime(ODataConsumer consumer)
    {
        System.out.println("Testing get Activities filter by PeakEvidenceTime");
        
        DateTime firstAct = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T07:18:45+00:00");
        DateTime lastAct = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T09:40:15+00:00");

        // filter by dates => and <=
        Enumerable<OEntity> ents = consumer.getEntities(ACTIVITIES_SET).filter("(PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45+00:00') and (PeakEvidenceTime le datetimeoffset'2012-02-29T09:40:15+00:00')").orderBy("PeakEvidenceTime").execute();
        testCondition(ents.count() == 9, "Incorrect number of entities returned (filter earliest/latest PeakEvidenceTime). Got " + ents.count());
        
        DateTime actDate = (DateTime)ents.first().getProperty("PeakEvidenceTime").getValue();
        testCondition(actDate.isEqual(firstAct), "Activity PeakEvidenceTime filter returned incorrect date");  

        actDate = (DateTime)ents.last().getProperty("PeakEvidenceTime").getValue();
        testCondition(actDate.isEqual(lastAct), "Activity PeakEvidenceTime filter returned incorrect date");  
       
        
        // filter by dates > and <=
        ents = consumer.getEntities(ACTIVITIES_SET).filter("(PeakEvidenceTime gt datetimeoffset'2012-02-29T07:18:45+00:00') and (PeakEvidenceTime le datetimeoffset'2012-02-29T09:40:15+00:00')").orderBy("PeakEvidenceTime").execute();
        testCondition(ents.count() == 8, "Incorrect number of entities returned (filter earliest/latest PeakEvidenceTime). Got " + ents.count());
        
    	actDate = (DateTime)ents.first().getProperty("PeakEvidenceTime").getValue();
        testCondition(actDate.isAfter(firstAct), "Activity PeakEvidenceTime filter returned incorrect date");  

        actDate = (DateTime)ents.last().getProperty("PeakEvidenceTime").getValue();
        testCondition(actDate.isEqual(lastAct), "Activity PeakEvidenceTime filter returned incorrect date");  
        

        // filter by dates > and <
        ents = consumer.getEntities(ACTIVITIES_SET).filter("(PeakEvidenceTime gt datetimeoffset'2012-02-29T07:18:45+00:00') and (PeakEvidenceTime lt datetimeoffset'2012-02-29T09:40:15+00:00')").orderBy("PeakEvidenceTime").execute();
        testCondition(ents.count() == 7, "Incorrect number of entities returned (filter earliest/latest date). Got " + ents.count());
        
    	actDate = (DateTime)ents.first().getProperty("PeakEvidenceTime").getValue();
        testCondition(actDate.isAfter(firstAct), "Activity PeakEvidenceTime filter returned incorrect date");  

        actDate = (DateTime)ents.last().getProperty("PeakEvidenceTime").getValue();
        testCondition(actDate.isBefore(lastAct), "Activity PeakEvidenceTime filter returned incorrect date");  
        

        // filter by dates => and <
        ents = consumer.getEntities(ACTIVITIES_SET).filter("(PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45+00:00') and (PeakEvidenceTime lt datetimeoffset'2012-02-29T09:40:15+00:00')").orderBy("PeakEvidenceTime").execute();
        testCondition(ents.count() == 8, "Incorrect number of entities returned (filter earliest/latest PeakEvidenceTime). Got " + ents.count());
        
        // ==
        ents = consumer.getEntities(ACTIVITIES_SET).filter("PeakEvidenceTime eq datetimeoffset'2012-02-29T07:18:45+00:00'").orderBy("PeakEvidenceTime").execute();
        testCondition(ents.count() == 1, "Incorrect number of entities returned (filter earliest/latest PeakEvidenceTime). Got " + ents.count());
        
    	actDate = (DateTime)ents.first().getProperty("PeakEvidenceTime").getValue();
        testCondition(actDate.isEqual(firstAct), "Activity PeakEvidenceTime filter returned incorrect date");  

        actDate = (DateTime)ents.last().getProperty("PeakEvidenceTime").getValue();
        testCondition(actDate.isBefore(lastAct), "Activity PeakEvidenceTime filter returned incorrect date");  
    }
    
    
    /**
     * Get Activities filtered by the FirstEvidenceTime & LastEvidenceTime 
     * and sort by PeakEvidenceTime, FirstEvidenceTime & LastEvidenceTime fields.
     * <p/>
     * Equivalent URIs:
     * <em>Note the '+' character must be Url encoded as %2B</em>
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(FirstEvidenceTime le datetimeoffset'2012-02-29T07:19:00%2B00:00') and (LastEvidenceTime ge datetimeoffset'2012-02-29T07:17:30+00:00')</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(LastEvidenceTime gt datetimeoffset'2012-02-29T07:19:00%2B00:00')&$orderby=LastEvidenceTime</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(LastEvidenceTime ge datetimeoffset'2012-02-29T07:19:00%2B00:00')&$orderby=lastEvidenceTime</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(FirstEvidenceTime ge datetimeoffset'2012-02-29T07:17:30%2B00:00')&$orderby=FirstEvidenceTime</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(FirstEvidenceTime le datetimeoffset'2012-02-29T10:16:30%2B00:00') and (LastEvidenceTime ge datetimeoffset'2012-02-29T10:16:00%2B00:00')</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=FirstEvidenceTime gt datetimeoffset'2012-02-29T09:00:00%2B00:00'</li>
     * </ul>
     * 
     * @param consumer
     */
    public void testGetActivitiesFilterByFirstLastTimes(ODataConsumer consumer)
    {
        System.out.println("Testing get Activities filter by First/Last times");
        
        DateTime firstTime = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T07:17:30+00:00");
        DateTime lastTime = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T07:19:00+00:00");

        // filter by dates => and <=
        Enumerable<OEntity> ents = consumer.getEntities(ACTIVITIES_SET).filter("(FirstEvidenceTime le datetimeoffset'2012-02-29T07:19:00+00:00') and (LastEvidenceTime ge datetimeoffset'2012-02-29T07:17:30+00:00')").execute();
        testCondition(ents.count() == 1, "Incorrect number of entities returned (filter First/Last EvidenceTime). Got " + ents.count());
        
        
        DateTime dateProp = (DateTime)ents.first().getProperty("FirstEvidenceTime").getValue();
        testCondition(dateProp.isEqual(firstTime), "Activity First/Last time filter returned incorrect date");  

        dateProp = (DateTime)ents.last().getProperty("LastEvidenceTime").getValue();
        testCondition(dateProp.isEqual(lastTime), "Activity First/Last time filter returned incorrect date");  
       
        // All activities after last time ordered by last time
        ents = consumer.getEntities(ACTIVITIES_SET).filter("(LastEvidenceTime gt datetimeoffset'2012-02-29T07:19:00+00:00')").orderBy("LastEvidenceTime").execute();
        testCondition(ents.count() > 0, "Incorrect number of entities returned (filter gt LastEvidenceTime). Got " + ents.count());
       
        dateProp = (DateTime)ents.first().getProperty("LastEvidenceTime").getValue();
        testCondition(dateProp.isAfter(lastTime), "Activity Last time gt filter returned incorrect date");  
        
        DateTime previousDate = dateProp;
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("LastEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activity LastEvidenceTime gt order incorrect");      
            previousDate = date;
        }
        
        
        // As previous ge rather than gt 
        ents = consumer.getEntities(ACTIVITIES_SET).filter("(LastEvidenceTime ge datetimeoffset'2012-02-29T07:19:00+00:00')").orderBy("LastEvidenceTime").execute();
        testCondition(ents.count() > 0, "Incorrect number of entities returned (filter gt LastEvidenceTime). Got " + ents.count());
       
        dateProp = (DateTime)ents.first().getProperty("LastEvidenceTime").getValue();
        testCondition(dateProp.isEqual(lastTime), "Activity Last time ge filter returned incorrect date");  
        
        previousDate = (DateTime)ents.first().getProperty("LastEvidenceTime").getValue();;
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("LastEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activity LastEvidenceTime ge order incorrect");      
            previousDate = date;
        }
        
        // get by first evidence time
        firstTime = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T07:54:00+00:00");
        ents = consumer.getEntities(ACTIVITIES_SET).filter("(FirstEvidenceTime ge datetimeoffset'2012-02-29T07:54:00+00:00')").orderBy("FirstEvidenceTime").execute();
        testCondition(ents.count() > 0, "Incorrect number of entities returned (filter ge FirstEvidenceTime). Got " + ents.count());
       
        dateProp = (DateTime)ents.first().getProperty("FirstEvidenceTime").getValue();
        testCondition(dateProp.isEqual(firstTime), "Activity first time ge filter returned incorrect date");  
        
        previousDate = (DateTime)ents.first().getProperty("FirstEvidenceTime").getValue();
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("FirstEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activity FirstEvidenceTime ge order incorrect");      
            previousDate = date;
        }
        
        // get a single activity
        ents = consumer.getEntities(ACTIVITIES_SET).filter("(FirstEvidenceTime le datetimeoffset'2012-02-29T10:16:30+00:00') and (LastEvidenceTime ge datetimeoffset'2012-02-29T10:16:00+00:00')").execute();
        testCondition(ents.count() ==1, "Incorrect number of entities returned filter by first and last time. Should be 1 got " + ents.count());
       
        // check default ordering - PeakEvidenceTime ascending
        ents = consumer.getEntities(ACTIVITIES_SET).filter("(FirstEvidenceTime gt datetimeoffset'2012-02-29T09:00:00+00:00')").execute();
        testCondition(ents.count() == 32, "Incorrect number of entities returned filter by first and last time. Should be 32 got " + ents.count());
        
        previousDate = (DateTime)ents.first().getProperty("PeakEvidenceTime").getValue();
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("PeakEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activity PeakEvidenceTime order incorrect");      
            previousDate = date;
        }
        
        // check default ordering - PeakEvidenceTime ascending
        ents = consumer.getEntities(ACTIVITIES_SET).filter("FirstEvidenceTime gt datetimeoffset'2012-02-29T09:00:00+00:00'").orderBy("lastevidencetime desc").execute(); 
        testCondition(ents.count() == 32, "Incorrect number of entities returned filter by first and last time. Should be 32 got " + ents.count());
        
        previousDate = (DateTime)ents.first().getProperty("LastEvidenceTime").getValue();
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("LastEvidenceTime").getValue();
            testCondition(date.isBefore(previousDate) || date.isEqual(previousDate), "Activity LastEvidenceTime desc order incorrect");      
            previousDate = date;
        }
        
        
        // check default ordering - PeakEvidenceTime ascending
        ents = consumer.getEntities(ACTIVITIES_SET).filter("FirstEvidenceTime gt datetimeoffset'2012-02-29T09:00:00+00:00' and lastEvidenceTime lt datetimeoffset'2012-02-29T10:00:00+00:00'").orderBy("firstevidencetime desc").execute(); 
        testCondition(ents.count() == 3, "Incorrect number of entities returned filter by first and last time. Should be 3 got " + ents.count());
        
        previousDate = (DateTime)ents.first().getProperty("FirstEvidenceTime").getValue();
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("FirstEvidenceTime").getValue();
            testCondition(date.isBefore(previousDate) || date.isEqual(previousDate), "Activity FirstEvidenceTime desc order incorrect");      
            previousDate = date;
        }
        
    }
    
    
    /**
     * Get Activities by the update time field.
     * <p/>
     * Equivalent URIs:
     * <em>Note the '+' character must be Url encoded as %2B</em>
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(UpdateTime ge datetimeoffset'2013-03-18T14:02:44+00')&$orderby=UpdateTime</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(UpdateTime ge datetimeoffset'2013-03-18T14:02:44+00')&$orderby=UpdateTime desc</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=((UpdateTime ge datetimeoffset'2013-03-18T14:02:44+00:00') and (UpdateTime le datetimeoffset'2013-03-18T14:03:30+00:00')&$orderby=UpdateTime</li>
     * </ul>
     * 
     * @param consumer
     */
    public void testGetActivitiesFilterUpdateTime(ODataConsumer consumer)
    {
        System.out.println("Testing get Activities filter by update time");

        // order by update time																				
        Enumerable<OEntity> ents = consumer.getEntities(ACTIVITIES_SET).filter("UpdateTime ge datetimeoffset'2013-03-18T14:02:44+00:00'").orderBy("updatetime").execute();
        testCondition(ents.count() > 0, "Filter by updatetime 0 entities returned");

        DateTime dateProp = (DateTime)ents.first().getProperty("UpdateTime").getValue();
        DateTime previousDate = dateProp;
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("UpdateTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activity UpdateTime order incorrect");      
            previousDate = date;
        }
    
        
        // order by update time desc
        ents = consumer.getEntities(ACTIVITIES_SET).filter("UpdateTime ge datetimeoffset'2013-03-18T14:02:44+00:00'").orderBy("updatetime desc").execute();
        testCondition(ents.count() > 0, "Filter by updatetime 0 entities returned");

        dateProp = (DateTime)ents.first().getProperty("UpdateTime").getValue();
        previousDate = dateProp;
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("UpdateTime").getValue();
            testCondition(date.isBefore(previousDate) || date.isEqual(previousDate), "Activity UpdateTime order desc incorrect");      
            previousDate = date;
        }
        
    
        // query between 2 dates
        ents = consumer.getEntities(ACTIVITIES_SET).filter("(UpdateTime ge datetimeoffset'2013-03-18T14:02:44+00:00') and (UpdateTime le datetimeoffset'2013-03-18T14:03:30+00:00')").orderBy("updatetime").execute();
        testCondition(ents.count() > 0, "Filter by updatetime 0 entities returned");

        DateTime firstTime = ISODateTimeFormat.dateTimeParser().parseDateTime("2013-03-18T14:02:44+00");
        DateTime lastTime = ISODateTimeFormat.dateTimeParser().parseDateTime("2013-03-18T14:03:30+00");
        
        dateProp = (DateTime)ents.first().getProperty("UpdateTime").getValue();
        testCondition(dateProp.isEqual(firstTime), "Activity UpdateTime ge filter returned incorrect first date");  
        
        dateProp = (DateTime)ents.last().getProperty("UpdateTime").getValue();
        testCondition(dateProp.isEqual(lastTime), "Activity UpdateTime ge filter returned incorrect last date");  
        
        dateProp = (DateTime)ents.first().getProperty("UpdateTime").getValue();
        previousDate = dateProp;
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("UpdateTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activity UpdateTime order incorrect");      
            previousDate = date;
        }
    
    }
    
    
    /**
     * Test get Activities by metric path. For each returned Activity get all its RelatedMetrics
     * and check one of them has the specified metric path. Tests 'and-ing' and 'or-ing' combinations
     * of metric paths.
     * <p/>
     * Equivalent URIs:
     * <em>Note the '|' character must be Url encoded as %7C</em>
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=metricPath eq 'middleware%7CWebSphere%7CWebSphere_MQ%7CBackends%7Cqx21 on server18 (DB2 DB)%7CSQL%7CDynamic%7CQuery%7CSELECT CURRENT SQLID FROM SYSIBM.SYSDUMMY1:Concurrent Invocations'</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(anomalyScore ge 20) and (metricPath eq 'middleware%7CWebSphere%7CWebSphere_MQ%7CBackends%7Cqx21 on server18 (DB2 DB)%7CSQL%7CDynamic%7CQuery%7CSELECT CURRENT SQLID FROM SYSIBM.SYSDUMMY1:Concurrent Invocations')&$orderby=anomalyScore</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=metricPath eq 'middleware%7CWebSphere%7CWebSphere_MQ%7CBackends%7Cqx21 on server18 (DB2 DB)%7CSQL%7CDynamic%7CQuery%7CSELECT CURRENT SQLID FROM SYSIBM.SYSDUMMY1:Concurrent Invocations'&$orderby=PeakEvidenceTime</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(metricPath eq 'middleware%7CWebSphere%7CWebSphere_MQ%7CBackends%7Cqx21 on server18 (DB2 DB)%7CSQL%7CDynamic%7CQuery%7CSELECT CURRENT SQLID FROM SYSIBM.SYSDUMMY1:Concurrent Invocations') and (PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45+00:00') and (PeakEvidenceTime le datetimeoffset'2012-02-29T09:40:15+00:00')&$orderby=PeakEvidenceTime</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(metricPath eq 'middleware%7CWebSphere%7CWebSphere_MQ%7CBackends%7Cqx21 on server18 (DB2 DB)%7CSQL%7CDynamic%7CQuery%7CSELECT CURRENT SQLID FROM SYSIBM.SYSDUMMY1:Concurrent Invocations') and metricpath eq middleware%7CWebSphere%7CWebSphere_MQ%7CBackends%7Cqx21 on server18 (DB2 DB)%7CSQL%7CDynamic%7CQuery%7CSELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1:Concurrent Invocations</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(metricPath eq 'middleware%7CWebSphere%7CWebSphere_MQ%7CBackends%7Cqx21 on server18 (DB2 DB)%7CSQL%7CDynamic%7CQuery%7CSELECT CURRENT SQLID FROM SYSIBM.SYSDUMMY1:Concurrent Invocations') or metricpath eq middleware%7CWebSphere%7CWebSphere_MQ%7CBackends%7Cqx21 on server18 (DB2 DB)%7CSQL%7CDynamic%7CQuery%7CSELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1:Concurrent Invocations</li>
     * </ul>
     * @param consumer
     */
    public void testGetActvitiesByMetricPath(ODataConsumer consumer)
    {
        System.out.println("Testing get Activities by Metric Path");
        
        String metricPath = "middleware|WebSphere|WebSphere_MQ|Backends|qx21 on server18 (DB2 DB)|SQL|Dynamic|Query|SELECT CURRENT SQLID FROM SYSIBM.SYSDUMMY1:Concurrent Invocations";
        
        Enumerable<OEntity> activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("metricPath eq '" + metricPath + "'").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities by MetricPath returned 0 Activities");
        
        for (OEntity activityOnPath : activitiesOnPath)
        {
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.equals(metricPath))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(foundPath, "Activity has no related metric on path");
        }   
        
        
        // filter by metric path and anomaly score GE
        int anomalyScore = 23;
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("(anomalyScore ge " + anomalyScore + ") and (metricPath eq '" + metricPath + "')").orderBy("anomalyScore").execute();
        testCondition(activitiesOnPath.count() == 2, "Get Activities by MetricPath & AnomalyScore ge " + anomalyScore + " returned != 2 Activities");
        
        for (OEntity activityOnPath : activitiesOnPath)
        {
        	int actAnomScore = (Integer)activityOnPath.getProperty("AnomalyScore").getValue();
        	testCondition(actAnomScore >= anomalyScore, "AnomalyScore = " +  actAnomScore + " is not >= " + anomalyScore); 
        	
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.equals(metricPath))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(foundPath, "Activity has no related metric on path");
        } 
        
        
        // filter by metric path and anomaly score GT, should be one less activity using GT
        anomalyScore = 23;
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("(anomalyScore gt " + anomalyScore + ") and (metricPath eq '" + metricPath + "')").orderBy("anomalyScore").execute();
        testCondition(activitiesOnPath.count() == 1, "Get Activities by MetricPath & AnomalyScore ge " + anomalyScore + " returned != 1 Activities");
        
        for (OEntity activityOnPath : activitiesOnPath)
        {
        	int actAnomScore = (Integer)activityOnPath.getProperty("AnomalyScore").getValue();
        	testCondition(actAnomScore >= anomalyScore, "AnomalyScore = " +  actAnomScore + " is not >= " + anomalyScore); 
        	
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.equals(metricPath))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(foundPath, "Activity has no related metric on path");
        } 
        
        
        // order by date
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("metricPath eq '" + metricPath + "'").orderBy("PeakEvidenceTime").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities by MetricPath orderBy PeakEvidenceTime returned 0 Activities");
        
        DateTime previousDate = (DateTime)activitiesOnPath.first().getProperty("PeakEvidenceTime").getValue();
        
        for (OEntity activityOnPath : activitiesOnPath)
        {
        	DateTime date = (DateTime)activityOnPath.getProperty("PeakEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activities on metric path PeakEvidenceTime order incorrect");      
            previousDate = date;
            
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.equals(metricPath))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(foundPath, "Activity (ordered by PeakEvidenceTime) has no related metric on path");
        }   
        
        
        // filter by dates => and <=
        DateTime firstActDate = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T07:18:45+00:00");
        DateTime lastActDate = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T09:40:15+00:00");

        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("(metricPath eq '" + metricPath + "') and (PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45+00:00') and (PeakEvidenceTime le datetimeoffset'2012-02-29T09:40:15Z')").orderBy("PeakEvidenceTime").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities by MetricPath filter by PeakEvidenceTime returned 0 Activities");
        
        DateTime actDate = (DateTime)activitiesOnPath.first().getProperty("PeakEvidenceTime").getValue();
        testCondition(actDate.isEqual(firstActDate) || actDate.isAfter(firstActDate), "Activity by MetricPath PeakEvidenceTime filter returned incorrect date");  

        actDate = (DateTime)activitiesOnPath.last().getProperty("PeakEvidenceTime").getValue();
        testCondition(actDate.isEqual(lastActDate) || actDate.isBefore(lastActDate), "Activity by MetricPath PeakEvidenceTime filter returned incorrect date");  
        
        previousDate = (DateTime)activitiesOnPath.first().getProperty("PeakEvidenceTime").getValue();
        for (OEntity activityOnPath : activitiesOnPath)
        {
        	DateTime date = (DateTime)activityOnPath.getProperty("PeakEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activities on metric path PeakEvidenceTime order incorrect");      
            previousDate = date;
            
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.equals(metricPath))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(foundPath, "Activity (filtered by PeakEvidenceTime) has no related metric on path");
        }  
        
        
        // metricpath and 
        String metricPath2 = "middleware|WebSphere|WebSphere_MQ|Backends|qx21 on server18 (DB2 DB)|SQL|Dynamic|Query|SELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1:Concurrent Invocations";
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("(metricPath eq '" + metricPath + "') and metricPath eq '" + metricPath2 + "'").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities by MetricPath AND returned 0 Activities");
        int andCount = activitiesOnPath.count();
        
        for (OEntity activityOnPath : activitiesOnPath)
        {
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath1 = false;
            boolean foundPath2 = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.equals(metricPath))
                {
                    foundPath1 = true;
                }
                else if (path.equals(metricPath2))
                {
                	foundPath2 = true;
                }
            }

            testCondition(foundPath1 && foundPath2, "Activity AND metric paths has no related metric on path");
        }  
        
        
        // metricpath or 
        metricPath2 = "middleware|WebSphere|WebSphere_MQ|Backends|qx21 on server18 (DB2 DB)|SQL|Dynamic|Query|SELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1:Concurrent Invocations";
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("(metricPath eq '" + metricPath + "') or metricPath eq '" + metricPath2 + "'").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities by MetricPath AND returned 0 Activities");
        
        int orCount = activitiesOnPath.count();
        testCondition(orCount > andCount, "Anded metric path query returned more activities than Or");
        
        
        for (OEntity activityOnPath : activitiesOnPath)
        {
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath1 = false;
            boolean foundPath2 = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.equals(metricPath))
                {
                    foundPath1 = true;
                    break;
                }
                else if (path.equals(metricPath2))
                {
                	foundPath2 = true;
                	break;
                }
            }

            testCondition(foundPath1 || foundPath2, "Activity AND metric paths has no related metric on path");
        }  
    }
    
    
    /**
     * Test query activities using SQL like matching on the metric paths.
     * Tests matching using both 'and' and 'or' conditions.
     * 
     * <p/>
     * Equivalent URIs:
     * <em>Note the '|' character must be Url encoded as %7C and '%' as %25</em>
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=MPQuery eq 'middleware%7CWebSphere%7CWebSphere!_MQ%7CBackends%7Cqx21 on server18 (DB2 DB)%7CSQL%7CDynamic%7CQuery%7CSELECT CURRENT SQLID FROM SYSIBM.SYSDUMMY1:Concurrent Invocations' and EscapeChar eq '!'</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=MPQuery eq '%25%7CBackends%7C%25'</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=EscapeChar eq '\' and (anomalyScore gt 20) and (MPQuery eq '%25%7CBackends%7C%25')&$orderby=anomalyScore</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=EscapeChar eq '\' and (MPQuery eq '%25WebSphere\_MQ%25')&$orderby=anomalyScore</li> 
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=EscapeChar eq '^' and ((MPQuery eq '%25%7CBackends%7C%25') or MPQuery eq '%25WebSphere^_MQ%25'))&$expand=ActivityMetrics</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=EscapeChar eq '^' and ((MPQuery eq '%25WebSphere^_MQ%25') or (MPQuery eq '%25%7CBackends%7C%25'))&$expand=ActivityMetrics</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=EscapeChar eq '^' and ((MPQuery eq '%25%7CBackends%7C%25') and MPQuery eq '%25WebSphere^_MQ%25'))&$expand=ActivityMetrics</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=EscapeChar eq '^' and ((MPQuery eq '%25WebSphere^_MQ%25') and (MPQuery eq '%25%7CBackends%7C%25'))&$expand=ActivityMetrics</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=MPQuery eq '%25WebSphere_MQ%25' and MPQuery eq '%25|Backends|%25' and MPQuery eq '%25:Average Response Time (ms)'</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=((MPQuery eq '%25WebSphere_MQ%25') or (MPQuery eq '%25%7CBackends%7C%25')) and (FirstEvidenceTime le datetimeoffset'2012-02-29T09:40:15Z') and (LastEvidenceTime ge datetimeoffset'2012-02-29T07:18:45Z')&$orderby=FirstEvidenceTime</li>
     * </ul>
     * @param consumer
     */
    public void testGetActvitiesLikeMetricPath(ODataConsumer consumer)
    {
        System.out.println("Testing get Activities Like Metric Path");
        
        String fullMetricPath = "middleware|WebSphere|WebSphere_MQ|Backends|qx21 on server18 (DB2 DB)|SQL|Dynamic|Query|SELECT CURRENT SQLID FROM SYSIBM.SYSDUMMY1:Concurrent Invocations";
        String escapedMetricPath = "middleware|WebSphere|WebSphere!_MQ|Backends|qx21 on server18 (DB2 DB)|SQL|Dynamic|Query|SELECT CURRENT SQLID FROM SYSIBM.SYSDUMMY1:Concurrent Invocations";
        String escapeChar = "!";
        
        // Check it works with the full path not a like expression
        Enumerable<OEntity> activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("MPQuery eq '" + escapedMetricPath + "' and EscapeChar eq '" + escapeChar + "'").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities like with full metric path returned 0 Activities");
        for (OEntity activityOnPath : activitiesOnPath)
        {
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.equals(fullMetricPath))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(foundPath, "Activity has no related metric on like path");
        } 
        
        // Use a like query
        String likePath = "%|Backends|%";
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("MPQuery eq '" + likePath + "'").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities like MPQuery '" + likePath + "' returned 0 Activities");
        
        for (OEntity activityOnPath : activitiesOnPath)
        {
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.contains("|Backends|"))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(foundPath, "Activity has no related metric on like path");
        } 
        
        
        // filter by metric path and anomaly score GT, order by anomaly score and a escape char that isn't used
        likePath = "%|Backends|%";
        escapeChar = "\\";
        int anomalyScore = 20;
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("EscapeChar eq '" + escapeChar + "' and (anomalyScore gt " + anomalyScore + ") and (MPQuery eq '" + likePath + "')").orderBy("anomalyScore").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities like MPQuery '" + likePath + "' & AnomalyScore ge " + anomalyScore + " returned 0 Activities");

        for (OEntity activityOnPath : activitiesOnPath)
        {
        	int actAnomScore = (Integer)activityOnPath.getProperty("AnomalyScore").getValue();
        	testCondition(actAnomScore >= anomalyScore, "AnomalyScore " +  actAnomScore + " != " + anomalyScore); 
        	
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                
                if (path.contains("|Backends|"))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(foundPath, "Activity has no related metric on like path");
        } 
        
        
        
        // different query
        likePath = "%WebSphere\\_MQ%";
        escapeChar = "\\";
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("MPQuery eq '" + likePath + "' and EscapeChar eq '" + escapeChar + "'").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities like MPQuery '" + likePath + "' returned 0 Activities");
        
        for (OEntity activityOnPath : activitiesOnPath)
        {
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.contains("WebSphere_MQ"))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(foundPath, "Activity has no related metric on like path '" + likePath + "'");
        } 
        
      
        // or multiple paths together and order by time
        String mpQuery = "((MPQuery eq '%WebSphere^_MQ%') or (MPQuery eq '%|Backends|%'))";
        escapeChar = "^";
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("EscapeChar eq '" + escapeChar + "' and " + mpQuery).expand(ACTIVITYMETRICS_NAV).orderBy("FirstEvidenceTime").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities like MPQuery '" + mpQuery +" returned 0 Activities");
        int activityCount = activitiesOnPath.count();
        
        Set<Integer> activityIds = new HashSet<Integer>();
        DateTime previousDate = (DateTime)activitiesOnPath.first().getProperty("FirstEvidenceTime").getValue();
        
        
        for (OEntity activityOnPath : activitiesOnPath)
        {
        	DateTime date = (DateTime)activityOnPath.getProperty("FirstEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activities like metric path FirstEvidenceTime order incorrect");      
            previousDate = date;
            
            activityIds.add((Integer)activityOnPath.getProperty("Id").getValue());

            List<OLink> relatedMetrics = activityOnPath.getLinks();
            testCondition(relatedMetrics.size() > 0, mpQuery + " expanded inline return no links.");
            
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedMetrics.get(0).getRelatedEntities())
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.contains("|Backends|") || path.contains("WebSphere_MQ"))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(foundPath, "Activity has no related metric on like path '" + mpQuery + "'");
        } 
        
        
        // same as previous query but change the order of operands
        mpQuery = "((MPQuery eq '%|Backends|%') or (MPQuery eq '%WebSphere^_MQ%'))";
        escapeChar = "^";
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("EscapeChar eq '" + escapeChar + "' and " + mpQuery).orderBy("FirstEvidenceTime").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities like MPQuery '" + mpQuery +" returned 0 Activities");
        // check same number of results as last time
        testCondition(activitiesOnPath.count() == activityCount, "Get Activities with or query returned different number of results when the operands were swapped");
        
        
        previousDate = (DateTime)activitiesOnPath.first().getProperty("FirstEvidenceTime").getValue();
        for (OEntity activityOnPath : activitiesOnPath)
        {
        	DateTime date = (DateTime)activityOnPath.getProperty("FirstEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activities like metric path FirstEvidenceTime order incorrect");      
            previousDate = date;
            
            Integer actId = (Integer)activityOnPath.getProperty("Id").getValue();
            testCondition(activityIds.contains(actId), "Get Activities with or query returned activity when the operands were swapped");
            
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.contains("|Backends|") || path.contains("WebSphere_MQ"))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(foundPath, "Activity has no related metric on like path '" + mpQuery + "'");
        }
        
        
        // and multiple paths together and order by time
        mpQuery = "((MPQuery eq '%|Backends|%') and (MPQuery eq '%WebSphere^_MQ%') )";
        escapeChar = "^";
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("EscapeChar eq '" + escapeChar + "' and " + mpQuery).orderBy("FirstEvidenceTime").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities like MPQuery '" + mpQuery + " returned 0 Activities");
        
        activityCount = activitiesOnPath.count();
        previousDate = (DateTime)activitiesOnPath.first().getProperty("FirstEvidenceTime").getValue();
        for (OEntity activityOnPath : activitiesOnPath)
        {
        	activityIds.add((Integer)activityOnPath.getProperty("Id").getValue());
        	
        	DateTime date = (DateTime)activityOnPath.getProperty("FirstEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activities like metric path FirstEvidenceTime order incorrect");      
            previousDate = date;
            
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundBackends = false;
            boolean foundWeb = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.contains("|Backends|")) 
                {
                	foundBackends = true;
                }
                
                if (path.contains("WebSphere_MQ"))
                {
                	foundWeb = true;
                }
                
                if (foundWeb && foundBackends)
                {
                	break;
                }
                
            }

            testCondition(foundBackends && foundWeb, "Activity has no related metrics on like paths '" + mpQuery + "'");
        } 
        
        
        // same as previous but swap order of operands.
        mpQuery = "((MPQuery eq '%WebSphere^_MQ%') and (MPQuery eq '%|Backends|%'))";
        escapeChar = "^";
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter("EscapeChar eq '" + escapeChar + "' and " + mpQuery).expand(ACTIVITYMETRICS_NAV).orderBy("FirstEvidenceTime").execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities like MPQuery '" + mpQuery + " returned 0 Activities");
        // check same number of results as last time
        testCondition(activitiesOnPath.count() == activityCount, "Get Activities with and query returned different number of results when the operands were swapped");
        
        previousDate = (DateTime)activitiesOnPath.first().getProperty("FirstEvidenceTime").getValue();
        for (OEntity activityOnPath : activitiesOnPath)
        {
            Integer actId = (Integer)activityOnPath.getProperty("Id").getValue();
            testCondition(activityIds.contains(actId), "Get Activities with and query returned a different activity when the operands were swapped");
            
        	DateTime date = (DateTime)activityOnPath.getProperty("FirstEvidenceTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activities like metric path FirstEvidenceTime order incorrect");      
            previousDate = date;
            
            boolean foundBackends = false;
            boolean foundWeb = false;
            List<OLink> relatedMetrics = activityOnPath.getLinks();
            testCondition(relatedMetrics.size() > 0, mpQuery + " expanded inline return no links.");
            
            for (OEntity relatedEnt : relatedMetrics.get(0).getRelatedEntities())
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.contains("|Backends|")) 
                {
                	foundBackends = true;
                }
                
                if (path.contains("WebSphere_MQ"))
                {
                	foundWeb = true;
                }
                
                if (foundWeb && foundBackends)
                {
                	break;
                }
            }

            testCondition(foundBackends && foundWeb, "Activity has no related metrics on like paths '" + mpQuery + "'");
        } 
        
        
        // a larger and using the default escape char
        mpQuery = "MPQuery eq '%|Backends|%' and MPQuery eq '%WebSphere\\_MQ%' and MPQuery eq '%:Average Response Time (ms)'";
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter(mpQuery).execute();
        testCondition(activitiesOnPath.count() > 0, "Get Activities like MPQuery '" + mpQuery + " returned 0 Activities");
        
        for (OEntity activityOnPath : activitiesOnPath)
        {
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundBackends = false;
            boolean foundWeb = false;
            boolean foundART = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.contains("|Backends|")) 
                {
                	foundBackends = true;
                }
                
                if (path.contains("WebSphere_MQ"))
                {
                	foundWeb = true;
                }
                
                if (path.contains(":Average Response Time (ms)"))
                {
                	foundART = true;
                }
                
                if (foundWeb && foundBackends && foundART)
                {
                	break;
                }
                
            }

            testCondition(foundBackends && foundWeb && foundART, "Activity has no related metrics on like paths '" + mpQuery + "'");
        } 
        
        // or multiple paths together and filter by date times by time
        // filter by dates => and <=
        DateTime firstActDate = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T07:18:45+00:00");
        DateTime lastActDate = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T09:40:15+00:00");
        
        // get only one activity
        mpQuery = "((MPQuery eq '%WebSphere_MQ%') or (MPQuery eq '%|Backends|%')) and (FirstEvidenceTime le datetimeoffset'2012-02-29T09:40:15Z') and " +
        		"(LastEvidenceTime ge datetimeoffset'2012-02-29T09:40:15Z')";
        
        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter(mpQuery).orderBy("FirstEvidenceTime").execute();
        testCondition(activitiesOnPath.count() == 1, "Get Activities like MPQuery '" + mpQuery + " returned 0 Activities");
        
        
        // get multiple activities
        mpQuery = "((MPQuery eq '%WebSphere_MQ%') or (MPQuery eq '%|Backends|%')) and (FirstEvidenceTime le datetimeoffset'2012-02-29T09:40:15Z') and " +
		"(LastEvidenceTime ge datetimeoffset'2012-02-29T07:18:45Z')";

        activitiesOnPath = consumer.getEntities(ACTIVITIES_SET).filter(mpQuery).orderBy("FirstEvidenceTime").execute();
        testCondition(activitiesOnPath.count() >= 1, "Get Activities like MPQuery '" + mpQuery + " returned 0 Activities");
        
        previousDate = (DateTime)activitiesOnPath.first().getProperty("FirstEvidenceTime").getValue();
        for (OEntity activityOnPath : activitiesOnPath)
        {
        	DateTime first = (DateTime)activityOnPath.getProperty("FirstEvidenceTime").getValue();
            testCondition(first.isAfter(previousDate) || first.isEqual(previousDate), "Activities like metric path FirstEvidenceTime order incorrect");      
            previousDate = first;
            
            DateTime last = (DateTime)activityOnPath.getProperty("LastEvidenceTime").getValue();
            testCondition((last.isAfter(firstActDate) || last.isEqual(firstActDate)) && (first.isBefore(lastActDate) || first.isEqual(lastActDate)), 
            		"Activities like metric path filter between dates incorrect");
            
            
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activityOnPath.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.contains("|Backends|") || path.contains("WebSphere_MQ"))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(foundPath, "Activity has no related metric on like path '" + mpQuery + "'");
        } 
    }
    
    
    /**
     * Test filters built form multiple expressions 'anded' and 'ored' together.
     * <p/>
     * Equivalent URIs:
     * <em>Note the '|' character must be Url encoded as %7C and '%' as %25</em>
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=(FirstEvidenceTime le datetimeoffset'2012-02-29T23:59:00Z' and LastEvidenceTime ge datetimeoffset'2012-02-29T06:00:00Z') and (MPQuery eq '%25%7CWebSphere%7C%25:Average Response Time (ms)' or AnomalyScore ge 70)</li>
     * </ul>   
     * @param consumer
     */
    public void testComplicatedFilters(ODataConsumer consumer)
    {
        System.out.println("Testing complicated and/or filters");
        
        String likePath = "%|WebSphere|%:Average Response Time (ms)";
        String filter = "(FirstEvidenceTime le datetimeoffset'2012-02-29T23:59:00Z' and LastEvidenceTime ge datetimeoffset'2012-02-29T06:00:00Z') and " + 
		"(MPQuery eq '" + likePath + "' or AnomalyScore ge 70)";
        
        DateTime endTime = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T23:59:00Z");
        DateTime startTime = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T06:00:00Z");
        
        // Check it works with the full path not a like expression
        Enumerable<OEntity> activities = consumer.getEntities(ACTIVITIES_SET).filter(filter).expand(ACTIVITYMETRICS_NAV).execute();
        testCondition(activities.count() > 0, "Get Activities filter =" + filter + " returned 0 activities");
        
        boolean gotActWithScoreOver70 = false;
        for (OEntity activity : activities)
        {
        	boolean isGe70 = (Integer)activity.getProperty("AnomalyScore").getValue() >= 70;
        	if (isGe70)
        	{
        		gotActWithScoreOver70 = true;
        	}
        	DateTime first = (DateTime)activity.getProperty("FirstEvidenceTime").getValue();
        	DateTime last = (DateTime)activity.getProperty("LastEvidenceTime").getValue();
        	
        	boolean isWithinDates = (first.isBefore(endTime) || first.isEqual(endTime)) && (last.isAfter(startTime) || last.isEqual(startTime)); 
        	
            Enumerable<OEntity> relatedEnts = consumer.getEntities(ACTIVITIES_SET).nav(activity.getEntityKey(), ACTIVITYMETRICS_NAV).execute();
            boolean foundPath = false;
            for (OEntity relatedEnt : relatedEnts)
            {
                String path = (String)relatedEnt.getProperty("MetricPath").getValue();
                if (path.contains("|WebSphere|") && path.endsWith(":Average Response Time (ms)"))
                {
                    foundPath = true;
                    break;
                }
            }

            testCondition(isWithinDates && (foundPath || isGe70), "Activity anomalyScore is < 70 or has no metrics on path");
        } 
        
        testCondition(gotActWithScoreOver70, "No activity with score >= 70 returned by " + filter);
    }
    
    
    /**
     * Test the navigation from Activity to RelatedMetrics with <code>$orderby</code> options.
     * <ul>
     *  <li>Order by significance</li>  
     *  <li>Order by datetime</li>  
     * </ul>
     * <p/>
     * Equivalent URIs:
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities(26)/ActivityMetrics</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities(26)/ActivityMetrics?$orderby=significance</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities(26)/ActivityMetrics?$orderby=significance desc</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities(196)/ActivityMetrics?&$orderby=dateTime </li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities(196)/ActivityMetrics?&$orderby=DateTime desc</li>
     * </ul>   
     * @param consumer
     */
    public void testActivityToRelatedMetricsNavigation(ODataConsumer consumer)
    {
        System.out.println("Testing Activity -> RelatedMetrics Navigaton");
        
        Enumerable<OEntity> ents = consumer.getEntities(ACTIVITIES_SET).nav(OEntityKey.parse("26"), ACTIVITYMETRICS_NAV).execute();
        testCondition(ents.count() == 4, "Incorrect number of RelatedMetrics returned");
        
        ents = consumer.getEntities(ACTIVITIES_SET).nav(OEntityKey.parse("26"), ACTIVITYMETRICS_NAV).orderBy("Significance").execute();
        testCondition(ents.count() == 4, "Incorrect number of RelatedMetrics returned");
        Integer previousSignificance = (Integer)ents.first().getProperty("Significance").getValue();
        for (OEntity ent : ents)
        {
            Integer significance = (Integer)ent.getProperty("Significance").getValue();
            testCondition(significance >= previousSignificance, "Activity significance order incorrect");
            previousSignificance = significance;
        }
        
        ents = consumer.getEntities(ACTIVITIES_SET).nav(OEntityKey.parse("26"), ACTIVITYMETRICS_NAV).orderBy("significance desc").execute();
        testCondition(ents.count() == 4, "Incorrect number of RelatedMetrics returned");
        previousSignificance = (Integer)ents.first().getProperty("Significance").getValue();
        for (OEntity ent : ents)
        {
            Integer significance = (Integer)ent.getProperty("Significance").getValue();
            testCondition(significance <= previousSignificance, "Activity significance descending order incorrect");
            previousSignificance = significance;
        }
        
        ents = consumer.getEntities(ACTIVITIES_SET).nav(OEntityKey.parse("196"), ACTIVITYMETRICS_NAV).orderBy("DateTime").execute();
        testCondition(ents.count() == 3, "Incorrect number of RelatedMetrics returned");
        DateTime previousDate = (DateTime)ents.first().getProperty("DateTime").getValue();
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("DateTime").getValue();
            testCondition(date.isAfter(previousDate) || date.isEqual(previousDate), "Activity dates order incorrect");      
            previousDate = date;
        }
        
        ents = consumer.getEntities(ACTIVITIES_SET).nav(OEntityKey.parse("196"), ACTIVITYMETRICS_NAV).orderBy("dateTime desc").execute();
        testCondition(ents.count() == 3, "Incorrect number of RelatedMetrics returned");
        previousDate = (DateTime)ents.first().getProperty("DateTime").getValue();
        for (OEntity ent : ents)
        {
        	DateTime date = (DateTime)ent.getProperty("DateTime").getValue();
            testCondition(date.isBefore(previousDate) || date.isEqual(previousDate), "Activity DateTime descending order incorrect");      
            previousDate = date;
        }
    }
    
    
    /**
     * Test the inline expansion of an single Activities' RelatedMetrics. 
     * Get an Activity and its RelatedMetrics in a single call. 
     * <p/>
     * Equivalent URI:
     * <ul>
     *   <li>http://localhost:8080/prelertApi/prelert.svc/Activities(26)?$expand=ActivityMetrics</li>
     * </ul>    
     * @param consumer
     */
    public void testActivityRelatedMetricsInlineExpansion(ODataConsumer consumer)
    {
        System.out.println("Testing a single Activities Related Metrics inline expansion");
        
        OEntity ent = consumer.getEntity(ACTIVITIES_SET, OEntityKey.parse("26")).expand(ACTIVITYMETRICS_NAV).execute();
        testCondition(ent.getLinks().get(0).getRelatedEntities().size() == 4, "Incorrect number of RelatedMetrics returned");
        
        List<Integer> evidenceIds = new ArrayList<Integer>();
        for (OLink link : ent.getLinks())
        {
            for (OEntity relatedEnt : link.getRelatedEntities())
            {
                evidenceIds.add((Integer)relatedEnt.getProperty("EvidenceId").getValue());
                
                testCondition(relatedEnt.getEntitySetName().equals(RELATEDMETRICS_SET), 
                    "RelatedMetric set name should equal " + RELATEDMETRICS_SET + " not " + relatedEnt.getEntitySetName());
            }
        }
        
        Collections.sort(evidenceIds);
        testCondition(evidenceIds.get(0) == 26, "Incorrect Evidence Id");
        testCondition(evidenceIds.get(1) == 27, "Incorrect Evidence Id");
        testCondition(evidenceIds.get(2) == 28, "Incorrect Evidence Id");
        testCondition(evidenceIds.get(3) == 29, "Incorrect Evidence Id");
    }
    
    
    /**
     * Test the RelatedMetrics inline expansion for a set of Activities. 
     * Get a set of Activities by the usual filter options and the 
     * RelatedMetrics in a single call. 
     * <p/>
     * Equivalent URIs:
     * <em>Note the '+' character must be url encoded as %2B</em>
     * <ul>
     *   <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$expand=ActivityMetrics</li>
     *   <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=anomalyScore gt 20&$expand=ActivityMetrics</li>
     *   <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=PeakEvidenceTime ge datetimeoffset'2012-02-29T07:18:45%2B00:00'&$expand=ActivityMetrics</li>
     *   <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$filter=PeakEvidenceTime lt datetimeoffset'1980-01-01T07:18:45%2B00:00'&$expand=ActivityMetrics</li>
     * </ul>    
     * @param consumer
     */
    public void testActivitiesRelatedMetricsInlineExpansion(ODataConsumer consumer)
    {
    	System.out.println("Testing Activities' Related Metrics inline expansion");
    	
    	Enumerable<OEntity> activitiesExpand = consumer.getEntities(ACTIVITIES_SET).expand(ACTIVITYMETRICS_NAV).execute();
    	testCondition(activitiesExpand.count() > 0, "Get Activities expand inline returned 0 entities.");
    	int inlineRelatedMetricCount = 0;
    	for (OEntity ent : activitiesExpand)
    	{
    		for (OLink link : ent.getLinks())
    		{
    			inlineRelatedMetricCount += link.getRelatedEntities().size();
    			testCondition(link.getRelatedEntities().size() != 0, "Inline expansion of Activities returned no Related Metrics");
    		}
    	}
    	
    	
    	activitiesExpand = consumer.getEntities(ACTIVITIES_SET).filter("anomalyScore ge 20").expand(ACTIVITYMETRICS_NAV).execute();
    	inlineRelatedMetricCount = 0;
    	for (OEntity ent : activitiesExpand)
    	{
    		int score = (Integer)ent.getProperty("AnomalyScore").getValue();
    		testCondition(score >= 20, "Get Activities expand inline anomaly score filter is not >= 20");
    		for (OLink link : ent.getLinks())
    		{
    			inlineRelatedMetricCount += link.getRelatedEntities().size();
    			testCondition(link.getRelatedEntities().size() != 0, "Inline expansion of Activities returned no Related Metrics");
    		}
    	}
    	
    	
    	activitiesExpand = consumer.getEntities(ACTIVITIES_SET).filter("PeakEvidenceTime gt datetimeoffset'2012-02-29T07:18:45+00:00'").expand(ACTIVITYMETRICS_NAV).orderBy("PeakEvidenceTime").execute();
    	testCondition(activitiesExpand.count() > 0, "Get Activities expand inline filter PeakEvidenceTime returned 0 entities.");

    	DateTime firstAct = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T07:18:45+00:00");    	
        for (OEntity ent : activitiesExpand)
        {
        	DateTime actDate = (DateTime)ent.getProperty("PeakEvidenceTime").getValue();
        	testCondition(actDate.isAfter(firstAct), "Activity PeakEvidenceTime filter returned incorrect date");  
        }
        
        // Test inline expansion when no activities are found (it should NOT throw an error)
    	activitiesExpand = consumer.getEntities(ACTIVITIES_SET).filter("PeakEvidenceTime lt datetimeoffset'1980-01-01T00:18:45+00:00'").expand(ACTIVITYMETRICS_NAV).orderBy("PeakEvidenceTime").execute();
    	testCondition(activitiesExpand.count() == 0, "Get Activities expand inline filter PeakEvidenceTime le 1980 should return 0 activities.");
    }
    
    
    /**
     * Get a single RelatedMetric by key.
     * <p/>
     * Equivalent URI:
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/RelatedMetrics(28)</li>
     * </ul>
     * @param consumer
     */
    public void testGetRelatedMetric(ODataConsumer consumer)
    { 
        System.out.println("Testing get RelatedMetric by key");
        
        OEntity ent = consumer.getEntity(RELATEDMETRICS_SET, OEntityKey.parse("28")).execute();
        
        DateTime date = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T07:18:45+00:00");
        DateTime server = ((DateTime)ent.getProperty("DateTime").getValue());
        
        testCondition(server.isEqual(date), "Incorrect DateTime") ;
        testCondition(((Integer)ent.getProperty("EvidenceId").getValue()).equals(28), "Incorrect evidenceId") ;
        testCondition(((String)ent.getProperty("SourceType").getValue()).equals("CA-APM"), "Incorrect sourceType");
        testCondition(((String)ent.getProperty("Description").getValue())
                .equals("Increase in Average Response Time (ms)"), "Incorrect description");
        testCondition(((Integer)ent.getProperty("Count").getValue()).equals(1), "Incorrect count");
        testCondition(((String)ent.getProperty("MetricPath").getValue())
                .equals("middleware|WebSphere|WebSphere_MQ|Backends|qx21 on server18 (DB2 DB)|SQL|Dynamic|Query|SELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1:Average Response Time (ms)"), "Incorrect metricPath");
        
        // Significance will always be 0 for single related metrics accessed via
        // this method as there is no context of an Activity for them the related
        // metric to have a significance in.
        testCondition(((Integer)ent.getProperty("Significance").getValue()).equals(0), "Incorrect significance");
    }
    
    
    /**
     * Test selecting properties of Activites and RelatedMetrics using the $select query.
     * <ul>
     *   <li>Seletct properties of a single Activity</li>
     *   <li>Select RelatedMetric properties through the <code>ActivityMetrics</code> navigation</li>
     *   <li>Select properties of all Activities</li>
     *   <li>Select properties of a single RelatedMetric</li>
     * </ul>
     * </p>
     * Equivalent URIs:
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities(26)?$select=AnomalyScore, RelatedMetricCount</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities(26)/ActivityMetrics?$select=EvidenceId, sourcetype</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/Activities?$select=RelatedMetricCount</li>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/RelatedMetrics(28)?$select=metricPath</li>
     * </ul>
     * @param consumer
     */
    public void testSelectQueries(ODataConsumer consumer)
    {
    	System.out.println("Testing Select filters");
    	
        OEntity actEnt = consumer.getEntity(ACTIVITIES_SET, OEntityKey.parse("26")).select("AnomalyScore, RelatedMetricCount").execute();
        testCondition(actEnt.getProperties().size() ==2, "Wrong number of Activity properties selected");
        testCondition(actEnt.getProperty("AnomalyScore") != null, "EvidenceId property not selected");
        testCondition(actEnt.getProperty("RelatedMetricCount") != null, "SourceType property not selected");
        
        actEnt = consumer.getEntity(ACTIVITIES_SET, OEntityKey.parse("26")).select("non_existent_prop").execute();
        testCondition(actEnt.getProperties().size() == 0, "Selecting a non existent prop from Activity returned properties");
        
        
        // Test selects by nav property
    	Enumerable<OEntity> ents = consumer.getEntities(ACTIVITIES_SET).nav(OEntityKey.parse("26"), ACTIVITYMETRICS_NAV).select("EvidenceId, sourcetype").execute();
        testCondition(ents.count() == 4, "Incorrect number of RelatedMetrics returned");
        for (OEntity ent : ents)
        {
            testCondition(ent.getProperties().size() ==2, "Wrong number of RelatedMetric properties selected");
            testCondition(ent.getProperty("EvidenceId") != null, "EvidenceId property not selected");
            testCondition(ent.getProperty("SourceType") != null, "SourceType property not selected");
        }
        
    	ents = consumer.getEntities(ACTIVITIES_SET).select("RelatedMetricCount").execute();
        for (OEntity ent : ents)
        {
            testCondition(ent.getProperties().size() == 1, "Wrong number of Activity properties selected");
            testCondition(ent.getProperty("RelatedMetricCount") != null, "RelatedMetricCount property not selected");
        }
        
        
        OEntity rmEnt = consumer.getEntity(RELATEDMETRICS_SET, OEntityKey.parse("28")).select("metricPath").execute();
        testCondition(rmEnt.getProperties().size() == 1, "Wrong number of RelatedMetric properties selected");
        testCondition(rmEnt.getProperty("MetricPath") != null, "metricPath property not selected");
        
        rmEnt = consumer.getEntity(RELATEDMETRICS_SET, OEntityKey.parse("28")).select("non_existent_prop").execute();
        testCondition(rmEnt.getProperties().size() == 0, "Selecting a non existent prop from RelatedMetric returned properties");
    }
    
    /**
     * Test the navigation from a RelatedMetric to the Activity it belongs to.
     * </p>
     * Equivalent URI:
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/RelatedMetrics(28)/MetricActivity</li>
     * </ul>
     * @param consumer
     */
    public void testMetricToActivityNavigation(ODataConsumer consumer)
    {
        System.out.println("Testing RelatedMetric -> Activity navigation");
        
        Enumerable<OEntity> ents =consumer.getEntities(RELATEDMETRICS_SET).nav(OEntityKey.parse("28"), METRIC_ACTIVITY_NAV).execute();
        testCondition(ents.count() == 1, "Incorrect number of Activities returned = " + ents.count());
        
        OEntity ent = ents.first();
        verifyActivity26(ent);      
    }
    
    
    /**
     * Test earliest activity time function.
     * <p/>
     * Equivalent URI:
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/EarliestActivityTime</li>
     * </ul>
     * @param consumer
     */
    public void testEarliestTime(ODataConsumer consumer)
    {
        System.out.println("Testing earliest time function");
                
        Enumerable<OObject> result = consumer.callFunction(EARLIEST_ACTIVITY_FUNC).execute();
        
        testCondition(result.count() == 1, "Wrong result count for " + EARLIEST_ACTIVITY_FUNC + " function");
        
        OObject obj = result.first();
        testCondition(obj.getType().equals(EdmSimpleType.DATETIMEOFFSET),
                "Wrong return type = " + result.first().getType() + " for " + EARLIEST_ACTIVITY_FUNC + " function");
        
        
        DateTime known = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T07:18:45Z");
        
        OSimpleObject<?> simpleObj = (OSimpleObject<?>)obj;

        testCondition(((DateTime)simpleObj.getValue()).isEqual(known), 
            "Earliest Activity DateTime does not match expected result");    
    }
        
    
    /**
     * Test latest activity time function.
     * <p/>
     * Equivalent URI:
     * <ul>
     *  <li>http://localhost:8080/prelertApi/prelert.svc/LatestActivityTime</li>
     * </ul>
     * 
     * @param consumer
     */
    public void testLatestTime(ODataConsumer consumer)
    {
        System.out.println("Testing latest time function");

        Enumerable<OObject> result = consumer.callFunction(LATEST_ACTIVITY_FUNC).execute();
        
        testCondition(result.count() == 1, "Wrong result count for " + LATEST_ACTIVITY_FUNC + " function");
        
        OObject obj = result.first();
        testCondition(obj.getType().equals(EdmSimpleType.DATETIMEOFFSET),
                "Wrong return type = " + result.first().getType() + " for " + LATEST_ACTIVITY_FUNC + " function");
        
        DateTime known = ISODateTimeFormat.dateTimeParser().parseDateTime("2012-02-29T19:30:00Z");
        
        OSimpleObject<?> simpleObj = (OSimpleObject<?>)obj;

        testCondition(((DateTime)simpleObj.getValue()).isEqual(known), 
                "Latest Activity DateTime does not match expected result");      
    }
    
    
    /**
     * Test requests in international timezones. 
     * Change the local timezone then parse the returned dates.
     * Test ensures the client will work with internationalised dates & times but cannot
     * control which timezone the server is running.
     * 
     * @param consumer
     */
    public void testTimezones(ODataConsumer consumer)
    {
    	System.out.println("Testing multiple timezones");
    	
    	
        OEntity ent = consumer.getEntity(ACTIVITIES_SET, OEntityKey.parse("26")).execute();
        
        String tmz = "EST";
        DateTime clientTime = ISODateTimeFormat.dateTimeParser().withZone(DateTimeZone.forID(tmz)).parseDateTime("2012-02-29T07:18:45+00:00");
        DateTime serverTime = (DateTime)ent.getProperty("PeakEvidenceTime").getValue();
        
        testCondition(clientTime.isEqual(serverTime), "Times not equal when using timezone " + tmz);

        
        tmz = "CET";
        clientTime = ISODateTimeFormat.dateTimeParser().withZone(DateTimeZone.forID(tmz)).parseDateTime("2012-02-29T07:18:45+00:00");
        serverTime = (DateTime)ent.getProperty("PeakEvidenceTime").getValue();
        
        testCondition(clientTime.isEqual(serverTime), "Times not equal when using timezone " + tmz);
    }
    
    
    /**
     * Run the performance tests and output timings.
     * <ol>
     *   <li>Get all Activities</li>
     *   <li>Get all Activities with their RelatedMetrics returned inline</li>
     *   <li>For each Activity get all its RelatedMetrics</li>
     * </ol>
     * 
     * @param consumer
     */
    public void performanceTest(ODataConsumer consumer)
    {
    	System.out.println("Starting performance test");
    	
    	long startTime = System.currentTimeMillis();
    	Enumerable<OEntity> activities = consumer.getEntities(ACTIVITIES_SET).execute();

    	long getActsSimpleTime = System.currentTimeMillis();
    	
    	Enumerable<OEntity> activitiesExpand = consumer.getEntities(ACTIVITIES_SET).expand(ACTIVITYMETRICS_NAV).execute();
    	int inlineRelatedMetricCount = 0;
    	for (OEntity ent : activitiesExpand)
    	{
    		for (OLink link : ent.getLinks())
    		{
    			inlineRelatedMetricCount += link.getRelatedEntities().size();
    			testCondition(link.getRelatedEntities().size() != 0, "Inline expansion of Activities returned no Related Metrics");
    		}
    	}
    	
    	long getActsAndMetricsTime = System.currentTimeMillis();
    	
    	int relatedMetricCount = 0;
    	for (OEntity activity : activities)
    	{
    		Enumerable<OEntity> relatedMetrics = consumer.getEntities(ACTIVITIES_SET).nav(activity.getEntityKey(), 
    				ACTIVITYMETRICS_NAV).execute();
    		testCondition(relatedMetrics.count() > 0, "No RelatedMetrics for the Activity(key=" 
    					+ activity.getEntityKey() + ")");
    		
    		relatedMetricCount += relatedMetrics.count();
    	}
    	
    	long endTime = System.currentTimeMillis();
    	
    	testCondition(inlineRelatedMetricCount == relatedMetricCount, 
    			"Inline metric count does not equal iterated count for all activities");
    	
    	System.out.println(String.format("Query get %d Activities in %dms.", 
    			activities.count(), getActsSimpleTime - startTime));
    	System.out.println(String.format("Query get %d Activities with their %d RelatedMetrics expanded inline in %dms.", 
    			activitiesExpand.count(), inlineRelatedMetricCount, getActsAndMetricsTime - getActsSimpleTime));
    	System.out.println(String.format("Get %d RelatedMetrics (total) via %d queries (one for each Activity) in %dms.", 
    			relatedMetricCount, activities.count(), endTime - getActsAndMetricsTime));
    }
    
    
    /**
     * If <code>condition</code> is false then print the message to <code>System.out</code> 
     * and log the failure.
     * 
     * @param condition if false then log <code>failureMessage</code>
     * and exit.
     * @param failureMessage
     */
    private void testCondition(boolean condition, String failureMessage)
    {
        if (condition == false)
        {
            System.out.println("ERROR: " + failureMessage);
            System.out.println("TEST FAILED");
            m_TestFailed = true;
        }
    }
}
