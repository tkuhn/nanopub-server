#!/bin/bash
#
#
# Replaces the local conf file with the values found in environment variables
# starting with NPS_.
#

cd "$( dirname "${BASH_SOURCE[0]}" )"
cd -P ..

scripts/env-to-properties.sh > src/main/resources/ch/tkuhn/nanopub/server/local.conf.properties
