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

package com.prelert.job.process.output.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.AlertObserver;
import com.prelert.job.persistence.JobResultsPersister;
import com.prelert.job.process.normaliser.BlockingQueueRenormaliser;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Detector;
import com.prelert.job.results.Influencer;
import com.prelert.utils.json.AutoDetectParseException;

/**
 * Tests for parsing the JSON output of autodetect_api
 */
public class AutoDetectResultsParserTest
{
    public static final String METRIC_OUTPUT_SAMPLE = "[{\"timestamp\":1359450000,\"detectors\":[],\"rawAnomalyScore\":0, \"maxNormalizedProbability\":0,\"anomalyScore\":0,\"recordCount\":0,\"eventCount\":806}" +
            ",{\"quantileState\":[\"normaliser 1.1\", \"normaliser 2.1\"]}" +
            ",{\"timestamp\":1359453600,\"detectors\":[{\"name\":\"individual metric/responsetime/airline\",\"records\":[{\"probability\":0.0637541,\"byFieldName\":\"airline\",\"byFieldValue\":\"JZA\",\"typical\":1020.08,\"actual\":1042.14,\"fieldName\":\"responsetime\",\"function\":\"max\",\"partitionFieldName\":\"\",\"partitionFieldValue\":\"\"},{\"probability\":0.00748292,\"byFieldName\":\"airline\",\"byFieldValue\":\"AMX\",\"typical\":20.2137,\"actual\":22.8855,\"fieldName\":\"responsetime\",\"function\":\"max\",\"partitionFieldName\":\"\",\"partitionFieldValue\":\"\"},{\"probability\":0.023494,\"byFieldName\":\"airline\",\"byFieldValue\":\"DAL\",\"typical\":382.177,\"actual\":358.934,\"fieldName\":\"responsetime\",\"function\":\"min\",\"partitionFieldName\":\"\",\"partitionFieldValue\":\"\"},{\"probability\":0.0473552,\"byFieldName\":\"airline\",\"byFieldValue\":\"SWA\",\"typical\":152.148,\"actual\":96.6425,\"fieldName\":\"responsetime\",\"function\":\"min\",\"partitionFieldName\":\"\",\"partitionFieldValue\":\"\"}]}],\"rawAnomalyScore\":0.0140005, \"anomalyScore\":20.22688,\"maxNormalizedProbability\":10.5688, \"recordCount\":4,\"eventCount\":820}" +
            ",{\"quantileState\":[\"normaliser 1.2\", \"normaliser 2.2\"]}" +
            ",{\"flush\":\"testing1\"}" +
            ",{\"quantileState\":[\"normaliser 1.3\", \"normaliser 2.3\"]}" +
            "]";

    public static final String POPULATION_OUTPUT_SAMPLE = "[{\"timestamp\":1379590200,\"detectors\":[{\"name\":\"population metric maximum/0/sum_cs_bytes_//cs_host/\",\"records\":[{\"probability\":1.38951e-08,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"mail.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":1.38951e-08,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"mail.google.com\",\"function\":\"max\",\"typical\":101534,\"actual\":9.19027e+07}],\"normalizedProbability\":100,\"anomalyScore\":44.7324},{\"probability\":3.86587e-07,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"armmf.adobe.com\",\"function\":\"max\",\"causes\":[{\"probability\":3.86587e-07,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"armmf.adobe.com\",\"function\":\"max\",\"typical\":101534,\"actual\":3.20093e+07}],\"normalizedProbability\":89.5834,\"anomalyScore\":44.7324},{\"probability\":0.00500083,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0.docs.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00500083,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0.docs.google.com\",\"function\":\"max\",\"typical\":101534,\"actual\":6.61812e+06}],\"normalizedProbability\":1.19856,\"anomalyScore\":44.7324},{\"probability\":0.0152333,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"emea.salesforce.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0152333,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"emea.salesforce.com\",\"function\":\"max\",\"typical\":101534,\"actual\":5.36373e+06}],\"normalizedProbability\":0.303996,\"anomalyScore\":44.7324}]}],\"rawAnomalyScore\":1.30397,\"anomalyScore\":44.7324,\"maxNormalizedProbability\":100,\"recordCount\":4,\"eventCount\":1235}" +
            ",{\"flush\":\"testing2\"}" +
            ",{\"timestamp\":1379590800,\"detectors\":[{\"name\":\"population metric maximum/0/sum_cs_bytes_//cs_host/\",\"records\":[{\"probability\":1.9008e-08,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"mail.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":1.9008e-08,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"mail.google.com\",\"function\":\"max\",\"typical\":31356,\"actual\":1.1498e+08}],\"normalizedProbability\":93.6213,\"anomalyScore\":1.19192},{\"probability\":1.01013e-06,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"armmf.adobe.com\",\"function\":\"max\",\"causes\":[{\"probability\":1.01013e-06,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"armmf.adobe.com\",\"function\":\"max\",\"typical\":31356,\"actual\":3.25808e+07}],\"normalizedProbability\":86.5825,\"anomalyScore\":1.19192},{\"probability\":0.000386185,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0.docs.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.000386185,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0.docs.google.com\",\"function\":\"max\",\"typical\":31356,\"actual\":3.22855e+06}],\"normalizedProbability\":17.1179,\"anomalyScore\":1.19192},{\"probability\":0.00208033,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"docs.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00208033,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"docs.google.com\",\"function\":\"max\",\"typical\":31356,\"actual\":1.43328e+06}],\"normalizedProbability\":3.0692,\"anomalyScore\":1.19192},{\"probability\":0.00312988,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"booking2.airasia.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00312988,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"booking2.airasia.com\",\"function\":\"max\",\"typical\":31356,\"actual\":1.15764e+06}],\"normalizedProbability\":1.99532,\"anomalyScore\":1.19192},{\"probability\":0.00379229,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.facebook.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00379229,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.facebook.com\",\"function\":\"max\",\"typical\":31356,\"actual\":1.0443e+06}],\"normalizedProbability\":1.62352,\"anomalyScore\":1.19192},{\"probability\":0.00623576,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.airasia.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00623576,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.airasia.com\",\"function\":\"max\",\"typical\":31356,\"actual\":792699}],\"normalizedProbability\":0.935134,\"anomalyScore\":1.19192},{\"probability\":0.00665308,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00665308,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.google.com\",\"function\":\"max\",\"typical\":31356,\"actual\":763985}],\"normalizedProbability\":0.868119,\"anomalyScore\":1.19192},{\"probability\":0.00709315,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0.drive.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00709315,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0.drive.google.com\",\"function\":\"max\",\"typical\":31356,\"actual\":736442}],\"normalizedProbability\":0.805994,\"anomalyScore\":1.19192},{\"probability\":0.00755789,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources2.news.com.au\",\"function\":\"max\",\"causes\":[{\"probability\":0.00755789,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources2.news.com.au\",\"function\":\"max\",\"typical\":31356,\"actual\":709962}],\"normalizedProbability\":0.748239,\"anomalyScore\":1.19192},{\"probability\":0.00834974,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.calypso.net.au\",\"function\":\"max\",\"causes\":[{\"probability\":0.00834974,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.calypso.net.au\",\"function\":\"max\",\"typical\":31356,\"actual\":669968}],\"normalizedProbability\":0.664644,\"anomalyScore\":1.19192},{\"probability\":0.0107711,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"ad.yieldmanager.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0107711,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"ad.yieldmanager.com\",\"function\":\"max\",\"typical\":31356,\"actual\":576067}],\"normalizedProbability\":0.485277,\"anomalyScore\":1.19192},{\"probability\":0.0123367,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.google-analytics.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0123367,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.google-analytics.com\",\"function\":\"max\",\"typical\":31356,\"actual\":530594}],\"normalizedProbability\":0.406783,\"anomalyScore\":1.19192},{\"probability\":0.0125647,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"bs.serving-sys.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0125647,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"bs.serving-sys.com\",\"function\":\"max\",\"typical\":31356,\"actual\":524690}],\"normalizedProbability\":0.396986,\"anomalyScore\":1.19192},{\"probability\":0.0141652,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.google.com.au\",\"function\":\"max\",\"causes\":[{\"probability\":0.0141652,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.google.com.au\",\"function\":\"max\",\"typical\":31356,\"actual\":487328}],\"normalizedProbability\":0.337075,\"anomalyScore\":1.19192},{\"probability\":0.0141742,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources1.news.com.au\",\"function\":\"max\",\"causes\":[{\"probability\":0.0141742,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources1.news.com.au\",\"function\":\"max\",\"typical\":31356,\"actual\":487136}],\"normalizedProbability\":0.336776,\"anomalyScore\":1.19192},{\"probability\":0.0145263,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"b.mail.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0145263,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"b.mail.google.com\",\"function\":\"max\",\"typical\":31356,\"actual\":479766}],\"normalizedProbability\":0.325385,\"anomalyScore\":1.19192},{\"probability\":0.0151447,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.rei.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0151447,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.rei.com\",\"function\":\"max\",\"typical\":31356,\"actual\":467450}],\"normalizedProbability\":0.306657,\"anomalyScore\":1.19192},{\"probability\":0.0164073,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"s3.amazonaws.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0164073,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"s3.amazonaws.com\",\"function\":\"max\",\"typical\":31356,\"actual\":444511}],\"normalizedProbability\":0.272805,\"anomalyScore\":1.19192},{\"probability\":0.0201927,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0-p-06-ash2.channel.facebook.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0201927,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0-p-06-ash2.channel.facebook.com\",\"function\":\"max\",\"typical\":31356,\"actual\":389243}],\"normalizedProbability\":0.196685,\"anomalyScore\":1.19192},{\"probability\":0.0218721,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"booking.airasia.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0218721,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"booking.airasia.com\",\"function\":\"max\",\"typical\":31356,\"actual\":369509}],\"normalizedProbability\":0.171353,\"anomalyScore\":1.19192},{\"probability\":0.0242411,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.yammer.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0242411,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.yammer.com\",\"function\":\"max\",\"typical\":31356,\"actual\":345295}],\"normalizedProbability\":0.141585,\"anomalyScore\":1.19192},{\"probability\":0.0258232,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"safebrowsing-cache.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0258232,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"safebrowsing-cache.google.com\",\"function\":\"max\",\"typical\":31356,\"actual\":331051}],\"normalizedProbability\":0.124748,\"anomalyScore\":1.19192},{\"probability\":0.0259695,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"fbcdn-profile-a.akamaihd.net\",\"function\":\"max\",\"causes\":[{\"probability\":0.0259695,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"fbcdn-profile-a.akamaihd.net\",\"function\":\"max\",\"typical\":31356,\"actual\":329801}],\"normalizedProbability\":0.123294,\"anomalyScore\":1.19192},{\"probability\":0.0268874,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.oag.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0268874,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.oag.com\",\"function\":\"max\",\"typical\":31356,\"actual\":322200}],\"normalizedProbability\":0.114537,\"anomalyScore\":1.19192},{\"probability\":0.0279146,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"booking.qatarairways.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0279146,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"booking.qatarairways.com\",\"function\":\"max\",\"typical\":31356,\"actual\":314153}],\"normalizedProbability\":0.105419,\"anomalyScore\":1.19192},{\"probability\":0.0309351,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources3.news.com.au\",\"function\":\"max\",\"causes\":[{\"probability\":0.0309351,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources3.news.com.au\",\"function\":\"max\",\"typical\":31356,\"actual\":292918}],\"normalizedProbability\":0.0821156,\"anomalyScore\":1.19192},{\"probability\":0.0335204,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources0.news.com.au\",\"function\":\"max\",\"causes\":[{\"probability\":0.0335204,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources0.news.com.au\",\"function\":\"max\",\"typical\":31356,\"actual\":277136}],\"normalizedProbability\":0.0655063,\"anomalyScore\":1.19192},{\"probability\":0.0354927,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.southwest.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0354927,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.southwest.com\",\"function\":\"max\",\"typical\":31356,\"actual\":266310}],\"normalizedProbability\":0.0544615,\"anomalyScore\":1.19192},{\"probability\":0.0392043,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"syndication.twimg.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0392043,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"syndication.twimg.com\",\"function\":\"max\",\"typical\":31356,\"actual\":248276}],\"normalizedProbability\":0.0366913,\"anomalyScore\":1.19192},{\"probability\":0.0400853,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"mts0.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0400853,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"mts0.google.com\",\"function\":\"max\",\"typical\":31356,\"actual\":244381}],\"normalizedProbability\":0.0329562,\"anomalyScore\":1.19192},{\"probability\":0.0407335,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.onthegotours.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0407335,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.onthegotours.com\",\"function\":\"max\",\"typical\":31356,\"actual\":241600}],\"normalizedProbability\":0.0303116,\"anomalyScore\":1.19192},{\"probability\":0.0470889,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"chatenabled.mail.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0470889,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"chatenabled.mail.google.com\",\"function\":\"max\",\"typical\":31356,\"actual\":217573}],\"normalizedProbability\":0.00823738,\"anomalyScore\":1.19192},{\"probability\":0.0491243,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"googleads.g.doubleclick.net\",\"function\":\"max\",\"causes\":[{\"probability\":0.0491243,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"googleads.g.doubleclick.net\",\"function\":\"max\",\"typical\":31356,\"actual\":210926}],\"normalizedProbability\":0.00237509,\"anomalyScore\":1.19192}]}],\"rawAnomalyScore\":1.26918,\"anomalyScore\":1.19192,\"maxNormalizedProbability\":93.6213,\"recordCount\":34,\"eventCount\":1159}" +
            "]";

    /**
     * Simple results persister stores buckets and state in a local array.
     */
    public class FlushWaiterThread extends Thread
    {
        private final AutoDetectResultsParser m_ResultsParser;
        private final String m_FlushId;
        private volatile boolean m_GotAcknowledgement;

        public FlushWaiterThread(AutoDetectResultsParser resultsParser,
                                 String flushId)
        {
            m_ResultsParser = resultsParser;
            m_FlushId = flushId;
        }


        @Override
        public void run()
        {
            try
            {
                m_ResultsParser.waitForParseStart();
                m_GotAcknowledgement = m_ResultsParser.waitForFlushAcknowledgement(m_FlushId);
            }
            catch (InterruptedException e)
            {
                fail("Flush waiter run should not have been interrupted");
            }
        }

        public void joinNoInterrupt()
        {
            try
            {
                join();
            }
            catch (InterruptedException e)
            {
                fail("Flush waiter join should not have been interrupted");
            }
        }


        public boolean gotAcknowledgement()
        {
            return m_GotAcknowledgement;
        }
    }


    /**
     * Simple results persister stores buckets and state in a local array.
     */
    public class ResultsPersister implements JobResultsPersister
    {
        List<Bucket> m_Buckets = new ArrayList<>();
        SortedMap<String, Quantiles> m_Quantiles = new TreeMap<>();
        List<Influencer> m_Influencers = new ArrayList<>();
        int m_BucketCount;

        @Override
        public void persistBucket(Bucket bucket)
        {
            m_Buckets.add(bucket);
        }

        @Override
        public void persistCategoryDefinition(CategoryDefinition category)
        {
            // Do nothing
        }

        @Override
        public void persistQuantiles(Quantiles quantiles)
        {
            m_Quantiles.put(quantiles.getId(), quantiles);
        }

        @Override
        public void persistModelSizeStats(ModelSizeStats memUsagae)
        {

        }

        @Override
        public boolean commitWrites()
        {
            return true;
        }

        public List<Bucket> getBuckets()
        {
            return m_Buckets;
        }


        public SortedMap<String, Quantiles> getQuantiles()
        {
            return m_Quantiles;
        }

        @Override
        public void incrementBucketCount(long count)
        {
            m_BucketCount += count;
        }

        @Override
        public void persistInfluencer(Influencer influencer)
        {
            m_Influencers.add(influencer);
        }

        @Override
        public void updateBucket(String jobId, Bucket bucket)
        {
            // Do nothing
        }

        @Override
        public void updateRecords(String jobId, String bucketId, List<AnomalyRecord> records)
        {
            // Do nothing
        }

        @Override
        public void updateInfluencer(String jobId, Influencer influencer)
        {
            // Do nothing
        }
    }


    public class AlertListener extends AlertObserver
    {
        public AlertListener(double normlizedProbThreshold, double anomalyThreshold)
        {
            super(normlizedProbThreshold, anomalyThreshold);
        }

        private boolean m_AlertFired = false;
        public double m_AnomalyScore;
        public double m_NormalisedProb;

        @Override
        public void fire(Bucket bucket)
        {
            m_AlertFired = true;
            m_AnomalyScore = bucket.getAnomalyScore();
            m_NormalisedProb = bucket.getMaxNormalizedProbability();
        }

        public boolean isFired()
        {
            return m_AlertFired;
        }
    }

    @Test
    public void testParser() throws JsonParseException, IOException,
    AutoDetectParseException, UnknownJobException
    {
        BasicConfigurator.configure();
        Logger logger = Logger.getLogger(AutoDetectResultsParserTest.class);

        InputStream inputStream = new ByteArrayInputStream(METRIC_OUTPUT_SAMPLE.getBytes(StandardCharsets.UTF_8));
        ResultsPersister persister = new ResultsPersister();
        BlockingQueueRenormaliser renormaliser = Mockito.mock(BlockingQueueRenormaliser.class);

        AutoDetectResultsParser parser = new AutoDetectResultsParser();

        FlushWaiterThread flushWaiter1 = new FlushWaiterThread(parser, "testing1");
        FlushWaiterThread flushWaiter2 = new FlushWaiterThread(parser, "testing2");
        flushWaiter1.start();
        flushWaiter2.start();

        parser.parseResults(inputStream, persister, renormaliser, logger);

        flushWaiter1.joinNoInterrupt();
        flushWaiter2.joinNoInterrupt();
        assertTrue(flushWaiter1.gotAcknowledgement());
        assertFalse(flushWaiter2.gotAcknowledgement());

        List<Bucket> buckets = persister.getBuckets();

        assertEquals(2, buckets.size());
        assertEquals(buckets.size(), persister.m_BucketCount);
        assertEquals(new Date(1359450000000L), buckets.get(0).getTimestamp());
        assertEquals(0, buckets.get(0).getRecordCount());

        int recordCount = 0;
        for (Detector d : buckets.get(0).getDetectors())
        {
            recordCount += d.getRecords().size();
        }
        assertEquals(recordCount, buckets.get(0).getRecordCount());

        assertEquals(buckets.get(0).getEventCount(), 806);
        assertEquals(0.0, buckets.get(0).getRawAnomalyScore(), 0.000001);

        assertEquals(new Date(1359453600000L), buckets.get(1).getTimestamp());
        assertEquals(4, buckets.get(1).getRecordCount());

        recordCount = 0;
        for (Detector d : buckets.get(1).getDetectors())
        {
            recordCount += d.getRecords().size();
        }
        assertEquals(recordCount, buckets.get(1).getRecordCount());

        assertEquals(buckets.get(1).getEventCount(), 820);
        assertEquals(0.0140005, buckets.get(1).getRawAnomalyScore(), 0.000001);

        com.prelert.job.results.Detector detector = buckets.get(1).getDetectors().get(0);

        assertEquals("individual metric/responsetime/airline", detector.getName());
        assertEquals(4, detector.getRecords().size());

        assertEquals(0.0637541, detector.getRecords().get(0).getProbability(), 0.000001);
        assertEquals("airline", detector.getRecords().get(0).getByFieldName());
        assertEquals("JZA", detector.getRecords().get(0).getByFieldValue());
        assertEquals(1020.08, 0.001, detector.getRecords().get(0).getTypical());
        assertEquals(1042.14, 0.001, detector.getRecords().get(0).getActual());
        assertEquals("responsetime", detector.getRecords().get(0).getFieldName());
        assertEquals("max", detector.getRecords().get(0).getFunction());
        assertEquals("", detector.getRecords().get(0).getPartitionFieldName());
        assertEquals("", detector.getRecords().get(0).getPartitionFieldValue());

        assertEquals(0.00748292, detector.getRecords().get(1).getProbability(), 0.000001);
        assertEquals("airline", detector.getRecords().get(1).getByFieldName());
        assertEquals("AMX", detector.getRecords().get(1).getByFieldValue());
        assertEquals(20.2137, 0.001, detector.getRecords().get(1).getTypical());
        assertEquals(22.8855, 0.001, detector.getRecords().get(1).getActual());
        assertEquals("responsetime", detector.getRecords().get(1).getFieldName());
        assertEquals("max", detector.getRecords().get(1).getFunction());
        assertEquals("", detector.getRecords().get(1).getPartitionFieldName());
        assertEquals("", detector.getRecords().get(1).getPartitionFieldValue());

        assertEquals(0.023494, detector.getRecords().get(2).getProbability(), 0.000001);
        assertEquals("airline", detector.getRecords().get(2).getByFieldName());
        assertEquals("DAL", detector.getRecords().get(2).getByFieldValue());
        assertEquals(382.177, 0.001, detector.getRecords().get(2).getTypical());
        assertEquals(358.934, 0.001, detector.getRecords().get(2).getActual());
        assertEquals("responsetime", detector.getRecords().get(2).getFieldName());
        assertEquals("min", detector.getRecords().get(2).getFunction());
        assertEquals("", detector.getRecords().get(2).getPartitionFieldName());
        assertEquals("", detector.getRecords().get(2).getPartitionFieldValue());

        assertEquals(0.0473552, detector.getRecords().get(3).getProbability(), 0.000001);
        assertEquals("airline", detector.getRecords().get(3).getByFieldName());
        assertEquals("SWA", detector.getRecords().get(3).getByFieldValue());
        assertEquals(152.148, 0.001, detector.getRecords().get(3).getTypical());
        assertEquals(96.6425, 0.001, detector.getRecords().get(3).getActual());
        assertEquals("responsetime", detector.getRecords().get(3).getFieldName());
        assertEquals("min", detector.getRecords().get(3).getFunction());
        assertEquals("", detector.getRecords().get(3).getPartitionFieldName());
        assertEquals("", detector.getRecords().get(3).getPartitionFieldValue());

        SortedMap<String, Quantiles> quantiles = persister.getQuantiles();

        assertEquals(1, quantiles.size());
        assertNotNull(quantiles.get("hierarchical"));
    }

    @Test
    public void testPopulationParser() throws JsonParseException, IOException,
    AutoDetectParseException, UnknownJobException
    {
        BasicConfigurator.configure();
        Logger logger = Logger.getLogger(AutoDetectResultsParserTest.class);

        InputStream inputStream = new ByteArrayInputStream(POPULATION_OUTPUT_SAMPLE.getBytes(StandardCharsets.UTF_8));
        ResultsPersister persister = new ResultsPersister();
        BlockingQueueRenormaliser renormaliser = Mockito.mock(BlockingQueueRenormaliser.class);

        AutoDetectResultsParser parser = new AutoDetectResultsParser();

        FlushWaiterThread flushWaiter1 = new FlushWaiterThread(parser, "testing1");
        FlushWaiterThread flushWaiter2 = new FlushWaiterThread(parser, "testing2");
        flushWaiter1.start();
        flushWaiter2.start();

        parser.parseResults(inputStream, persister, renormaliser, logger);

        flushWaiter1.joinNoInterrupt();
        flushWaiter2.joinNoInterrupt();
        assertFalse(flushWaiter1.gotAcknowledgement());
        assertTrue(flushWaiter2.gotAcknowledgement());

        List<Bucket> buckets = persister.getBuckets();

        assertEquals(2, buckets.size());
        assertEquals(buckets.size(), persister.m_BucketCount);
        assertEquals(new Date(1379590200000L), buckets.get(0).getTimestamp());
        assertEquals(4, buckets.get(0).getRecordCount());
        int recordCount = 0;
        for (Detector d : buckets.get(0).getDetectors())
        {
            recordCount += d.getRecords().size();
        }
        assertEquals(recordCount, buckets.get(0).getRecordCount());

        assertEquals(buckets.get(0).getEventCount(), 1235);
        assertEquals(1.30397, buckets.get(0).getRawAnomalyScore(), 0.000001);

        com.prelert.job.results.Detector detector = buckets.get(0).getDetectors().get(0);

        assertEquals("population metric maximum/0/sum_cs_bytes_//cs_host/", detector.getName());
        assertEquals(4, detector.getRecords().size());

        assertEquals(1.38951e-08, detector.getRecords().get(0).getProbability(), 0.000001);
        assertEquals("sum_cs_bytes_", detector.getRecords().get(0).getFieldName());
        assertEquals("max", detector.getRecords().get(0).getFunction());
        assertEquals("cs_host", detector.getRecords().get(0).getOverFieldName());
        assertEquals("mail.google.com", detector.getRecords().get(0).getOverFieldValue());
        assertNotNull(detector.getRecords().get(0).getCauses());

        assertEquals(new Date(1379590800000L), buckets.get(1).getTimestamp());
        assertEquals(34, buckets.get(1).getRecordCount());

        recordCount = 0;
        for (Detector d : buckets.get(1).getDetectors())
        {
            recordCount += d.getRecords().size();
        }
        assertEquals(recordCount, buckets.get(1).getRecordCount());

        assertEquals(buckets.get(1).getEventCount(), 1159);
        assertEquals(1.26918, buckets.get(1).getRawAnomalyScore(), 0.000001);

        detector = buckets.get(1).getDetectors().get(0);

        assertEquals("population metric maximum/0/sum_cs_bytes_//cs_host/", detector.getName());
        assertEquals(34, detector.getRecords().size());
    }

    /**
     * Register an alert listener and test it is fired
     * @throws AutoDetectParseException
     * @throws IOException
     * @throws JsonParseException
     */
    @Test
    public void testAlerting()
    throws JsonParseException, IOException, AutoDetectParseException
    {
        BasicConfigurator.configure();
        Logger logger = Logger.getLogger(AutoDetectResultsParserTest.class);

        // 1. normalised prob threshold
        InputStream inputStream = new ByteArrayInputStream(METRIC_OUTPUT_SAMPLE.getBytes(StandardCharsets.UTF_8));
        ResultsPersister persister = new ResultsPersister();
        BlockingQueueRenormaliser renormaliser = Mockito.mock(BlockingQueueRenormaliser.class);

        double probThreshold = 9.0;
        double scoreThreshold = 100.0;
        AlertListener listener = new AlertListener(probThreshold, scoreThreshold);

        AutoDetectResultsParser parser = new AutoDetectResultsParser();
        parser.addObserver(listener);
        parser.parseResults(inputStream, persister, renormaliser, logger);


        assertEquals(0, parser.observerCount());
        assertTrue(listener.isFired());
        assertTrue(listener.m_NormalisedProb >= probThreshold);

        // 2. anomaly score threshold
        inputStream = new ByteArrayInputStream(METRIC_OUTPUT_SAMPLE.getBytes(StandardCharsets.UTF_8));

        probThreshold = 100.0;
        scoreThreshold = 18.0;
        listener = new AlertListener(probThreshold, scoreThreshold);

        parser = new AutoDetectResultsParser();
        parser.addObserver(listener);
        parser.parseResults(inputStream, persister, renormaliser, logger);

        assertEquals(0, parser.observerCount());
        assertTrue(listener.isFired());
        assertTrue(listener.m_AnomalyScore >= scoreThreshold);

        // 3. neither threshold is reached
        inputStream = new ByteArrayInputStream(METRIC_OUTPUT_SAMPLE.getBytes(StandardCharsets.UTF_8));

        probThreshold = 100.0;
        scoreThreshold = 100.0;
        listener = new AlertListener(probThreshold, scoreThreshold);

        parser = new AutoDetectResultsParser();
        parser.addObserver(listener);
        parser.parseResults(inputStream, persister, renormaliser, logger);

        assertEquals(1, parser.observerCount());
        assertFalse(listener.isFired());
        assertTrue(listener.m_AnomalyScore < scoreThreshold  &&
                listener.m_NormalisedProb < probThreshold);


        // 4. register 2 listeners only one of which is fired
        inputStream = new ByteArrayInputStream(METRIC_OUTPUT_SAMPLE.getBytes(StandardCharsets.UTF_8));

        probThreshold = 100.0;
        scoreThreshold = 100.0;
        listener = new AlertListener(probThreshold, scoreThreshold);

        parser = new AutoDetectResultsParser();
        parser.addObserver(listener);

        probThreshold = 2.0;
        scoreThreshold = 1.0;
        AlertListener firedListener = new AlertListener(probThreshold, scoreThreshold);
        parser.addObserver(firedListener);

        parser.parseResults(inputStream, persister, renormaliser, logger);

        assertEquals(1, parser.observerCount());
        assertFalse(listener.isFired());
        assertTrue(listener.m_AnomalyScore < scoreThreshold  &&
                listener.m_NormalisedProb < probThreshold);

        assertTrue(firedListener.isFired());
        assertTrue(firedListener.m_AnomalyScore >= scoreThreshold  ||
                firedListener.m_NormalisedProb >= probThreshold);
    }

}
