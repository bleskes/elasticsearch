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

package org.elasticsearch.xpack.action;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.license.core.License;
import org.elasticsearch.xpack.XPackBuild;
import org.elasticsearch.xpack.XPackFeatureSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 */
public class XPackUsageResponse extends ActionResponse {

    private List<XPackFeatureSet.Usage> usages;

    public XPackUsageResponse() {}

    public XPackUsageResponse(List<XPackFeatureSet.Usage> usages) {
        this.usages = usages;
    }

    public List<XPackFeatureSet.Usage> getUsages() {
        return usages;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(usages.size());
        for (XPackFeatureSet.Usage usage : usages) {
            out.writeNamedWriteable(usage);
        }
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int size = in.readVInt();
        usages = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            usages.add(in.readNamedWriteable(XPackFeatureSet.Usage.class));
        }
    }
}
