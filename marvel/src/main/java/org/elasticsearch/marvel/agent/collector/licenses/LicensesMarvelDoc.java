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

package org.elasticsearch.marvel.agent.collector.licenses;

import org.elasticsearch.license.core.License;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;

import java.util.List;

public class LicensesMarvelDoc extends MarvelDoc<LicensesMarvelDoc.Payload> {

    private final Payload payload;

    LicensesMarvelDoc(String index, String type, String id, String clusterName, long timestamp, Payload payload) {
        super(index, type, id, clusterName, timestamp);
        this.payload = payload;
    }

    @Override
    public LicensesMarvelDoc.Payload payload() {
        return payload;
    }

    public static LicensesMarvelDoc createMarvelDoc(String index, String type, String id, String clusterName, long timestamp, String version, List<License> licenses) {
        return new LicensesMarvelDoc(index, type, id, clusterName, timestamp, new Payload(version, licenses));
    }

    public static class Payload {

        private final String version;
        private final List<License> licenses;

        public Payload(String version, List<License> licenses) {
            this.version = version;
            this.licenses = licenses;
        }

        public String getVersion() {
            return version;
        }

        public List<License> getLicenses() {
            return licenses;
        }
    }
}
