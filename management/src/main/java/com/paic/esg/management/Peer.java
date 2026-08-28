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
import com.paic.esg.configuration.controller.ConfigurationController;
import com.paic.esg.configuration.dto.Configuration;
import com.paic.esg.configuration.dto.Peers;
import picocli.CommandLine;
import picocli.CommandLine.IVersionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.jar.Manifest;

@CommandLine.Command(   
        synopsisSubcommandLabel="",
        commandListHeading = "",     
        usageHelpAutoWidth = true,        
        separator = "   ",
        sortOptions = false,
        customSynopsis =  " peer [options]",
        headerHeading = "%n",
        description = "%nOperations:%n",
        mixinStandardHelpOptions = false, 
        versionProvider = Peer.ManifestVersionProvider.class)

public class Peer implements Callable<String> {

    private ConfigurationController controller = new ConfigurationController();

    @CommandLine.Option(names = {"-list"}, description = "       Returns connection list")
    private boolean list;


    @CommandLine.Option(names = "peer",    description = "       initializator")
    private boolean initialToken;

   
    @CommandLine.Option(names = "-add", description = "        Add a new Peer to the configuratio file")
    private boolean add;

    @CommandLine.Option(names = "-edit", description = "        Open a selected Peer for modification")
    private boolean edit;

    @CommandLine.Option(names = "-remove", paramLabel="Remove peer  ",description = "        Remove the peer matching the provided index")
    private Integer remove;

    @CommandLine.Option(names = "-details", paramLabel="Peer details  ",description = "        Display a given Peer details")
    private Integer detailIndex;

    @CommandLine.Option(names = "-start", paramLabel="Start peer  ",description = "        Start the Peer connexion")
    private Integer peerToStart;

    @CommandLine.Option(names = "-stop",paramLabel="Stop peer  ", description = "        Stop the Peer connexion")
    private Integer peerToStop;

    @CommandLine.Option(names = "-cause", paramLabel="0,1,2", description = "        The cause to stop the Peer connexion: %n        [0 -> REBOOTING]%n        [1 -> BUSY]%n        [2 -> DO NOT WANT TO TALK TO YOU]")
    private Integer peerToStopCause;

    @CommandLine.Option(names = "-reset", paramLabel="Reset peer  ", description = "        Reset the Peer connexion")
    private Integer peerToReset;

    @CommandLine.Option(names = {" -h", "--help"},  description = "        Display this help and exit")
    private boolean help;

    Configuration configuration = controller.getConfiguration();
    Scanner optionalScanner = new Scanner(System.in);
    String optionalInput = "";

    @Override
    public String call() {      
        if (list) {
            listPeers();
        }

        if (detailIndex != null && detailIndex != 0) {
            showPeerDetails();
        }

        if (add) {

            Scanner sc = new Scanner(System.in);

            com.paic.esg.configuration.dto.Peer peer = new com.paic.esg.configuration.dto.Peer();
            System.out.print("Enter the Peer Name - ");
            peer.setName(sc.next());

            System.out.print("Attempt Connect? (Y / N) - ");
            boolean attemptConnect = sc.next().equalsIgnoreCase("Y");
            peer.setAttemptConnect(attemptConnect);

            System.out.print("Enter Peer Rating - ");
            peer.setRating(sc.nextInt());

            System.out.print("Enter Peer ip - ");
            optionalInput = optionalScanner.nextLine();
            peer.setIp(optionalInput.isEmpty() ? "" : optionalInput);

            System.out.print("Enter Peer StandBy Address - ");
            optionalScanner = new Scanner(System.in);
            optionalInput = optionalScanner.nextLine();
            peer.setStandbyAddresses(optionalInput.isEmpty() ? "" : optionalInput);

            controller.addNewPeer(peer);

            System.out.println("Peer added successfully!");
            listPeers();
        }

        if (remove != null) {
            removePeer();
        }

        if (edit) {
            openPeerForModify();
        }

        if (peerToStart != null) {
            startPeer(peerToStart);
        }

        if (peerToStop != null) {
            if (peerToStopCause != null) {
                stopPeer(peerToStop, peerToStopCause);
            } else {
                System.out.println("To stop the Peer , add the option -cause [0 -> REBOOTING; 1 -> BUSY; 2 -> DO NOT WANT TO TALK TO YOU]");
            }

        }

        if (peerToReset != null) {
            resetPeer(peerToReset);
        }
        return "";
    }

    private String getStringParam(com.paic.esg.configuration.dto.Peer peer) {
        return peer.getName() + "|" +
                peer.isAttemptConnect() + "|" +
                peer.getRating() + "|" +
                peer.getIp() + "|" +
                peer.getStandbyAddresses() + "|" +
                controller.getLocalRealm();
    }

    private void startPeer(int peerIndex) {
        try {
            com.paic.esg.configuration.dto.Peer peer = configuration.getNetwork().getPeers().getPeerList().get(peerIndex - 1);
            if (peer != null) {
                ClientSocketCLI.sendAction("peer#start#" + getStringParam(peer));
                System.out.println("Peer started successfully!");
            } else {
                System.out.println("The Peer doesn't exist");
            }
        } catch (IndexOutOfBoundsException indexEx) {
            System.out.println("The Peer doesn't exist");
        } catch (Exception ex) {
            System.out.println("Error on try to get Peer " + ex.getMessage());
        }
        System.out.println();
    }

    private void stopPeer(int peerIndex, int cause) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            com.paic.esg.configuration.dto.Peer peer = configuration.getNetwork().getPeers().getPeerList().get(peerIndex - 1);
            if (peer != null) {
                stringBuilder.append(peer.getName()).append("|");
                stringBuilder.append(cause).append("|");
                stringBuilder.append(peer.isAttemptConnect());
                ClientSocketCLI.sendAction("peer#stop#" + stringBuilder);
                System.out.println("Peer stopped successfully!");
            } else {
                System.out.println("The Peer doesn't exist");
            }
        } catch (IndexOutOfBoundsException indexEx) {
            System.out.println("The Peer doesn't exist");
        } catch (Exception ex) {
            System.out.println("Error on try to get Peer " + ex.getMessage());
        }
        System.out.println();
    }

    private void resetPeer(int peerIndex) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            com.paic.esg.configuration.dto.Peer peer = configuration.getNetwork().getPeers().getPeerList().get(peerIndex - 1);
            if (peer != null) {
                stringBuilder.append(peer.getName()).append("|");
                stringBuilder.append(0).append("|");
                stringBuilder.append(peer.isAttemptConnect());
                ClientSocketCLI.sendAction("peer#stop#" + stringBuilder);
                Thread.sleep(6000);
                ClientSocketCLI.sendAction("peer#start#" + getStringParam(peer));
                System.out.println("Peer rested successfully!");
            } else {
                System.out.println("The Peer doesn't exist");
            }
        } catch (IndexOutOfBoundsException indexEx) {
            System.out.println("The Peer doesn't exist");
        } catch (Exception ex) {
            System.out.println("Error on try to get Peer " + ex.getMessage());
        }
        System.out.println();
    }

    private void listPeers() {
        System.out.println();
        String response = ClientSocketCLI.sendAction("peer#list");
        List<String> peerListStatus = Arrays.asList(response.split("#"));
        configuration = controller.getConfiguration();
        if (configuration != null && configuration.getNetwork() != null && configuration.getNetwork().getPeers() != null) {
            Peers peers = configuration.getNetwork().getPeers();
            int peerCounter = 0;
            String peerStatus;
            Optional<String> peerStatusFound;
            for (com.paic.esg.configuration.dto.Peer peer : peers.getPeerList()) {
                peerCounter++;
                peerStatusFound = peerListStatus.stream().filter(f -> f.contains(cleanPeerName(peer.getName()))).findFirst();
                if (peerStatusFound.isPresent()) {
                    peerStatus = peerStatusFound.get().split("\\|")[1];
                } else {
                    peerStatus = "DOWN";
                }
                System.out.println("Peer " + peerCounter + " -> " + peer.getName() + " - Status -> " + peerStatus);
            }
        }
        System.out.println();
    }

    private String cleanPeerName(String peerName) {
        String result;
        if (peerName.contains(":")) {
            String[] peerNameArray = peerName.split(":");
            if (peerNameArray.length > 2) {
                result = peerNameArray[1];
            } else {
                result = peerNameArray[0];
            }
        } else {
            result = peerName;
        }
        return result;
    }

    private boolean canModify(com.paic.esg.configuration.dto.Peer peer) {
        boolean response = true;
        String msgResponse = ClientSocketCLI.sendAction("peer#list");
        List<String> peerListStatus = Arrays.asList(msgResponse.split("#"));
        String peerStatus;
        Optional<String> peerStatusFound;
        peerStatusFound = peerListStatus.stream().filter(f -> f.contains(cleanPeerName(peer.getName()))).findFirst();
        if (peerStatusFound.isPresent()) {
            peerStatus = peerStatusFound.get().split("\\|")[1];
            response = (!peerStatus.equals("OKAY"));
        }
        return response;

    }

    private void showPeerDetails() {
        Peers peers = configuration.getNetwork().getPeers();
        com.paic.esg.configuration.dto.Peer selectedPeer = peers.getPeerList().get(this.detailIndex - 1);
        System.out.println();
        System.out.println("Peer name : " + selectedPeer.getName());
        System.out.println("Attempt Connect : " + (selectedPeer.isAttemptConnect() ? "YES" : "NO"));
        System.out.println("Peer Rating : " + selectedPeer.getRating());
        String currentPeerIP = selectedPeer.getIp() == null ? "" : selectedPeer.getIp();
        System.out.println("Peer ip : " + currentPeerIP);
        System.out.println("Peer StandBy Address : " + selectedPeer.getStandbyAddresses());
        System.out.println();
    }

    private void openPeerForModify() {
        listPeers();
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the Peer index for modification - ");
        int index = scanner.nextInt() - 1;
        Configuration configuration = controller.getConfiguration();
        Peers peers = configuration.getNetwork().getPeers();
        try {
            com.paic.esg.configuration.dto.Peer selectedPeer = peers.getPeerList().get(index);
            if (canModify(selectedPeer)) {
                System.out.println();
                System.out.println("Actual Peer Name is " + selectedPeer.getName());
                System.out.print("Enter new Peer Name - ");
                selectedPeer.setName(scanner.next());

                System.out.println("Actual Attempt Connection is setted to " + selectedPeer.isAttemptConnect());
                System.out.print("Enter new Attempt Connection value (Y / N) - ");
                selectedPeer.setAttemptConnect(scanner.next().equalsIgnoreCase("Y"));

                System.out.println("Actual Peer rating is " + selectedPeer.getRating());
                System.out.print("Enter new Peer Rating - ");
                selectedPeer.setRating(scanner.nextInt());

                String peerIp = selectedPeer.getIp() == null ? "" : selectedPeer.getIp();
                System.out.println("Actual Peer ip is " + peerIp);
                System.out.print("Enter new Peer ip - ");

                optionalScanner = new Scanner(System.in);
                optionalInput = optionalScanner.nextLine();
                selectedPeer.setIp(optionalInput.isEmpty() ? "" : optionalInput);

                System.out.println("Actual Peer StandBy Address is " + selectedPeer.getStandbyAddresses());
                System.out.print("Enter new Peer StandBy Address - ");

                optionalScanner = new Scanner(System.in);
                optionalInput = optionalScanner.nextLine();
                selectedPeer.setStandbyAddresses(optionalInput.isEmpty() ? "" : optionalInput);

                controller.modifyPeer(index, selectedPeer);
                System.out.println("Peer modified successfully!");
                listPeers();
            } else {
                System.out.println("Please stop the Peer, before modifying it");
            }

        } catch (IndexOutOfBoundsException indexEx) {
            System.out.println("The Peer doesn't exist");
        }
        System.out.println();
    }

    

    private void removePeer() {
        Configuration configuration = controller.getConfiguration();
        Peers peers = configuration.getNetwork().getPeers();
        try {
            com.paic.esg.configuration.dto.Peer selectedPeer = peers.getPeerList().get(this.remove - 1);
            if (canModify(selectedPeer)) {
                ClientSocketCLI.sendAction("peer#remove#" + 0);
                controller.remove(this.remove);
                System.out.println("Peer removed successfully!");
                listPeers();
            } else {
                System.out.println("Please stop the Peer, before removing it");
            }

        } catch (IndexOutOfBoundsException indexEx) {
            System.out.println("The Peer doesn't exist");
        }
        System.out.println();
    }


    static class ManifestVersionProvider implements IVersionProvider {

        public String[] getVersion() {
            return new String[]{"DRA ".concat(readVersion())};
        }

        /**
         * Reads Implementation-Version from the manifest of the jar this class was
         * loaded from. Returns "unknown" when running outside a packaged jar, which
         * is the case in an IDE or straight off target/classes.
         */
        private static String readVersion() {
            String version = ManifestVersionProvider.class.getPackage().getImplementationVersion();
            if (version != null && !version.isEmpty()) {
                return version;
            }
            try (InputStream manifest = ManifestVersionProvider.class
                    .getResourceAsStream("/META-INF/MANIFEST.MF")) {
                if (manifest != null) {
                    version = new Manifest(manifest).getMainAttributes()
                            .getValue("Implementation-Version");
                }
            } catch (IOException ioEx) {
                version = null;
            }
            return (version == null || version.isEmpty()) ? "unknown" : version;
        }

    }
}