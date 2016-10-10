
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.alert.AlertObserver;
import org.elasticsearch.xpack.prelert.job.alert.AlertTrigger;
import org.elasticsearch.xpack.prelert.job.alert.AlertType;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.prelert.job.process.normalizer.Renormaliser;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.job.results.ModelDebugOutput;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.mockito.Mockito.mock;

/**
 * Tests for parsing the JSON output of autodetect_api
 */
public class AutoDetectResultsParserTest extends ESTestCase {
    private static final double EPSILON = 0.000001;

    public static final String METRIC_OUTPUT_SAMPLE = "[{\"timestamp\":1359450000,\"records\":[],\"maxNormalizedProbability\":0,\"anomalyScore\":0,\"recordCount\":0,\"eventCount\":806,\"bucketInfluencers\":[{\"rawAnomalyScore\":0, \"probability\":0.0,\"influencerFieldName\":\"bucketTime\",\"initialAnomalyScore\":0.0}]}" +
            ",{\"quantileState\":[\"normaliser 1.1\", \"normaliser 2.1\"]}" +
            ",{\"timestamp\":1359453600,\"records\":[{\"probability\":0.0637541,\"byFieldName\":\"airline\",\"byFieldValue\":\"JZA\",\"typical\":[1020.08],\"actual\":[1042.14],\"fieldName\":\"responsetime\",\"function\":\"max\",\"partitionFieldName\":\"\",\"partitionFieldValue\":\"\"},{\"probability\":0.00748292,\"byFieldName\":\"airline\",\"byFieldValue\":\"AMX\",\"typical\":[20.2137],\"actual\":[22.8855],\"fieldName\":\"responsetime\",\"function\":\"max\",\"partitionFieldName\":\"\",\"partitionFieldValue\":\"\"},{\"probability\":0.023494,\"byFieldName\":\"airline\",\"byFieldValue\":\"DAL\",\"typical\":[382.177],\"actual\":[358.934],\"fieldName\":\"responsetime\",\"function\":\"min\",\"partitionFieldName\":\"\",\"partitionFieldValue\":\"\"},{\"probability\":0.0473552,\"byFieldName\":\"airline\",\"byFieldValue\":\"SWA\",\"typical\":[152.148],\"actual\":[96.6425],\"fieldName\":\"responsetime\",\"function\":\"min\",\"partitionFieldName\":\"\",\"partitionFieldValue\":\"\"}],\"rawAnomalyScore\":0.0140005, \"anomalyScore\":20.22688,\"maxNormalizedProbability\":10.5688, \"recordCount\":4,\"eventCount\":820,\"bucketInfluencers\":[{\"rawAnomalyScore\":0.0140005, \"probability\":0.01,\"influencerFieldName\":\"bucketTime\",\"initialAnomalyScore\":20.22688},{\"rawAnomalyScore\":0.005, \"probability\":0.03,\"influencerFieldName\":\"foo\",\"initialAnomalyScore\":10.5}]}" +
            ",{\"quantileState\":[\"normaliser 1.2\", \"normaliser 2.2\"]}" +
            ",{\"flush\":\"testing1\"}" +
            ",{\"quantileState\":[\"normaliser 1.3\", \"normaliser 2.3\"]}" +
            "]";

    public static final String POPULATION_OUTPUT_SAMPLE = "[{\"timestamp\":1379590200,\"records\":[{\"probability\":1.38951e-08,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"mail.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":1.38951e-08,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"mail.google.com\",\"function\":\"max\",\"typical\":[101534],\"actual\":[9.19027e+07]}],\"normalizedProbability\":100,\"anomalyScore\":44.7324},{\"probability\":3.86587e-07,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"armmf.adobe.com\",\"function\":\"max\",\"causes\":[{\"probability\":3.86587e-07,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"armmf.adobe.com\",\"function\":\"max\",\"typical\":[101534],\"actual\":[3.20093e+07]}],\"normalizedProbability\":89.5834,\"anomalyScore\":44.7324},{\"probability\":0.00500083,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0.docs.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00500083,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0.docs.google.com\",\"function\":\"max\",\"typical\":[101534],\"actual\":[6.61812e+06]}],\"normalizedProbability\":1.19856,\"anomalyScore\":44.7324},{\"probability\":0.0152333,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"emea.salesforce.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0152333,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"emea.salesforce.com\",\"function\":\"max\",\"typical\":[101534],\"actual\":[5.36373e+06]}],\"normalizedProbability\":0.303996,\"anomalyScore\":44.7324}],\"rawAnomalyScore\":1.30397,\"anomalyScore\":44.7324,\"maxNormalizedProbability\":100,\"recordCount\":4,\"eventCount\":1235}" +
            ",{\"flush\":\"testing2\"}" +
            ",{\"timestamp\":1379590800,\"records\":[{\"probability\":1.9008e-08,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"mail.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":1.9008e-08,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"mail.google.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[1.1498e+08]}],\"normalizedProbability\":93.6213,\"anomalyScore\":1.19192},{\"probability\":1.01013e-06,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"armmf.adobe.com\",\"function\":\"max\",\"causes\":[{\"probability\":1.01013e-06,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"armmf.adobe.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[3.25808e+07]}],\"normalizedProbability\":86.5825,\"anomalyScore\":1.19192},{\"probability\":0.000386185,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0.docs.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.000386185,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0.docs.google.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[3.22855e+06]}],\"normalizedProbability\":17.1179,\"anomalyScore\":1.19192},{\"probability\":0.00208033,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"docs.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00208033,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"docs.google.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[1.43328e+06]}],\"normalizedProbability\":3.0692,\"anomalyScore\":1.19192},{\"probability\":0.00312988,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"booking2.airasia.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00312988,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"booking2.airasia.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[1.15764e+06]}],\"normalizedProbability\":1.99532,\"anomalyScore\":1.19192},{\"probability\":0.00379229,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.facebook.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00379229,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.facebook.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[1.0443e+06]}],\"normalizedProbability\":1.62352,\"anomalyScore\":1.19192},{\"probability\":0.00623576,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.airasia.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00623576,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.airasia.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[792699]}],\"normalizedProbability\":0.935134,\"anomalyScore\":1.19192},{\"probability\":0.00665308,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00665308,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.google.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[763985]}],\"normalizedProbability\":0.868119,\"anomalyScore\":1.19192},{\"probability\":0.00709315,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0.drive.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.00709315,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0.drive.google.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[736442]}],\"normalizedProbability\":0.805994,\"anomalyScore\":1.19192},{\"probability\":0.00755789,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources2.news.com.au\",\"function\":\"max\",\"causes\":[{\"probability\":0.00755789,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources2.news.com.au\",\"function\":\"max\",\"typical\":[31356],\"actual\":[709962]}],\"normalizedProbability\":0.748239,\"anomalyScore\":1.19192},{\"probability\":0.00834974,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.calypso.net.au\",\"function\":\"max\",\"causes\":[{\"probability\":0.00834974,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.calypso.net.au\",\"function\":\"max\",\"typical\":[31356],\"actual\":[669968]}],\"normalizedProbability\":0.664644,\"anomalyScore\":1.19192},{\"probability\":0.0107711,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"ad.yieldmanager.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0107711,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"ad.yieldmanager.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[576067]}],\"normalizedProbability\":0.485277,\"anomalyScore\":1.19192},{\"probability\":0.0123367,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.google-analytics.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0123367,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.google-analytics.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[530594]}],\"normalizedProbability\":0.406783,\"anomalyScore\":1.19192},{\"probability\":0.0125647,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"bs.serving-sys.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0125647,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"bs.serving-sys.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[524690]}],\"normalizedProbability\":0.396986,\"anomalyScore\":1.19192},{\"probability\":0.0141652,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.google.com.au\",\"function\":\"max\",\"causes\":[{\"probability\":0.0141652,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.google.com.au\",\"function\":\"max\",\"typical\":[31356],\"actual\":[487328]}],\"normalizedProbability\":0.337075,\"anomalyScore\":1.19192},{\"probability\":0.0141742,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources1.news.com.au\",\"function\":\"max\",\"causes\":[{\"probability\":0.0141742,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources1.news.com.au\",\"function\":\"max\",\"typical\":[31356],\"actual\":[487136]}],\"normalizedProbability\":0.336776,\"anomalyScore\":1.19192},{\"probability\":0.0145263,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"b.mail.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0145263,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"b.mail.google.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[479766]}],\"normalizedProbability\":0.325385,\"anomalyScore\":1.19192},{\"probability\":0.0151447,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.rei.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0151447,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.rei.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[467450]}],\"normalizedProbability\":0.306657,\"anomalyScore\":1.19192},{\"probability\":0.0164073,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"s3.amazonaws.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0164073,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"s3.amazonaws.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[444511]}],\"normalizedProbability\":0.272805,\"anomalyScore\":1.19192},{\"probability\":0.0201927,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0-p-06-ash2.channel.facebook.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0201927,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"0-p-06-ash2.channel.facebook.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[389243]}],\"normalizedProbability\":0.196685,\"anomalyScore\":1.19192},{\"probability\":0.0218721,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"booking.airasia.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0218721,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"booking.airasia.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[369509]}],\"normalizedProbability\":0.171353,\"anomalyScore\":1.19192},{\"probability\":0.0242411,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.yammer.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0242411,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.yammer.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[345295]}],\"normalizedProbability\":0.141585,\"anomalyScore\":1.19192},{\"probability\":0.0258232,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"safebrowsing-cache.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0258232,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"safebrowsing-cache.google.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[331051]}],\"normalizedProbability\":0.124748,\"anomalyScore\":1.19192},{\"probability\":0.0259695,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"fbcdn-profile-a.akamaihd.net\",\"function\":\"max\",\"causes\":[{\"probability\":0.0259695,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"fbcdn-profile-a.akamaihd.net\",\"function\":\"max\",\"typical\":[31356],\"actual\":[329801]}],\"normalizedProbability\":0.123294,\"anomalyScore\":1.19192},{\"probability\":0.0268874,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.oag.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0268874,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.oag.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[322200]}],\"normalizedProbability\":0.114537,\"anomalyScore\":1.19192},{\"probability\":0.0279146,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"booking.qatarairways.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0279146,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"booking.qatarairways.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[314153]}],\"normalizedProbability\":0.105419,\"anomalyScore\":1.19192},{\"probability\":0.0309351,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources3.news.com.au\",\"function\":\"max\",\"causes\":[{\"probability\":0.0309351,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources3.news.com.au\",\"function\":\"max\",\"typical\":[31356],\"actual\":[292918]}],\"normalizedProbability\":0.0821156,\"anomalyScore\":1.19192},{\"probability\":0.0335204,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources0.news.com.au\",\"function\":\"max\",\"causes\":[{\"probability\":0.0335204,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"resources0.news.com.au\",\"function\":\"max\",\"typical\":[31356],\"actual\":[277136]}],\"normalizedProbability\":0.0655063,\"anomalyScore\":1.19192},{\"probability\":0.0354927,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.southwest.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0354927,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.southwest.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[266310]}],\"normalizedProbability\":0.0544615,\"anomalyScore\":1.19192},{\"probability\":0.0392043,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"syndication.twimg.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0392043,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"syndication.twimg.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[248276]}],\"normalizedProbability\":0.0366913,\"anomalyScore\":1.19192},{\"probability\":0.0400853,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"mts0.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0400853,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"mts0.google.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[244381]}],\"normalizedProbability\":0.0329562,\"anomalyScore\":1.19192},{\"probability\":0.0407335,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.onthegotours.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0407335,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"www.onthegotours.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[241600]}],\"normalizedProbability\":0.0303116,\"anomalyScore\":1.19192},{\"probability\":0.0470889,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"chatenabled.mail.google.com\",\"function\":\"max\",\"causes\":[{\"probability\":0.0470889,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"chatenabled.mail.google.com\",\"function\":\"max\",\"typical\":[31356],\"actual\":[217573]}],\"normalizedProbability\":0.00823738,\"anomalyScore\":1.19192},{\"probability\":0.0491243,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"googleads.g.doubleclick.net\",\"function\":\"max\",\"causes\":[{\"probability\":0.0491243,\"fieldName\":\"sum_cs_bytes_\",\"overFieldName\":\"cs_host\",\"overFieldValue\":\"googleads.g.doubleclick.net\",\"function\":\"max\",\"typical\":[31356],\"actual\":[210926]}],\"normalizedProbability\":0.00237509,\"anomalyScore\":1.19192}],\"rawAnomalyScore\":1.26918,\"anomalyScore\":1.19192,\"maxNormalizedProbability\":93.6213,\"recordCount\":34,\"eventCount\":1159}" +
            "]";

    /**
     * Simple results persister stores buckets and state in a local array.
     */
    public class FlushWaiterThread extends Thread {
        private final AutoDetectResultsParser resultsParser;
        private final String flushId;
        private volatile boolean gotAcknowledgement;

        public FlushWaiterThread(AutoDetectResultsParser resultsParser,
                                 String flushId) {
            this.resultsParser = resultsParser;
            this.flushId = flushId;
        }


        @Override
        public void run() {
            resultsParser.waitForParseStart();
            while (!gotAcknowledgement) {
                gotAcknowledgement = resultsParser.waitForFlushAcknowledgement(flushId, Duration.ofMillis(1000));
            }
        }

        public void joinNoInterrupt() {
            try {
                join();
            } catch (InterruptedException e) {
                fail("Flush waiter join should not have been interrupted");
            }
        }


        public boolean gotAcknowledgement() {
            return gotAcknowledgement;
        }
    }


    /**
     * Simple results persister stores buckets and state in a local array.
     */
    public class ResultsPersister implements JobResultsPersister {
        final List<Bucket> buckets = new ArrayList<>();
        final SortedMap<String, Quantiles> quantiles = new TreeMap<>();
        final List<Influencer> influencers = new ArrayList<>();
        final List<CategoryDefinition> categoryDefinitions = new ArrayList<>();
        final List<ModelSizeStats> modelSizeStats = new ArrayList<>();
        final List<ModelDebugOutput> modelDebugOutput = new ArrayList<>();
        final SortedMap<String, ModelSnapshot> modelSnapshots = new TreeMap<>();
        int bucketCount;
        long processingTimeMs;

        @Override
        public void persistBucket(Bucket bucket) {
            buckets.add(bucket);
        }

        @Override
        public void persistCategoryDefinition(CategoryDefinition category) {
            categoryDefinitions.add(category);
        }

        @Override
        public void persistQuantiles(Quantiles quantiles) {
            this.quantiles.put(Quantiles.QUANTILES_ID, quantiles);
        }

        @Override
        public void persistModelSnapshot(ModelSnapshot modelSnapshot) {
            modelSnapshots.put(modelSnapshot.getSnapshotId(), modelSnapshot);
        }

        @Override
        public void persistModelSizeStats(ModelSizeStats memUsagae) {
            modelSizeStats.add(memUsagae);
        }

        @Override
        public void persistModelDebugOutput(ModelDebugOutput modelDebugOutput) {
            this.modelDebugOutput.add(modelDebugOutput);
        }

        @Override
        public boolean commitWrites() {
            return true;
        }

        public List<Bucket> getBuckets() {
            return buckets;
        }


        public SortedMap<String, Quantiles> getQuantiles() {
            return quantiles;
        }

        @Override
        public void incrementBucketCount(long count) {
            bucketCount += count;
        }

        @Override
        public void persistInfluencer(Influencer influencer) {
            influencers.add(influencer);
        }

        @Override
        public void deleteInterimResults() {
        }

        @Override
        public void updateAverageBucketProcessingTime(long timeMs) {
            processingTimeMs = timeMs;
        }
    }


    public class AlertListener extends AlertObserver {
        public AlertListener(double normlizedProbThreshold, double anomalyThreshold) {
            super(new AlertTrigger[]{new AlertTrigger(normlizedProbThreshold, anomalyThreshold,
                    AlertType.BUCKET)}, "foo-job");
        }

        private boolean alertFired = false;
        public double anomalyScore;
        public double normalisedProb;

        @Override
        public void fire(Bucket bucket, AlertTrigger trigger) {
            alertFired = true;
            anomalyScore = bucket.getAnomalyScore();
            normalisedProb = bucket.getMaxNormalizedProbability();
        }

        public boolean isFired() {
            return alertFired;
        }
    }

    public void testParser() throws IOException {
        Logger logger = Loggers.getLogger(AutoDetectResultsParserTest.class);

        InputStream inputStream = new ByteArrayInputStream(METRIC_OUTPUT_SAMPLE.getBytes(StandardCharsets.UTF_8));
        ResultsPersister persister = new ResultsPersister();
        Renormaliser renormaliser = Mockito.mock(Renormaliser.class);

        AutoDetectResultsParser parser = new AutoDetectResultsParser();

        FlushWaiterThread flushWaiter = new FlushWaiterThread(parser, "testing1");
        flushWaiter.start();

        parser.parseResults(inputStream, persister, renormaliser, logger);

        flushWaiter.joinNoInterrupt();
        assertTrue(flushWaiter.gotAcknowledgement());

        List<Bucket> buckets = persister.getBuckets();

        assertEquals(2, buckets.size());
        assertEquals(buckets.size(), persister.bucketCount);
        assertEquals(new Date(1359450000000L), buckets.get(0).getTimestamp());
        assertEquals(0, buckets.get(0).getRecordCount());

        assertEquals(buckets.get(0).getEventCount(), 806);

        List<BucketInfluencer> bucketInfluencers = buckets.get(0).getBucketInfluencers();
        assertEquals(1, bucketInfluencers.size());
        assertEquals(0.0, bucketInfluencers.get(0).getRawAnomalyScore(), EPSILON);
        assertEquals(0.0, bucketInfluencers.get(0).getAnomalyScore(), EPSILON);
        assertEquals(0.0, bucketInfluencers.get(0).getProbability(), EPSILON);
        assertEquals("bucketTime", bucketInfluencers.get(0).getInfluencerFieldName());

        assertEquals(new Date(1359453600000L), buckets.get(1).getTimestamp());
        assertEquals(4, buckets.get(1).getRecordCount());

        assertEquals(buckets.get(1).getEventCount(), 820);
        bucketInfluencers = buckets.get(1).getBucketInfluencers();
        assertEquals(2, bucketInfluencers.size());
        assertEquals(0.0140005, bucketInfluencers.get(0).getRawAnomalyScore(), EPSILON);
        assertEquals(20.22688, bucketInfluencers.get(0).getAnomalyScore(), EPSILON);
        assertEquals(0.01, bucketInfluencers.get(0).getProbability(), EPSILON);
        assertEquals("bucketTime", bucketInfluencers.get(0).getInfluencerFieldName());
        assertEquals(0.005, bucketInfluencers.get(1).getRawAnomalyScore(), EPSILON);
        assertEquals(10.5, bucketInfluencers.get(1).getAnomalyScore(), EPSILON);
        assertEquals(0.03, bucketInfluencers.get(1).getProbability(), EPSILON);
        assertEquals("foo", bucketInfluencers.get(1).getInfluencerFieldName());

        Bucket secondBucket = buckets.get(1);

        assertEquals(0.0637541, secondBucket.getRecords().get(0).getProbability(), EPSILON);
        assertEquals("airline", secondBucket.getRecords().get(0).getByFieldName());
        assertEquals("JZA", secondBucket.getRecords().get(0).getByFieldValue());
        assertEquals(1020.08, secondBucket.getRecords().get(0).getTypical()[0], EPSILON);
        assertEquals(1042.14, secondBucket.getRecords().get(0).getActual()[0], EPSILON);
        assertEquals("responsetime", secondBucket.getRecords().get(0).getFieldName());
        assertEquals("max", secondBucket.getRecords().get(0).getFunction());
        assertEquals("", secondBucket.getRecords().get(0).getPartitionFieldName());
        assertEquals("", secondBucket.getRecords().get(0).getPartitionFieldValue());

        assertEquals(0.00748292, secondBucket.getRecords().get(1).getProbability(), EPSILON);
        assertEquals("airline", secondBucket.getRecords().get(1).getByFieldName());
        assertEquals("AMX", secondBucket.getRecords().get(1).getByFieldValue());
        assertEquals(20.2137, secondBucket.getRecords().get(1).getTypical()[0], EPSILON);
        assertEquals(22.8855, secondBucket.getRecords().get(1).getActual()[0], EPSILON);
        assertEquals("responsetime", secondBucket.getRecords().get(1).getFieldName());
        assertEquals("max", secondBucket.getRecords().get(1).getFunction());
        assertEquals("", secondBucket.getRecords().get(1).getPartitionFieldName());
        assertEquals("", secondBucket.getRecords().get(1).getPartitionFieldValue());

        assertEquals(0.023494, secondBucket.getRecords().get(2).getProbability(), EPSILON);
        assertEquals("airline", secondBucket.getRecords().get(2).getByFieldName());
        assertEquals("DAL", secondBucket.getRecords().get(2).getByFieldValue());
        assertEquals(382.177, secondBucket.getRecords().get(2).getTypical()[0], EPSILON);
        assertEquals(358.934, secondBucket.getRecords().get(2).getActual()[0], EPSILON);
        assertEquals("responsetime", secondBucket.getRecords().get(2).getFieldName());
        assertEquals("min", secondBucket.getRecords().get(2).getFunction());
        assertEquals("", secondBucket.getRecords().get(2).getPartitionFieldName());
        assertEquals("", secondBucket.getRecords().get(2).getPartitionFieldValue());

        assertEquals(0.0473552, secondBucket.getRecords().get(3).getProbability(), EPSILON);
        assertEquals("airline", secondBucket.getRecords().get(3).getByFieldName());
        assertEquals("SWA", secondBucket.getRecords().get(3).getByFieldValue());
        assertEquals(152.148, secondBucket.getRecords().get(3).getTypical()[0], EPSILON);
        assertEquals(96.6425, secondBucket.getRecords().get(3).getActual()[0], EPSILON);
        assertEquals("responsetime", secondBucket.getRecords().get(3).getFieldName());
        assertEquals("min", secondBucket.getRecords().get(3).getFunction());
        assertEquals("", secondBucket.getRecords().get(3).getPartitionFieldName());
        assertEquals("", secondBucket.getRecords().get(3).getPartitionFieldValue());

        SortedMap<String, Quantiles> quantiles = persister.getQuantiles();

        assertEquals(1, quantiles.size());
        assertNotNull(quantiles.get("hierarchical"));
    }

    public void testPopulationParser() throws IOException, UnknownJobException {
        Logger logger = Loggers.getLogger(AutoDetectResultsParserTest.class);

        InputStream inputStream = new ByteArrayInputStream(POPULATION_OUTPUT_SAMPLE.getBytes(StandardCharsets.UTF_8));
        ResultsPersister persister = new ResultsPersister();
        Renormaliser renormaliser = Mockito.mock(Renormaliser.class);

        AutoDetectResultsParser parser = new AutoDetectResultsParser();

        FlushWaiterThread flushWaiter = new FlushWaiterThread(parser, "testing2");
        flushWaiter.start();

        parser.parseResults(inputStream, persister, renormaliser, logger);

        flushWaiter.joinNoInterrupt();
        assertTrue(flushWaiter.gotAcknowledgement());

        List<Bucket> buckets = persister.getBuckets();

        assertEquals(2, buckets.size());
        assertEquals(buckets.size(), persister.bucketCount);
        assertEquals(new Date(1379590200000L), buckets.get(0).getTimestamp());
        assertEquals(4, buckets.get(0).getRecordCount());
        assertEquals(buckets.get(0).getEventCount(), 1235);

        Bucket firstBucket = buckets.get(0);
        assertEquals(1.38951e-08, firstBucket.getRecords().get(0).getProbability(), EPSILON);
        assertEquals("sum_cs_bytes_", firstBucket.getRecords().get(0).getFieldName());
        assertEquals("max", firstBucket.getRecords().get(0).getFunction());
        assertEquals("cs_host", firstBucket.getRecords().get(0).getOverFieldName());
        assertEquals("mail.google.com", firstBucket.getRecords().get(0).getOverFieldValue());
        assertNotNull(firstBucket.getRecords().get(0).getCauses());

        assertEquals(new Date(1379590800000L), buckets.get(1).getTimestamp());
        assertEquals(34, buckets.get(1).getRecordCount());
        assertEquals(buckets.get(1).getEventCount(), 1159);
    }

    /**
     * Register an alert listener and test it is fired
     *
     * @throws ElasticsearchParseException
     * @throws IOException
     * @throws ElasticsearchParseException
     */
    public void testAlerting() throws IOException {
        Logger logger = Loggers.getLogger(AutoDetectResultsParserTest.class);

        // 1. normalised prob threshold
        InputStream inputStream = new ByteArrayInputStream(METRIC_OUTPUT_SAMPLE.getBytes(StandardCharsets.UTF_8));
        ResultsPersister persister = new ResultsPersister();
        Renormaliser renormaliser = Mockito.mock(Renormaliser.class);

        double probThreshold = 9.0;
        double scoreThreshold = 100.0;
        AlertListener listener = new AlertListener(probThreshold, scoreThreshold);

        AutoDetectResultsParser parser = new AutoDetectResultsParser();
        parser.addObserver(listener);
        parser.parseResults(inputStream, persister, renormaliser, logger);


        assertEquals(0, parser.observerCount());
        assertTrue(listener.isFired());
        assertTrue(listener.normalisedProb >= probThreshold);

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
        assertTrue(listener.anomalyScore >= scoreThreshold);

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
        assertTrue(listener.anomalyScore < scoreThreshold &&
                listener.normalisedProb < probThreshold);


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
        assertTrue(listener.anomalyScore < scoreThreshold &&
                listener.normalisedProb < probThreshold);

        assertTrue(firedListener.isFired());
        assertTrue(firedListener.anomalyScore >= scoreThreshold ||
                firedListener.normalisedProb >= probThreshold);
    }


    public void testParse_GivenEmptyArray() throws ElasticsearchParseException, IOException {
        String json = "[]";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        Logger logger = mock(Logger.class);
        JobResultsPersister persister = mock(JobResultsPersister.class);

        Renormaliser renormaliser = mock(Renormaliser.class);

        AutoDetectResultsParser parser = new AutoDetectResultsParser();

        parser.parseResults(inputStream, persister, renormaliser, logger);

        Mockito.verifyZeroInteractions(persister);
    }


    public void testParse_GivenModelSizeStats() throws ElasticsearchParseException, IOException {
        String json = "{\"modelBytes\":300}";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        Logger logger = mock(Logger.class);
        ResultsPersister persister = new ResultsPersister();
        Renormaliser renormaliser = mock(Renormaliser.class);

        AutoDetectResultsParser parser = new AutoDetectResultsParser();

        parser.parseResults(inputStream, persister, renormaliser, logger);

        assertEquals(1, persister.modelSizeStats.size());
        assertEquals(300, persister.modelSizeStats.get(0).getModelBytes());
    }


    public void testParse_GivenCategoryDefinition() throws IOException {
        String json = "[{\"categoryDefinition\":18}]";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        Logger logger = mock(Logger.class);
        ResultsPersister persister = new ResultsPersister();
        Renormaliser renormaliser = mock(Renormaliser.class);

        AutoDetectResultsParser parser = new AutoDetectResultsParser();

        parser.parseResults(inputStream, persister, renormaliser, logger);

        assertEquals(1, persister.categoryDefinitions.size());
        assertEquals(18, persister.categoryDefinitions.get(0).getCategoryId());
    }


    public void testParse_GivenUnknownObject() throws ElasticsearchParseException, IOException {
        String json = "{\"unknown\":18}";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        Logger logger = mock(Logger.class);
        ResultsPersister persister = new ResultsPersister();
        Renormaliser renormaliser = mock(Renormaliser.class);

        AutoDetectResultsParser parser = new AutoDetectResultsParser();

        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> parser.parseResults(inputStream, persister, renormaliser, logger));

        assertEquals(e.getMessage(), "Invalid JSON  - unexpected object parsed from output - first field unknown");
    }


    public void testParse_GivenArrayContainsAnotherArray() throws ElasticsearchParseException, IOException {

        String json = "[[]]";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        Logger logger = mock(Logger.class);
        ResultsPersister persister = new ResultsPersister();
        Renormaliser renormaliser = mock(Renormaliser.class);

        AutoDetectResultsParser parser = new AutoDetectResultsParser();

        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> parser.parseResults(inputStream, persister, renormaliser, logger));

        assertEquals(e.getMessage(), "Invalid JSON should start with an array of objects or an object = START_ARRAY");
    }


    public void testRemoveObserver() throws ElasticsearchParseException, IOException {
        AutoDetectResultsParser parser = new AutoDetectResultsParser();
        AlertObserver alertObserver = mock(AlertObserver.class);
        parser.addObserver(alertObserver);

        assertEquals(1, parser.observerCount());

        parser.removeObserver(alertObserver);

        assertEquals(0, parser.observerCount());
    }


    public void testParse_GivenInterimBucket_ShouldNotNotifyObserver() throws ElasticsearchParseException,
            IOException {
        String json = "{\"timestamp\":1359450000,\"anomalyScore\":99.0, \"isInterim\":true}";


        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        Logger logger = mock(Logger.class);
        ResultsPersister persister = new ResultsPersister();
        Renormaliser renormaliser = mock(Renormaliser.class);
        AutoDetectResultsParser parser = new AutoDetectResultsParser();
        AlertListener alertListener = new AlertListener(90.0, 90.0);
        parser.addObserver(alertListener);

        parser.parseResults(inputStream, persister, renormaliser, logger);

        assertEquals(1, persister.bucketCount);

        assertFalse(alertListener.isFired());
    }


    public void testParse_GivenBucketWithInterimFalse_ShouldNotifyObserver() throws IOException {
        String json = "{\"timestamp\":1359450000,\"anomalyScore\":99.0, \"isInterim\":false}";


        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        Logger logger = mock(Logger.class);
        ResultsPersister persister = new ResultsPersister();
        Renormaliser renormaliser = mock(Renormaliser.class);
        AutoDetectResultsParser parser = new AutoDetectResultsParser();
        AlertListener alertListener = new AlertListener(90.0, 90.0);
        parser.addObserver(alertListener);

        parser.parseResults(inputStream, persister, renormaliser, logger);

        assertEquals(1, persister.bucketCount);

        assertTrue(alertListener.isFired());
    }
}