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

package org.elasticsearch.watcher.support;

import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.watcher.WatcherException;
import org.elasticsearch.watcher.support.init.proxy.ClientProxy;
import org.elasticsearch.watcher.watch.WatchStore;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 */
public class TemplateUtils extends AbstractComponent {

    private final ClientProxy client;

    @Inject
    public TemplateUtils(Settings settings, ClientProxy client) {
        super(settings);
        this.client = client;
    }

    /**
     * Resolves the template with the specified templateName from the classpath, optionally adds extra settings and
     * puts the index template into the cluster.
     *
     * This method blocks until the template has been created.
     */
    public void putTemplate(String templateName, Settings customSettings) {
        try (InputStream is = WatchStore.class.getResourceAsStream("/" + templateName + ".json")) {
            if (is == null) {
                throw new FileNotFoundException("Resource [/" + templateName + ".json] not found in classpath");
            }
            final byte[] template = Streams.copyToByteArray(is);
            PutIndexTemplateRequest request = new PutIndexTemplateRequest(templateName).source(template);
            if (customSettings != null) {
                Settings updatedSettings = ImmutableSettings.builder()
                        .put(request.settings())
                        .put(customSettings)
                        .build();
                request.settings(updatedSettings);
            }
            PutIndexTemplateResponse response = client.putTemplate(request);
        } catch (Exception e) {
            // throwing an exception to stop exporting process - we don't want to send data unless
            // we put in the template for it.
            throw new WatcherException("failed to load [{}.json]", e, templateName);
        }
    }

}
