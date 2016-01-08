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

package org.elasticsearch.bench;

import org.elasticsearch.common.Randomness;

import com.carrotsearch.randomizedtesting.generators.RandomStrings;
import org.elasticsearch.common.metrics.MeanMetric;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.shield.authc.support.Hasher;
import org.elasticsearch.shield.authc.support.SecuredString;

public class HasherBenchmark {

    private static final int WARMING_ITERS = 1000;
    private static final int BENCH_ITERS = 10000;


    public static void main(String[] args) throws Exception {
        test(Hasher.SSHA256).print();
        test(Hasher.MD5).print();
        test(Hasher.SHA1).print();
        test(Hasher.BCRYPT4).print();
    }

    protected static Metrics test(Hasher hasher) {

        Metrics metrics = new Metrics(hasher);

        System.out.print("warming up [" + hasher.name() + "]...");

        for (int i = 0; i < WARMING_ITERS; i++) {
            SecuredString str = new SecuredString(RandomStrings.randomAsciiOfLength(Randomness.get(), 8).toCharArray());
            char[] hash = hasher.hash(str);
            hasher.verify(str, hash);
        }

        System.out.println("done!");
        System.out.print("starting benchmark for [" + hasher.name() + "]...");

        long start;

        for (int i = 0; i < BENCH_ITERS; i++) {
            SecuredString str = new SecuredString(RandomStrings.randomAsciiOfLength(Randomness.get(), 8).toCharArray());

            start = System.nanoTime();
            char[] hash = hasher.hash(str);
            metrics.hash.inc(System.nanoTime() - start);

            start = System.nanoTime();
            hasher.verify(str, hash);
            metrics.verify.inc(System.nanoTime() - start);
            if (i % 1000000 == 0) {
                System.out.println("finished " + i + " iterations");
            }
        }

        System.out.println("done!");

        return metrics;
    }

    private static class Metrics {

        final String name;
        final MeanMetric hash = new MeanMetric();
        final MeanMetric verify = new MeanMetric();

        public Metrics(Hasher hasher) {
            this.name = hasher.name();
        }

        void print() {
            System.out.println(name);
            System.out.println("\tHash (total): " + TimeValue.timeValueNanos(hash.sum()).format());
            System.out.println("\tHash (avg): " + hash.mean() + " nanos");
            System.out.println("\tVerify (total): " + TimeValue.timeValueNanos(verify.sum()).format());
            System.out.println("\tVerify (avg): " + verify.mean() + " nanos");
        }
    }
}
