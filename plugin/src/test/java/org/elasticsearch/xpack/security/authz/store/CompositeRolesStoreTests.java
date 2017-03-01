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

package org.elasticsearch.xpack.security.authz.store;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.authz.RoleDescriptor.IndicesPrivileges;
import org.elasticsearch.xpack.security.authz.permission.Role;
import org.elasticsearch.xpack.security.authz.RoleDescriptor;
import org.elasticsearch.xpack.security.authz.permission.FieldPermissionsCache;
import org.elasticsearch.xpack.security.authz.privilege.IndexPrivilege;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.elasticsearch.mock.orig.Mockito.times;
import static org.elasticsearch.mock.orig.Mockito.verifyNoMoreInteractions;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CompositeRolesStoreTests extends ESTestCase {

    public void testRolesWhenDlsFlsUnlicensed() {
        XPackLicenseState licenseState = mock(XPackLicenseState.class);
        when(licenseState.isDocumentAndFieldLevelSecurityAllowed()).thenReturn(false);
        RoleDescriptor flsRole = new RoleDescriptor("fls", null, new IndicesPrivileges[] {
                IndicesPrivileges.builder()
                        .grantedFields("*")
                        .deniedFields("foo")
                        .indices("*")
                        .privileges("read")
                        .build()
        }, null);
        RoleDescriptor dlsRole = new RoleDescriptor("dls", null, new IndicesPrivileges[] {
                IndicesPrivileges.builder()
                        .indices("*")
                        .privileges("read")
                        .query(QueryBuilders.matchAllQuery().buildAsBytes())
                        .build()
        }, null);
        RoleDescriptor flsDlsRole = new RoleDescriptor("fls_dls", null, new IndicesPrivileges[] {
                IndicesPrivileges.builder()
                        .indices("*")
                        .privileges("read")
                        .grantedFields("*")
                        .deniedFields("foo")
                        .query(QueryBuilders.matchAllQuery().buildAsBytes())
                        .build()
        }, null);
        RoleDescriptor noFlsDlsRole = new RoleDescriptor("no_fls_dls", null, new IndicesPrivileges[] {
                IndicesPrivileges.builder()
                        .indices("*")
                        .privileges("read")
                        .build()
        }, null);
        FileRolesStore fileRolesStore = mock(FileRolesStore.class);
        when(fileRolesStore.roleDescriptors(Collections.singleton("fls"))).thenReturn(Collections.singleton(flsRole));
        when(fileRolesStore.roleDescriptors(Collections.singleton("dls"))).thenReturn(Collections.singleton(dlsRole));
        when(fileRolesStore.roleDescriptors(Collections.singleton("fls_dls"))).thenReturn(Collections.singleton(flsDlsRole));
        when(fileRolesStore.roleDescriptors(Collections.singleton("no_fls_dls"))).thenReturn(Collections.singleton(noFlsDlsRole));
        CompositeRolesStore compositeRolesStore = new CompositeRolesStore(Settings.EMPTY, fileRolesStore, mock(NativeRolesStore.class),
                mock(ReservedRolesStore.class), Collections.emptyList(), new ThreadContext(Settings.EMPTY), licenseState);

        FieldPermissionsCache fieldPermissionsCache = new FieldPermissionsCache(Settings.EMPTY);
        PlainActionFuture<Role> roleFuture = new PlainActionFuture<>();
        compositeRolesStore.roles(Collections.singleton("fls"), fieldPermissionsCache, roleFuture);
        assertEquals(Role.EMPTY, roleFuture.actionGet());

        roleFuture = new PlainActionFuture<>();
        compositeRolesStore.roles(Collections.singleton("dls"), fieldPermissionsCache, roleFuture);
        assertEquals(Role.EMPTY, roleFuture.actionGet());

        roleFuture = new PlainActionFuture<>();
        compositeRolesStore.roles(Collections.singleton("fls_dls"), fieldPermissionsCache, roleFuture);
        assertEquals(Role.EMPTY, roleFuture.actionGet());

        roleFuture = new PlainActionFuture<>();
        compositeRolesStore.roles(Collections.singleton("no_fls_dls"), fieldPermissionsCache, roleFuture);
        assertNotEquals(Role.EMPTY, roleFuture.actionGet());
    }

    public void testRolesWhenDlsFlsLicensed() {
        XPackLicenseState licenseState = mock(XPackLicenseState.class);
        when(licenseState.isDocumentAndFieldLevelSecurityAllowed()).thenReturn(true);
        RoleDescriptor flsRole = new RoleDescriptor("fls", null, new IndicesPrivileges[] {
                IndicesPrivileges.builder()
                        .grantedFields("*")
                        .deniedFields("foo")
                        .indices("*")
                        .privileges("read")
                        .build()
        }, null);
        RoleDescriptor dlsRole = new RoleDescriptor("dls", null, new IndicesPrivileges[] {
                IndicesPrivileges.builder()
                        .indices("*")
                        .privileges("read")
                        .query(QueryBuilders.matchAllQuery().buildAsBytes())
                        .build()
        }, null);
        RoleDescriptor flsDlsRole = new RoleDescriptor("fls_dls", null, new IndicesPrivileges[] {
                IndicesPrivileges.builder()
                        .indices("*")
                        .privileges("read")
                        .grantedFields("*")
                        .deniedFields("foo")
                        .query(QueryBuilders.matchAllQuery().buildAsBytes())
                        .build()
        }, null);
        RoleDescriptor noFlsDlsRole = new RoleDescriptor("no_fls_dls", null, new IndicesPrivileges[] {
                IndicesPrivileges.builder()
                        .indices("*")
                        .privileges("read")
                        .build()
        }, null);
        FileRolesStore fileRolesStore = mock(FileRolesStore.class);
        when(fileRolesStore.roleDescriptors(Collections.singleton("fls"))).thenReturn(Collections.singleton(flsRole));
        when(fileRolesStore.roleDescriptors(Collections.singleton("dls"))).thenReturn(Collections.singleton(dlsRole));
        when(fileRolesStore.roleDescriptors(Collections.singleton("fls_dls"))).thenReturn(Collections.singleton(flsDlsRole));
        when(fileRolesStore.roleDescriptors(Collections.singleton("no_fls_dls"))).thenReturn(Collections.singleton(noFlsDlsRole));
        CompositeRolesStore compositeRolesStore = new CompositeRolesStore(Settings.EMPTY, fileRolesStore, mock(NativeRolesStore.class),
                mock(ReservedRolesStore.class), Collections.emptyList(), new ThreadContext(Settings.EMPTY), licenseState);

        FieldPermissionsCache fieldPermissionsCache = new FieldPermissionsCache(Settings.EMPTY);
        PlainActionFuture<Role> roleFuture = new PlainActionFuture<>();
        compositeRolesStore.roles(Collections.singleton("fls"), fieldPermissionsCache, roleFuture);
        assertNotEquals(Role.EMPTY, roleFuture.actionGet());

        roleFuture = new PlainActionFuture<>();
        compositeRolesStore.roles(Collections.singleton("dls"), fieldPermissionsCache, roleFuture);
        assertNotEquals(Role.EMPTY, roleFuture.actionGet());

        roleFuture = new PlainActionFuture<>();
        compositeRolesStore.roles(Collections.singleton("fls_dls"), fieldPermissionsCache, roleFuture);
        assertNotEquals(Role.EMPTY, roleFuture.actionGet());

        roleFuture = new PlainActionFuture<>();
        compositeRolesStore.roles(Collections.singleton("no_fls_dls"), fieldPermissionsCache, roleFuture);
        assertNotEquals(Role.EMPTY, roleFuture.actionGet());
    }

    public void testNegativeLookupsAreCached() {
        final FileRolesStore fileRolesStore = mock(FileRolesStore.class);
        when(fileRolesStore.roleDescriptors(anySetOf(String.class))).thenReturn(Collections.emptySet());
        final NativeRolesStore nativeRolesStore = mock(NativeRolesStore.class);
        doAnswer((invocationOnMock) -> {
            ActionListener<Set<RoleDescriptor>> callback = (ActionListener<Set<RoleDescriptor>>) invocationOnMock.getArguments()[1];
            callback.onResponse(Collections.emptySet());
            return null;
        }).when(nativeRolesStore).getRoleDescriptors(isA(String[].class), any(ActionListener.class));
        final ReservedRolesStore reservedRolesStore = spy(new ReservedRolesStore());

        final CompositeRolesStore compositeRolesStore =
                new CompositeRolesStore(Settings.EMPTY, fileRolesStore, nativeRolesStore, reservedRolesStore,
                                        Collections.emptyList(), new ThreadContext(Settings.EMPTY), new XPackLicenseState());
        verify(fileRolesStore).addListener(any(Runnable.class)); // adds a listener in ctor

        final String roleName = randomAsciiOfLengthBetween(1, 10);
        PlainActionFuture<Role> future = new PlainActionFuture<>();
        final FieldPermissionsCache fieldPermissionsCache = new FieldPermissionsCache(Settings.EMPTY);
        compositeRolesStore.roles(Collections.singleton(roleName), fieldPermissionsCache, future);
        final Role role = future.actionGet();
        assertEquals(Role.EMPTY, role);
        verify(reservedRolesStore).roleDescriptors();
        verify(fileRolesStore).roleDescriptors(eq(Collections.singleton(roleName)));
        verify(nativeRolesStore).getRoleDescriptors(isA(String[].class), any(ActionListener.class));

        final int numberOfTimesToCall = scaledRandomIntBetween(0, 32);
        final boolean getSuperuserRole = randomBoolean() && roleName.equals(ReservedRolesStore.SUPERUSER_ROLE.name()) == false;
        final Set<String> names = getSuperuserRole ? Sets.newHashSet(roleName, ReservedRolesStore.SUPERUSER_ROLE.name()) :
                Collections.singleton(roleName);
        for (int i = 0; i < numberOfTimesToCall; i++) {
            future = new PlainActionFuture<>();
            compositeRolesStore.roles(names, fieldPermissionsCache, future);
            future.actionGet();
        }

        if (getSuperuserRole && numberOfTimesToCall > 0) {
            // the superuser role was requested so we get the role descriptors again
            verify(reservedRolesStore, times(2)).roleDescriptors();
        }
        verifyNoMoreInteractions(fileRolesStore, reservedRolesStore, nativeRolesStore);
    }

    public void testCustomRolesProviders() {
        final FileRolesStore fileRolesStore = mock(FileRolesStore.class);
        when(fileRolesStore.roleDescriptors(anySetOf(String.class))).thenReturn(Collections.emptySet());
        final NativeRolesStore nativeRolesStore = mock(NativeRolesStore.class);
        doAnswer((invocationOnMock) -> {
            ActionListener<Set<RoleDescriptor>> callback = (ActionListener<Set<RoleDescriptor>>) invocationOnMock.getArguments()[1];
            callback.onResponse(Collections.emptySet());
            return null;
        }).when(nativeRolesStore).getRoleDescriptors(isA(String[].class), any(ActionListener.class));
        final ReservedRolesStore reservedRolesStore = spy(new ReservedRolesStore());

        final InMemoryRolesProvider inMemoryProvider1 = spy(new InMemoryRolesProvider((roles) -> {
            Set<RoleDescriptor> descriptors = new HashSet<>();
            if (roles.contains("roleA")) {
                descriptors.add(new RoleDescriptor("roleA", null,
                    new IndicesPrivileges[] {
                        IndicesPrivileges.builder().privileges("READ").indices("foo").grantedFields("*").build()
                    }, null));
            }
            return descriptors;
        }));

        final InMemoryRolesProvider inMemoryProvider2 = spy(new InMemoryRolesProvider((roles) -> {
            Set<RoleDescriptor> descriptors = new HashSet<>();
            if (roles.contains("roleA")) {
                // both role providers can resolve role A, this makes sure that if the first
                // role provider in order resolves a role, the second provider does not override it
                descriptors.add(new RoleDescriptor("roleA", null,
                    new IndicesPrivileges[] {
                        IndicesPrivileges.builder().privileges("WRITE").indices("*").grantedFields("*").build()
                    }, null));
            }
            if (roles.contains("roleB")) {
                descriptors.add(new RoleDescriptor("roleB", null,
                    new IndicesPrivileges[] {
                        IndicesPrivileges.builder().privileges("READ").indices("bar").grantedFields("*").build()
                    }, null));
            }
            return descriptors;
        }));

        final CompositeRolesStore compositeRolesStore =
        new CompositeRolesStore(Settings.EMPTY, fileRolesStore, nativeRolesStore, reservedRolesStore,
                                Arrays.asList(inMemoryProvider1, inMemoryProvider2), new ThreadContext(Settings.EMPTY),
                                new XPackLicenseState());

        final Set<String> roleNames = Sets.newHashSet("roleA", "roleB", "unknown");
        PlainActionFuture<Role> future = new PlainActionFuture<>();
        final FieldPermissionsCache fieldPermissionsCache = new FieldPermissionsCache(Settings.EMPTY);
        compositeRolesStore.roles(roleNames, fieldPermissionsCache, future);
        final Role role = future.actionGet();

        // make sure custom roles providers populate roles correctly
        assertEquals(2, role.indices().groups().length);
        assertEquals(IndexPrivilege.READ, role.indices().groups()[0].privilege());
        assertThat(role.indices().groups()[0].indices()[0], anyOf(equalTo("foo"), equalTo("bar")));
        assertEquals(IndexPrivilege.READ, role.indices().groups()[1].privilege());
        assertThat(role.indices().groups()[1].indices()[0], anyOf(equalTo("foo"), equalTo("bar")));

        // make sure negative lookups are cached
        verify(inMemoryProvider1).accept(anySetOf(String.class), any(ActionListener.class));
        verify(inMemoryProvider2).accept(anySetOf(String.class), any(ActionListener.class));

        final int numberOfTimesToCall = scaledRandomIntBetween(1, 8);
        for (int i = 0; i < numberOfTimesToCall; i++) {
            future = new PlainActionFuture<>();
            compositeRolesStore.roles(Collections.singleton("unknown"), fieldPermissionsCache, future);
            future.actionGet();
        }

        verifyNoMoreInteractions(inMemoryProvider1, inMemoryProvider2);
    }

    public void testCustomRolesProviderFailures() throws Exception {
        final FileRolesStore fileRolesStore = mock(FileRolesStore.class);
        when(fileRolesStore.roleDescriptors(anySetOf(String.class))).thenReturn(Collections.emptySet());
        final NativeRolesStore nativeRolesStore = mock(NativeRolesStore.class);
        doAnswer((invocationOnMock) -> {
            ActionListener<Set<RoleDescriptor>> callback = (ActionListener<Set<RoleDescriptor>>) invocationOnMock.getArguments()[1];
            callback.onResponse(Collections.emptySet());
            return null;
        }).when(nativeRolesStore).getRoleDescriptors(isA(String[].class), any(ActionListener.class));
        final ReservedRolesStore reservedRolesStore = new ReservedRolesStore();

        final InMemoryRolesProvider inMemoryProvider1 = new InMemoryRolesProvider((roles) -> {
            Set<RoleDescriptor> descriptors = new HashSet<>();
            if (roles.contains("roleA")) {
                descriptors.add(new RoleDescriptor("roleA", null,
                    new IndicesPrivileges[] {
                        IndicesPrivileges.builder().privileges("READ").indices("foo").grantedFields("*").build()
                    }, null));
            }
            return descriptors;
        });

        final BiConsumer<Set<String>, ActionListener<Set<RoleDescriptor>>> failingProvider =
            (roles, listener) -> listener.onFailure(new Exception("fake failure"));

        final CompositeRolesStore compositeRolesStore =
            new CompositeRolesStore(Settings.EMPTY, fileRolesStore, nativeRolesStore, reservedRolesStore,
                                    Arrays.asList(inMemoryProvider1, failingProvider), new ThreadContext(Settings.EMPTY),
                                    new XPackLicenseState());

        final Set<String> roleNames = Sets.newHashSet("roleA", "roleB", "unknown");
        PlainActionFuture<Role> future = new PlainActionFuture<>();
        final FieldPermissionsCache fieldPermissionsCache = new FieldPermissionsCache(Settings.EMPTY);
        compositeRolesStore.roles(roleNames, fieldPermissionsCache, future);
        try {
            future.get();
            fail("provider should have thrown a failure");
        } catch (ExecutionException e) {
            assertEquals("fake failure", e.getCause().getMessage());
        }
    }

    private static class InMemoryRolesProvider implements BiConsumer<Set<String>, ActionListener<Set<RoleDescriptor>>> {
        private final Function<Set<String>, Set<RoleDescriptor>> roleDescriptorsFunc;

        InMemoryRolesProvider(Function<Set<String>, Set<RoleDescriptor>> roleDescriptorsFunc) {
            this.roleDescriptorsFunc = roleDescriptorsFunc;
        }

        @Override
        public void accept(Set<String> roles, ActionListener<Set<RoleDescriptor>> listener) {
            listener.onResponse(roleDescriptorsFunc.apply(roles));
        }
    }
}
