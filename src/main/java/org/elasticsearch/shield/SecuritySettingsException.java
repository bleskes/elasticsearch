package org.elasticsearch.shield;

/**
 *
 */
public class SecuritySettingsException extends SecurityException {

    public SecuritySettingsException(String msg) {
        super(msg);
    }

    public SecuritySettingsException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
