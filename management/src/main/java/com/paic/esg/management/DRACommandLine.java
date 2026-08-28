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

import com.paic.esg.configuration.controller.ClientSocketCLI;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

public class DRACommandLine {

    private static Map<String, Object> data;
    public static void main(String[] args) {
        loadApplicationProp();
        if (ClientSocketCLI.initialize(Integer.parseInt(data.get("cliPort").toString())) > 0) {
            ClientSocketCLI.sendAction("init");
            try {
                if (args != null && args[0] != null) {
                    String operation = args[0];
                    if (operation.equals("assoc")) {
                        int exitCode = new CommandLine(new Association()).execute(args);
                        ClientSocketCLI.stopConnection();
                        System.exit(exitCode);
                    } else if (operation.equals("peer")) {
                        int exitCode = new CommandLine(new Peer()).execute(args);
                        ClientSocketCLI.stopConnection();
                        System.exit(exitCode);
                    } else {
                        System.out.println("Invalid Command");
                        ClientSocketCLI.stopConnection();
                    }
                }

            } catch (Exception ex) {
                ClientSocketCLI.stopConnection();
                System.out.println(ex.getMessage());
            }
        }
    }

    private static void loadApplicationProp() {
        try {
            InputStream inputStream = new FileInputStream("../conf/application.yaml");
            Yaml yaml = new Yaml();
            data = yaml.load(inputStream);
        } catch (Exception ex ) {
            System.out.println("Error on load the application.yaml file " + ex.getMessage());
        }
    }
}