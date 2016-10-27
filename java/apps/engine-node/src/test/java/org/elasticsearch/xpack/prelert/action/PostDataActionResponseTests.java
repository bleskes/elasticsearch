package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;
import org.joda.time.DateTime;

import java.util.function.Consumer;

public class PostDataActionResponseTests extends AbstractStreamableTestCase<PostDataAction.Response> {

    @Override
    protected PostDataAction.Response createTestInstance() {
        DataCounts counts = new DataCounts();
        ifRandomTrueSetRandomLong(counts::setBucketCount);
        ifRandomTrueSetRandomLong(counts::setProcessedRecordCount);
        ifRandomTrueSetRandomLong(counts::setProcessedFieldCount);
        ifRandomTrueSetRandomLong(counts::setInputBytes);
        ifRandomTrueSetRandomLong(counts::setInputFieldCount);
        ifRandomTrueSetRandomLong(counts::setInvalidDateCount);
        ifRandomTrueSetRandomLong(counts::setMissingFieldCount);
        ifRandomTrueSetRandomLong(counts::setOutOfOrderTimeStampCount);
        ifRandomTrueSetRandomLong(counts::setFailedTransformCount);
        ifRandomTrueSetRandomLong(counts::setExcludedRecordCount);
        if (randomBoolean()) {
            counts.setLatestRecordTimeStamp(new DateTime(randomDateTimeZone()).toDate());
        }

        return new PostDataAction.Response(counts);
    }

    private void ifRandomTrueSetRandomLong(Consumer<Long> consumer) {
        if (randomBoolean()) {
            consumer.accept(randomPositiveLong());
        }
    }

    @Override
    protected PostDataAction.Response createBlankInstance() {
        return new PostDataAction.Response() ;
    }
}
