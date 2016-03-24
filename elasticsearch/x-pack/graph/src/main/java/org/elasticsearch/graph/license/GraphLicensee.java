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

package org.elasticsearch.graph.license;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.core.AbstractLicenseeComponent;
import org.elasticsearch.license.plugin.core.LicenseeRegistry;
import org.elasticsearch.graph.Graph;

public class GraphLicensee extends AbstractLicenseeComponent<GraphLicensee> {

    public static final String ID = Graph.NAME;

    @Inject
    public GraphLicensee(Settings settings, LicenseeRegistry clientService) {
        super(settings, ID, clientService);
    }

    @Override
    public String[] expirationMessages() {
        return new String[] {
                "Graph explore APIs are disabled"
        };
    }

    @Override
    public String[] acknowledgmentMessages(License currentLicense, License newLicense) {
        if (newLicense.operationMode().allFeaturesEnabled() == false) {
            if (currentLicense != null && currentLicense.operationMode().allFeaturesEnabled()) {
                return new String[] { "Graph will be disabled" };
            }
        }
        return Strings.EMPTY_ARRAY;
    }

    public boolean isGraphExploreEnabled() {
        // status is volatile
        Status localStatus = status;
        return localStatus.getLicenseState().isActive() && localStatus.getMode().allFeaturesEnabled();
    }
}
