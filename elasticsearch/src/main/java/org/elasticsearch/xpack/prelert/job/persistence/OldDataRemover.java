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
public class OldDataRemover {

    private final Function<String, JobDataDeleter> dataDeleterFactory;

    public OldDataRemover(Function<String, JobDataDeleter> dataDeleterFactory) {
        this.dataDeleterFactory = Objects.requireNonNull(dataDeleterFactory);
    }

    /**
     * Removes results between the time given and the current time
     */
    public void deleteResultsAfter(ActionListener<BulkResponse> listener, String jobId, long cutoffEpochMs) {
        JobDataDeleter deleter = dataDeleterFactory.apply(jobId);
        deleter.deleteResultsFromTime(cutoffEpochMs, new ActionListener<Boolean>() {
            @Override
            public void onResponse(Boolean success) {
                if (success) {
                    deleter.commit(listener);
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }
}
