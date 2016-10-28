
package org.elasticsearch.xpack.prelert.transforms.date;

import org.elasticsearch.xpack.prelert.transforms.TransformException;

public class ParseTimestampException extends TransformException {

    public ParseTimestampException(String message) {
        super(message);
    }

}
