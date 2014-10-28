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

package org.elasticsearch.license.core;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

public class ESLicense implements Comparable<ESLicense>, ToXContent {

    private final String uid;
    private final String issuer;
    private final String issuedTo;
    private final long issueDate;
    private final String type;
    private final String subscriptionType;
    private final String feature;
    private final String signature;
    private final long expiryDate;
    private final int maxNodes;

    private ESLicense(String uid, String issuer, String issuedTo, long issueDate, String type,
                     String subscriptionType, String feature, String signature, long expiryDate, int maxNodes) {
        this.uid = uid;
        this.issuer = issuer;
        this.issuedTo = issuedTo;
        this.issueDate = issueDate;
        this.type = type;
        this.subscriptionType = subscriptionType;
        this.feature = feature;
        this.signature = signature;
        this.expiryDate = expiryDate;
        this.maxNodes = maxNodes;
    }


    /**
     * @return a unique identifier for a license (currently just a UUID)
     */
    public String uid() {
        return uid;
    }

    /**
     * @return type of the license [trial, subscription, internal]
     */
    public String type() {
        return type;
    }

    /**
     * @return subscription type of the license [none, silver, gold, platinum]
     */
    public String subscriptionType() {
        return subscriptionType;
    }

    /**
     * @return the issueDate in milliseconds
     */
    public long issueDate() {
        return issueDate;
    }

    /**
     * @return the featureType for the license [shield, marvel]
     */
    public String feature() {
        return feature;
    }

    /**
     * @return the expiry date in milliseconds
     */
    public long expiryDate() {
        return expiryDate;
    }

    /**
     * @return the maximum number of nodes this license has been issued for
     */
    public int maxNodes() {
        return maxNodes;
    }

    /**
     * @return a string representing the entity this licenses has been issued to
     */
    public String issuedTo() {
        return issuedTo;
    }

    /**
     * @return a string representing the entity responsible for issuing this license (internal)
     */
    public String issuer() {
        return issuer;
    }

    /**
     * @return a string representing the signature of the license used for license verification
     */
    public String signature() {
        return signature;
    }

    @Override
    public int compareTo(ESLicense o) {
        assert o != null;
        return Long.compare(expiryDate, o.expiryDate);
    }

    static ESLicense readESLicense(StreamInput in) throws IOException {
        in.readVInt(); // Version for future extensibility
        Builder builder = builder();
        builder.uid(in.readString());
        builder.type(in.readString());
        builder.subscriptionType(in.readString());
        builder.issueDate(in.readLong());
        builder.feature(in.readString());
        builder.expiryDate(in.readLong());
        builder.maxNodes(in.readInt());
        builder.issuedTo(in.readString());
        builder.issuer(in.readString());
        builder.signature(in.readOptionalString());
        return builder.verifyAndBuild();
    }

    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(VERSION);
        out.writeString(uid);
        out.writeString(type);
        out.writeString(subscriptionType);
        out.writeLong(issueDate);
        out.writeString(feature);
        out.writeLong(expiryDate);
        out.writeInt(maxNodes);
        out.writeString(issuedTo);
        out.writeString(issuer);
        out.writeOptionalString(signature);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(XFields.UID, uid);
        builder.field(XFields.TYPE, type);
        builder.field(XFields.SUBSCRIPTION_TYPE, subscriptionType);
        builder.field(XFields.ISSUE_DATE, issueDate);
        builder.field(XFields.FEATURE, feature);
        builder.field(XFields.EXPIRY_DATE, expiryDate);
        builder.field(XFields.MAX_NODES, maxNodes);
        builder.field(XFields.ISSUED_TO, issuedTo);
        builder.field(XFields.ISSUER, issuer);
        if (signature != null) {
            builder.field(XFields.SIGNATURE, signature);
        }
        builder.endObject();
        return builder;
    }

    private final static int VERSION = 1;

    final static class Fields {
        static final String UID = "uid";
        static final String TYPE = "type";
        static final String SUBSCRIPTION_TYPE = "subscription_type";
        static final String ISSUE_DATE = "issue_date";
        static final String FEATURE = "feature";
        static final String EXPIRY_DATE = "expiry_date";
        static final String MAX_NODES = "max_nodes";
        static final String ISSUED_TO = "issued_to";
        static final String ISSUER = "issuer";
        static final String SIGNATURE = "signature";
    }

    private final static class XFields {
        static final XContentBuilderString UID = new XContentBuilderString(Fields.UID);
        static final XContentBuilderString TYPE = new XContentBuilderString(Fields.TYPE);
        static final XContentBuilderString SUBSCRIPTION_TYPE = new XContentBuilderString(Fields.SUBSCRIPTION_TYPE);
        static final XContentBuilderString ISSUE_DATE = new XContentBuilderString(Fields.ISSUE_DATE);
        static final XContentBuilderString FEATURE = new XContentBuilderString(Fields.FEATURE);
        static final XContentBuilderString EXPIRY_DATE = new XContentBuilderString(Fields.EXPIRY_DATE);
        static final XContentBuilderString MAX_NODES = new XContentBuilderString(Fields.MAX_NODES);
        static final XContentBuilderString ISSUED_TO = new XContentBuilderString(Fields.ISSUED_TO);
        static final XContentBuilderString ISSUER = new XContentBuilderString(Fields.ISSUER);
        static final XContentBuilderString SIGNATURE = new XContentBuilderString(Fields.SIGNATURE);
    }

    public static ESLicense fromXContent(XContentParser parser) throws IOException {
        Builder builder = new Builder();
        XContentParser.Token token = parser.currentToken();
        if (token == XContentParser.Token.START_OBJECT) {
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    String currentFieldName = parser.currentName();
                    token = parser.nextToken();
                    if (token.isValue()) {
                        if (Fields.UID.equals(currentFieldName)) {
                            builder.uid(parser.text());
                        } else if (Fields.TYPE.equals(currentFieldName)) {
                            builder.type(parser.text());
                        } else if (Fields.SUBSCRIPTION_TYPE.equals(currentFieldName)) {
                            builder.subscriptionType(parser.text());
                        } else if (Fields.ISSUE_DATE.equals(currentFieldName)) {
                            builder.issueDate(parser.longValue());
                        } else if (Fields.FEATURE.equals(currentFieldName)) {
                            builder.feature(parser.text());
                        } else if (Fields.EXPIRY_DATE.equals(currentFieldName)) {
                            builder.expiryDate(parser.longValue());
                        } else if (Fields.MAX_NODES.equals(currentFieldName)) {
                            builder.maxNodes(parser.intValue());
                        } else if (Fields.ISSUED_TO.equals(currentFieldName)) {
                            builder.issuedTo(parser.text());
                        } else if (Fields.ISSUER.equals(currentFieldName)) {
                            builder.issuer(parser.text());
                        } else if (Fields.SIGNATURE.equals(currentFieldName)) {
                            builder.signature(parser.text());
                        }
                        // Ignore unknown elements - might be new version of license
                    } else if (token == XContentParser.Token.START_ARRAY) {
                        // It was probably created by newer version - ignoring
                        parser.skipChildren();
                    } else if (token == XContentParser.Token.START_OBJECT) {
                        // It was probably created by newer version - ignoring
                        parser.skipChildren();
                    }
                }
            }
        } else {
            throw new ElasticsearchParseException("failed to parse licenses expected a license object");
        }
        return builder.verifyAndBuild();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String uid;
        private String issuer;
        private String issuedTo;
        private long issueDate = -1;
        private String type;
        private String subscriptionType = "none";
        private String feature;
        private String signature;
        private long expiryDate = -1;
        private int maxNodes;


        public Builder uid(String uid) {
            this.uid = uid;
            return this;
        }

        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder issuedTo(String issuedTo) {
            this.issuedTo = issuedTo;
            return this;
        }

        public Builder issueDate(long issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder subscriptionType(String subscriptionType) {
            this.subscriptionType = subscriptionType;
            return this;
        }

        public Builder feature(String feature) {
            this.feature = feature;
            return this;
        }

        public Builder expiryDate(long expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder maxNodes(int maxNodes) {
            this.maxNodes = maxNodes;
            return this;
        }

        public Builder signature(String signature) {
            if (signature != null) {
                this.signature = signature;
            }
            return this;
        }

        public Builder fromLicenseSpec(ESLicense license, String signature) {
            return uid(license.uid())
                    .issuedTo(license.issuedTo())
                    .issueDate(license.issueDate())
                    .type(license.type())
                    .subscriptionType(license.subscriptionType())
                    .feature(license.feature())
                    .maxNodes(license.maxNodes())
                    .expiryDate(license.expiryDate())
                    .issuer(license.issuer())
                    .signature(signature);
        }

        public ESLicense verifyAndBuild() {
            verify();
            return new ESLicense(uid, issuer, issuedTo, issueDate, type,
                    subscriptionType, feature, signature, expiryDate, maxNodes);
        }

        public ESLicense build() {
            return new ESLicense(uid, issuer, issuedTo, issueDate, type,
                    subscriptionType, feature, signature, expiryDate, maxNodes);
        }

        private void verify() {
            if (issuer == null) {
               throw new IllegalStateException("issuer can not be null");
            } else if (issuedTo == null) {
               throw new IllegalStateException("issuedTo can not be null");
            } else if (issueDate == -1) {
               throw new IllegalStateException("issueDate has to be set");
            } else if (type == null) {
               throw new IllegalStateException("type can not be null");
            } else if (subscriptionType == null) {
               throw new IllegalStateException("subscriptionType can not be null");
            } else if (uid == null) {
               throw new IllegalStateException("uid can not be null");
            } else if (feature == null) {
               throw new IllegalStateException("at least one feature has to be enabled");
            } else if (signature == null) {
               throw new IllegalStateException("signature can not be null");
            } else if (maxNodes == -1) {
               throw new IllegalStateException("maxNodes has to be set");
            } else if (expiryDate == -1) {
               throw new IllegalStateException("expiryDate has to be set");
            }
        }
    }

}
