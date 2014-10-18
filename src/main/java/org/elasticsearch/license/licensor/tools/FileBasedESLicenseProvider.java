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

package org.elasticsearch.license.licensor.tools;

import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.core.LicenseBuilders;
import org.elasticsearch.license.manager.ESLicenseProvider;

import java.util.Set;

/**
 */
public class FileBasedESLicenseProvider implements ESLicenseProvider {
    private ESLicenses esLicenses;

    public FileBasedESLicenseProvider(ESLicenses esLicenses) {
        this.esLicenses = esLicenses;
    }

    public FileBasedESLicenseProvider(Set<ESLicenses> esLicensesSet) {
        this(merge(esLicensesSet));
    }

    @Override
    public ESLicenses.ESLicense getESLicense(ESLicenses.FeatureType featureType) {
        return esLicenses.get(featureType);
    }

    @Override
    public ESLicenses getEffectiveLicenses() {
        return esLicenses;
    }

    // For testing
    public void setLicenses(ESLicenses esLicenses) {
        this.esLicenses = esLicenses;
    }

    private static ESLicenses merge(Set<ESLicenses> esLicensesSet) {
        ESLicenses mergedLicenses = null;
        for (ESLicenses licenses : esLicensesSet) {
            mergedLicenses = LicenseBuilders.merge(mergedLicenses, licenses);
        }
        return mergedLicenses;
    }

}
