/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.license.plugin.core.trial;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.core.DateUtils;
import org.elasticsearch.license.core.ESLicense;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import static org.elasticsearch.license.plugin.core.trial.TrialLicenses.TrialLicense;

public class TrialLicensesBuilder {

    public static TrialLicenses EMPTY = trialLicensesBuilder().build();

    public static final String ISSUER = "elasticsearch";

    public static final ESLicense.Type TYPE = ESLicense.Type.TRIAL;

    public static final ESLicense.SubscriptionType SUBSCRIPTION_TYPE = ESLicense.SubscriptionType.NONE;

    public static TrialLicensesBuilder trialLicensesBuilder() {
        return new TrialLicensesBuilder();
    }

    public static TrialLicenseBuilder trialLicenseBuilder() {
        return new TrialLicenseBuilder();
    }


    public static TrialLicenses merge(TrialLicenses trialLicenses, TrialLicenses mergeTrialLicenses) {
        if (trialLicenses == null && mergeTrialLicenses == null) {
            throw new IllegalArgumentException("both licenses can not be null");
        } else if (trialLicenses == null) {
            return mergeTrialLicenses;
        } else if (mergeTrialLicenses == null) {
            return trialLicenses;
        } else {
            return trialLicensesBuilder()
                    .licenses(trialLicenses.trialLicenses())
                    .licenses(mergeTrialLicenses.trialLicenses())
                    .build();
        }
    }

    private final ImmutableMap.Builder<String, TrialLicense> licenseBuilder;

    public TrialLicensesBuilder() {
        licenseBuilder = ImmutableMap.builder();
    }

    public TrialLicensesBuilder license(TrialLicense trialLicense) {
        licenseBuilder.put(trialLicense.feature(), trialLicense);
        return this;
    }

    public TrialLicensesBuilder licenses(TrialLicenses trialLicenses) {
        return licenses(trialLicenses.trialLicenses());
    }

    public TrialLicensesBuilder licenses(Collection<TrialLicense> trialLicenses) {
        for (TrialLicense trialLicense : trialLicenses) {
            license(trialLicense);
        }
        return this;
    }

    public TrialLicenses build() {
        final ImmutableMap<String, TrialLicense> licenseMap = licenseBuilder.build();
        return new TrialLicenses() {

            @Override
            public Collection<TrialLicense> trialLicenses() {
                return licenseMap.values();
            }

            @Override
            public TrialLicense getTrialLicense(String feature) {
                return licenseMap.get(feature);
            }

            @Override
            public Iterator<TrialLicense> iterator() {
                return licenseMap.values().iterator();
            }
        };
    }

    public static class TrialLicenseBuilder {
        private String featureType;
        private long expiryDate = -1;
        private long issueDate = -1;
        private TimeValue duration;
        private int maxNodes = -1;
        private String uid = null;
        private String issuedTo;

        public TrialLicenseBuilder() {
        }

        public TrialLicenseBuilder uid(String uid) {
            this.uid = uid;
            return this;
        }

        public TrialLicenseBuilder issuedTo(String issuedTo) {
            this.issuedTo = issuedTo;
            return this;
        }

        public TrialLicenseBuilder maxNodes(int maxNodes) {
            this.maxNodes = maxNodes;
            return this;
        }

        public TrialLicenseBuilder feature(String featureType) {
            this.featureType = featureType;
            return this;
        }

        public TrialLicenseBuilder issueDate(long issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public TrialLicenseBuilder duration(TimeValue duration) {
            this.duration = duration;
            return this;
        }

        public TrialLicenseBuilder expiryDate(long expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public TrialLicense build() {
            verify();
            if (expiryDate == -1) {
                expiryDate = issueDate + duration.millis();
            }
            if (uid == null) {
                uid = UUID.randomUUID().toString();
            }
            return new TrialLicense() {

                @Override
                public String issuedTo() {
                    return issuedTo;
                }

                @Override
                public String feature() {
                    return featureType;
                }

                @Override
                public long issueDate() {
                    return issueDate;
                }

                @Override
                public long expiryDate() {
                    return expiryDate;
                }

                @Override
                public int maxNodes() {
                    return maxNodes;
                }

                @Override
                public String uid() {
                    return uid;
                }
            };
        }

        private void verify() {
            String msg = null;
            if (issuedTo == null) {
                msg = "issuedTo has to be set";
            } else if (featureType == null) {
                msg = "feature has to be set";
            } else if (issueDate == -1) {
                msg = "issueDate has to be set";
            } else if (duration == null && expiryDate == -1) {
                msg = "duration or expiryDate has to be set";
            } else if (maxNodes == -1) {
                msg = "maxNodes has to be set";
            }
            if (msg != null) {
                throw new IllegalArgumentException(msg);
            }
        }
    }
}
