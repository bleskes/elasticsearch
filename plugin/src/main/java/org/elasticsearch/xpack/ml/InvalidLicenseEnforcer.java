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
package org.elasticsearch.xpack.ml;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.ml.datafeed.DatafeedManager;
import org.elasticsearch.xpack.ml.job.process.autodetect.AutodetectProcessManager;

public class InvalidLicenseEnforcer extends AbstractComponent {

    private final ThreadPool threadPool;
    private final XPackLicenseState licenseState;
    private final DatafeedManager datafeedManager;
    private final AutodetectProcessManager autodetectProcessManager;

    InvalidLicenseEnforcer(Settings settings, XPackLicenseState licenseState, ThreadPool threadPool,
                           DatafeedManager datafeedManager, AutodetectProcessManager autodetectProcessManager) {
        super(settings);
        this.threadPool = threadPool;
        this.licenseState = licenseState;
        this.datafeedManager = datafeedManager;
        this.autodetectProcessManager = autodetectProcessManager;
        licenseState.addListener(this::closeJobsAndDatafeedsIfLicenseExpired);
    }

    private void closeJobsAndDatafeedsIfLicenseExpired() {
        if (licenseState.isMachineLearningAllowed() == false) {
            threadPool.generic().execute(new AbstractRunnable() {
                @Override
                public void onFailure(Exception e) {
                    logger.warn("cannot close all jobs", e);
                }

                @Override
                protected void doRun() throws Exception {
                    datafeedManager.stopAllDatafeeds("invalid license");
                    autodetectProcessManager.closeAllJobs("invalid license");
                }
            });
        }
    }
}
