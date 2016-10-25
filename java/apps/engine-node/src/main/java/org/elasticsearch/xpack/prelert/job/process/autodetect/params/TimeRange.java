package org.elasticsearch.xpack.prelert.job.process.autodetect.params;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.utils.time.TimeUtils;

public class TimeRange {

    public static final String START_PARAM = "start";
    public static final String END_PARAM = "end";
    public static final String NOW = "now";
    public static final int MILLISECONDS_IN_SECOND = 1000;

    private final Long start;
    private final Long end;

    private TimeRange(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    public String getStart() {
        return start == null ? "" : String.valueOf(start);
    }

    public String getEnd() {
        return end == null ? "" : String.valueOf(end);
    }

    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {

        private String start = "";
        private String end = "";

        private Builder() {
        }

        public Builder startTime(String start) {
            this.start = start;
            return this;
        }

        public Builder endTime(String end) {
            this.end = end;
            return this;
        }

        /**
         * Create a new TimeRange instance after validating the start and end params.
         * Throws {@link ElasticsearchStatusException} if the validation fails
         * @return The time range
         */
        public TimeRange build() {
            return createTimeRange(start, end);
        }

        private TimeRange createTimeRange(String start, String end) {
            Long epochStart = null;
            Long epochEnd = null;
            if (!start.isEmpty()) {
                epochStart = paramToEpochIfValidOrThrow(START_PARAM, start) / MILLISECONDS_IN_SECOND;
                epochEnd = paramToEpochIfValidOrThrow(END_PARAM, end) / MILLISECONDS_IN_SECOND;
                if (end.isEmpty() || epochEnd.equals(epochStart)) {
                    epochEnd = epochStart + 1;
                }
                if (epochEnd < epochStart) {
                    String msg = Messages.getMessage(Messages.REST_START_AFTER_END, end, start);
                    throwInvalidFlushParamsException(msg, ErrorCodes.END_DATE_BEFORE_START_DATE);
                }
            } else {
                if (!end.isEmpty()) {
                    epochEnd = paramToEpochIfValidOrThrow(END_PARAM, end) / MILLISECONDS_IN_SECOND;
                }
            }
            return new TimeRange(epochStart, epochEnd);
        }

        /**
         * Returns epoch milli seconds
         *
         * @param paramName
         * @param date
         * @return
         */
        private long paramToEpochIfValidOrThrow(String paramName, String date) {
            if (NOW.equals(date)) {
                return System.currentTimeMillis();
            }
            long epoch = 0;
            if (date.isEmpty() == false) {
                epoch = TimeUtils.dateStringToEpoch(date);
                if (epoch < 0) {
                    String msg = Messages.getMessage(Messages.REST_INVALID_DATETIME_PARAMS, paramName, date);
                    throwInvalidFlushParamsException(msg, ErrorCodes.UNPARSEABLE_DATE_ARGUMENT);
                }
            }

            return epoch;
        }

        private void throwInvalidFlushParamsException(String msg, ErrorCodes errorCode) {
            throw ExceptionsHelper.invalidRequestException(msg, errorCode);
        }
    }
}
