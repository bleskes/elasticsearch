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

package org.elasticsearch.xpack.security.authc.ldap.support;

import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import org.elasticsearch.common.logging.ESLogger;

import javax.naming.ldap.Rdn;
import java.text.MessageFormat;
import java.util.Locale;

public final class LdapUtils {

    public static final Filter OBJECT_CLASS_PRESENCE_FILTER = Filter.createPresenceFilter("objectClass");

    private LdapUtils() {
    }

    public static DN dn(String dn) {
        try {
            return new DN(dn);
        } catch (LDAPException e) {
            throw new IllegalArgumentException("invalid DN [" + dn + "]", e);
        }
    }

    public static String relativeName(DN dn) {
        return dn.getRDNString().split("=")[1].trim();
    }

    public static String escapedRDNValue(String rdn) {
        // We can't use UnboundID RDN here because it expects attribute=value, not just value
        return Rdn.escapeValue(rdn);
    }

    /**
     * This method performs a LDAPConnection.search(...) operation while handling referral exceptions. This is necessary
     * to maintain backwards compatibility with the original JNDI implementation
     */
    public static SearchResult search(LDAPInterface ldap, SearchRequest searchRequest, ESLogger logger) throws LDAPException {
        SearchResult results;
        try {
            results = ldap.search(searchRequest);
        } catch (LDAPSearchException e) {
            if (e.getResultCode().equals(ResultCode.REFERRAL) && e.getSearchResult() != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("a referral could not be followed for request [{}] so some results may not have been retrieved", e,
                            searchRequest);
                }
                results = e.getSearchResult();
            } else {
                throw e;
            }
        }
        return results;
    }

    /**
     * This method performs a LDAPConnection.searchForEntry(...) operation while handling referral exceptions. This is necessary
     * to maintain backwards compatibility with the original JNDI implementation
     */
    public static SearchResultEntry searchForEntry(LDAPInterface ldap, SearchRequest searchRequest, ESLogger logger) throws LDAPException {
        SearchResultEntry entry;
        try {
            entry = ldap.searchForEntry(searchRequest);
        } catch (LDAPSearchException e) {
            if (e.getResultCode().equals(ResultCode.REFERRAL) && e.getSearchResult() != null && e.getSearchResult().getEntryCount() > 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("a referral could not be followed for request [{}] so some results may not have been retrieved", e,
                            searchRequest);
                }
                entry = e.getSearchResult().getSearchEntries().get(0);
            } else {
                throw e;
            }
        }
        return entry;
    }

    public static Filter createFilter(String filterTemplate, String... arguments) throws LDAPException {
        return Filter.create(new MessageFormat(filterTemplate, Locale.ROOT).format((Object[]) encodeFilterValues(arguments),
                new StringBuffer(), null).toString());
    }

    public static String[] attributesToSearchFor(String[] attributes) {
        return attributes == null ? new String[] { SearchRequest.NO_ATTRIBUTES } : attributes;
    }

    static String[] encodeFilterValues(String... arguments) {
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = Filter.encodeValue(arguments[i]);
        }
        return arguments;
    }
}
