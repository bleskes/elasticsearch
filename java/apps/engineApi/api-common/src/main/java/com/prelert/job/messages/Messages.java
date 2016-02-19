/****************************************************************************
 *                                                                          *
 * Copyright 2015-2016 Prelert Ltd                                          *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 *                                                                          *
 ***************************************************************************/
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

    public static final String JOB_AUDIT_CREATED = "job.audit.created";
    public static final String JOB_AUDIT_DELETED = "job.audit.deleted";
    public static final String JOB_AUDIT_UPDATED = "job.audit.updated";
    public static final String JOB_AUDIT_SCHEDULER_STARTED_FROM_TO = "job.audit.scheduler.started.from.to";
    public static final String JOB_AUDIT_SCHEDULER_STARTED_REALTIME = "job.audit.scheduler.started.realtime";
    public static final String JOB_AUDIT_SCHEDULER_LOOKBACK_COMPLETED = "job.audit.scheduler.lookback.completed";
    public static final String JOB_AUDIT_SCHEDULER_STOPPED = "job.audit.scheduler.stopped";
    public static final String JOB_AUDIT_SCHEDULER_NO_DATA = "job.audit.scheduler.no.data";
    public static final String JOB_AUDIR_SCHEDULER_DATA_SEEN_AGAIN = "job.audit.scheduler.data.seen.again";
    public static final String JOB_AUDIT_SCHEDULER_DATA_EXTRACTION_ERROR = "job.audit.scheduler.data.extraction.error";
    public static final String JOB_AUDIT_SCHEDULER_DATA_EXTRACTION_RECOVERED = "job.audit.scheduler.data.extraction.recovered";

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
    public static final String JOB_CONFIG_UPDATE_CUSTOM_SETTINGS_INVALID = "job.config.update.custom.settings.invalid";
    public static final String JOB_CONFIG_UPDATE_DESCRIPTION_INVALID = "job.config.update.description.invalid";
    public static final String JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_INVALID = "job.config.update.detector.description.invalid";
    public static final String JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_INVALID_DETECTOR_INDEX = "job.config.update.detector.description.invalid.detector.index";
    public static final String JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_DETECTOR_INDEX_SHOULD_BE_INTEGER = "job.config.update.detector.description.detector.index.should.be.integer";
    public static final String JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_MISSING_PARAMS = "job.config.update.detector.description.missing.params";
    public static final String JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_SHOULD_BE_STRING = "job.config.update.detector.description.should.be.string";
    public static final String JOB_CONFIG_UPDATE_DETECTOR_DESCRIPTION_FAILED = "job.config.update.detector.description.failed";
    public static final String JOB_CONFIG_UPDATE_INVALID_KEY = "job.config.update.invalid.key";
    public static final String JOB_CONFIG_UPDATE_MODEL_DEBUG_CONFIG_PARSE_ERROR = "job.config.update.model.debug.config.parse.error";
    public static final String JOB_CONFIG_UPDATE_NO_OBJECT = "job.config.update.no.object";
    public static final String JOB_CONFIG_UPDATE_PARSE_ERROR = "job.config.update.parse.error";
    public static final String JOB_CONFIG_UPDATE_RENORMALIZATION_WINDOW_INVALID = "job.config.update.renormalization.window.invalid";
    public static final String JOB_CONFIG_UPDATE_MODEL_SNAPSHOT_RETENTION_DAYS_INVALID = "job.config.update.model.snapshot.retention.days.invalid";
    public static final String JOB_CONFIG_UPDATE_RESULTS_RETENTION_DAYS_INVALID = "job.config.update.results.retention.days.invalid";
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
    public static final String JOB_CONFIG_SCHEDULER_UNKNOWN_DATASOURCE = "job.config.scheduler.unknown.datasource";
    public static final String JOB_CONFIG_SCHEDULER_FIELD_NOT_SUPPORTED = "job.config.scheduler.field.not.supported";
    public static final String JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE = "job.config.scheduler.invalid.option.value";
    public static final String JOB_CONFIG_SCHEDULER_REQUIRES_BUCKET_SPAN = "job.config.scheduler.requires.bucket.span";
    public static final String JOB_CONFIG_SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY = "job.config.scheduler.elasticsearch.does.not.support.latency";
    public static final String JOB_CONFIG_SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD = "job.config.scheduler.aggregations.requires.summary.count.field";
    public static final String JOB_CONFIG_SCHEDULER_ELASTICSEARCH_REQUIRES_DATAFORMAT_ELASTICSEARCH = "job.config.scheduler.elasticsearch.requires.dataformat.elasticsearch";

    public static final String JOB_DATA_CONCURRENT_USE_CLOSE = "job.data.concurrent.use.close";
    public static final String JOB_DATA_CONCURRENT_USE_DELETE = "job.data.concurrent.use.delete";
    public static final String JOB_DATA_CONCURRENT_USE_FLUSH = "job.data.concurrent.use.flush";
    public static final String JOB_DATA_CONCURRENT_USE_UPDATE = "job.data.concurrent.use.update";
    public static final String JOB_DATA_CONCURRENT_USE_UPLOAD = "job.data.concurrent.use.upload";

    public static final String JOB_SCHEDULER_CANNOT_START = "job.scheduler.cannot.start";
    public static final String JOB_SCHEDULER_CANNOT_STOP_IN_CURRENT_STATE = "job.scheduler.cannot.stop.in.current.state";
    public static final String JOB_SCHEDULER_FAILED_TO_STOP = "job.scheduler.failed.to.stop";
    public static final String JOB_SCHEDULER_NO_SUCH_SCHEDULED_JOB = "job.scheduler.no.such.scheduled.job";

    public static final String JOB_MISSING_QUANTILES = "job.missing.quantiles";
    public static final String JOB_UNKNOWN_ID = "job.unknown.id";

    public static final String JSON_JOB_CONFIG_MAPPING = "json.job.config.mapping.error";
    public static final String JSON_JOB_CONFIG_PARSE = "json.job.config.parse.error";

    public static final String JSON_DETECTOR_CONFIG_MAPPING = "json.detector.config.mapping.error";
    public static final String JSON_DETECTOR_CONFIG_PARSE = "json.detector.config.parse.error";

    public static final String REST_ACTION_NOT_ALLOWED_FOR_SCHEDULED_JOB = "rest.action.not.allowed.for.scheduled.job";

    public static final String REST_INVALID_DATETIME_PARAMS = "rest.invalid.datetime.params";
    public static final String REST_INVALID_FLUSH_PARAMS_MISSING = "rest.invalid.flush.params.missing.argument";
    public static final String REST_INVALID_FLUSH_PARAMS_UNEXPECTED = "rest.invalid.flush.params.unexpected";
    public static final String REST_INVALID_RESET_PARAMS = "rest.invalid.reset.params";
    public static final String REST_INVALID_SKIP = "rest.invalid.skip";
    public static final String REST_INVALID_TAKE = "rest.invalid.take";
    public static final String REST_INVALID_SKIP_TAKE_SUM = "rest.invalid.skip.take.sum";
    public static final String REST_GZIP_ERROR = "rest.gzip.error";
    public static final String REST_START_AFTER_END = "rest.start.after.end";
    public static final String REST_RESET_BUCKET_NO_LATENCY = "rest.reset.bucket.no.latency";

    public static final String REST_ALERT_MISSING_ARGUMENT = "rest.alert.missing.argument";
    public static final String REST_ALERT_INVALID_TIMEOUT = "rest.alert.invalid.timeout";
    public static final String REST_ALERT_INVALID_THRESHOLD = "rest.alert.invalid.threshold";
    public static final String REST_ALERT_CANT_USE_PROB = "rest.alert.cant.use.prob";
    public static final String REST_ALERT_INVALID_TYPE = "rest.alert.invalid.type";

    public static final String PROCESS_ACTION_CLOSING_JOB = "process.action.closing.job";
    public static final String PROCESS_ACTION_DELETING_JOB = "process.action.deleting.job";
    public static final String PROCESS_ACTION_FLUSHING_JOB = "process.action.flushing.job";
    public static final String PROCESS_ACTION_UPDATING_JOB = "process.action.updating.job";
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
