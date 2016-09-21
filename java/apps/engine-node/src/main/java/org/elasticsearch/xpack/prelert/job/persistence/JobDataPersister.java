
package org.elasticsearch.xpack.prelert.job.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Persist the records sent the the API.
 * Only the analysis fields are written. Records are mapped by the
 * by, over, partition and metric fields.
 *
 * Concrete classes need to implement the {@link #persistRecord(long, String[])},
 * {@linkplain #deleteData()} and {@linkplain #flushRecords()} methods.
 */
public abstract class JobDataPersister
{
    public static final String FIELDS = "fields";
    public static final String BY_FIELDS = "byFields";
    public static final String OVER_FIELDS = "overFields";
    public static final String PARTITION_FIELDS = "partitionFields";


    protected String [] fieldNames;
    protected int [] fieldMappings;
    protected int [] byFieldMappings;
    protected int [] overFieldMappings;
    protected int [] partitionFieldMappings;

    public void setFieldMappings(List<String> fields,
            List<String> byFields, List<String> overFields,
            List<String> partitionFields, Map<String, Integer> fieldMap)
    {
        fieldNames = fields.toArray(new String[fields.size()]);
        fieldMappings = extractMappings(fields, fieldMap);
        byFieldMappings = extractMappings(byFields, fieldMap);
        overFieldMappings = extractMappings(overFields, fieldMap);
        partitionFieldMappings = extractMappings(partitionFields, fieldMap);
    }

    private static int[] extractMappings(List<String> fields, Map<String, Integer> fieldMap)
    {
        List<Integer> mappings = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++)
        {
            Integer index = fieldMap.get(fields.get(i));
            if (index != null)
            {
                mappings.add(index);
            }
        }
        return mappings.stream().mapToInt(i->i).toArray();
    }

    /**
     * Save the record as per the field mappings
     * set up in {@linkplain #setFieldMappings(List, List, List, List, String[])}
     * setFieldMappings must have been called so this class knows where to
     *
     *
     * @param epoch
     * @param record
     */
    public abstract void persistRecord(long epoch, String[] record);

    /**
     * Delete all the persisted records
     *
     * @return
     */
    public abstract boolean deleteData();

    /**
     * Flush any records that may not have been persisted yet
     */
    public abstract void flushRecords();
}
