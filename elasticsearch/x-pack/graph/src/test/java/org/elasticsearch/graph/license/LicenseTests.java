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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.graph.GraphLicensee;
import org.elasticsearch.license.core.License.OperationMode;
import org.elasticsearch.license.plugin.core.AbstractLicenseeTestCase;

import static org.hamcrest.Matchers.is;

public class LicenseTests extends AbstractLicenseeTestCase {

    private SimpleLicenseeRegistry licenseeRegistry = new SimpleLicenseeRegistry();

    public void testPlatinumTrialLicenseCanDoEverything() throws Exception {
        licenseeRegistry.setOperationMode(randomTrialOrPlatinumMode());
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicensePlatinumTrialBehaviour(graphLicensee);
    }

    public void testBasicLicenseIsDisabled() throws Exception {
        licenseeRegistry.setOperationMode(OperationMode.BASIC);
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicenseBasicOrStandardGoldOrNoneOrExpiredBehaviour(graphLicensee);
    }
    
    public void testStandardLicenseIsDisabled() throws Exception {
        licenseeRegistry.setOperationMode(OperationMode.STANDARD);
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicenseBasicOrStandardGoldOrNoneOrExpiredBehaviour(graphLicensee);
    }    

    public void testNoLicenseDoesNotWork() {
        licenseeRegistry.setOperationMode(OperationMode.BASIC);
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);
        licenseeRegistry.disable();

        assertLicenseBasicOrStandardGoldOrNoneOrExpiredBehaviour(graphLicensee);
    }

    public void testExpiredPlatinumTrialLicenseIsRestricted() throws Exception {
        licenseeRegistry.setOperationMode(randomTrialOrPlatinumMode());
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);
        licenseeRegistry.disable();

        assertLicenseBasicOrStandardGoldOrNoneOrExpiredBehaviour(graphLicensee);
    }

    public void testUpgradingFromBasicLicenseWorks() {
        licenseeRegistry.setOperationMode(OperationMode.BASIC);
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicenseBasicOrStandardGoldOrNoneOrExpiredBehaviour(graphLicensee);

        licenseeRegistry.setOperationMode(randomTrialOrPlatinumMode());
        assertLicensePlatinumTrialBehaviour(graphLicensee);
    }

    public void testDowngradingToBasicLicenseWorks() {
        licenseeRegistry.setOperationMode(randomTrialOrPlatinumMode());
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicensePlatinumTrialBehaviour(graphLicensee);

        licenseeRegistry.setOperationMode(OperationMode.BASIC);
        assertLicenseBasicOrStandardGoldOrNoneOrExpiredBehaviour(graphLicensee);
    }
    
    public void testUpgradingFromStandardLicenseWorks() {
        licenseeRegistry.setOperationMode(OperationMode.STANDARD);
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicenseBasicOrStandardGoldOrNoneOrExpiredBehaviour(graphLicensee);

        licenseeRegistry.setOperationMode(randomTrialOrPlatinumMode());
        assertLicensePlatinumTrialBehaviour(graphLicensee);
    }

    public void testDowngradingToStandardLicenseWorks() {
        licenseeRegistry.setOperationMode(randomTrialOrPlatinumMode());
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicensePlatinumTrialBehaviour(graphLicensee);

        licenseeRegistry.setOperationMode(OperationMode.STANDARD);
        assertLicenseBasicOrStandardGoldOrNoneOrExpiredBehaviour(graphLicensee);
    }    
    
    public void testDowngradingToGoldLicenseWorks() {
        licenseeRegistry.setOperationMode(randomTrialOrPlatinumMode());
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);

        assertLicensePlatinumTrialBehaviour(graphLicensee);

        licenseeRegistry.setOperationMode(OperationMode.GOLD);
        assertLicenseBasicOrStandardGoldOrNoneOrExpiredBehaviour(graphLicensee);
    }    

    public void testUpgradingExpiredLicenseWorks() {
        licenseeRegistry.setOperationMode(randomTrialOrPlatinumMode());
        GraphLicensee graphLicensee = new GraphLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(graphLicensee);
        licenseeRegistry.disable();

        assertLicenseBasicOrStandardGoldOrNoneOrExpiredBehaviour(graphLicensee);

        licenseeRegistry.setOperationMode(randomTrialOrPlatinumMode());
        assertLicensePlatinumTrialBehaviour(graphLicensee);
    }

    private void assertLicensePlatinumTrialBehaviour(GraphLicensee graphLicensee) {
        assertThat("Expected graph exploration to be allowed", graphLicensee.isAvailable(), is(true));
    }

    private void assertLicenseBasicOrStandardGoldOrNoneOrExpiredBehaviour(GraphLicensee graphLicensee) {
        assertThat("Expected graph exploration not to be allowed", graphLicensee.isAvailable(), is(false));
    }
}
