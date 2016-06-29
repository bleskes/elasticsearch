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

package org.elasticsearch.xpack.monitoring.agent.exporter.http;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class HttpExporterUtils {

    public static URL parseHostWithPath(String host, String path) throws URISyntaxException, MalformedURLException {

        if (!host.contains("://")) {
            // prefix with http
            host = "http://" + host;
        }
        if (!host.endsWith("/")) {
            // make sure we can safely resolves sub paths and not replace parent folders
            host = host + "/";
        }

        URL hostUrl = new URL(host);

        if (hostUrl.getPort() == -1) {
            // url has no port, default to 9200 - sadly we need to rebuild..
            StringBuilder newUrl = new StringBuilder(hostUrl.getProtocol() + "://");
            newUrl.append(hostUrl.getHost()).append(":9200").append(hostUrl.toURI().getPath());
            hostUrl = new URL(newUrl.toString());

        }
        return new URL(hostUrl, path);

    }

}
