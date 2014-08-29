package org.elasticsearch.shield.authc.ldap;

/**
 * This factory holds settings needed for authenticating to LDAP and creating LdapConnections.
 * Each created LdapConnection needs to be closed or else connections will pill up consuming resources.
 *
 * A standard looking usage pattern could look like this:
 <pre>
    try (LdapConnection session = ldapFac.bindXXX(...);
        ...do stuff with the session
    }
 </pre>
 */
public interface LdapConnectionFactory {

    static final String URLS_SETTING = "urls"; //comma separated

    /**
     * Password authenticated bind
     * @param user name of the user to authenticate the connection with.
     */
    LdapConnection bind(String user, char[] password) ;

}
