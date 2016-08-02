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

/**
 * Licensing for xpack.
 *
 * A {@link org.elasticsearch.license.License} is a signed set of json properties that determine what features
 * are available in a running cluster. Licenses are registered through a
 * {@link org.elasticsearch.license.PutLicenseRequest}. This action is handled by the master node, which places
 * the signed license into the cluster state. Each node listens for cluster state updates via the
 * {@link org.elasticsearch.license.LicenseService}, and updates its local copy of the license when it detects
 * changes in the cluster state.
 *
 * The logic for which features are available given the current license is handled by
 * {@link org.elasticsearch.license.XPackLicenseState}, which is updated by the
 * {@link org.elasticsearch.license.LicenseService} when the license changes.
 */
package org.elasticsearch.license;