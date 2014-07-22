package org.elasticsearch.shield.authc.ldap;

/**
 * LdapExceptions typically wrap jndi Naming exceptions, and have an additional
 * parameter of DN attached to each message.
 */
public class LdapException extends SecurityException {
    public LdapException(String msg){
        super(msg);
    }

    public LdapException(String msg, Throwable cause){
        super(msg, cause);
    }
    public LdapException(String msg, String dn) {
        this(msg, dn, null);
    }

    public LdapException(String msg, String dn, Throwable cause) {
        super( msg + "; LDAP DN=[" + dn + "]", cause);
    }
}
