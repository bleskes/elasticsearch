/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated
 *  All Rights Reserved.
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
package org.elasticsearch.license.plugin.core;

import org.elasticsearch.common.inject.ImplementedBy;

import static org.elasticsearch.license.plugin.core.LicensesService.TrialLicenseOptions;

@ImplementedBy(LicensesService.class)
public interface LicensesClientService {

    public interface Listener {

        /**
         * Called to enable a feature
         */
        public void onEnabled();

        /**
         * Called to disable a feature
         */
        public void onDisabled();
    }

    /**
     * Registers a feature for licensing
     *
     * @param feature             - name of the feature to register (must be in sync with license Generator feature name)
     * @param trialLicenseOptions - Trial license specification used to generate a one-time trial license for the feature;
     *                            use <code>null</code> if no trial license should be generated for the feature
     * @param listener            - used to notify on feature enable/disable
     */
    void register(String feature, TrialLicenseOptions trialLicenseOptions, Listener listener);
}
