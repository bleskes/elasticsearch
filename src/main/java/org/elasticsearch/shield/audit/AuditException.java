package org.elasticsearch.shield.audit;

/**
 *
 */
public class AuditException extends org.elasticsearch.shield.SecurityException {

    public AuditException(String msg) {
        super(msg);
    }

    public AuditException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
