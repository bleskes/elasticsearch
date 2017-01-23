/*
 * ELASTICSEARCH CONFIDENTIAL
 *  __________________
 *
 * [2014] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.license;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.XPackClient;
import org.elasticsearch.xpack.rest.XPackRestHandler;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;

public class RestDeleteLicenseAction extends XPackRestHandler {
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
    public RestChannelConsumer doPrepareRequest(final RestRequest request, final XPackClient client) throws IOException {
        return channel -> client.es().admin().cluster().execute(DeleteLicenseAction.INSTANCE,
                                              new DeleteLicenseRequest(),
                                              new AcknowledgedRestListener<>(channel));
    }

}
