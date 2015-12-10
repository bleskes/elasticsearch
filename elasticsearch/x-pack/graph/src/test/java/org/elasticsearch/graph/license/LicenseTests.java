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

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.core.LicenseState;
import org.elasticsearch.license.plugin.core.Licensee;
import org.elasticsearch.license.plugin.core.LicenseeRegistry;
import org.elasticsearch.test.ESTestCase;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;

public class LicenseTests extends ESTestCase {

    private SimpleLicenseeRegistry licenseeRegistry = new SimpleLicenseeRegistry();

    public void testPlatinumTrialLicenseCanDoEverything() throws Exception {
        licenseeRegistry.setOperationMode(
                randomFrom(License.OperationMode.PLATINUM, License.OperationMode.TRIAL));
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicensePlatinumTrialBehaviour(graphLicensee);
    }

    public void testBasicLicenseIsDisabled() throws Exception {
        licenseeRegistry.setOperationMode(License.OperationMode.BASIC);
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicenseBasicOrGoldOrNoneOrExpiredBehaviour(graphLicensee);
    }

    public void testNoLicenseDoesNotWork() {
        licenseeRegistry.setOperationMode(License.OperationMode.BASIC);
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);
        licenseeRegistry.disable();

        assertLicenseBasicOrGoldOrNoneOrExpiredBehaviour(graphLicensee);
    }

    public void testExpiredPlatinumTrialLicenseIsRestricted() throws Exception {
        licenseeRegistry.setOperationMode(
                randomFrom(License.OperationMode.PLATINUM, License.OperationMode.TRIAL));
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);
        licenseeRegistry.disable();

        assertLicenseBasicOrGoldOrNoneOrExpiredBehaviour(graphLicensee);
    }

    public void testUpgradingFromBasicLicenseWorks() {
        licenseeRegistry.setOperationMode(License.OperationMode.BASIC);
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicenseBasicOrGoldOrNoneOrExpiredBehaviour(graphLicensee);

        licenseeRegistry.setOperationMode(
                randomFrom(License.OperationMode.PLATINUM, License.OperationMode.TRIAL));
        assertLicensePlatinumTrialBehaviour(graphLicensee);
    }

    public void testDowngradingToBasicLicenseWorks() {
        licenseeRegistry.setOperationMode(
                randomFrom(License.OperationMode.PLATINUM, License.OperationMode.TRIAL));
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicensePlatinumTrialBehaviour(graphLicensee);

        licenseeRegistry.setOperationMode(License.OperationMode.BASIC);
        assertLicenseBasicOrGoldOrNoneOrExpiredBehaviour(graphLicensee);
    }
    
    public void testDowngradingToGoldLicenseWorks() {
        licenseeRegistry.setOperationMode(
                randomFrom(License.OperationMode.PLATINUM, License.OperationMode.TRIAL));
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicensePlatinumTrialBehaviour(graphLicensee);

        licenseeRegistry.setOperationMode(License.OperationMode.GOLD);
        assertLicenseBasicOrGoldOrNoneOrExpiredBehaviour(graphLicensee);
    }    

    public void testUpgradingExpiredLicenseWorks() {
        licenseeRegistry.setOperationMode(
                randomFrom(License.OperationMode.PLATINUM, License.OperationMode.TRIAL));
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);
        licenseeRegistry.disable();

        assertLicenseBasicOrGoldOrNoneOrExpiredBehaviour(graphLicensee);

        licenseeRegistry.setOperationMode(
                randomFrom(License.OperationMode.PLATINUM, License.OperationMode.TRIAL));
        assertLicensePlatinumTrialBehaviour(graphLicensee);
    }

    private void assertLicensePlatinumTrialBehaviour(GraphLicensee graphLicensee) {
        assertThat("Expected graph exploration to be allowed", graphLicensee.isGraphExploreAllowed(), is(true));
    }

    private void assertLicenseBasicOrGoldOrNoneOrExpiredBehaviour(GraphLicensee graphLicensee) {
        assertThat("Expected graph exploration not to be allowed", graphLicensee.isGraphExploreAllowed(), is(false));
    }

    public static class SimpleLicenseeRegistry extends AbstractComponent implements LicenseeRegistry {
        private final List<Licensee> licensees = new ArrayList<>();
        private License.OperationMode operationMode;

        public SimpleLicenseeRegistry() {
            super(Settings.EMPTY);
        }

        @Override
        public void register(Licensee licensee) {
            licensees.add(licensee);
            enable();
        }

        public void enable() {
            for (Licensee licensee : licensees) {
                licensee.onChange(new Licensee.Status(operationMode, randomBoolean() ? LicenseState.ENABLED : LicenseState.GRACE_PERIOD));
            }
        }

        public void disable() {
            for (Licensee licensee : licensees) {
                licensee.onChange(new Licensee.Status(operationMode, LicenseState.DISABLED));
            }
        }

        public void setOperationMode(License.OperationMode operationMode) {
            this.operationMode = operationMode;
            enable();
        }
    }
}
