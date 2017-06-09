/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.monitoring.test;

import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.plugins.IngestPlugin;
import org.elasticsearch.plugins.Plugin;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mock representation of the Ingest Common plugin for the subset of processors that are needed for the pipelines in Monitoring's exporters.
 */
public class MockIngestPlugin extends Plugin implements IngestPlugin {

    @Override
    public Map<String, Processor.Factory> getProcessors(final Processor.Parameters parameters) {
        final Map<String, String[]> processorFields = MapBuilder.<String, String[]>newMapBuilder()
                .put("gsub", new String[] { "field", "pattern", "replacement" })
                .put("rename", new String[] { "field", "target_field" })
                .put("set", new String[] { "field", "value" })
                .put("script", new String[] { "source" })
                .map();

        return processorFields.entrySet()
                              .stream()
                              .map(MockProcessorFactory::new)
                              .collect(Collectors.toMap(factory -> factory.type, factory -> factory));
    }

    static class MockProcessorFactory implements Processor.Factory {

        private final String type;
        private final String[] fields;

        MockProcessorFactory(final Map.Entry<String, String[]> factory) {
            this(factory.getKey(), factory.getValue());
        }

        MockProcessorFactory(final String type, final String[] fields) {
            this.type = type;
            this.fields = fields;
        }

        @Override
        public Processor create(Map<String, Processor.Factory> processorFactories,
                                String tag,
                                Map<String, Object> config) throws Exception {
            // read fields so the processor succeeds
            for (final String field : fields) {
                ConfigurationUtils.readObject(type, tag, config, field);
            }

            return new MockProcessor(type, tag);
        }

    }

    static class MockProcessor implements Processor {

        private final String type;
        private final String tag;

        MockProcessor(final String type, final String tag) {
            this.type = type;
            this.tag = tag;
        }

        @Override
        public void execute(IngestDocument ingestDocument) throws Exception {
            // mock processor does nothing
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getTag() {
            return tag;
        }

    }

}
