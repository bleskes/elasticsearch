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
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.test.ESTestCase;


public class ElasticsearchMappingsTests extends ESTestCase {
    // NORELEASE change this test to not use reflection
    //    private void parseJson(JsonParser parser, Set<String> expected)
    //            throws IOException {
    //        try {
    //            JsonToken token = parser.nextToken();
    //            while (token != null && token != JsonToken.END_OBJECT) {
    //                switch (token) {
    //                case START_OBJECT:
    //                    parseJson(parser, expected);
    //                    break;
    //                case FIELD_NAME:
    //                    String fieldName = parser.getCurrentName();
    //                    expected.add(fieldName);
    //                    break;
    //                default:
    //                    break;
    //                }
    //                token = parser.nextToken();
    //            }
    //        } catch (JsonParseException e) {
    //            fail("Cannot parse JSON: " + e);
    //        }
    //    }
    //
    //    public void testReservedFields()
    //            throws IOException, ClassNotFoundException, IllegalAccessException,
    //            IllegalArgumentException, InvocationTargetException {
    //        Set<String> overridden = new HashSet<>();
    //
    //        // These are not reserved because they're Elasticsearch keywords, not field names
    //        overridden.add(ElasticsearchMappings.ALL);
    //        overridden.add(ElasticsearchMappings.ANALYZER);
    //        overridden.add(ElasticsearchMappings.COPY_TO);
    //        overridden.add(ElasticsearchMappings.DYNAMIC);
    //        overridden.add(ElasticsearchMappings.ENABLED);
    //        overridden.add(ElasticsearchMappings.INCLUDE_IN_ALL);
    //        overridden.add(ElasticsearchMappings.INDEX);
    //        overridden.add(ElasticsearchMappings.NESTED);
    //        overridden.add(ElasticsearchMappings.NO);
    //        overridden.add(ElasticsearchMappings.PARENT);
    //        overridden.add(ElasticsearchMappings.PROPERTIES);
    //        overridden.add(ElasticsearchMappings.TYPE);
    //        overridden.add(ElasticsearchMappings.WHITESPACE);
    //
    //        // These are not reserved because they're data types, not field names
    //        overridden.add(AnomalyRecord.TYPE.getPreferredName());
    //        overridden.add(AuditActivity.TYPE);
    //        overridden.add(AuditMessage.TYPE);
    //        overridden.add(Bucket.TYPE.getPreferredName());
    //        overridden.add(DataCounts.TYPE.getPreferredName());
    //        overridden.add(ReservedFieldNames.BUCKET_PROCESSING_TIME_TYPE);
    //        overridden.add(BucketInfluencer.TYPE.getPreferredName());
    //        overridden.add(CategorizerState.TYPE);
    //        overridden.add(CategoryDefinition.TYPE.getPreferredName());
    //        overridden.add(Influencer.TYPE.getPreferredName());
    //        overridden.add(JobDetails.TYPE);
    //        overridden.add(ListDocument.TYPE.getPreferredName());
    //        overridden.add(ModelDebugOutput.TYPE.getPreferredName());
    //        overridden.add(ModelState.TYPE);
    //        overridden.add(ModelSnapshot.TYPE.getPreferredName());
    //        overridden.add(ModelSizeStats.TYPE.getPreferredName());
    //        overridden.add(Quantiles.TYPE.getPreferredName());
    //        overridden.add(Usage.TYPE);
    //
    //        // These are not reserved because they're in the prelert-int index, not prelertresults-*
    //        overridden.add(ListDocument.ID.getPreferredName());
    //        overridden.add(ListDocument.ITEMS.getPreferredName());
    //
    //        // These are not reserved because they're analyzed strings, i.e. the same type as user-specified fields
    //        overridden.add(JobDetails.DESCRIPTION.getPreferredName());
    //        overridden.add(JobDetails.STATUS.getPreferredName());
    //        overridden.add(ModelSnapshot.DESCRIPTION.getPreferredName());
    //        overridden.add(SchedulerConfig.USERNAME.getPreferredName());
    //
    //        Set<String> expected = new HashSet<>();
    //
    //        Class<?> c = Class.forName("org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchMappings");
    //        Class<?> contentBuilder = Class.forName("org.elasticsearch.common.xcontent.XContentBuilder");
    //
    //        Method[] allMethods = c.getDeclaredMethods();
    //        for (Method m : allMethods) {
    //            if (Modifier.isStatic(m.getModifiers()) && m.getReturnType().equals(contentBuilder)) {
    //                XContentBuilder builder;
    //                if (m.getParameterCount() == 0) {
    //                    builder = (XContentBuilder) m.invoke(null);
    //                } else if (m.getParameterCount() == 1) {
    //                    List<Object> args = new ArrayList<>();
    //                    args.add(null);
    //
    //                    builder = (XContentBuilder) m.invoke(null, args);
    //                } else {
    //                    continue;
    //                }
    //                BufferedInputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(
    //                        builder.string().getBytes(StandardCharsets.UTF_8)));
    //
    //                JsonParser parser = new JsonFactory().createParser(inputStream);
    //                parseJson(parser, expected);
    //            }
    //        }
    //
    //        expected.removeAll(overridden);
    //        for (String s : expected) {
    //            // By comparing like this the failure messages say which string is missing
    //            String reserved = ReservedFieldNames.RESERVED_FIELD_NAMES.contains(s) ? s : null;
    //            assertEquals(s, reserved);
    //        }
    //    }

}
