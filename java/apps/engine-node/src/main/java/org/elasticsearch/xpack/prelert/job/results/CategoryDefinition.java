
package org.elasticsearch.xpack.prelert.job.results;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.*;

@JsonInclude(Include.NON_NULL)
public class CategoryDefinition implements StorageSerialisable
{
    public static final String TYPE = "categoryDefinition";
    public static final String CATEGORY_ID = "categoryId";
    public static final String TERMS = "terms";
    public static final String REGEX = "regex";
    public static final String MAX_MATCHING_LENGTH = "maxMatchingLength";
    public static final String EXAMPLES = "examples";

    private long id = 0L;
    private String terms = "";
    private String regex = "";
    private long maxMatchingLength = 0L;
    private final Set<String> examples = new TreeSet<>();

    public long getCategoryId()
    {
        return id;
    }

    public void setCategoryId(long categoryId)
    {
        id = categoryId;
    }

    public String getTerms()
    {
        return terms;
    }

    public void setTerms(String terms)
    {
        this.terms = terms;
    }

    public String getRegex()
    {
        return regex;
    }

    public void setRegex(String regex)
    {
        this.regex = regex;
    }

    public long getMaxMatchingLength()
    {
        return maxMatchingLength;
    }

    public void setMaxMatchingLength(long maxMatchingLength)
    {
        this.maxMatchingLength = maxMatchingLength;
    }

    public List<String> getExamples()
    {
        return new ArrayList<>(examples);
    }

    public void setExamples(Collection<String> examples)
    {
        this.examples.clear();
        this.examples.addAll(examples);
    }

    public void addExample(String example)
    {
        examples.add(example);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (other instanceof CategoryDefinition == false)
        {
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
    public int hashCode()
    {
        return Objects.hash(id, terms, regex, maxMatchingLength, examples);
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.add(CATEGORY_ID, id)
                  .add(TERMS, terms)
                  .add(REGEX, regex)
                  .add(MAX_MATCHING_LENGTH, maxMatchingLength)
                  .add(EXAMPLES, examples.toArray(new String[examples.size()]));
    }
}
