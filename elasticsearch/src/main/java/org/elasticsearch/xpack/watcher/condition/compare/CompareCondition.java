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

package org.elasticsearch.xpack.watcher.condition.compare;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.condition.Condition;
import org.elasticsearch.xpack.common.xcontent.XContentUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 *
 */
public class CompareCondition implements Condition {

    public static final String TYPE = "compare";

    private String path;
    private Op op;
    private Object value;

    public CompareCondition(String path, Op op, Object value) {
        this.path = path;
        this.op = op;
        this.value = value;
    }

    @Override
    public final String type() {
        return TYPE;
    }

    public String getPath() {
        return path;
    }

    public Op getOp() {
        return op;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompareCondition condition = (CompareCondition) o;

        if (!path.equals(condition.path)) return false;
        if (op != condition.op) return false;
        return !(value != null ? !value.equals(condition.value) : condition.value != null);
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + op.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
                .startObject(path)
                    .field(op.id(), value)
                .endObject()
            .endObject();
    }

    public static CompareCondition parse(String watchId, XContentParser parser) throws IOException {
        if (parser.currentToken() != XContentParser.Token.START_OBJECT) {
            throw new ElasticsearchParseException("could not parse [{}] condition for watch [{}]. expected an object but found [{}] " +
                    "instead", TYPE, watchId, parser.currentToken());
        }
        String path = null;
        Object value = null;
        Op op = null;

        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                path = parser.currentName();
            } else if (path == null) {
                throw new ElasticsearchParseException("could not parse [{}] condition for watch [{}]. expected a field indicating the " +
                        "compared path, but found [{}] instead", TYPE, watchId, token);
            } else if (token == XContentParser.Token.START_OBJECT) {
                token = parser.nextToken();
                if (token != XContentParser.Token.FIELD_NAME) {
                    throw new ElasticsearchParseException("could not parse [{}] condition for watch [{}]. expected a field indicating the" +
                            " comparison operator, but found [{}] instead", TYPE, watchId, token);
                }
                try {
                    op = Op.resolve(parser.currentName());
                } catch (IllegalArgumentException iae) {
                    throw new ElasticsearchParseException("could not parse [{}] condition for watch [{}]. unknown comparison operator " +
                            "[{}]", TYPE, watchId, parser.currentName());
                }
                token = parser.nextToken();
                if (!op.supportsStructures() && !token.isValue() && token != XContentParser.Token.VALUE_NULL) {
                    throw new ElasticsearchParseException("could not parse [{}] condition for watch [{}]. compared value for [{}] with " +
                            "operation [{}] must either be a numeric, string, boolean or null value, but found [{}] instead", TYPE,
                            watchId, path, op.name().toLowerCase(Locale.ROOT), token);
                }
                value = XContentUtils.readValue(parser, token);
                token = parser.nextToken();
                if (token != XContentParser.Token.END_OBJECT) {
                    throw new ElasticsearchParseException("could not parse [{}] condition for watch [{}]. expected end of path object, " +
                            "but found [{}] instead", TYPE, watchId, token);
                }
            } else {
                throw new ElasticsearchParseException("could not parse [{}] condition for watch [{}]. expected an object for field [{}] " +
                        "but found [{}] instead", TYPE, watchId, path, token);
            }
        }

        return new CompareCondition(path, op, value);
    }

    public static class Result extends Condition.Result {

        @Nullable private final Map<String, Object> resolveValues;

        Result(Map<String, Object> resolveValues, boolean met) {
            super(TYPE, met);
            this.resolveValues = resolveValues;
        }

        public Map<String, Object> getResolveValues() {
            return resolveValues;
        }

        @Override
        protected XContentBuilder typeXContent(XContentBuilder builder, Params params) throws IOException {
            if (resolveValues == null) {
                return builder;
            }
            return builder.startObject(type)
                    .field(Field.RESOLVED_VALUES.getPreferredName(), resolveValues)
                    .endObject();
        }
    }

    public enum Op {

        EQ() {
            @Override
            public boolean eval(Object v1, Object v2) {
                Integer compVal = LenientCompare.compare(v1, v2);
                return compVal != null && compVal == 0;
            }

            @Override
            public boolean supportsStructures() {
                return true;
            }
        },
        NOT_EQ() {
            @Override
            public boolean eval(Object v1, Object v2) {
                Integer compVal = LenientCompare.compare(v1, v2);
                return compVal == null || compVal != 0;
            }

            @Override
            public boolean supportsStructures() {
                return true;
            }
        },
        LT() {
            @Override
            public boolean eval(Object v1, Object v2) {
                Integer compVal = LenientCompare.compare(v1, v2);
                return compVal != null && compVal < 0;
            }
        },
        LTE() {
            @Override
            public boolean eval(Object v1, Object v2) {
                Integer compVal = LenientCompare.compare(v1, v2);
                return compVal != null && compVal <= 0;
            }
        },
        GT() {
            @Override
            public boolean eval(Object v1, Object v2) {
                Integer compVal = LenientCompare.compare(v1, v2);
                return compVal != null && compVal > 0;
            }
        },
        GTE() {
            @Override
            public boolean eval(Object v1, Object v2) {
                Integer compVal = LenientCompare.compare(v1, v2);
                return compVal != null && compVal >= 0;
            }
        };

        public abstract boolean eval(Object v1, Object v2);

        public boolean supportsStructures() {
            return false;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Op resolve(String id) {
            return Op.valueOf(id.toUpperCase(Locale.ROOT));
        }
    }

    interface Field extends Condition.Field {
        ParseField RESOLVED_VALUES = new ParseField("resolved_values");
    }
}
