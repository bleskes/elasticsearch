/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
