/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.SpecialPermission;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectProcess;
import org.elasticsearch.xpack.prelert.job.process.autodetect.AutodetectProcessFactory;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.lists.ListDocument;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class NativeAutodetectProcessFactory implements AutodetectProcessFactory {

    private static final Logger LOGGER = Loggers.getLogger(NativeAutodetectProcessFactory.class);
    private final JobProvider jobProvider;
    private Environment env;
    private Settings settings;

    public NativeAutodetectProcessFactory(JobProvider jobProvider, Environment env, Settings settings) {
        this.env = env;
        this.settings = settings;
        this.jobProvider = Objects.requireNonNull(jobProvider);
    }

    @Override
    public AutodetectProcess createAutodetectProcess(JobDetails jobDetails, boolean ignoreDowntime) {
        List<Path> filesToDelete = new ArrayList<>();
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SpecialPermission());
        }
        Process nativeProcess = AccessController.doPrivileged((PrivilegedAction<Process>) () -> {
            return createNativeProcess(jobDetails, ignoreDowntime, filesToDelete);
        });
        int numberOfAnalysisFields = jobDetails.getAnalysisConfig().analysisFields().size();
        return new NativeAutodetectProcess(nativeProcess, numberOfAnalysisFields, filesToDelete);
    }

    private Process createNativeProcess(JobDetails job, boolean ignoreDowntime, List<Path> filesToDelete) {

        String jobId = job.getId();
        Quantiles quantiles = jobProvider.getQuantiles(jobId);
        List<ModelSnapshot> modelSnapshots = jobProvider.modelSnapshots(jobId, 0, 1).hits();

        Process nativeProcess = null;

        try {
            AutodetectBuilder autodetectBuilder = new AutodetectBuilder(job, filesToDelete, LOGGER, env, settings)
                    .ignoreDowntime(ignoreDowntime)
                    .referencedLists(resolveLists(job.getAnalysisConfig().extractReferencedLists()));

            // if state is null or empty it will be ignored
            // else it is used to restore the quantiles
            if (quantiles != null) {
                autodetectBuilder.quantiles(quantiles);
            }

            if (modelSnapshots != null && !modelSnapshots.isEmpty()) {
                autodetectBuilder.modelSnapshot(modelSnapshots.get(0));
            }
            nativeProcess = autodetectBuilder.build();
        } catch (IOException e) {
            String msg = "Failed to launch process for job " + job.getId();
            LOGGER.error(msg);
            throw ExceptionsHelper.serverError(msg, e, ErrorCodes.NATIVE_PROCESS_START_ERROR);
        }

        return nativeProcess;
    }

    private Set<ListDocument> resolveLists(Set<String> listIds) {
        Set<ListDocument> resolved = new HashSet<>();
        for (String listId : listIds) {
            Optional<ListDocument> list = jobProvider.getList(listId);
            if (list.isPresent()) {
                resolved.add(list.get());
            } else {
                LOGGER.warn("List '" + listId + "' could not be retrieved.");
            }
        }
        return resolved;
    }
}

