#!/bin/bash
#
# Configures the nanopub server via environment variables, and then starts it.
#
# Environment variables can be set before this script is run like this:
#
# $ export NPS_PUBLIC_URL=http://np.inn.ac/
# $ export NPS_ADMIN="John Doe <john@example.com>"
# $ export NPS_COLLECT_NANOPUBS_ENABLED=true
#

# Go to parent directory of this script file:
cd "$( dirname "${BASH_SOURCE[0]}" )" && cd -P ..

# Load environment variables to local config file:
scripts/set-localconf-from-env.sh

# Run Tomcat web application server:
catalina.sh run
