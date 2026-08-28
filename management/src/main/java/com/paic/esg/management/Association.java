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

package com.paic.esg.management;

import com.paic.esg.configuration.controller.ConfigurationController;
import com.paic.esg.configuration.dto.Configuration;
import com.paic.esg.configuration.dto.Peer;
import com.paic.esg.configuration.dto.Peers;
import picocli.CommandLine;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "assoc", description = "Performs associations manipulation operations", mixinStandardHelpOptions = true, version = "DRA CLI 1.0")
public class Association implements Callable<String> {
    @CommandLine.Option(names = "assoc", description = "initializator")
    private boolean initialToken;

    @CommandLine.Option(names = "-edit", description = "Edit a given association")
    private String edit;

    @CommandLine.Option(names = "-list", description = "Returns associations list")
    private boolean list;


    @Override
    public String call() throws Exception {
        if (edit != null) {
            System.out.println("Association edited, new name is " + edit);
        }

        if (list) {
            ConfigurationController controller = new ConfigurationController();
            Configuration configuration = controller.getConfiguration();

            if (configuration !=null && configuration.getNetwork() != null && configuration.getNetwork().getPeers()!=null){
                Peers peers = configuration.getNetwork().getPeers();
                int peerCounter = 0;
                for (Peer peer : peers.getPeerList()){
                    peerCounter++;
                    System.out.println("Peer " + peerCounter + " -> " + peer.getName());
                }

            }

        }
        return "";
    }
}