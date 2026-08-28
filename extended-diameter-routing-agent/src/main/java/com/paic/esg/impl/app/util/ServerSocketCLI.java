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

package com.paic.esg.impl.app.util;

import com.paic.esg.impl.ExtendedSignalingGateway;
import com.paic.esg.network.layers.DiameterLayer;
import org.jdiameter.api.Peer;
import org.jdiameter.api.PeerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class ServerSocketCLI extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ServerSocketCLI.class);

    private static ServerSocketCLI instance = null;
    private static DiameterLayer diameterLayer = null;
    private static ExtendedSignalingGateway extendedSignalingGateway;
    private int cliPort = 0;

    private static void getInstance(ExtendedSignalingGateway extendedSignalingGateway, int cliPort) {
        if (instance == null) {
            instance = new ServerSocketCLI(extendedSignalingGateway, cliPort);
        }
    }

    public ServerSocketCLI(ExtendedSignalingGateway extendedSignalingGateway, int cliPort) {
        ServerSocketCLI.extendedSignalingGateway = extendedSignalingGateway;
        this.cliPort = cliPort;
    }

    public static ServerSocketCLI initialize(ExtendedSignalingGateway extendedSignalingGateway, int cliPort) {
        ServerSocketCLI.getInstance(extendedSignalingGateway, cliPort);
        return instance;
    }

    @Override
    public void run() {
        try {
            logger.info("Starting Server Socket in port: " + this.cliPort);
            ServerSocket serverSocket = new ServerSocket(this.cliPort);
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }

        } catch (Exception e) {
            logger.error("Error on start server socket " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                String[] params = new String[0];
                StringBuilder stringBuilder = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    byte[] decodedBytes = Base64.getDecoder().decode(inputLine);
                    String decodedMsg = new String(decodedBytes);
                    logger.info("Getting this in socket server " + decodedMsg);
                    String[] dataSocket = decodedMsg.split("#");
                    if ("peer".equals(dataSocket[0])) {
                        if (dataSocket.length > 2) {
                            params = dataSocket[2].split("\\|");
                        }
                        HashMap<String, Object> paramsData = new HashMap<>();
                        switch (dataSocket[1]) {
                            case "list":
                                List<Peer> peerList = diameterLayer.getPeerList();
                                for (Peer peer : peerList) {
                                    stringBuilder.append(peer.getUri()).append("|");
                                    stringBuilder.append(peer.getState(PeerState.class).toString());
                                    stringBuilder.append("#");
                                }
                                break;

                            case "start":
                                paramsData.put("peerURI", params[0]);
                                paramsData.put("connecting", params[1]);
                                paramsData.put("ip", params[3]);
                                paramsData.put("realm", params[5]);
                                diameterLayer.addPeer(paramsData);
                                break;

                            case "stop":
                                paramsData.put("peerName", params[0]);
                                paramsData.put("disconnectCause", params[1]);
                                paramsData.put("connecting", params[2]);
                                diameterLayer.stopPeer(paramsData);
                                break;
                        }
                    } else if ("init".equals(dataSocket[0])) {
                        ServerSocketCLI.diameterLayer = (DiameterLayer) extendedSignalingGateway.getLayer("diameter");
                    }
                    out.println(stringBuilder);
                }

                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Error on server socket " + e.getMessage());
            }

        }
    }
}
