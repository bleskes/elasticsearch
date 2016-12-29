/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2016] Elasticsearch Incorporated. All Rights Reserved.
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
package org.elasticsearch.xpack.security.authc.esnative;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.elasticsearch.Version;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.user.LogstashSystemUser;
import org.junit.Before;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class NativeRealmMigratorTests extends ESTestCase {

    private Consumer<ActionListener<Void>> ensureDisabledHandler;
    private NativeUsersStore nativeUsersStore;
    private NativeRealmMigrator migrator;

    @Before
    public void setupMocks() {
        ensureDisabledHandler = listener -> listener.onResponse(null);
        nativeUsersStore = Mockito.mock(NativeUsersStore.class);
        Mockito.doAnswer(invocation -> {
            ActionListener<Void> listener = (ActionListener<Void>) invocation.getArguments()[1];
            ensureDisabledHandler.accept(listener);
            return null;
        }).when(nativeUsersStore).ensureReservedUserIsDisabled(any(), any());

        final Settings settings = Settings.EMPTY;
        migrator = new NativeRealmMigrator(settings, nativeUsersStore);
    }

    public void testNoChangeOnFreshInstall() throws Exception {
        verifyNoOpUpgrade(null);
    }

    public void testNoChangeOnUpgradeOnOrAfterV5_2() throws Exception {
        verifyNoOpUpgrade(randomFrom(Version.V_5_2_0_UNRELEASED));
    }

    public void testDisableLogstashOnUpgradeFromVersionPriorToV5_2() throws Exception {
        verifyUpgradeDisablesLogstashSystemUser(randomFrom(Version.V_5_1_1_UNRELEASED, Version.V_5_0_2, Version.V_5_0_0));
    }

    public void testExceptionInUsersStoreIsPropagatedToListener() throws Exception {
        final RuntimeException thrown = new RuntimeException("Forced failure");
        this.ensureDisabledHandler = listener -> listener.onFailure(thrown);
        final PlainActionFuture<Boolean> future = doUpgrade(Version.V_5_0_0);
        final ExecutionException caught = expectThrows(ExecutionException.class, future::get);
        assertThat(caught.getCause(), is(thrown));
    }

    private void verifyNoOpUpgrade(Version fromVersion) throws ExecutionException, InterruptedException {
        final PlainActionFuture<Boolean> future = doUpgrade(fromVersion);
        verifyNoMoreInteractions(nativeUsersStore);
        assertThat(future.get(), is(Boolean.FALSE));
    }

    private void verifyUpgradeDisablesLogstashSystemUser(Version fromVersion) throws ExecutionException, InterruptedException {
        final PlainActionFuture<Boolean> future = doUpgrade(fromVersion);
        verify(nativeUsersStore).ensureReservedUserIsDisabled(eq(LogstashSystemUser.NAME), any());
        verifyNoMoreInteractions(nativeUsersStore);
        assertThat(future.get(), is(Boolean.TRUE));
    }

    private PlainActionFuture<Boolean> doUpgrade(Version fromVersion) {
        final PlainActionFuture<Boolean> future = new PlainActionFuture<>();
        migrator.performUpgrade(fromVersion, future);
        return future;
    }
}