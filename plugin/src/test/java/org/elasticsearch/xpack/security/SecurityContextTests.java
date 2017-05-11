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

package org.elasticsearch.xpack.security;

import org.elasticsearch.Version;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.common.util.concurrent.ThreadContext.StoredContext;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.VersionUtils;
import org.elasticsearch.xpack.security.authc.Authentication;
import org.elasticsearch.xpack.security.authc.Authentication.RealmRef;
import org.elasticsearch.xpack.security.authc.AuthenticationService;
import org.elasticsearch.xpack.security.crypto.CryptoService;
import org.elasticsearch.xpack.security.user.SystemUser;
import org.elasticsearch.xpack.security.user.User;
import org.junit.Before;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class SecurityContextTests extends ESTestCase {

    private Settings settings;
    private ThreadContext threadContext;
    private CryptoService cryptoService;
    private SecurityContext securityContext;

    @Before
    public void buildSecurityContext() throws IOException {
        boolean signHeader = randomBoolean();
        settings = Settings.builder()
                .put("path.home", createTempDir())
                .put(AuthenticationService.SIGN_USER_HEADER.getKey(), signHeader)
                .build();
        threadContext = new ThreadContext(settings);
        cryptoService = new CryptoService(settings, new Environment(settings));
        securityContext = new SecurityContext(settings, threadContext, cryptoService);
        assertSettingDeprecationsAndWarnings(new Setting<?>[]{AuthenticationService.SIGN_USER_HEADER});
    }

    public void testGetAuthenticationAndUserInEmptyContext() throws IOException {
        assertNull(securityContext.getAuthentication());
        assertNull(securityContext.getUser());
    }

    public void testGetAuthenticationAndUser() throws IOException {
        final User user = new User("test");
        final Authentication authentication = new Authentication(user, new RealmRef("ldap", "foo", "node1"), null);
        authentication.writeToContext(threadContext, cryptoService, settings, Version.CURRENT, true);

        assertEquals(authentication, securityContext.getAuthentication());
        assertEquals(user, securityContext.getUser());
    }

    public void testSetUser() {
        final User user = new User("test");
        assertNull(securityContext.getAuthentication());
        assertNull(securityContext.getUser());
        securityContext.setUser(user, Version.CURRENT);
        assertEquals(user, securityContext.getUser());

        IllegalStateException e = expectThrows(IllegalStateException.class,
                () -> securityContext.setUser(randomFrom(user, SystemUser.INSTANCE), Version.CURRENT));
        assertEquals("authentication is already present in the context", e.getMessage());
    }

    public void testExecuteAsUser() throws IOException {
        final User original;
        if (randomBoolean()) {
            original = new User("test");
            final Authentication authentication = new Authentication(original, new RealmRef("ldap", "foo", "node1"), null);
            authentication.writeToContext(threadContext, cryptoService, settings, Version.CURRENT, true);
        } else {
            original = null;
        }

        final User executionUser = new User("executor");
        final AtomicReference<StoredContext> contextAtomicReference = new AtomicReference<>();
        securityContext.executeAsUser(executionUser, (originalCtx) -> {
            assertEquals(executionUser, securityContext.getUser());
            contextAtomicReference.set(originalCtx);
        }, Version.CURRENT);

        final User userAfterExecution = securityContext.getUser();
        assertEquals(original, userAfterExecution);
        StoredContext originalContext = contextAtomicReference.get();
        assertNotNull(originalContext);
        originalContext.restore();
        assertEquals(original, securityContext.getUser());
    }

    public void testExecuteAfterRewritingAuthentication() throws IOException {
        User user = new User("test", null, new User("authUser"));
        RealmRef authBy = new RealmRef("ldap", "foo", "node1");
        final Authentication original = new Authentication(user, authBy, authBy);
        original.writeToContext(threadContext, cryptoService, settings, Version.CURRENT, true);

        final AtomicReference<StoredContext> contextAtomicReference = new AtomicReference<>();
        securityContext.executeAfterRewritingAuthentication(originalCtx -> {
            Authentication authentication = securityContext.getAuthentication();
            assertEquals(original.getUser(), authentication.getUser());
            assertEquals(original.getAuthenticatedBy(), authentication.getAuthenticatedBy());
            assertEquals(original.getLookedUpBy(), authentication.getLookedUpBy());
            assertEquals(VersionUtils.getPreviousVersion(), authentication.getVersion());
            contextAtomicReference.set(originalCtx);
        }, VersionUtils.getPreviousVersion());

        final Authentication authAfterExecution = securityContext.getAuthentication();
        assertEquals(original, authAfterExecution);
        StoredContext originalContext = contextAtomicReference.get();
        assertNotNull(originalContext);
        originalContext.restore();
        assertEquals(original, securityContext.getAuthentication());
    }
}
