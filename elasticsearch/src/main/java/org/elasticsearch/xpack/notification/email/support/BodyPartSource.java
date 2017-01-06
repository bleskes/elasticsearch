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

package org.elasticsearch.xpack.notification.email.support;

import org.elasticsearch.SpecialPermission;
import org.elasticsearch.common.xcontent.ToXContentObject;

import javax.activation.FileTypeMap;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import java.security.AccessController;
import java.security.PrivilegedAction;

public abstract class BodyPartSource implements ToXContentObject {

    protected static FileTypeMap fileTypeMap;
    static {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SpecialPermission());
        }
        fileTypeMap = AccessController.doPrivileged(
            (PrivilegedAction<FileTypeMap>)() -> FileTypeMap.getDefaultFileTypeMap());
    }

    protected final String id;
    protected final String name;
    protected final String contentType;

    public BodyPartSource(String id, String contentType) {
        this(id, id, contentType);
    }

    public BodyPartSource(String id, String name, String contentType) {
        this.id = id;
        this.name = name;
        this.contentType = contentType;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String contentType() {
        return contentType;
    }

    public abstract MimeBodyPart bodyPart() throws MessagingException;

    // exists only to allow ensuring class is initialized
    public static void init() {}

}
