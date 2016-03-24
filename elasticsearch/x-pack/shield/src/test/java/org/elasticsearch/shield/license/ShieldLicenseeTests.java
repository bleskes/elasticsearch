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
package org.elasticsearch.shield.license;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.core.License.OperationMode;
import org.elasticsearch.license.plugin.core.AbstractLicenseeTestCase;
import org.elasticsearch.license.plugin.core.Licensee.Status;
import org.elasticsearch.license.plugin.core.LicenseeRegistry;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests {@link ShieldLicensee}.
 * <p>
 * If you change the behavior of these tests, then it means that licensing changes for Security!
 */
public class ShieldLicenseeTests extends AbstractLicenseeTestCase {
    private final ShieldLicenseState shieldState = mock(ShieldLicenseState.class);
    private final LicenseeRegistry registry = mock(LicenseeRegistry.class);

    public void testStartsWithoutTribeNode() {
        ShieldLicensee licensee = new ShieldLicensee(Settings.EMPTY, registry, shieldState);

        // starting the Licensee start trigger it being registered
        licensee.start();

        verify(registry).register(licensee);
        verifyNoMoreInteractions(registry, shieldState);
    }

    public void testDoesNotStartWithTribeNode() {
        Settings settings = Settings.settingsBuilder().put("tribe.fake.cluster.name", "notchecked").build();
        ShieldLicensee licensee = new ShieldLicensee(settings, registry, shieldState);

        // starting the Licensee as a tribe node should not trigger it being registered
        licensee.start();

        verifyNoMoreInteractions(registry, shieldState);
    }

    public void testOnChangeModifiesShieldLicenseState() {
        Status status = mock(Status.class);

        ShieldLicensee licensee = new ShieldLicensee(Settings.EMPTY, registry, shieldState);

        licensee.onChange(status);

        assertSame(status, licensee.getStatus());

        verify(shieldState).updateStatus(status);
        verifyNoMoreInteractions(registry, shieldState);
    }

    public void testAcknowledgementMessagesFromFreeToAnyIsNoOp() {
        assertEmptyAck(randomFreeMode(), randomFrom(OperationMode.values()), this::buildLicensee);
    }

    public void testAcknowledgementMessagesFromAnyToNonGoldPaidIsNoOp() {
        assertEmptyAck(randomFrom(OperationMode.values()), randomFromPaidExcept(OperationMode.GOLD), this::buildLicensee);
    }

    public void testAcknowledgementMessagesFromPaidToFreeNotesLimits() {
        String[] messages = ackLicenseChange(randomPaidMode(), randomFreeMode(), this::buildLicensee);

        // leaving messages up to inspection
        assertThat(messages.length, equalTo(1));
    }

    public void testAcknowledgementMessagesFromNonGoldPaidToGoldNotesLimits() {
        String[] messages = ackLicenseChange(randomFromPaidExcept(OperationMode.GOLD), OperationMode.GOLD, this::buildLicensee);

        // leaving messages up to inspection
        assertThat(messages.length, equalTo(2));
    }

    private ShieldLicensee buildLicensee() {
        return new ShieldLicensee(Settings.EMPTY, registry, shieldState);
    }
}
