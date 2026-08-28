#!/bin/bash
#
# Extended Diameter Routing Agent
# Copyright (C) 2019-2026 PAiCore Technology
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
#
# Extended Diameter Routing Agent - start script.
#
# Run from the bin directory of the installation. Configuration is resolved
# relative to it, in ../conf.
#
# The VERSION token is substituted by the Ant release build.

set -u

BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONF_DIR="${DRA_CONF_DIR:-${BIN_DIR}/../conf}"
HEAP="${DRA_HEAP:-6g}"
SCTP_BUFFER="${DRA_SCTP_BUFFER:-50000000}"

cd "${BIN_DIR}" || exit 1

trap 'kill -TERM $PID' TERM INT

java -Xms"${HEAP}" -Xmx"${HEAP}" \
     -cp "extended-diameter-routing-agent-VERSION.jar:libs/*" \
     -Dlogback.configurationFile="${CONF_DIR}/logback.xml" \
     -Dorg.restcomm.sctp.bufferSize="${SCTP_BUFFER}" \
     -DmainConfig.path="${CONF_DIR}" \
     com.paic.esg.impl.DiameterRoutingAgent &

PID=$!
wait $PID
trap - TERM INT
wait $PID
exit $?
