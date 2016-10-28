
package org.elasticsearch.xpack.prelert.transforms;

public abstract class TransformException extends Exception {

    public TransformException(String message) {
        super(message);
    }
}
