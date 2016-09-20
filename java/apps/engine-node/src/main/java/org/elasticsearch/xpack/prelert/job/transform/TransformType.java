/****************************************************************************
 *                                                                          *
 * Copyright 2015-2016 Prelert Ltd                                          *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 *                                                                          *
 ***************************************************************************/

package org.elasticsearch.xpack.prelert.job.transform;

import com.google.common.collect.Range;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Enum type representing the different transform functions
 * with functions for converting between the enum and its
 * pretty name i.e. human readable string.
 */
public enum TransformType {
    // Name, arity, arguments, outputs, default output names, has condition
    DOMAIN_SPLIT(Names.DOMAIN_SPLIT_NAME, Range.singleton(1), Range.singleton(0),
            Range.closed(1, 2), Arrays.asList("subDomain", "hrd")),
    CONCAT(Names.CONCAT_NAME, Range.atLeast(2), Range.closed(0, 1), Range.singleton(1),
            Arrays.asList("concat")),
    REGEX_EXTRACT(Names.EXTRACT_NAME, Range.singleton(1), Range.singleton(1), Range.atLeast(1),
            Arrays.asList("extract"), false),
    REGEX_SPLIT(Names.SPLIT_NAME, Range.singleton(1), Range.singleton(1), Range.atLeast(1),
            Arrays.asList("split"), false),
    EXCLUDE(Names.EXCLUDE_NAME, Range.atLeast(1), Range.singleton(0), Range.singleton(0),
            Arrays.asList(), true),
    LOWERCASE(Names.LOWERCASE_NAME, Range.singleton(1), Range.singleton(0), Range.singleton(1),
            Arrays.asList("lowercase")),
    UPPERCASE(Names.UPPERCASE_NAME, Range.singleton(1), Range.singleton(0), Range.singleton(1),
            Arrays.asList("uppercase")),
    TRIM(Names.TRIM_NAME, Range.singleton(1), Range.singleton(0), Range.singleton(1),
            Arrays.asList("trim")),
    GEO_UNHASH(Names.GEO_UNHASH_NAME, Range.singleton(1), Range.singleton(0), Range.singleton(1),
            Arrays.asList("latLong"));

    /**
     * Transform names.
     * <p>
     * Enums cannot use static fields in their constructors as the
     * enum values are initialised before the statics.
     * Having the static fields in nested class means they are created
     * when required.
     */
    public class Names {
        public static final String DOMAIN_SPLIT_NAME = "domain_split";
        public static final String CONCAT_NAME = "concat";
        public static final String EXTRACT_NAME = "extract";
        public static final String SPLIT_NAME = "split";
        public static final String EXCLUDE_NAME = "exclude";
        public static final String LOWERCASE_NAME = "lowercase";
        public static final String UPPERCASE_NAME = "uppercase";
        public static final String TRIM_NAME = "trim";
        public static final String GEO_UNHASH_NAME = "geo_unhash";

        private Names() {
        }
    }

    private final Range<Integer> arityRange;
    private final Range<Integer> argumentsRange;
    private final Range<Integer> outputsRange;
    private final String prettyName;
    private final List<String> defaultOutputNames;
    private final boolean hasCondition;

    private TransformType(String prettyName, Range<Integer> arityRange,
                          Range<Integer> argumentsRange, Range<Integer> outputsRange,
                          List<String> defaultOutputNames) {
        this(prettyName, arityRange, argumentsRange, outputsRange, defaultOutputNames, false);
    }

    private TransformType(String prettyName, Range<Integer> arityRange,
                          Range<Integer> argumentsRange, Range<Integer> outputsRange,
                          List<String> defaultOutputNames, boolean hasCondition) {
        this.arityRange = arityRange;
        this.argumentsRange = argumentsRange;
        this.outputsRange = outputsRange;
        this.prettyName = prettyName;
        this.defaultOutputNames = defaultOutputNames;
        this.hasCondition = hasCondition;
    }

    /**
     * The count range of inputs the transform expects.
     *
     * @return
     */
    public Range<Integer> arityRange() {
        return this.arityRange;
    }

    /**
     * The count range of arguments the transform expects.
     *
     * @return
     */
    public Range<Integer> argumentsRange() {
        return this.argumentsRange;
    }

    /**
     * The count range of outputs the transform expects.
     *
     * @return
     */
    public Range<Integer> outputsRange() {
        return this.outputsRange;
    }

    public String prettyName() {
        return this.prettyName;
    }

    public List<String> defaultOutputNames() {
        return this.defaultOutputNames;
    }

    public boolean hasCondition() {
        return this.hasCondition;
    }

    @Override
    public String toString() {
        return this.prettyName();
    }

    /**
     * Get the enum for the given pretty name.
     * The static function valueOf() cannot be overridden so use
     * this method instead when converting from the pretty name
     * to enum.
     *
     * @param prettyName
     * @return
     */
    public static TransformType fromString(String prettyName) throws IllegalArgumentException {
        Set<TransformType> all = EnumSet.allOf(TransformType.class);

        for (TransformType type : all) {
            if (type.prettyName().equals(prettyName)) {
                return type;
            }
        }

        throw new IllegalArgumentException(prettyName);
    }

}
