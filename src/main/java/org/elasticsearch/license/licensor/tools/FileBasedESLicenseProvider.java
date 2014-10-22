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

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.manager.Utils;

import java.util.Map;
import java.util.Set;

/**
 */
public class FileBasedESLicenseProvider {
    private ImmutableMap<String, ESLicense> esLicenses;

    public FileBasedESLicenseProvider(Set<ESLicense> esLicenses) {
        this.esLicenses = Utils.reduceAndMap(esLicenses);
    }

    public ESLicense getESLicense(String feature) {
        return esLicenses.get(feature);
    }

    public Map<String, ESLicense> getEffectiveLicenses() {
        return esLicenses;
    }

    // For testing
    public void setLicenses(Set<ESLicense> esLicenses) {
        this.esLicenses = Utils.reduceAndMap(esLicenses);
    }
}
