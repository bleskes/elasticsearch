/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.upgrade;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;

import java.io.IOException;
import java.util.Locale;

/**
 * Indicates the type of the upgrade required for the index
 */
public enum UpgradeActionRequired implements Writeable {
    NOT_APPLICABLE,   // Indicates that the check is not applicable to this index type, the next check will be performed
    UP_TO_DATE,       // Indicates that the check finds this index to be up to date - no additional checks are required
    REINDEX,          // The index should be reindex
    UPGRADE;          // The index should go through the upgrade procedure

    public static UpgradeActionRequired fromString(String value) {
        return UpgradeActionRequired.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public static UpgradeActionRequired readFromStream(StreamInput in) throws IOException {
        return in.readEnum(UpgradeActionRequired.class);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeEnum(this);
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }

}
