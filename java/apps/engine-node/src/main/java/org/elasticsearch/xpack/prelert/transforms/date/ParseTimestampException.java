
package org.elasticsearch.xpack.prelert.transforms.date;

import org.elasticsearch.xpack.prelert.transforms.TransformException;

public class ParseTimestampException extends TransformException {
    private static final long serialVersionUID = -184672266466521404L;

    public ParseTimestampException(String message) {
        super(message);
    }

}
