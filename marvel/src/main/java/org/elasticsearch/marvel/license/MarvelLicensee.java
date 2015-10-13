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

package org.elasticsearch.marvel.license;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.support.LoggerMessageFormat;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.core.*;
import org.elasticsearch.marvel.MarvelPlugin;


public class MarvelLicensee extends AbstractLicenseeComponent<MarvelLicensee> implements Licensee {

    @Inject
    public MarvelLicensee(Settings settings, LicenseeRegistry clientService) {
        super(settings, MarvelPlugin.NAME, clientService);
    }

    @Override
    public String[] expirationMessages() {
        return new String[] {
                "The agent will stop collecting cluster and indices metrics"
        };
    }

    @Override
    public String[] acknowledgmentMessages(License currentLicense, License newLicense) {
        switch (newLicense.operationMode()) {
            case BASIC:
                if (currentLicense != null) {
                    switch (currentLicense.operationMode()) {
                        case TRIAL:
                        case GOLD:
                        case PLATINUM:
                            return new String[] {
                                    LoggerMessageFormat.format(
                                            "Multi-cluster support is disabled for clusters with [{}] license. If you are\n" +
                                            "running multiple clusters, users won't be able to access the clusters with\n" +
                                            "[{}] licenses from within a single Marvel instance. You will have to deploy a\n" +
                                            "separate and dedicated Marvel instance for each [{}] cluster you wish to monitor.",
                                            newLicense.type(), newLicense.type(), newLicense.type())
                            };
                    }
                }
        }
        return Strings.EMPTY_ARRAY;
    }

    public boolean collectionEnabled() {
        // when checking multiple parts of the status, we should get a local reference to the status object since it is
        // volatile and can change between check statements...
        Status status = this.status;
        return status.getMode() != License.OperationMode.NONE &&
                status.getLicenseState() != LicenseState.DISABLED;
    }

}
