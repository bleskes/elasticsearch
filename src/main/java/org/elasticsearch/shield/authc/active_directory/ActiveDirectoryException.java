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

/**
 * ActiveDirectoryExceptions typically wrap jndi Naming exceptions, and have an additional
 * parameter of DN attached to each message.
 */
public class ActiveDirectoryException extends SecurityException {

    public ActiveDirectoryException(String msg){
        super(msg);
    }

    public ActiveDirectoryException(String msg, Throwable cause){
        super(msg, cause);
    }

    public ActiveDirectoryException(String msg, String dn) {
        this(msg, dn, null);
    }

    public ActiveDirectoryException(String msg, String dn, Throwable cause) {
        super( msg + "; DN=[" + dn + "]", cause);
    }
}
