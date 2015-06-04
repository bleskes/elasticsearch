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
package com.prelert.job.messages;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Defines the keys for all the message strings
 */
public class Messages
{
    /**
     * The base name of the bundle without the .properties extension
     * or locale
     */
    private static final String BUNDLE_NAME = "com.prelert.job.messages.prelert_messages";


    public static final String CPU_LIMIT_JOB = "cpu.limit.jobs";

    public static final String DATASTORE_ERROR_DELETING = "datastore.error.deleting";
    public static final String DATASTORE_ERROR_DELETING_MISSING_INDEX = "datastore.error.deleting.missing.index";

    public static final String LICENSE_LIMIT_DETECTORS = "license.limit.detectors";
    public static final String LICENSE_LIMIT_JOBS = "license.limit.jobs";
    public static final String LICENSE_LIMIT_JOBS_REACTIVATE = "license.limit.jobs.reactivate";
    public static final String LICENSE_LIMIT_PARTITIONS = "license.limit.partitions";

    public static final String LOGFILE_MISSING = "logfile.missing";
    public static final String LOGFILE_MISSING_DIRECTORY = "logfile.missing.directory";

    public static final String JOB_CONFIG_BYFIELD_INCOMPATIBLE_FUNCTION = "job.config.byField.incompatible.function";
    public static final String JOB_CONFIG_BYFIELD_NEEDS_ANOTHER = "job.config.byField.needs.another";
    public static final String JOB_CONFIG_DATAFORMAT_REQUIRES_TRANSFORM = "job.config.dataformat.requires.transform";
    public static final String JOB_CONFIG_FIELDNAME_INCOMPATIBLE_FUNCTION = "job.config.fieldname.incompatible.function";
    public static final String JOB_CONFIG_FUNCTION_REQUIRES_BYFIELD = "job.config.function.requires.byfield";
    public static final String JOB_CONFIG_FUNCTION_REQUIRES_FIELDNAME = "job.config.function.requires.fieldname";
    public static final String JOB_CONFIG_FUNCTION_REQUIRES_OVERFIELD = "job.config.function.requires.overfield";
    public static final String JOB_CONFIG_ID_CONTAINS_CONTROL_CHARS = "job.config.id.contains.control.chars";
    public static final String JOB_CONFIG_ID_CONTAINS_UPPERCASE_CHARS = "job.config.id.contains.uppercase.chars";
    public static final String JOB_CONFIG_ID_TOO_LONG = "job.config.id.too.long";
    public static final String JOB_CONFIG_ID_ALREADY_TAKEN = "job.config.id.already.taken";
    public static final String JOB_CONFIG_INVALID_FIELDNAME_CHARS = "job.config.invalid.fieldname.chars";
    public static final String JOB_CONFIG_INVALID_JOBID_CHARS = "job.config.invalid.jobid.chars";
    public static final String JOB_CONFIG_INVALID_TIMEFORMAT = "job.config.invalid.timeformat";
    public static final String JOB_CONFIG_FUNCTION_INCOMPATIBLE_PRESUMMARIZED = "job.config.function.incompatible.presummarized";
    public static final String JOB_CONFIG_MISSING_ANALYSISCONFIG = "job.config.missing.analysisconfig";
    public static final String JOB_CONFIG_NEGATIVE_FIELD_VALUE = "job.config.negative.field.value";
    public static final String JOB_CONFIG_NO_ANALYSIS_FIELD = "job.config.no.analysis.field";
    public static final String JOB_CONFIG_NO_ANALYSIS_FIELD_NOT_COUNT = "job.config.no.analysis.field.not.count";
    public static final String JOB_CONFIG_NO_DETECTORS = "job.config.no.detectors";
    public static final String JOB_CONFIG_OVERFIELD_INCOMPATIBLE_FUNCTION = "job.config.overField.incompatible.function";
    public static final String JOB_CONFIG_OVERFIELD_NEEDS_ANOTHER = "job.config.overField.needs.another";
    public static final String JOB_CONFIG_UNKNOWN_FUNCTION = "job.config.unknown.function";

    public static final String JOB_DATA_CONCURRENT_USE_CLOSE = "job.data.concurrent.use.close";
    public static final String JOB_DATA_CONCURRENT_USE_FLUSH = "job.data.concurrent.use.flush";
    public static final String JOB_DATA_CONCURRENT_USE_UPLOAD = "job.data.concurrent.use.upload";

    public static final String JOB_MISSING_QUANTILES = "job.missing.quantiles";
    public static final String JOB_UNKNOWN_ID = "job.unknown.id";
    public static final String JOB_UNKNOWN_REFERENCE = "job.unknown.reference";

    public static final String REST_INVALID_FLUSH_PARAMS_MISSING = "rest.invalid.flush.params.missing.argument";
    public static final String REST_INVALID_FLUSH_PARAMS_UNEXPECTED = "rest.invalid.flush.params.unexpected";
    public static final String REST_INVALID_RESET_PARAMS = "rest.invalid.reset.params";
    public static final String REST_GZIP_ERROR = "rest.gzip.error";
    public static final String REST_START_AFTER_END = "rest.start.after.end";
    public static final String REST_RESET_BUCKET_NO_LATENCY = "rest.reset.bucket.no.latency";

    public static final String REST_ALERT_MISSING_ARGUMENT = "rest.alert.missing.argument";
    public static final String REST_ALERT_INVALID_TIMEOUT = "rest.alert.invalid.timeout";
    public static final String REST_ALERT_INVALID_THRESHOLD = "rest.alert.invalid.threshold";

    public static ResourceBundle load()
    {
        return ResourceBundle.getBundle(Messages.BUNDLE_NAME);
    }

    /**
     * Look up the message string from the resource bundle.
     *
     * @param key Must be one of the statics defined in this file
     * @return
     */
    public static String getMessage(String key)
    {
        return load().getString(key);
    }

    /**
     * Look up the message string from the resource bundle and format with
     * the supplied arguments
     * @param key
     * @param args MessageFormat arguments. See {@linkplain MessageFormat#format(Object)}
     * @return
     */
    public static String getMessage(String key, Object...args)
    {
        return MessageFormat.format(load().getString(key), args);
    }
}
