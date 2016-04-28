#!/bin/bash
#
# Uses logging configuration from the template.
#

# Go to parent directory of this script file:
cd "$( dirname "${BASH_SOURCE[0]}" )" && cd -P ..

cp src/main/resources/log4j.properties.template src/main/resources/log4j.properties
