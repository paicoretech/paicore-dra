/*
 * Extended Diameter Routing Agent
 * Copyright (C) 2019-2026 PAiCore Technology
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.paic.esg.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.paic.esg.helpers.ExtendedResource;
import com.paic.esg.impl.app.util.GettingRules;
import com.paic.esg.impl.app.util.ServerSocketCLI;
import com.paic.esg.impl.db.DataSource;
import com.paic.esg.impl.db.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiameterRoutingAgent extends ExtendedSignalingGateway {

    private static final Logger logger = LoggerFactory.getLogger(DiameterRoutingAgent.class);
    public static ExtendedSignalingGateway extendedSignalingGateway;
    private static Template applicationProp;
    public static void main(String[] args) {
        loadApplicationProp();
        DataSource.initialize();
        GettingRules.initialize().start();
        extendedSignalingGateway = DiameterRoutingAgent.initialize(args);
        extendedSignalingGateway.start();
        ServerSocketCLI.initialize(extendedSignalingGateway, applicationProp.getCliPort()).start();
    }

    private static void loadApplicationProp() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();
            applicationProp = mapper.readValue(new ExtendedResource("application.yaml").getAsStream(), Template.class);
        } catch (Exception ex ) {
            logger.error("Error on load the application.yaml file " + ex.getMessage());
        }
    }

}