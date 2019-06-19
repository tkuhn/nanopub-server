#!/bin/bash
#
# Uses logging configuration from the template.
#

# Go to parent directory of this script file:
cd "$( dirname "${BASH_SOURCE[0]}" )" && cd -P ..

cp target/nanopub-server/WEB-INF/classes/log4j.properties.template target/nanopub-server/WEB-INF/classes/log4j.properties
