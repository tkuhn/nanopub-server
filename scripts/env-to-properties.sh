#!/bin/bash
#
# This script generates lines for .properties files from environment
# variables starting with 'NPS_'. The environment variable
# 'NPS_POST_NANOPUBS_ENABLED', for example, maps to the key name
# 'post.nanopubs.enabled' in the .properties file.
#
# Replace local conf file:
# $ scripts/env-to-properties.sh > src/main/resources/ch/tkuhn/nanopub/server/local.conf.properties
#
# Append to local conf file:
# $ scripts/env-to-properties.sh >> src/main/resources/ch/tkuhn/nanopub/server/local.conf.properties
#

printenv | egrep '^NPS_' | while read enventry; do
  key=`echo "$enventry" | sed -r 's/^NPS_([^=]+)=.*/\1/' | sed 's/_/./g' | awk '{print tolower($0)}'`
  value=`echo "$enventry" | sed -r 's/^NPS_[^=]+=(.*)/\1/'`
  echo "$key=$value"
done
