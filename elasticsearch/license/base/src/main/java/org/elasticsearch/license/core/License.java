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

import org.apache.lucene.util.CollectionUtil;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Data structure for license. Use {@link Builder} to build a license.
 * Provides serialization/deserialization &amp; validation methods for license object
 */
public class License implements ToXContent {
    public final static int VERSION_START = 1;
    public final static int VERSION_NO_FEATURE_TYPE = 2;
    public final static int VERSION_CURRENT = VERSION_NO_FEATURE_TYPE;

    /**
     * XContent param name to deserialize license(s) with
     * an additional <code>status</code> field, indicating whether a
     * particular license is 'active' or 'expired' and no signature
     * and in a human readable format
     */
    public static final String REST_VIEW_MODE = "rest_view";
    /**
     * XContent param name to deserialize license(s) with
     * no signature
     */
    public static final String LICENSE_SPEC_VIEW_MODE = "license_spec_view";
    /**
     * XContent param name to deserialize licenses according
     * to a specific license version
     */
    public static final String LICENSE_VERSION_MODE = "license_version";

    public final static Comparator<License> LATEST_ISSUE_DATE_FIRST = new Comparator<License>() {
        @Override
        public int compare(License right, License left) {
            return Long.compare(left.issueDate(), right.issueDate());
        }
    };

    private final int version;
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
    private final OperationMode operationMode;

    /**
     * Decouples operation mode of a license from the license type value.
     * <p>
     * Note: The mode indicates features that should be made available, but it does not indicate whether the license is active!
     */
    public enum OperationMode {
        MISSING,
        TRIAL,
        BASIC,
        STANDARD,
        GOLD,
        PLATINUM;

        public static OperationMode resolve(String type) {
            switch (type.toLowerCase(Locale.ROOT)) {
                case "missing":
                    return MISSING;
                case "trial":
                case "none": // bwc for 1.x subscription_type field
                case "dev": // bwc for 1.x subscription_type field
                case "development": // bwc for 1.x subscription_type field
                    return TRIAL;
                case "basic":
                    return BASIC;
                case "standard":
                    return STANDARD;
                case "silver":
                case "gold":
                    return GOLD;
                case "platinum":
                case "internal": // bwc for 1.x subscription_type field
                    return PLATINUM;
                default:
                    throw new IllegalArgumentException("unknown type [" + type + "]");
            }
        }
    }

    private License(int version, String uid, String issuer, String issuedTo, long issueDate, String type,
                    String subscriptionType, String feature, String signature, long expiryDate, int maxNodes) {
        this.version = version;
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
        if (version == VERSION_START) {
            // in 1.x: the acceptable values for 'subscription_type': none | dev | silver | gold | platinum
            this.operationMode = OperationMode.resolve(subscriptionType);
        } else {
            // in 2.x: the acceptable values for 'type': trial | basic | silver | dev | gold | platinum
            this.operationMode = OperationMode.resolve(type);
        }
        validate();
    }

    /**
     * @return version of the license
     */
    public int version() {
        return version;
    }

    /**
     * @return a unique identifier for a license
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
     * @return the issueDate in milliseconds
     */
    public long issueDate() {
        return issueDate;
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

    /**
     * @return the operation mode of the license as computed from the license type
     */
    public OperationMode operationMode() {
        return operationMode;
    }

    /**
     * @return the current license's status
     */
    public Status status() {
        long now = System.currentTimeMillis();
        if (issueDate > now) {
            return Status.INVALID;
        } else if (expiryDate < now) {
            return Status.EXPIRED;
        }
        return Status.ACTIVE;
    }

    private void validate() {
        if (issuer == null) {
            throw new IllegalStateException("issuer can not be null");
        } else if (issuedTo == null) {
            throw new IllegalStateException("issuedTo can not be null");
        } else if (issueDate == -1) {
            throw new IllegalStateException("issueDate has to be set");
        } else if (type == null) {
            throw new IllegalStateException("type can not be null");
        } else if (subscriptionType == null && version == VERSION_START) {
            throw new IllegalStateException("subscriptionType can not be null");
        } else if (uid == null) {
            throw new IllegalStateException("uid can not be null");
        } else if (feature == null && version == VERSION_START) {
            throw new IllegalStateException("feature can not be null");
        } else if (maxNodes == -1) {
            throw new IllegalStateException("maxNodes has to be set");
        } else if (expiryDate == -1) {
            throw new IllegalStateException("expiryDate has to be set");
        }
    }

    public static License readLicense(StreamInput in) throws IOException {
        int version = in.readVInt(); // Version for future extensibility
        if (version > VERSION_CURRENT) {
            throw new ElasticsearchException("Unknown license version found, please upgrade all nodes to the latest elasticsearch-license" +
                    " plugin");
        }
        Builder builder = builder();
        builder.version(version);
        builder.uid(in.readString());
        builder.type(in.readString());
        if (version == VERSION_START) {
            builder.subscriptionType(in.readString());
        }
        builder.issueDate(in.readLong());
        if (version == VERSION_START) {
            builder.feature(in.readString());
        }
        builder.expiryDate(in.readLong());
        builder.maxNodes(in.readInt());
        builder.issuedTo(in.readString());
        builder.issuer(in.readString());
        builder.signature(in.readOptionalString());
        return builder.build();
    }

    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(version);
        out.writeString(uid);
        out.writeString(type);
        if (version == VERSION_START) {
            out.writeString(subscriptionType);
        }
        out.writeLong(issueDate);
        if (version == VERSION_START) {
            out.writeString(feature);
        }
        out.writeLong(expiryDate);
        out.writeInt(maxNodes);
        out.writeString(issuedTo);
        out.writeString(issuer);
        out.writeOptionalString(signature);
    }

    @Override
    public String toString() {
        try {
            final XContentBuilder builder = XContentFactory.jsonBuilder();
            toXContent(builder, ToXContent.EMPTY_PARAMS);
            return builder.string();
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        toInnerXContent(builder, params);
        builder.endObject();
        return builder;
    }

    public XContentBuilder toInnerXContent(XContentBuilder builder, Params params) throws IOException {
        boolean licenseSpecMode = params.paramAsBoolean(LICENSE_SPEC_VIEW_MODE, false);
        boolean restViewMode = params.paramAsBoolean(REST_VIEW_MODE, false);
        boolean previouslyHumanReadable = builder.humanReadable();
        if (licenseSpecMode && restViewMode) {
            throw new IllegalArgumentException("can have either " + REST_VIEW_MODE + " or " + LICENSE_SPEC_VIEW_MODE);
        } else if (restViewMode) {
            if (!previouslyHumanReadable) {
                builder.humanReadable(true);
            }
        }
        final int version;
        if (params.param(LICENSE_VERSION_MODE) != null && restViewMode) {
            version = Integer.parseInt(params.param(LICENSE_VERSION_MODE));
        } else {
            version = this.version;
        }
        if (restViewMode) {
            builder.field(Fields.STATUS, status().label());
        }
        builder.field(Fields.UID, uid);
        builder.field(Fields.TYPE, type);
        if (version == VERSION_START) {
            builder.field(Fields.SUBSCRIPTION_TYPE, subscriptionType);
        }
        builder.dateValueField(Fields.ISSUE_DATE_IN_MILLIS, Fields.ISSUE_DATE, issueDate);
        if (version == VERSION_START) {
            builder.field(Fields.FEATURE, feature);
        }
        builder.dateValueField(Fields.EXPIRY_DATE_IN_MILLIS, Fields.EXPIRY_DATE, expiryDate);
        builder.field(Fields.MAX_NODES, maxNodes);
        builder.field(Fields.ISSUED_TO, issuedTo);
        builder.field(Fields.ISSUER, issuer);
        if (!licenseSpecMode && !restViewMode && signature != null) {
            builder.field(Fields.SIGNATURE, signature);
        }
        if (restViewMode) {
            builder.humanReadable(previouslyHumanReadable);
        }
        return builder;
    }

    public static License fromXContent(XContentParser parser) throws IOException {
        Builder builder = new Builder();
        XContentParser.Token token;
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
                        builder.issueDate(parseDate(parser, "issue", false));
                    } else if (Fields.ISSUE_DATE_IN_MILLIS.equals(currentFieldName)) {
                        builder.issueDate(parser.longValue());
                    } else if (Fields.FEATURE.equals(currentFieldName)) {
                        builder.feature(parser.text());
                    } else if (Fields.EXPIRY_DATE.equals(currentFieldName)) {
                        builder.expiryDate(parseDate(parser, "expiration", true));
                    } else if (Fields.EXPIRY_DATE_IN_MILLIS.equals(currentFieldName)) {
                        builder.expiryDate(parser.longValue());
                    } else if (Fields.MAX_NODES.equals(currentFieldName)) {
                        builder.maxNodes(parser.intValue());
                    } else if (Fields.ISSUED_TO.equals(currentFieldName)) {
                        builder.issuedTo(parser.text());
                    } else if (Fields.ISSUER.equals(currentFieldName)) {
                        builder.issuer(parser.text());
                    } else if (Fields.SIGNATURE.equals(currentFieldName)) {
                        builder.signature(parser.text());
                    } else if (Fields.VERSION.equals(currentFieldName)) {
                        builder.version(parser.intValue());
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
        // not a license spec
        if (builder.signature != null) {
            byte[] signatureBytes = Base64.getDecoder().decode(builder.signature);
            ByteBuffer byteBuffer = ByteBuffer.wrap(signatureBytes);
            int version = byteBuffer.getInt();
            // we take the absolute version, because negative versions
            // mean that the license was generated by the cluster (see TrialLicense)
            // and positive version means that the license was signed
            if (version < 0) {
                version *= -1;
            }
            if (version == 0) {
                throw new ElasticsearchException("malformed signature for license [" + builder.uid + "]");
            } else if (version > VERSION_CURRENT) {
                throw new ElasticsearchException("Unknown license version found, please upgrade all nodes to the latest " +
                        "elasticsearch-license plugin");
            }
            // signature version is the source of truth
            builder.version(version);
        }
        return builder.build();
    }

    /**
     * Returns true if the license was auto-generated (by license plugin),
     * false otherwise
     */
    public static boolean isAutoGeneratedLicense(String signature) {
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            ByteBuffer byteBuffer = ByteBuffer.wrap(signatureBytes);
            return byteBuffer.getInt() < 0;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }


    public static License fromSource(String content) throws IOException {
        return fromSource(content.getBytes(StandardCharsets.UTF_8));
    }

    public static License fromSource(byte[] bytes) throws IOException {
        final XContentParser parser = XContentFactory.xContent(bytes).createParser(bytes);
        License license = null;
        if (parser.nextToken() == XContentParser.Token.START_OBJECT) {
            if (parser.nextToken() == XContentParser.Token.FIELD_NAME) {
                String currentFieldName = parser.currentName();
                if (Fields.LICENSES.equals(currentFieldName)) {
                    final List<License> pre20Licenses = new ArrayList<>();
                    if (parser.nextToken() == XContentParser.Token.START_ARRAY) {
                        while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                            pre20Licenses.add(License.fromXContent(parser));
                        }
                        // take the latest issued unexpired license
                        CollectionUtil.timSort(pre20Licenses, LATEST_ISSUE_DATE_FIRST);
                        long now = System.currentTimeMillis();
                        for (License oldLicense : pre20Licenses) {
                            if (oldLicense.expiryDate() > now) {
                                license = oldLicense;
                                break;
                            }
                        }
                        if (license == null && !pre20Licenses.isEmpty()) {
                            license = pre20Licenses.get(0);
                        }
                    } else {
                        throw new ElasticsearchParseException("failed to parse licenses expected an array of licenses");
                    }
                } else if (Fields.LICENSE.equals(currentFieldName)) {
                    license = License.fromXContent(parser);
                }
                // Ignore all other fields - might be created with new version
            } else {
                throw new ElasticsearchParseException("failed to parse licenses expected field");
            }
        } else {
            throw new ElasticsearchParseException("failed to parse licenses expected start object");
        }
        return license;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        License license = (License) o;

        if (issueDate != license.issueDate) return false;
        if (expiryDate != license.expiryDate) return false;
        if (maxNodes != license.maxNodes) return false;
        if (version != license.version) return false;
        if (uid != null ? !uid.equals(license.uid) : license.uid != null) return false;
        if (issuer != null ? !issuer.equals(license.issuer) : license.issuer != null) return false;
        if (issuedTo != null ? !issuedTo.equals(license.issuedTo) : license.issuedTo != null) return false;
        if (type != null ? !type.equals(license.type) : license.type != null) return false;
        if (subscriptionType != null ? !subscriptionType.equals(license.subscriptionType) : license.subscriptionType != null)
            return false;
        if (feature != null ? !feature.equals(license.feature) : license.feature != null) return false;
        return !(signature != null ? !signature.equals(license.signature) : license.signature != null);

    }

    @Override
    public int hashCode() {
        int result = uid != null ? uid.hashCode() : 0;
        result = 31 * result + (issuer != null ? issuer.hashCode() : 0);
        result = 31 * result + (issuedTo != null ? issuedTo.hashCode() : 0);
        result = 31 * result + (int) (issueDate ^ (issueDate >>> 32));
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (subscriptionType != null ? subscriptionType.hashCode() : 0);
        result = 31 * result + (feature != null ? feature.hashCode() : 0);
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        result = 31 * result + (int) (expiryDate ^ (expiryDate >>> 32));
        result = 31 * result + maxNodes;
        result = 31 * result + version;
        return result;
    }

    public final static class Fields {
        public static final String STATUS = "status";
        public static final String UID = "uid";
        public static final String TYPE = "type";
        public static final String SUBSCRIPTION_TYPE = "subscription_type";
        public static final String ISSUE_DATE_IN_MILLIS = "issue_date_in_millis";
        public static final String ISSUE_DATE = "issue_date";
        public static final String FEATURE = "feature";
        public static final String EXPIRY_DATE_IN_MILLIS = "expiry_date_in_millis";
        public static final String EXPIRY_DATE = "expiry_date";
        public static final String MAX_NODES = "max_nodes";
        public static final String ISSUED_TO = "issued_to";
        public static final String ISSUER = "issuer";
        public static final String VERSION = "version";
        public static final String SIGNATURE = "signature";

        public static final String LICENSES = "licenses";
        public static final String LICENSE = "license";

    }

    private static long parseDate(XContentParser parser, String description, boolean endOfTheDay) throws IOException {
        if (parser.currentToken() == XContentParser.Token.VALUE_NUMBER) {
            return parser.longValue();
        } else {
            try {
                if (endOfTheDay) {
                    return DateUtils.endOfTheDay(parser.text());
                } else {
                    return DateUtils.beginningOfTheDay(parser.text());
                }
            } catch (IllegalArgumentException ex) {
                throw new ElasticsearchParseException("invalid " + description + " date format " + parser.text());
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int version = License.VERSION_CURRENT;
        private String uid;
        private String issuer;
        private String issuedTo;
        private long issueDate = -1;
        private String type;
        private String subscriptionType;
        private String feature;
        private String signature;
        private long expiryDate = -1;
        private int maxNodes = -1;

        public Builder uid(String uid) {
            this.uid = uid;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
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

        public Builder fromLicenseSpec(License license, String signature) {
            return uid(license.uid())
                    .version(license.version())
                    .issuedTo(license.issuedTo())
                    .issueDate(license.issueDate())
                    .type(license.type())
                    .subscriptionType(license.subscriptionType)
                    .feature(license.feature)
                    .maxNodes(license.maxNodes())
                    .expiryDate(license.expiryDate())
                    .issuer(license.issuer())
                    .signature(signature);
        }

        /**
         * Returns a builder that converts pre 2.0 licenses
         * to the new license format
         */
        public Builder fromPre20LicenseSpec(License pre20License) {
            return uid(pre20License.uid())
                    .issuedTo(pre20License.issuedTo())
                    .issueDate(pre20License.issueDate())
                    .maxNodes(pre20License.maxNodes())
                    .expiryDate(pre20License.expiryDate());
        }

        public License build() {
            return new License(version, uid, issuer, issuedTo, issueDate, type,
                    subscriptionType, feature, signature, expiryDate, maxNodes);
        }

        public Builder validate() {
            if (issuer == null) {
                throw new IllegalStateException("issuer can not be null");
            } else if (issuedTo == null) {
                throw new IllegalStateException("issuedTo can not be null");
            } else if (issueDate == -1) {
                throw new IllegalStateException("issueDate has to be set");
            } else if (type == null) {
                throw new IllegalStateException("type can not be null");
            } else if (uid == null) {
                throw new IllegalStateException("uid can not be null");
            } else if (signature == null) {
                throw new IllegalStateException("signature can not be null");
            } else if (maxNodes == -1) {
                throw new IllegalStateException("maxNodes has to be set");
            } else if (expiryDate == -1) {
                throw new IllegalStateException("expiryDate has to be set");
            }
            return this;
        }
    }

    public enum Status {

        ACTIVE("active"),
        INVALID("invalid"),
        EXPIRED("expired");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(label);
        }

        public static Status readFrom(StreamInput in) throws IOException {
            String value = in.readString();
            switch (value) {
                case "active":
                    return ACTIVE;
                case "invalid":
                    return INVALID;
                case "expired":
                    return EXPIRED;
                default:
                    throw new IllegalArgumentException("unknown license status [" + value + "]");
            }
        }
    }
}
