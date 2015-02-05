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

package org.elasticsearch.alerts.support;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.action.admin.indices.template.put.TransportPutIndexTemplateAction;
import org.elasticsearch.alerts.AlertsStore;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.settings.Settings;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class TemplateUtils extends AbstractComponent {

    private final TransportPutIndexTemplateAction transportPutIndexTemplateAction;

    @Inject
    public TemplateUtils(Settings settings, TransportPutIndexTemplateAction transportPutIndexTemplateAction) {
        super(settings);
        this.transportPutIndexTemplateAction = transportPutIndexTemplateAction;
    }

    /**
     * Checks if the template with the specified name exists and has the expected version.
     * If that isn't the case then the template from the classpath will be uploaded to the cluster.
     *
     * In the the template doesn't exists this method blocks until the template has been created.
     */
    public void ensureIndexTemplateIsLoaded(ClusterState state, final String templateName) {
        final byte[] template;
        try {
            InputStream is = AlertsStore.class.getResourceAsStream("/" + templateName + ".json");
            if (is == null) {
                throw new FileNotFoundException("Resource [/" + templateName + ".json] not found in classpath");
            }
            template = Streams.copyToByteArray(is);
            is.close();
        } catch (IOException e) {
            // throwing an exception to stop exporting process - we don't want to send data unless
            // we put in the template for it.
            throw new RuntimeException("failed to load " + templateName + ".json", e);
        }

        try {
            int expectedVersion = parseIndexVersionFromTemplate(template);
            if (expectedVersion < 0) {
                throw new RuntimeException("failed to find an index version in pre-configured index template");
            }

            IndexTemplateMetaData templateMetaData = state.metaData().templates().get(templateName);
            if (templateMetaData != null) {
                int foundVersion = templateMetaData.getSettings().getAsInt("index.alerts.template_version", -1);
                if (foundVersion < 0) {
                    logger.warn("found an existing index template [{}] but couldn't extract it's version. leaving it as is.", templateName);
                    return;
                } else if (foundVersion >= expectedVersion) {
                    logger.info("accepting existing index template [{}] (version [{}], needed [{}])", templateName, foundVersion, expectedVersion);
                    return;
                } else {
                    logger.info("replacing existing index template [{}] (version [{}], needed [{}])", templateName, foundVersion, expectedVersion);
                }
            } else {
                logger.info("Adding index template [{}], because none was found", templateName);
            }

            PutIndexTemplateRequest request = new PutIndexTemplateRequest(templateName).source(template);
            // We're already running on the master and TransportPutIndexTemplateAction#executor() is SAME, so it is ok to wait:
            ActionFuture<PutIndexTemplateResponse> future = transportPutIndexTemplateAction.execute(request);
            PutIndexTemplateResponse response = future.actionGet();
        } catch (IOException e) {
            // if we're not sure of the template, we can't send data... re-raise exception.
            throw new RuntimeException("failed to load/verify index template", e);
        }
    }

    private static int parseIndexVersionFromTemplate(byte[] template) throws UnsupportedEncodingException {
        Pattern versionRegex = Pattern.compile("alerts.template_version\"\\s*:\\s*\"?(\\d+)\"?");
        Matcher matcher = versionRegex.matcher(new String(template, "UTF-8"));
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            return -1;
        }
    }

}
