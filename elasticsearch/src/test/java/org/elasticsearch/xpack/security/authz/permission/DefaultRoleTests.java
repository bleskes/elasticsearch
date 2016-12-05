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

package org.elasticsearch.xpack.security.authz.permission;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthAction;
import org.elasticsearch.action.admin.cluster.state.ClusterStateAction;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.license.GetLicenseAction;
import org.elasticsearch.xpack.security.action.user.AuthenticateRequestBuilder;
import org.elasticsearch.xpack.security.action.user.ChangePasswordRequestBuilder;
import org.elasticsearch.xpack.security.authc.Authentication;
import org.elasticsearch.xpack.security.authc.Authentication.RealmRef;
import org.elasticsearch.xpack.security.authc.esnative.NativeRealm;
import org.elasticsearch.xpack.security.authc.esnative.ReservedRealm;
import org.elasticsearch.xpack.security.authc.file.FileRealm;
import org.elasticsearch.xpack.security.authc.ldap.LdapRealm;
import org.elasticsearch.xpack.security.authc.pki.PkiRealm;
import org.elasticsearch.xpack.security.user.User;
import org.elasticsearch.xpack.security.action.user.AuthenticateAction;
import org.elasticsearch.xpack.security.action.user.AuthenticateRequest;
import org.elasticsearch.xpack.security.action.user.ChangePasswordAction;
import org.elasticsearch.xpack.security.action.user.ChangePasswordRequest;
import org.elasticsearch.xpack.security.action.user.DeleteUserAction;
import org.elasticsearch.xpack.security.action.user.PutUserAction;
import org.elasticsearch.xpack.security.action.user.UserRequest;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.transport.TransportRequest;

import java.util.Iterator;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link DefaultRole}
 */
public class DefaultRoleTests extends ESTestCase {

    public void testDefaultRoleHasNoIndicesPrivileges() {
        Iterator<IndicesPermission.Group> iter = DefaultRole.INSTANCE.indices().iterator();
        assertThat(iter.hasNext(), is(false));
    }

    public void testDefaultRoleHasNoRunAsPrivileges() {
        assertThat(DefaultRole.INSTANCE.runAs().isEmpty(), is(true));
    }

    public void testDefaultRoleAllowsUser() {
        final User user = new User("joe");
        final boolean changePasswordRequest = randomBoolean();
        final TransportRequest request = changePasswordRequest ?
                new ChangePasswordRequestBuilder(mock(Client.class)).username(user.principal()).request() :
                new AuthenticateRequestBuilder(mock(Client.class)).username(user.principal()).request();
        final String action = changePasswordRequest ? ChangePasswordAction.NAME : AuthenticateAction.NAME;
        final Authentication authentication = mock(Authentication.class);
        final RealmRef authenticatedBy = mock(RealmRef.class);
        when(authentication.getUser()).thenReturn(user);
        when(authentication.getRunAsUser()).thenReturn(user);
        when(authentication.getAuthenticatedBy()).thenReturn(authenticatedBy);
        when(authenticatedBy.getType())
                .thenReturn(changePasswordRequest ? randomFrom(ReservedRealm.TYPE, NativeRealm.TYPE) : randomAsciiOfLengthBetween(4, 12));

        assertThat(request, instanceOf(UserRequest.class));
        assertThat(DefaultRole.INSTANCE.cluster().check(action, request, authentication), is(true));
    }

    public void testDefaultRoleDoesNotAllowNonMatchingUsername() {
        final User user = new User("joe");
        final boolean changePasswordRequest = randomBoolean();
        final String username = randomFrom("", "joe" + randomAsciiOfLengthBetween(1, 5), randomAsciiOfLengthBetween(3, 10));
        final TransportRequest request = changePasswordRequest ?
                new ChangePasswordRequestBuilder(mock(Client.class)).username(username).request() :
                new AuthenticateRequestBuilder(mock(Client.class)).username(username).request();
        final String action = changePasswordRequest ? ChangePasswordAction.NAME : AuthenticateAction.NAME;
        final Authentication authentication = mock(Authentication.class);
        final RealmRef authenticatedBy = mock(RealmRef.class);
        when(authentication.getUser()).thenReturn(user);
        when(authentication.getRunAsUser()).thenReturn(user);
        when(authentication.getAuthenticatedBy()).thenReturn(authenticatedBy);
        when(authenticatedBy.getType())
                .thenReturn(changePasswordRequest ? randomFrom(ReservedRealm.TYPE, NativeRealm.TYPE) : randomAsciiOfLengthBetween(4, 12));

        assertThat(request, instanceOf(UserRequest.class));
        assertThat(DefaultRole.INSTANCE.cluster().check(action, request, authentication), is(false));

        final User user2 = new User("admin", new String[] { "bar" }, user);
        when(authentication.getUser()).thenReturn(user2);
        when(authentication.getRunAsUser()).thenReturn(user);
        final RealmRef lookedUpBy = mock(RealmRef.class);
        when(authentication.getLookedUpBy()).thenReturn(lookedUpBy);
        when(lookedUpBy.getType())
                .thenReturn(changePasswordRequest ? randomFrom(ReservedRealm.TYPE, NativeRealm.TYPE) : randomAsciiOfLengthBetween(4, 12));
        // this should still fail since the username is still different
        assertThat(DefaultRole.INSTANCE.cluster().check(action, request, authentication), is(false));

        if (request instanceof ChangePasswordRequest) {
            ((ChangePasswordRequest)request).username("joe");
        } else {
            ((AuthenticateRequest)request).username("joe");
        }
        assertThat(DefaultRole.INSTANCE.cluster().check(action, request, authentication), is(true));
    }

    public void testDefaultRoleDoesNotAllowOtherActions() {
        final User user = mock(User.class);
        final TransportRequest request = mock(TransportRequest.class);
        final String action = randomFrom(PutUserAction.NAME, DeleteUserAction.NAME, ClusterHealthAction.NAME, ClusterStateAction.NAME,
                ClusterStatsAction.NAME, GetLicenseAction.NAME);
        final Authentication authentication = mock(Authentication.class);
        final RealmRef authenticatedBy = mock(RealmRef.class);
        when(authentication.getUser()).thenReturn(user);
        when(authentication.getRunAsUser()).thenReturn(randomBoolean() ? user : new User("runAs"));
        when(authentication.getAuthenticatedBy()).thenReturn(authenticatedBy);
        when(authenticatedBy.getType())
                .thenReturn(randomAsciiOfLengthBetween(4, 12));

        assertThat(DefaultRole.INSTANCE.cluster().check(action, request, authentication), is(false));
        verifyZeroInteractions(user, request, authentication);
    }

    public void testDefaultRoleWithRunAsChecksAuthenticatedBy() {
        final String username = "joe";
        final User runAs = new User(username);
        final User user = new User("admin", new String[] { "bar" }, runAs);
        final boolean changePasswordRequest = randomBoolean();
        final TransportRequest request = changePasswordRequest ?
                new ChangePasswordRequestBuilder(mock(Client.class)).username(username).request() :
                new AuthenticateRequestBuilder(mock(Client.class)).username(username).request();
        final String action = changePasswordRequest ? ChangePasswordAction.NAME : AuthenticateAction.NAME;
        final Authentication authentication = mock(Authentication.class);
        final RealmRef authenticatedBy = mock(RealmRef.class);
        final RealmRef lookedUpBy = mock(RealmRef.class);
        when(authentication.getUser()).thenReturn(user);
        when(authentication.getRunAsUser()).thenReturn(runAs);
        when(authentication.getAuthenticatedBy()).thenReturn(authenticatedBy);
        when(authentication.getLookedUpBy()).thenReturn(lookedUpBy);
        when(authentication.isRunAs()).thenReturn(true);
        when(lookedUpBy.getType())
                .thenReturn(changePasswordRequest ? randomFrom(ReservedRealm.TYPE, NativeRealm.TYPE) : randomAsciiOfLengthBetween(4, 12));

        assertThat(DefaultRole.INSTANCE.cluster().check(action, request, authentication), is(true));

        when(authentication.getRunAsUser()).thenReturn(user);
        assertThat(DefaultRole.INSTANCE.cluster().check(action, request, authentication), is(false));
    }

    public void testDefaultRoleDoesNotAllowChangePasswordForOtherRealms() {
        final User user = new User("joe");
        final ChangePasswordRequest request = new ChangePasswordRequestBuilder(mock(Client.class)).username(user.principal()).request();
        final String action = ChangePasswordAction.NAME;
        final Authentication authentication = mock(Authentication.class);
        final RealmRef authenticatedBy = mock(RealmRef.class);
        when(authentication.getUser()).thenReturn(user);
        when(authentication.getRunAsUser()).thenReturn(user);
        when(authentication.isRunAs()).thenReturn(false);
        when(authentication.getAuthenticatedBy()).thenReturn(authenticatedBy);
        when(authenticatedBy.getType()).thenReturn(randomFrom(LdapRealm.LDAP_TYPE, FileRealm.TYPE, LdapRealm.AD_TYPE, PkiRealm.TYPE,
                        randomAsciiOfLengthBetween(4, 12)));

        assertThat(request, instanceOf(UserRequest.class));
        assertThat(DefaultRole.INSTANCE.cluster().check(action, request, authentication), is(false));
        verify(authenticatedBy).getType();
        verify(authentication).getRunAsUser();
        verify(authentication).getAuthenticatedBy();
        verify(authentication).isRunAs();
        verifyNoMoreInteractions(authenticatedBy, authentication);
    }

    public void testDefaultRoleDoesNotAllowChangePasswordForLookedUpByOtherRealms() {
        final User runAs = new User("joe");
        final User user = new User("admin", new String[] { "bar" }, runAs);
        final ChangePasswordRequest request = new ChangePasswordRequestBuilder(mock(Client.class)).username(runAs.principal()).request();
        final String action = ChangePasswordAction.NAME;
        final Authentication authentication = mock(Authentication.class);
        final RealmRef authenticatedBy = mock(RealmRef.class);
        final RealmRef lookedUpBy = mock(RealmRef.class);
        when(authentication.getUser()).thenReturn(user);
        when(authentication.getRunAsUser()).thenReturn(runAs);
        when(authentication.isRunAs()).thenReturn(true);
        when(authentication.getAuthenticatedBy()).thenReturn(authenticatedBy);
        when(authentication.getLookedUpBy()).thenReturn(lookedUpBy);
        when(lookedUpBy.getType()).thenReturn(randomFrom(LdapRealm.LDAP_TYPE, FileRealm.TYPE, LdapRealm.AD_TYPE, PkiRealm.TYPE,
                randomAsciiOfLengthBetween(4, 12)));

        assertThat(request, instanceOf(UserRequest.class));
        assertThat(DefaultRole.INSTANCE.cluster().check(action, request, authentication), is(false));
        verify(authentication).getLookedUpBy();
        verify(authentication).getRunAsUser();
        verify(authentication).isRunAs();
        verify(lookedUpBy).getType();
        verifyNoMoreInteractions(authentication, lookedUpBy, authenticatedBy);
    }
}
