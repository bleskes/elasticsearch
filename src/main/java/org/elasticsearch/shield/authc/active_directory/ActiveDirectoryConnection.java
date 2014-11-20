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
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.shield.authc.support.ldap.AbstractLdapConnection;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.List;

/**
 * An Ldap Connection customized for active directory.
 */
public class ActiveDirectoryConnection extends AbstractLdapConnection {
    private static final ESLogger logger = ESLoggerFactory.getLogger(ActiveDirectoryConnection.class.getName());

    private final String groupSearchDN;

    /**
     * This object is intended to be constructed by the LdapConnectionFactory
     */
    ActiveDirectoryConnection(DirContext ctx, String boundName, String groupSearchDN) {
        super(ctx, boundName);
        this.groupSearchDN = groupSearchDN;
    }

    /**
     * LDAP connections should be closed to clean up resources.  However, the jndi contexts have the finalize
     * implemented properly so that it will clean up on garbage collection.
     */
    @Override
    public void close(){
        try {
            jndiContext.close();
        } catch (NamingException e) {
            throw new ActiveDirectoryException("Could not close the LDAP connection", e);
        }
    }

    @Override
    public List<String> groups() {

        String groupsSearchFilter = buildGroupQuery();
        logger.debug("group SID to DN search filter: [{}]", groupsSearchFilter);

        // Search for groups the user belongs to in order to get their names
        //Create the search controls
        SearchControls groupsSearchCtls = new SearchControls();

        //Specify the search scope
        groupsSearchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        groupsSearchCtls.setReturningAttributes(Strings.EMPTY_ARRAY);  //we only need the entry DN

        ImmutableList.Builder<String> groups = ImmutableList.builder();
        try {
            //Search for objects using the filter
            NamingEnumeration groupsAnswer = jndiContext.search(groupSearchDN, groupsSearchFilter, groupsSearchCtls);

            //Loop through the search results
            while (groupsAnswer.hasMoreElements()) {
                SearchResult sr = (SearchResult) groupsAnswer.next();
                groups.add(sr.getNameInNamespace());
            }
        } catch (NamingException ne) {
            throw new ActiveDirectoryException("Exception occurred fetching AD groups", bindDn, ne);
        }
        List<String> groupList = groups.build();
        if (logger.isDebugEnabled()) {
            logger.debug("Found these groups [{}] for userDN [{}]", groupList, this.bindDn);
        }
        return groupList;
    }

    private String buildGroupQuery() {
        StringBuilder groupsSearchFilter = new StringBuilder("(|");
        try {
            SearchControls userSearchCtls = new SearchControls();
            userSearchCtls.setSearchScope(SearchControls.OBJECT_SCOPE);

            //specify the LDAP search filter to find the user in question
            String userSearchFilter = "(objectClass=user)";
            String userReturnedAtts[] = { "tokenGroups" };
            userSearchCtls.setReturningAttributes(userReturnedAtts);
            NamingEnumeration userAnswer = jndiContext.search(authenticatedUserDn(), userSearchFilter, userSearchCtls);

            //Loop through the search results
            while (userAnswer.hasMoreElements()) {

                SearchResult sr = (SearchResult) userAnswer.next();
                Attributes attrs = sr.getAttributes();

                if (attrs != null) {
                    for (NamingEnumeration ae = attrs.getAll(); ae.hasMore();) {
                        Attribute attr = (Attribute) ae.next();
                        for (NamingEnumeration e = attr.getAll(); e.hasMore();) {
                            byte[] sid = (byte[]) e.next();
                            groupsSearchFilter.append("(objectSid=");
                            groupsSearchFilter.append(binarySidToStringSid(sid));
                            groupsSearchFilter.append(")");
                        }
                        groupsSearchFilter.append(")");
                    }
                }
            }

        } catch (NamingException ne) {
            throw new ActiveDirectoryException("Exception occurred fetching AD groups", bindDn, ne);
        }
        return groupsSearchFilter.toString();
    }

    @Override
    public String authenticatedUserDn() {
        return bindDn;
    }

    /**
     * To better understand what the sid is and how its string representation looks like, see
     * http://blogs.msdn.com/b/alextch/archive/2007/06/18/sample-java-application-that-retrieves-group-membership-of-an-active-directory-user-account.aspx
     * @param SID byte encoded security ID
     */
    static public String binarySidToStringSid( byte[] SID ) {
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

        for (int i = 0;i<4;i++) {
            authority <<= 8;
            authority += SID[4+i] & 0xFF;
        }

        strSID = strSID + "-" + Long.toString(authority);
        count = SID[2];
        count <<= 8;
        count += SID[1] & 0xFF;
        for (int j=0;j<count;j++) {
            rid = SID[11 + (j*4)] & 0xFF;
            for (int k=1;k<4;k++) {
                rid <<= 8;
                rid += SID[11-k + (j*4)] & 0xFF;
            }
            strSID = strSID + "-" + Long.toString(rid);
        }
        return strSID;
    }
}
