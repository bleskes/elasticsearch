package org.elasticsearch.shield.authc;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.shield.authc.esusers.ESUsersRealm;
import org.elasticsearch.shield.authc.ldap.LdapRealm;

import java.util.ArrayList;
import java.util.List;

/**
 * Serves as a realms registry (also responsible for ordering the realms appropriately)
 */
public class Realms {

    private final Realm[] realms;

    @Inject
    public Realms(@Nullable ESUsersRealm esusers, @Nullable LdapRealm ldap) {
        List<Realm> realms = new ArrayList<>();
        if (esusers != null) {
            realms.add(esusers);
        }
        if (ldap != null) {
            realms.add(ldap);
        }
        this.realms = realms.toArray(new Realm[realms.size()]);
    }

    Realm[] realms() {
        return realms;
    }

}
