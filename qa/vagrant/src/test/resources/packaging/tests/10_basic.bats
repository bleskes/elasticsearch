#!/usr/bin/env bats

# This file is used to test the X-Pack package.

# WARNING: This testing file must be executed as root and can
# dramatically change your system. It removes the 'elasticsearch'
# user/group and also many directories. Do not execute this file
# unless you know exactly what you are doing.

# ELASTICSEARCH CONFIDENTIAL
# __________________
#
#  [2014] Elasticsearch Incorporated. All Rights Reserved.
#
# NOTICE:  All information contained herein is, and remains
# the property of Elasticsearch Incorporated and its suppliers,
# if any.  The intellectual and technical concepts contained
# herein are proprietary to Elasticsearch Incorporated
# and its suppliers and may be covered by U.S. and Foreign Patents,
# patents in process, and are protected by trade secret or copyright law.
# Dissemination of this information or reproduction of this material
# is strictly forbidden unless prior written permission is obtained
# from Elasticsearch Incorporated.

# Load test utilities
load $BATS_UTILS/utils.bash
load $BATS_UTILS/tar.bash
load $BATS_UTILS/plugins.bash

setup() {
    skip_not_tar_gz
    export ESHOME=/tmp/elasticsearch
    export_elasticsearch_paths
    export ESPLUGIN_COMMAND_USER=elasticsearch
}

@test "[X-PACK] install x-pack" {
    # Cleans everything for the 1st execution
    clean_before_test

    # Install the archive
    install_archive

    count=$(find . -type f -name 'x-pack*.zip' | wc -l)
    [ "$count" -eq 1 ]

    install_and_check_plugin x pack x-pack-*.jar
}

@test "[X-PACK] verify x-pack installation" {
    assert_file "$ESHOME/bin/x-pack" d elasticsearch elasticsearch 755
    assert_file "$ESHOME/bin/x-pack/certgen" f elasticsearch elasticsearch 755
    assert_file "$ESHOME/bin/x-pack/croneval" f elasticsearch elasticsearch 755
    assert_file "$ESHOME/bin/x-pack/extension" f elasticsearch elasticsearch 755
    assert_file "$ESHOME/bin/x-pack/migrate" f elasticsearch elasticsearch 755
    assert_file "$ESHOME/bin/x-pack/syskeygen" f elasticsearch elasticsearch 755
    assert_file "$ESHOME/bin/x-pack/users" f elasticsearch elasticsearch 755
    assert_file "$ESCONFIG/x-pack" d elasticsearch elasticsearch 750
    assert_file "$ESCONFIG/x-pack/users" f elasticsearch elasticsearch 660
    assert_file "$ESCONFIG/x-pack/users_roles" f elasticsearch elasticsearch 660
    assert_file "$ESCONFIG/x-pack/roles.yml" f elasticsearch elasticsearch 660
    assert_file "$ESCONFIG/x-pack/role_mapping.yml" f elasticsearch elasticsearch 660
    assert_file "$ESCONFIG/x-pack/log4j2.properties" f elasticsearch elasticsearch 660
}