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

package org.elasticsearch.license.plugin.core;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.ack.ClusterStateUpdateResponse;
import org.elasticsearch.common.inject.ImplementedBy;
import org.elasticsearch.license.core.ESLicense;

import java.util.List;
import java.util.Set;

import static org.elasticsearch.license.plugin.core.LicensesService.DeleteLicenseRequestHolder;
import static org.elasticsearch.license.plugin.core.LicensesService.LicensesUpdateResponse;
import static org.elasticsearch.license.plugin.core.LicensesService.PutLicenseRequestHolder;

@ImplementedBy(LicensesService.class)
public interface LicensesManagerService {

    /**
     * Registers new licenses in the cluster
     * <p/>
     * This method can be only called on the master node. It tries to create a new licenses on the master
     * and if provided license(s) is VALID it is added to cluster state metadata {@link org.elasticsearch.license.plugin.core.LicensesMetaData}
     */
    public void registerLicenses(final PutLicenseRequestHolder requestHolder, final ActionListener<LicensesUpdateResponse> listener);

    /**
     * Remove only signed license(s) for provided features from the cluster state metadata
     */
    public void removeLicenses(final DeleteLicenseRequestHolder requestHolder, final ActionListener<ClusterStateUpdateResponse> listener);

    /**
     * @return the set of features that are currently enabled
     */
    public Set<String> enabledFeatures();

    /**
     * @return a list of licenses, contains one license (with the latest expiryDate) per registered features sorted by latest issueDate
     */
    public List<ESLicense> getLicenses();
}
