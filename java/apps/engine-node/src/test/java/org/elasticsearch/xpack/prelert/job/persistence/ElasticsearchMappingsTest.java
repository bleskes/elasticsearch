
package org.elasticsearch.xpack.prelert.job.persistence;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.test.ESTestCase;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.elasticsearch.xpack.prelert.job.CategorizerState;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.ModelState;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.audit.AuditActivity;
import org.elasticsearch.xpack.prelert.job.audit.AuditMessage;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.elasticsearch.xpack.prelert.job.results.BucketProcessingTime;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.job.results.ModelDebugOutput;
import org.elasticsearch.xpack.prelert.job.results.ReservedFieldNames;
import org.elasticsearch.xpack.prelert.job.usage.Usage;
import org.elasticsearch.xpack.prelert.lists.ListDocument;


public class ElasticsearchMappingsTest extends ESTestCase {
    private void parseJson(JsonParser parser, Set<String> expected)
            throws IOException {
        try {
            JsonToken token = parser.nextToken();
            while (token != null && token != JsonToken.END_OBJECT) {
                switch (token) {
                case START_OBJECT:
                    parseJson(parser, expected);
                    break;
                case FIELD_NAME:
                    String fieldName = parser.getCurrentName();
                    expected.add(fieldName);
                    break;
                default:
                    break;
                }
                token = parser.nextToken();
            }
        } catch (JsonParseException e) {
            fail("Cannot parse JSON: " + e);
        }
    }

    public void testReservedFields()
            throws IOException, ClassNotFoundException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Set<String> overridden = new HashSet<>();

        // These are not reserved because they're Elasticsearch keywords, not field names
        overridden.add(ElasticsearchMappings.ALL);
        overridden.add(ElasticsearchMappings.ANALYZER);
        overridden.add(ElasticsearchMappings.COPY_TO);
        overridden.add(ElasticsearchMappings.DYNAMIC);
        overridden.add(ElasticsearchMappings.ENABLED);
        overridden.add(ElasticsearchMappings.INCLUDE_IN_ALL);
        overridden.add(ElasticsearchMappings.INDEX);
        overridden.add(ElasticsearchMappings.NESTED);
        overridden.add(ElasticsearchMappings.NO);
        overridden.add(ElasticsearchMappings.PARENT);
        overridden.add(ElasticsearchMappings.PROPERTIES);
        overridden.add(ElasticsearchMappings.TYPE);
        overridden.add(ElasticsearchMappings.WHITESPACE);

        // These are not reserved because they're data types, not field names
        overridden.add(AnomalyRecord.TYPE.getPreferredName());
        overridden.add(AuditActivity.TYPE);
        overridden.add(AuditMessage.TYPE);
        overridden.add(Bucket.TYPE);
        overridden.add(BucketProcessingTime.TYPE);
        overridden.add(BucketInfluencer.TYPE);
        overridden.add(CategorizerState.TYPE);
        overridden.add(CategoryDefinition.TYPE);
        overridden.add(Influencer.TYPE);
        overridden.add(JobDetails.TYPE);
        overridden.add(ListDocument.TYPE.getPreferredName());
        overridden.add(ModelDebugOutput.TYPE);
        overridden.add(ModelState.TYPE);
        overridden.add(ModelSnapshot.TYPE);
        overridden.add(ModelSizeStats.TYPE);
        overridden.add(Quantiles.TYPE);
        overridden.add(Usage.TYPE);

        // These are not reserved because they're in the prelert-int index, not prelertresults-*
        overridden.add(ListDocument.ID.getPreferredName());
        overridden.add(ListDocument.ITEMS.getPreferredName());

        // These are not reserved because they're analyzed strings, i.e. the same type as user-specified fields
        overridden.add(JobDetails.DESCRIPTION);
        overridden.add(JobDetails.STATUS);
        overridden.add(ModelSnapshot.DESCRIPTION);
        overridden.add(SchedulerConfig.USERNAME.getPreferredName());

        Set<String> expected = new HashSet<>();

        Class<?> c = Class.forName("org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchMappings");
        Class<?> contentBuilder = Class.forName("org.elasticsearch.common.xcontent.XContentBuilder");

        Method[] allMethods = c.getDeclaredMethods();
        for (Method m : allMethods) {
            if (Modifier.isStatic(m.getModifiers()) && m.getReturnType().equals(contentBuilder)) {
                XContentBuilder builder;
                if (m.getParameterCount() == 0) {
                    builder = (XContentBuilder) m.invoke(null);
                } else if (m.getParameterCount() == 1) {
                    List<Object> args = new ArrayList<>();
                    args.add(null);

                    builder = (XContentBuilder) m.invoke(null, args);
                } else {
                    continue;
                }
                BufferedInputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(
                        builder.string().getBytes(StandardCharsets.UTF_8)));

                JsonParser parser = new JsonFactory().createParser(inputStream);
                parseJson(parser, expected);
            }
        }

        expected.removeAll(overridden);
        for (String s : expected) {
            // By comparing like this the failure messages say which string is missing
            String reserved = ReservedFieldNames.RESERVED_FIELD_NAMES.contains(s) ? s : null;
            assertEquals(s, reserved);
        }
    }

}
