/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.transforms;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.prelert.job.TransformConfig;
import com.prelert.job.TransformType;

public class DependencySorterTest
{

    @Test
    public void testFindDependencies_GivenNoDependencies()
    {
        List<TransformConfig> transforms = new ArrayList<>();
        List<TransformConfig> deps = DependencySorter.findDependencies("metricField", transforms);
        assertEquals(0, deps.size());
    }

    @Test
    public void testFindDependencies_Given1Dependency()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        List<String> inputs = Arrays.asList("ina", "inb");
        List<String> outputs = Arrays.asList("ab");
        TransformConfig concat = createContatTransform(inputs, outputs);
        transforms.add(concat);

        List<String> inputs2 = Arrays.asList("inc", "ind");
        List<String> outputs2 = Arrays.asList("cd");
        TransformConfig concat2 = createContatTransform(inputs2, outputs2);
        transforms.add(concat2);


        List<TransformConfig> deps = DependencySorter.findDependencies("cd", transforms);
        assertEquals(1, deps.size());
        assertEquals(deps.get(0), concat2);
    }

    @Test
    public void testFindDependencies_Given2Dependencies()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        List<String> inputs = Arrays.asList("ina", "inb");
        List<String> outputs = Arrays.asList("ab");
        TransformConfig concat = createContatTransform(inputs, outputs);
        transforms.add(concat);

        List<String> inputs2 = Arrays.asList("inc", "ind");
        List<String> outputs2 = Arrays.asList("cd");
        TransformConfig concat2 = createContatTransform(inputs2, outputs2);
        transforms.add(concat2);


        List<TransformConfig> deps = DependencySorter.findDependencies(Arrays.asList("cd", "ab"),
                                                                    transforms);
        assertEquals(2, deps.size());
        assertTrue(deps.contains(concat));
        assertTrue(deps.contains(concat2));
    }

    @Test
    public void testFindDependencies_GivenChainOfDependencies()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        List<String> inputs = Arrays.asList("ina", "inb");
        List<String> outputs = Arrays.asList("ab");
        TransformConfig concat = createContatTransform(inputs, outputs);
        transforms.add(concat);

        List<String> inputs2 = Arrays.asList("ab", "inc");
        List<String> outputs2 = Arrays.asList("abc");
        TransformConfig dependentConcat = createContatTransform(inputs2, outputs2);
        transforms.add(dependentConcat);

        List<TransformConfig> deps = DependencySorter.findDependencies("abc",
                                                                    transforms);
        assertEquals(2, deps.size());
        assertEquals(concat, deps.get(0));
        assertEquals(dependentConcat, deps.get(1));
    }

    /**
     * 2 separate inputs with chain of dependencies one of which is shared
     */
    @Test
    public void testFindDependencies_Given2ChainsAndSharedDependencys()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        List<String> inputs = Arrays.asList("ina", "inb");
        List<String> outputs = Arrays.asList("ab");
        TransformConfig concat = createContatTransform(inputs, outputs);
        transforms.add(concat);

        List<String> inputs2 = Arrays.asList("ab", "inc");
        List<String> outputs2 = Arrays.asList("abc");
        TransformConfig dependentConcat1 = createContatTransform(inputs2, outputs2);
        transforms.add(dependentConcat1);

        List<String> inputs3 = Arrays.asList("ab", "ind");
        List<String> outputs3 = Arrays.asList("abd");
        TransformConfig dependentConcat2 = createContatTransform(inputs3, outputs3);
        transforms.add(dependentConcat2);

        List<TransformConfig> deps = DependencySorter.findDependencies(Arrays.asList("abc", "abd"),
                                                                    transforms);
        assertEquals(3, deps.size());
        assertEquals(concat, deps.get(0));
        assertEquals(dependentConcat1, deps.get(1));
        assertEquals(dependentConcat2, deps.get(2));
    }

    @Test
    public void testFindDependencies_CatchCircularDependency()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        List<String> inputs = Arrays.asList("ina", "ab");
        List<String> outputs = Arrays.asList("ab");
        TransformConfig concat = createContatTransform(inputs, outputs);
        transforms.add(concat);

        List<TransformConfig> deps = DependencySorter.findDependencies(Arrays.asList("ab"),
                transforms);

        fail();
    }

    @Test
    public void testFindDependencies_CatchCircularDependencyTransitive()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        List<String> inputs = Arrays.asList("ina", "inb");
        List<String> outputs = Arrays.asList("ab"); // default output
        TransformConfig concat = createContatTransform(inputs, outputs);
        transforms.add(concat);

        TransformConfig concat1 = createContatTransform(Arrays.asList("ina", "inc"),
                                                        Arrays.asList("ac"));
        transforms.add(concat1);

        TransformConfig concat2 = createContatTransform(Arrays.asList("concat", "ac"),
                Arrays.asList());
        transforms.add(concat2);

        TransformConfig concat3 = createContatTransform(Arrays.asList("ina", "inb"),
                Arrays.asList());
        transforms.add(concat3);

        List<TransformConfig> deps = DependencySorter.findDependencies(Arrays.asList("ab"),
                transforms);

        fail();
    }

    @Test
    public void testSortByDependency_NoDependencies()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        TransformConfig concat = createContatTransform(Arrays.asList("ina", "inb"),
                                                            Arrays.asList("ab"));
        transforms.add(concat);

        TransformConfig hrd1 = createHrdTransform(Arrays.asList("dns"),
                                                        Arrays.asList("subdomain", "hrd"));
        transforms.add(hrd1);


        TransformConfig hrd2 = createHrdTransform(Arrays.asList("dns2"),
                Arrays.asList("subdomain"));
        transforms.add(hrd2);

        List<TransformConfig> orderedDeps = DependencySorter.sortByDependency(transforms);

        assertEquals(transforms.size(), orderedDeps.size());
    }

    @Test
    public void testSortByDependency_SingleChain()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        // Chain of 3 dependencies
        TransformConfig chain1Concat = createContatTransform(Arrays.asList("ina", "inb"),
                                                            Arrays.asList("ab"));
        transforms.add(chain1Concat);

        TransformConfig chain1Hrd = createHrdTransform(Arrays.asList("ab"),
                                                        Arrays.asList("subdomain", "hrd"));
        transforms.add(chain1Hrd);

        TransformConfig chain1Concat2 = createContatTransform(Arrays.asList("subdomain", "port"),
                Arrays.asList());
        transforms.add(chain1Concat2);

        List<TransformConfig> orderedDeps = DependencySorter.sortByDependency(transforms);

        assertEquals(transforms.size(), orderedDeps.size());

        int chain1ConcatIndex = orderedDeps.indexOf(chain1Concat);
        assertTrue(chain1ConcatIndex >= 0);
        int chain1HrdIndex = orderedDeps.indexOf(chain1Hrd);
        assertTrue(chain1HrdIndex >= 0);
        int chain1Concat2Index = orderedDeps.indexOf(chain1Concat2);
        assertTrue(chain1Concat2Index >= 0);

        assertTrue(chain1ConcatIndex < chain1HrdIndex);
        assertTrue(chain1HrdIndex < chain1Concat2Index);
    }

    @Test
    public void testSortByDependency_3Chains()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        // Chain of 2 dependencies
        TransformConfig chain1Concat = createContatTransform(Arrays.asList("ina", "inb"),
                                                            Arrays.asList("ab"));
        transforms.add(chain1Concat);

        TransformConfig chain1Hrd = createHrdTransform(Arrays.asList("ab"),
                                                        Arrays.asList("subdomain", "hrd"));
        transforms.add(chain1Hrd);

        // Chain of 2 dependencies
        TransformConfig chain2Concat = createContatTransform(Arrays.asList("inc", "ind"),
                Arrays.asList("cd"));
        transforms.add(chain2Concat);

        TransformConfig chain2Concat2 = createContatTransform(Arrays.asList("cd", "ine"),
                Arrays.asList("cde"));
        transforms.add(chain2Concat2);

        // Chain of 1
        TransformConfig noChainHrd = createHrdTransform(Arrays.asList("dns"),
                Arrays.asList("subdomain"));
        transforms.add(noChainHrd);

        List<TransformConfig> orderedDeps = DependencySorter.sortByDependency(transforms);

        assertEquals(transforms.size(), orderedDeps.size());

        int chain1ConcatIndex = orderedDeps.indexOf(chain1Concat);
        assertTrue(chain1ConcatIndex >= 0);
        int chain1HrdIndex = orderedDeps.indexOf(chain1Hrd);
        assertTrue(chain1HrdIndex >= 0);
        assertTrue(chain1ConcatIndex < chain1HrdIndex);

        int chain2ConcatIndex = orderedDeps.indexOf(chain2Concat);
        assertTrue(chain2ConcatIndex >= 0);
        int chain2Concat2Index = orderedDeps.indexOf(chain2Concat2);
        assertTrue(chain2Concat2Index >= 0);
        assertTrue(chain2ConcatIndex < chain2Concat2Index);
    }


    private TransformConfig createContatTransform(List<String> inputs, List<String> outputs)
    {
        TransformConfig concat = new TransformConfig();
        concat.setTransform(TransformType.CONCAT.prettyName());
        concat.setInputs(inputs);
        concat.setOutputs(outputs);
        return concat;
    }

    private TransformConfig createHrdTransform(List<String> inputs, List<String> outputs)
    {
        TransformConfig concat = new TransformConfig();
        concat.setTransform(TransformType.DOMAIN_LOOKUP.prettyName());
        concat.setInputs(inputs);
        concat.setOutputs(outputs);
        return concat;
    }
}
