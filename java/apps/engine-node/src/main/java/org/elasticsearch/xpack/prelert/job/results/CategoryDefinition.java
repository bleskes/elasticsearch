package org.elasticsearch.xpack.prelert.job.results;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.*;

@JsonInclude(Include.NON_NULL)
public class CategoryDefinition extends ToXContentToBytes implements Writeable, StorageSerialisable {

    public static final ParseField TYPE = new ParseField("categoryDefinition");
    public static final ParseField CATEGORY_ID = new ParseField("categoryId");
    public static final ParseField TERMS = new ParseField("terms");
    public static final ParseField REGEX = new ParseField("regex");
    public static final ParseField MAX_MATCHING_LENGTH = new ParseField("maxMatchingLength");
    public static final ParseField EXAMPLES = new ParseField("examples");

    public static final ObjectParser<CategoryDefinition, ParseFieldMatcherSupplier> PARSER =
            new ObjectParser<>(TYPE.getPreferredName(), CategoryDefinition::new);

    static {
        PARSER.declareLong(CategoryDefinition::setCategoryId, CATEGORY_ID);
        PARSER.declareString(CategoryDefinition::setTerms, TERMS);
        PARSER.declareString(CategoryDefinition::setRegex, REGEX);
        PARSER.declareLong(CategoryDefinition::setMaxMatchingLength, MAX_MATCHING_LENGTH);
        PARSER.declareStringArray(CategoryDefinition::setExamples, EXAMPLES);
    }

    private long id = 0L;
    private String terms = "";
    private String regex = "";
    private long maxMatchingLength = 0L;
    private final Set<String> examples;

    public CategoryDefinition() {
        examples = new TreeSet<>();
    }

    public CategoryDefinition(StreamInput in) throws IOException {
        id = in.readLong();
        terms = in.readString();
        regex = in.readString();
        maxMatchingLength = in.readLong();
        examples = new TreeSet<>(in.readList(StreamInput::readString));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeLong(id);
        out.writeString(terms);
        out.writeString(regex);
        out.writeLong(maxMatchingLength);
        out.writeStringList(new ArrayList<>(examples));
    }

    public long getCategoryId() {
        return id;
    }

    public void setCategoryId(long categoryId) {
        id = categoryId;
    }

    public String getTerms() {
        return terms;
    }

    public void setTerms(String terms) {
        this.terms = terms;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public long getMaxMatchingLength() {
        return maxMatchingLength;
    }

    public void setMaxMatchingLength(long maxMatchingLength) {
        this.maxMatchingLength = maxMatchingLength;
    }

    public List<String> getExamples() {
        return new ArrayList<>(examples);
    }

    public void setExamples(Collection<String> examples) {
        this.examples.clear();
        this.examples.addAll(examples);
    }

    public void addExample(String example) {
        examples.add(example);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(CATEGORY_ID.getPreferredName(), id);
        builder.field(TERMS.getPreferredName(), terms);
        builder.field(REGEX.getPreferredName(), regex);
        builder.field(MAX_MATCHING_LENGTH.getPreferredName(), maxMatchingLength);
        builder.field(EXAMPLES.getPreferredName(), examples);
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof CategoryDefinition == false) {
            return false;
        }
        CategoryDefinition that = (CategoryDefinition) other;
        return Objects.equals(this.id, that.id)
                && Objects.equals(this.terms, that.terms)
                && Objects.equals(this.regex, that.regex)
                && Objects.equals(this.maxMatchingLength, that.maxMatchingLength)
                && Objects.equals(this.examples, that.examples);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, terms, regex, maxMatchingLength, examples);
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException {
        serialiser.add(CATEGORY_ID.getPreferredName(), id)
                .add(TERMS.getPreferredName(), terms)
                .add(REGEX.getPreferredName(), regex)
                .add(MAX_MATCHING_LENGTH.getPreferredName(), maxMatchingLength)
                .add(EXAMPLES.getPreferredName(), examples.toArray(new String[examples.size()]));
    }
}
