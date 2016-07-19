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

package org.elasticsearch.xpack.security.authc.activedirectory;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.security.authc.ldap.support.LdapSearchScope;
import org.elasticsearch.xpack.security.authc.ldap.support.LdapSession.GroupsResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.xpack.security.authc.ldap.support.LdapUtils.OBJECT_CLASS_PRESENCE_FILTER;
import static org.elasticsearch.xpack.security.authc.ldap.support.LdapUtils.search;
import static org.elasticsearch.xpack.security.authc.ldap.support.LdapUtils.searchForEntry;

/**
 *
 */
public class ActiveDirectoryGroupsResolver implements GroupsResolver {

    private final String baseDn;
    private final LdapSearchScope scope;

    public ActiveDirectoryGroupsResolver(Settings settings, String baseDnDefault) {
        this.baseDn = settings.get("base_dn", baseDnDefault);
        this.scope = LdapSearchScope.resolve(settings.get("scope"), LdapSearchScope.SUB_TREE);
    }

    @Override
    public List<String> resolve(LDAPInterface connection, String userDn, TimeValue timeout, ESLogger logger,
                                Collection<Attribute> attributes) {
        Filter groupSearchFilter = buildGroupQuery(connection, userDn, timeout, logger);
        logger.debug("group SID to DN search filter: [{}]", groupSearchFilter);
        if (groupSearchFilter == null) {
            return Collections.emptyList();
        }

        SearchRequest searchRequest = new SearchRequest(baseDn, scope.scope(), groupSearchFilter, SearchRequest.NO_ATTRIBUTES);
        searchRequest.setTimeLimitSeconds(Math.toIntExact(timeout.seconds()));
        SearchResult results;
        try {
            results = search(connection, searchRequest, logger);
        } catch (LDAPException e) {
            logger.error("failed to fetch AD groups for DN [{}]", e, userDn);
            return Collections.emptyList();
        }

        List<String> groupList = new ArrayList<>();
        for (SearchResultEntry entry : results.getSearchEntries()) {
            groupList.add(entry.getDN());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("found these groups [{}] for userDN [{}]", groupList, userDn);
        }
        return groupList;
    }

    @Override
    public String[] attributes() {
        // we have to return null since the tokenGroups attribute is computed and can only be retrieved using a BASE level search
        return null;
    }

    static Filter buildGroupQuery(LDAPInterface connection, String userDn, TimeValue timeout, ESLogger logger) {
        try {
            SearchRequest request = new SearchRequest(userDn, SearchScope.BASE, OBJECT_CLASS_PRESENCE_FILTER, "tokenGroups");
            request.setTimeLimitSeconds(Math.toIntExact(timeout.seconds()));
            SearchResultEntry entry = searchForEntry(connection, request, logger);
            if (entry == null) {
                return null;
            }
            Attribute attribute = entry.getAttribute("tokenGroups");
            byte[][] tokenGroupSIDBytes = attribute.getValueByteArrays();
            List<Filter> orFilters = new ArrayList<>(tokenGroupSIDBytes.length);
            for (byte[] SID : tokenGroupSIDBytes) {
                orFilters.add(Filter.createEqualityFilter("objectSid", binarySidToStringSid(SID)));
            }
            return Filter.createORFilter(orFilters);
        } catch (LDAPException e) {
            logger.error("failed to fetch AD groups for DN [{}]", e, userDn);
            return null;
        }
    }

    /**
     * To better understand what the sid is and how its string representation looks like, see
     * http://blogs.msdn.com/b/alextch/archive/2007/06/18/sample-java-application-that-retrieves-group-membership-of-an-active-directory
     * -user-account.aspx
     *
     * @param SID byte encoded security ID
     */
    public static String binarySidToStringSid(byte[] SID) {
        String strSID;

        //convert the SID into string format

        long version;
        long authority;
        long count;
        long rid;

        strSID = "S";
        version = SID[0];
        strSID = strSID + "-" + Long.toString(version);
        authority = SID[4];

        for (int i = 0; i < 4; i++) {
            authority <<= 8;
            authority += SID[4 + i] & 0xFF;
        }

        strSID = strSID + "-" + Long.toString(authority);
        count = SID[2];
        count <<= 8;
        count += SID[1] & 0xFF;
        for (int j = 0; j < count; j++) {
            rid = SID[11 + (j * 4)] & 0xFF;
            for (int k = 1; k < 4; k++) {
                rid <<= 8;
                rid += SID[11 - k + (j * 4)] & 0xFF;
            }
            strSID = strSID + "-" + Long.toString(rid);
        }
        return strSID;
    }

}
