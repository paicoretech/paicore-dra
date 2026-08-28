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

package com.paic.esg.configuration.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;

public class ClientSocketCLI {
    private static PrintWriter out;
    private static BufferedReader in;
    private static Socket clientSocket;

    private static ClientSocketCLI instance = null;

    private static void getInstance() {
        if (instance == null) {
            instance = new ClientSocketCLI();
        }
    }

    public static int initialize(int cliPort) {
        int response = 1;
        ClientSocketCLI.getInstance();
        try {
            clientSocket = new Socket("127.0.0.1", cliPort);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (Exception ex) {
            response = 0;
            System.out.println("An error has occurred while attempting to initialize the socket client, please check if the Extended DRA is running");
        }
        return response;
    }

    public static String sendAction(String msg) {
        String encodeMsg = Base64.getEncoder().encodeToString(msg.getBytes());
        out.println(encodeMsg);
        String resp = null;
        try {
            resp = in.readLine();
        } catch (IOException ex) {
            System.out.println("An error has occurred while attempting to send action to the socket server " + ex.getMessage());
        }
        return resp;
    }

    public static void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (Exception ex) {
            System.out.println("An error has occurred while attempting to stop the socket client " + ex.getMessage());
        }

    }
}
