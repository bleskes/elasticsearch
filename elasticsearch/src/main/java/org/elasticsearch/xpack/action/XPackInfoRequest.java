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

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Locale;

/**
 *
 */
public class XPackInfoRequest extends ActionRequest<XPackInfoRequest> {

    public enum Category {
        BUILD, LICENSE, FEATURES;

        public static EnumSet<Category> toSet(String... categories) {
            EnumSet<Category> set = EnumSet.noneOf(Category.class);
            for (String category : categories) {
                switch (category) {
                    case "_all":
                        return EnumSet.allOf(Category.class);
                    case "_none":
                        return EnumSet.noneOf(Category.class);
                    default:
                        set.add(Category.valueOf(category.toUpperCase(Locale.ROOT)));
                }
            }
            return set;
        }
    }

    private boolean verbose;
    private EnumSet<Category> categories = EnumSet.noneOf(Category.class);

    public XPackInfoRequest() {}

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setCategories(EnumSet<Category> categories) {
        this.categories = categories;
    }

    public EnumSet<Category> getCategories() {
        return categories;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        this.verbose = in.readBoolean();
        EnumSet<Category> categories = EnumSet.noneOf(Category.class);
        int size = in.readVInt();
        for (int i = 0; i < size; i++) {
            categories.add(Category.valueOf(in.readString()));
        }
        this.categories = categories;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeBoolean(verbose);
        out.writeVInt(categories.size());
        for (Category category : categories) {
            out.writeString(category.name());
        }
    }
}
