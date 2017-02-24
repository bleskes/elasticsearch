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
package org.elasticsearch.xpack.ml.datafeed;

import org.elasticsearch.xpack.ml.job.messages.Messages;
import org.elasticsearch.xpack.ml.notifications.Auditor;

import java.util.Objects;

/**
 * <p>
 * Keeps track of problems the datafeed encounters and audits
 * messages appropriately.
 * </p>
 * <p>
 * The {@code ProblemTracker} is expected to interact with multiple
 * threads (lookback executor, real-time executor). However, each
 * thread will be accessing in a sequential manner therefore we
 * only need to ensure correct visibility.
 * </p>
 */
class ProblemTracker {

    private static final int EMPTY_DATA_WARN_COUNT = 10;

    private final Auditor auditor;
    private final String jobId;

    private volatile boolean hasProblems;
    private volatile boolean hadProblems;
    private volatile String previousProblem;
    private volatile int emptyDataCount;

    ProblemTracker(Auditor auditor, String jobId) {
        this.auditor = Objects.requireNonNull(auditor);
        this.jobId = Objects.requireNonNull(jobId);
    }

    /**
     * Reports as analysis problem if it is different than the last seen problem
     *
     * @param problemMessage the problem message
     */
    public void reportAnalysisProblem(String problemMessage) {
        reportProblem(Messages.JOB_AUDIT_DATAFEED_DATA_ANALYSIS_ERROR, problemMessage);
    }

    /**
     * Reports as extraction problem if it is different than the last seen problem
     *
     * @param problemMessage the problem message
     */
    public void reportExtractionProblem(String problemMessage) {
        reportProblem(Messages.JOB_AUDIT_DATAFEED_DATA_EXTRACTION_ERROR, problemMessage);
    }

    /**
     * Reports the problem if it is different than the last seen problem
     *
     * @param problemMessage the problem message
     */
    private void reportProblem(String template, String problemMessage) {
        hasProblems = true;
        if (!Objects.equals(previousProblem, problemMessage)) {
            previousProblem = problemMessage;
            auditor.error(jobId, Messages.getMessage(template, problemMessage));
        }
    }

    /**
     * Updates the tracking of empty data cycles. If the number of consecutive empty data
     * cycles reaches {@code EMPTY_DATA_WARN_COUNT}, a warning is reported.
     */
    public void reportEmptyDataCount() {
        if (emptyDataCount < EMPTY_DATA_WARN_COUNT) {
            emptyDataCount++;
            if (emptyDataCount == EMPTY_DATA_WARN_COUNT) {
                auditor.warning(jobId, Messages.getMessage(Messages.JOB_AUDIT_DATAFEED_NO_DATA));
            }
        }
    }

    public void reportNoneEmptyCount() {
        if (emptyDataCount >= EMPTY_DATA_WARN_COUNT) {
            auditor.info(jobId, Messages.getMessage(Messages.JOB_AUDIR_DATAFEED_DATA_SEEN_AGAIN));
        }
        emptyDataCount = 0;
    }

    public boolean hasProblems() {
        return hasProblems;
    }

    /**
     * Issues a recovery message if appropriate and prepares for next report
     */
    public void finishReport() {
        if (!hasProblems && hadProblems) {
            auditor.info(jobId, Messages.getMessage(Messages.JOB_AUDIT_DATAFEED_RECOVERED));
        }

        hadProblems = hasProblems;
        hasProblems = false;
    }
}
