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

package org.elasticsearch.shield.authz.store;

import org.elasticsearch.shield.SecurityContext;
import org.elasticsearch.shield.authz.permission.KibanaRole;
import org.elasticsearch.shield.authz.permission.SuperuserRole;
import org.elasticsearch.shield.authz.permission.TransportClientRole;
import org.elasticsearch.shield.user.KibanaUser;
import org.elasticsearch.shield.user.SystemUser;
import org.elasticsearch.shield.user.User;
import org.elasticsearch.shield.user.XPackUser;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link ReservedRolesStore}
 */
public class ReservedRolesStoreTests extends ESTestCase {

    private final User user = new User("joe");
    private SecurityContext securityContext;
    private ReservedRolesStore reservedRolesStore;

    @Before
    public void setupMocks() {
        securityContext = mock(SecurityContext.class);
        when(securityContext.getUser()).thenReturn(user);
        reservedRolesStore = new ReservedRolesStore(securityContext);
    }

    public void testRetrievingReservedRolesNonKibanaUser() {
        if (randomBoolean()) {
            when(securityContext.getUser()).thenReturn(XPackUser.INSTANCE);
        }

        assertThat(reservedRolesStore.role(SuperuserRole.NAME), sameInstance(SuperuserRole.INSTANCE));
        assertThat(reservedRolesStore.roleDescriptor(SuperuserRole.NAME), sameInstance(SuperuserRole.DESCRIPTOR));

        assertThat(reservedRolesStore.role(TransportClientRole.NAME), sameInstance(TransportClientRole.INSTANCE));
        assertThat(reservedRolesStore.roleDescriptor(TransportClientRole.NAME), sameInstance(TransportClientRole.DESCRIPTOR));

        assertThat(reservedRolesStore.roleDescriptors(), contains(SuperuserRole.DESCRIPTOR, TransportClientRole.DESCRIPTOR));

        assertThat(reservedRolesStore.role(KibanaRole.NAME), nullValue());
        assertThat(reservedRolesStore.roleDescriptor(KibanaRole.NAME), nullValue());

        assertThat(reservedRolesStore.role(SystemUser.ROLE_NAME), nullValue());
    }

    public void testRetrievingReservedRoleKibanaUser() {
        when(securityContext.getUser()).thenReturn(KibanaUser.INSTANCE);
        assertThat(reservedRolesStore.role(SuperuserRole.NAME), sameInstance(SuperuserRole.INSTANCE));
        assertThat(reservedRolesStore.roleDescriptor(SuperuserRole.NAME), sameInstance(SuperuserRole.DESCRIPTOR));

        assertThat(reservedRolesStore.role(TransportClientRole.NAME), sameInstance(TransportClientRole.INSTANCE));
        assertThat(reservedRolesStore.roleDescriptor(TransportClientRole.NAME), sameInstance(TransportClientRole.DESCRIPTOR));

        assertThat(reservedRolesStore.role(KibanaRole.NAME), sameInstance(KibanaRole.INSTANCE));
        assertThat(reservedRolesStore.roleDescriptor(KibanaRole.NAME), sameInstance(KibanaRole.DESCRIPTOR));
        assertThat(reservedRolesStore.roleDescriptors(),
                contains(SuperuserRole.DESCRIPTOR, TransportClientRole.DESCRIPTOR, KibanaRole.DESCRIPTOR));

        assertThat(reservedRolesStore.role(SystemUser.ROLE_NAME), nullValue());
    }

    public void testIsReserved() {
        assertThat(ReservedRolesStore.isReserved(KibanaRole.NAME), is(true));
        assertThat(ReservedRolesStore.isReserved(SuperuserRole.NAME), is(true));
        assertThat(ReservedRolesStore.isReserved("foobar"), is(false));
        assertThat(ReservedRolesStore.isReserved(SystemUser.ROLE_NAME), is(true));
        assertThat(ReservedRolesStore.isReserved(TransportClientRole.NAME), is(true));
    }
}
