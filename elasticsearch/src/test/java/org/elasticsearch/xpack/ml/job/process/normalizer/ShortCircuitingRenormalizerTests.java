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
package org.elasticsearch.xpack.ml.job.process.normalizer;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.quantiles.Quantiles;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class ShortCircuitingRenormalizerTests extends ESTestCase {
    private static final String JOB_ID = "foo";
    // Never reduce this below 4, otherwise some of the logic in the test will break
    private static final int TEST_SIZE = 1000;

    public void testNormalize() throws InterruptedException {
        ExecutorService threadpool = Executors.newScheduledThreadPool(10);
        try {
            ScoresUpdater scoresUpdater = mock(ScoresUpdater.class);

            boolean isPerPartitionNormalization = randomBoolean();

            ShortCircuitingRenormalizer renormalizer = new ShortCircuitingRenormalizer(JOB_ID, scoresUpdater, threadpool,
                    isPerPartitionNormalization);

            // Blast through many sets of quantiles in quick succession, faster than the normalizer can process them
            for (int i = 1; i < TEST_SIZE / 2; ++i) {
                Quantiles quantiles = new Quantiles(JOB_ID, new Date(), Integer.toString(i));
                renormalizer.renormalize(quantiles);
            }
            renormalizer.waitUntilIdle();
            for (int i = TEST_SIZE / 2; i <= TEST_SIZE; ++i) {
                Quantiles quantiles = new Quantiles(JOB_ID, new Date(), Integer.toString(i));
                renormalizer.renormalize(quantiles);
            }
            renormalizer.waitUntilIdle();

            ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
            verify(scoresUpdater, atLeastOnce()).update(stateCaptor.capture(), anyLong(), anyLong(), eq(isPerPartitionNormalization));

            List<String> quantilesUsed = stateCaptor.getAllValues();
            assertFalse(quantilesUsed.isEmpty());
            assertTrue("quantilesUsed.size() is " + quantilesUsed.size(), quantilesUsed.size() <= TEST_SIZE);

            // Last quantiles state that was actually used must be the last quantiles state we supplied
            assertEquals(Integer.toString(TEST_SIZE), quantilesUsed.get(quantilesUsed.size() - 1));

            // Earlier quantiles states that were processed must have been processed in the supplied order
            int previous = 0;
            for (String state : quantilesUsed) {
                int current = Integer.parseInt(state);
                assertTrue("Out of sequence states were " + previous + " and " + current + " in " + quantilesUsed, current > previous);
                previous = current;
            }

            // The quantiles immediately before the intermediate wait for idle must have been processed
            int intermediateWaitPoint = TEST_SIZE / 2 - 1;
            assertTrue(quantilesUsed + " should contain " + intermediateWaitPoint,
                    quantilesUsed.contains(Integer.toString(intermediateWaitPoint)));
        } finally {
            threadpool.shutdown();
        }
        assertTrue(threadpool.awaitTermination(1, TimeUnit.SECONDS));
    }
}
