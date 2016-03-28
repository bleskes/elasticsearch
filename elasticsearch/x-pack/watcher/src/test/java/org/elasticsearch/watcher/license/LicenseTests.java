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

package org.elasticsearch.watcher.license;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.core.License.OperationMode;
import org.elasticsearch.license.plugin.core.AbstractLicenseeTestCase;

import static org.hamcrest.Matchers.is;

public class LicenseTests extends AbstractLicenseeTestCase {

    private SimpleLicenseeRegistry licenseeRegistry = new SimpleLicenseeRegistry();

    public void testPlatinumGoldTrialLicenseCanDoEverything() throws Exception {
        licenseeRegistry.setOperationMode(randomTrialStandardGoldOrPlatinumMode());
        WatcherLicensee watcherLicensee = new WatcherLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(watcherLicensee);

        assertLicenseGoldPlatinumTrialBehaviour(watcherLicensee);
    }

    public void testBasicLicenseIsDisabled() throws Exception {
        licenseeRegistry.setOperationMode(OperationMode.BASIC);
        WatcherLicensee watcherLicensee = new WatcherLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(watcherLicensee);

        assertLicenseBasicOrNoneOrExpiredBehaviour(watcherLicensee);
    }

    public void testNoLicenseDoesNotWork() {
        licenseeRegistry.setOperationMode(OperationMode.BASIC);
        WatcherLicensee watcherLicensee = new WatcherLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(watcherLicensee);
        licenseeRegistry.disable();

        assertLicenseBasicOrNoneOrExpiredBehaviour(watcherLicensee);
    }

    public void testExpiredPlatinumGoldTrialLicenseIsRestricted() throws Exception {
        licenseeRegistry.setOperationMode(randomTrialStandardGoldOrPlatinumMode());
        WatcherLicensee watcherLicensee = new WatcherLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(watcherLicensee);
        licenseeRegistry.disable();

        assertLicenseBasicOrNoneOrExpiredBehaviour(watcherLicensee);
    }

    public void testUpgradingFromBasicLicenseWorks() {
        licenseeRegistry.setOperationMode(OperationMode.BASIC);
        WatcherLicensee watcherLicensee = new WatcherLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(watcherLicensee);

        assertLicenseBasicOrNoneOrExpiredBehaviour(watcherLicensee);

        licenseeRegistry.setOperationMode(randomTrialStandardGoldOrPlatinumMode());
        assertLicenseGoldPlatinumTrialBehaviour(watcherLicensee);
    }

    public void testDowngradingToBasicLicenseWorks() {
        licenseeRegistry.setOperationMode(randomTrialStandardGoldOrPlatinumMode());
        WatcherLicensee watcherLicensee = new WatcherLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(watcherLicensee);

        assertLicenseGoldPlatinumTrialBehaviour(watcherLicensee);

        licenseeRegistry.setOperationMode(OperationMode.BASIC);
        assertLicenseBasicOrNoneOrExpiredBehaviour(watcherLicensee);
    }

    public void testUpgradingExpiredLicenseWorks() {
        licenseeRegistry.setOperationMode(randomTrialStandardGoldOrPlatinumMode());
        WatcherLicensee watcherLicensee = new WatcherLicensee(Settings.EMPTY, licenseeRegistry);
        licenseeRegistry.register(watcherLicensee);
        licenseeRegistry.disable();

        assertLicenseBasicOrNoneOrExpiredBehaviour(watcherLicensee);

        licenseeRegistry.setOperationMode(randomTrialStandardGoldOrPlatinumMode());
        assertLicenseGoldPlatinumTrialBehaviour(watcherLicensee);
    }

    private void assertLicenseGoldPlatinumTrialBehaviour(WatcherLicensee watcherLicensee) {
        assertThat("Expected putting a watch to be allowed", watcherLicensee.isPutWatchAllowed(), is(true));
        assertThat("Expected getting a watch to be allowed", watcherLicensee.isGetWatchAllowed(), is(true));
        assertThat("Expected watcher transport actions to be allowed", watcherLicensee.isWatcherTransportActionAllowed(), is(true));
        assertThat("Expected actions of a watch to be executed", watcherLicensee.isExecutingActionsAllowed(), is(true));
    }

    private void assertLicenseBasicOrNoneOrExpiredBehaviour(WatcherLicensee watcherLicensee) {
        assertThat("Expected putting a watch not to be allowed", watcherLicensee.isPutWatchAllowed(), is(false));
        assertThat("Expected getting a watch not to be allowed", watcherLicensee.isGetWatchAllowed(), is(false));
        assertThat("Expected watcher transport actions not to be allowed", watcherLicensee.isWatcherTransportActionAllowed(), is(false));
        assertThat("Expected actions of a watch not to be executed", watcherLicensee.isExecutingActionsAllowed(), is(false));
    }
}
