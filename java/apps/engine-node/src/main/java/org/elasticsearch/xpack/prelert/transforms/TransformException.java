
package org.elasticsearch.xpack.prelert.transforms;

public abstract class TransformException extends Exception {
    private static final long serialVersionUID = -4162269539818180916L;

    public TransformException(String message) {
        super(message);
    }
}
