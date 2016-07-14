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

package org.elasticsearch.xpack.graph;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.core.License.OperationMode;
import org.elasticsearch.license.plugin.core.AbstractLicenseeComponent;
import org.elasticsearch.license.plugin.core.LicenseState;

public class GraphLicensee extends AbstractLicenseeComponent {

    public static final String ID = Graph.NAME;

    public GraphLicensee(Settings settings) {
        super(settings, ID);
    }

    @Override
    public String[] expirationMessages() {
        return new String[] {
                "Graph explore APIs are disabled"
        };
    }

    @Override
    public String[] acknowledgmentMessages(OperationMode currentMode, OperationMode newMode) {
        switch (newMode) {
            case BASIC:
            case STANDARD:
            case GOLD:
                switch (currentMode) {
                    case TRIAL:
                    case PLATINUM:
                        return new String[] { "Graph will be disabled" };
                }
                break;
        }
        return Strings.EMPTY_ARRAY;
    }

    /**
     * Determine if Graph Exploration should be enabled.
     * <p>
     * Exploration is only disabled when the license has expired or if the mode is not:
     * <ul>
     * <li>{@link OperationMode#PLATINUM}</li>
     * <li>{@link OperationMode#TRIAL}</li>
     * </ul>
     *
     * @return {@code true} as long as the license is valid. Otherwise {@code false}.
     */
    public boolean isAvailable() {
        // status is volatile
        Status localStatus = status;
        OperationMode operationMode = localStatus.getMode();

        boolean licensed = operationMode == OperationMode.TRIAL || operationMode == OperationMode.PLATINUM;

        return licensed && localStatus.getLicenseState() != LicenseState.DISABLED;
    }
}
