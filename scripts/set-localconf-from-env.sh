#!/bin/bash
#
#
# Replaces the local conf file with the values found in environment variables
# starting with NPS_.
#

# Go to parent directory of this script file:
cd "$( dirname "${BASH_SOURCE[0]}" )" && cd -P ..

scripts/env-to-properties.sh > target/nanopub-server/WEB-INF/classes/ch/tkuhn/nanopub/server/local.conf.properties
