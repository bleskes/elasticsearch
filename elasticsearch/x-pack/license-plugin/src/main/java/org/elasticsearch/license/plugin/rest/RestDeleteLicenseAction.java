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
package org.elasticsearch.license.plugin.rest;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.action.delete.DeleteLicenseAction;
import org.elasticsearch.license.plugin.action.delete.DeleteLicenseRequest;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.support.AcknowledgedRestListener;
import org.elasticsearch.xpack.XPackClient;
import org.elasticsearch.xpack.rest.XPackRestHandler;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;

public class RestDeleteLicenseAction extends XPackRestHandler {

    @Inject
    public RestDeleteLicenseAction(Settings settings, RestController controller) {
        super(settings);
        // @deprecated Remove deprecations in 6.0
        controller.registerWithDeprecatedHandler(DELETE, URI_BASE + "/license", this,
                                                 DELETE, "/_license", deprecationLogger);

        // Remove _licenses support entirely in 6.0
        controller.registerAsDeprecatedHandler(DELETE, "/_licenses", this,
                                               "[DELETE /_licenses] is deprecated! Use " +
                                               "[DELETE /_xpack/license] instead.",
                                               deprecationLogger);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final XPackClient client) {
        client.es().admin().cluster().execute(DeleteLicenseAction.INSTANCE,
                                              new DeleteLicenseRequest(),
                                              new AcknowledgedRestListener<>(channel));
    }
}
