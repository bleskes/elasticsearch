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
public final class Messages
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
    public static final String JOB_CONFIG_UPDATE_DESCRIPTION_INVALID = "job.config.update.description.invalid";
    public static final String JOB_CONFIG_UPDATE_INVALID_KEY = "job.config.update.invalid.key";
    public static final String JOB_CONFIG_UPDATE_MODEL_DEBUG_CONFIG_PARSE_ERROR = "job.config.update.model.debug.config.parse.error";
    public static final String JOB_CONFIG_UPDATE_NO_OBJECT = "job.config.update.no.object";
    public static final String JOB_CONFIG_UPDATE_PARSE_ERROR = "job.config.update.parse.error";
    public static final String JOB_CONFIG_TRANSFORM_CIRCULAR_DEPENDENCY = "job.config.transform.circular.dependency";
    public static final String JOB_CONFIG_TRANSFORM_CONDITION_INVALID_OPERATOR = "job.config.transform.condition.invalid.operator";
    public static final String JOB_CONFIG_TRANSFORM_CONDITION_INVALID_VALUE_NULL = "job.config.transform.condition.invalid.value.null";
    public static final String JOB_CONFIG_TRANSFORM_CONDITION_INVALID_VALUE_NUMBER = "job.config.transform.condition.invalid.value.numeric";
    public static final String JOB_CONFIG_TRANSFORM_CONDITION_INVALID_VALUE_REGEX = "job.config.transform.condition.invalid.value.regex";
    public static final String JOB_CONFIG_TRANSFORM_CONDITION_REQUIRED = "job.config.transform.condition.required";
    public static final String JOB_CONFIG_TRANSFORM_CONDITION_UNKNOWN_OPERATOR = "job.config.transform.condition.unknown.operator";
    public static final String JOB_CONFIG_TRANSFORM_DUPLICATED_OUTPUT_NAME = "job.config.transform.duplicated.output.name";
    public static final String JOB_CONFIG_TRANSFORM_EXTRACT_GROUPS_SHOULD_MATCH_OUTPUT_COUNT = "job.config.transform.extract.groups.should.match.output.count";
    public static final String JOB_CONFIG_TRANSFORM_INPUTS_CONTAIN_EMPTY_STRING = "job.config.transform.inputs.contain.empty.string";
    public static final String JOB_CONFIG_TRANSFORM_INVALID_ARGUMENT = "job.config.transform.invalid.argument";
    public static final String JOB_CONFIG_TRANSFORM_INVALID_ARGUMENT_COUNT = "job.config.transform.invalid.argument.count";
    public static final String JOB_CONFIG_TRANSFORM_INVALID_INPUT_COUNT = "job.config.transform.invalid.input.count";
    public static final String JOB_CONFIG_TRANSFORM_INVALID_OUTPUT_COUNT = "job.config.transform.invalid.output.count";
    public static final String JOB_CONFIG_TRANSFORM_OUTPUTS_CONTAIN_EMPTY_STRING = "job.config.transform.outputs.contain.empty.string";
    public static final String JOB_CONFIG_TRANSFORM_OUTPUTS_UNUSED = "job.config.transform.outputs.unused";
    public static final String JOB_CONFIG_TRANSFORM_OUTPUT_NAME_USED_MORE_THAN_ONCE = "job.config.transform.output.name.used.more.than.once";
    public static final String JOB_CONFIG_TRANSFORM_UNKNOWN_TYPE = "job.config.transform.unknown.type";
    public static final String JOB_CONFIG_UNKNOWN_FUNCTION = "job.config.unknown.function";

    public static final String JOB_DATA_CONCURRENT_USE_CLOSE = "job.data.concurrent.use.close";
    public static final String JOB_DATA_CONCURRENT_USE_FLUSH = "job.data.concurrent.use.flush";
    public static final String JOB_DATA_CONCURRENT_USE_UPDATE = "job.data.concurrent.use.update";
    public static final String JOB_DATA_CONCURRENT_USE_UPLOAD = "job.data.concurrent.use.upload";

    public static final String JOB_MISSING_QUANTILES = "job.missing.quantiles";
    public static final String JOB_UNKNOWN_ID = "job.unknown.id";
    public static final String JOB_UNKNOWN_REFERENCE = "job.unknown.reference";

    public static final String JSON_JOB_CONFIG_MAPPING = "json.job.config.mapping.error";
    public static final String JSON_JOB_CONFIG_PARSE = "json.job.config.parse.error";

    public static final String REST_INVALID_DATETIME_PARAMS = "rest.invalid.datetime.params";
    public static final String REST_INVALID_FLUSH_PARAMS_MISSING = "rest.invalid.flush.params.missing.argument";
    public static final String REST_INVALID_FLUSH_PARAMS_UNEXPECTED = "rest.invalid.flush.params.unexpected";
    public static final String REST_INVALID_RESET_PARAMS = "rest.invalid.reset.params";
    public static final String REST_INVALID_SKIP = "rest.invalid.skip";
    public static final String REST_INVALID_TAKE = "rest.invalid.take";
    public static final String REST_GZIP_ERROR = "rest.gzip.error";
    public static final String REST_START_AFTER_END = "rest.start.after.end";
    public static final String REST_RESET_BUCKET_NO_LATENCY = "rest.reset.bucket.no.latency";

    public static final String REST_ALERT_MISSING_ARGUMENT = "rest.alert.missing.argument";
    public static final String REST_ALERT_INVALID_TIMEOUT = "rest.alert.invalid.timeout";
    public static final String REST_ALERT_INVALID_THRESHOLD = "rest.alert.invalid.threshold";

    public static final String PROCESS_ACTION_CLOSING_JOB = "process.action.closing.job";
    public static final String PROCESS_ACTION_FLUSHING_JOB = "process.action.flushing.job";
    public static final String PROCESS_ACTION_UPDATING_JOB = "process.action.updating.job";
    public static final String PROCESS_ACTION_USING_JOB = "process.action.using.job";
    public static final String PROCESS_ACTION_WRITING_JOB = "process.action.writing.job";

    private Messages()
    {
    }

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
