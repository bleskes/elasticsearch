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

package org.elasticsearch.shield.authc.active_directory;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.shield.authc.ldap.LdapException;
import org.elasticsearch.shield.authc.support.ldap.AbstractLdapConnection;
import org.elasticsearch.shield.authc.support.ldap.ClosableNamingEnumeration;
import org.elasticsearch.shield.authc.support.ldap.SearchScope;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.List;

/**
 *
 */
public class ActiveDirectoryGroupsResolver implements AbstractLdapConnection.GroupsResolver {

    private final ESLogger logger;
    private final String baseDn;

    public ActiveDirectoryGroupsResolver(ESLogger logger, String baseDn) {
        this.logger = logger;
        this.baseDn = baseDn;
    }

    public List<String> resolve(DirContext ctx, String userDn, TimeValue timeout) {

        String groupsSearchFilter = buildGroupQuery(ctx, userDn, timeout);
        logger.debug("group SID to DN search filter: [{}]", groupsSearchFilter);

        // Search for groups the user belongs to in order to get their names
        //Create the search controls
        SearchControls groupsSearchCtls = new SearchControls();

        //Specify the search scope
        groupsSearchCtls.setSearchScope(SearchScope.SUB_TREE.scope());
        groupsSearchCtls.setReturningAttributes(Strings.EMPTY_ARRAY);  //we only need the entry DN
        groupsSearchCtls.setTimeLimit((int) timeout.millis());

        ImmutableList.Builder<String> groups = ImmutableList.builder();
        try (ClosableNamingEnumeration<SearchResult> groupsAnswer = new ClosableNamingEnumeration<>(
                ctx.search(baseDn, groupsSearchFilter, groupsSearchCtls))) {

            //Loop through the search results
            while (groupsAnswer.hasMoreElements()) {
                SearchResult sr = groupsAnswer.next();
                groups.add(sr.getNameInNamespace());
            }
        } catch (NamingException | LdapException ne) {
            throw new ActiveDirectoryException("failed to fetch AD groups", userDn, ne);
        }
        List<String> groupList = groups.build();
        if (logger.isDebugEnabled()) {
            logger.debug("found these groups [{}] for userDN [{}]", groupList, userDn);
        }
        return groupList;

    }

    static String buildGroupQuery(DirContext ctx, String userDn, TimeValue timeout) {
        StringBuilder groupsSearchFilter = new StringBuilder("(|");
        try {
            SearchControls userSearchCtls = new SearchControls();

            userSearchCtls.setSearchScope(SearchControls.OBJECT_SCOPE);
            userSearchCtls.setTimeLimit((int) timeout.millis());

            userSearchCtls.setReturningAttributes(new String[] { "tokenGroups" });
            try (ClosableNamingEnumeration<SearchResult> userAnswer = new ClosableNamingEnumeration<>(
                    ctx.search(userDn, "(objectClass=user)", userSearchCtls))) {

                while (userAnswer.hasMoreElements()) {

                    SearchResult sr = userAnswer.next();
                    Attributes attrs = sr.getAttributes();

                    if (attrs != null) {
                        try (ClosableNamingEnumeration<? extends Attribute> ae = new ClosableNamingEnumeration<>(attrs.getAll())) {
                            while (ae.hasMore() ) {
                                Attribute attr = ae.next();
                                for (NamingEnumeration e = attr.getAll(); e.hasMore(); ) {
                                    byte[] sid = (byte[]) e.next();
                                    groupsSearchFilter.append("(objectSid=");
                                    groupsSearchFilter.append(binarySidToStringSid(sid));
                                    groupsSearchFilter.append(")");
                                }
                                groupsSearchFilter.append(")");
                            }
                        }
                    }
                }
            }

        } catch (NamingException | LdapException ne) {
            throw new ActiveDirectoryException("failed to fetch AD groups", userDn, ne);
        }
        return groupsSearchFilter.toString();
    }

    /**
     * To better understand what the sid is and how its string representation looks like, see
     * http://blogs.msdn.com/b/alextch/archive/2007/06/18/sample-java-application-that-retrieves-group-membership-of-an-active-directory-user-account.aspx
     *
     * @param SID byte encoded security ID
     */
    static public String binarySidToStringSid(byte[] SID) {
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
