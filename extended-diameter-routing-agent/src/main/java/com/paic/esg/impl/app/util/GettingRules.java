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

import com.paic.esg.helpers.ExtendedResource;
import com.paic.esg.impl.app.model.ExtendedSignalingGatewayRules;
import com.paic.esg.impl.app.model.Rules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

/**
 * @author joram
 */
public class GettingRules extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(GettingRules.class);
    private static final String FILE_NAME = "extended-signaling-gateway.xml";
    private static final Integer APPLICATION_COUNT = 0;
    private static ExtendedSignalingGatewayRules extendedSGRule;
    private static Unmarshaller unmarshaller;
    private static Rules rules = null;

    private static GettingRules instance = null;

    private static GettingRules getInstance() {
        if (instance == null) {
            instance = new GettingRules();
        }
        return instance;
    }

    /**
     * Create the JAXBContext instance
     */
    public static GettingRules initialize() {

        JAXBContext jaxbContext;
        GettingRules.getInstance();
        try {
            logger.info("Creating  JAXBContextInstance");
            jaxbContext = JAXBContext.newInstance(ExtendedSignalingGatewayRules.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            logger.error("Exception caught while initialize GettingRules! ", e);
        }
        loadXml();
        return instance;
    }

    /**
     * Invoke this constructor when you need to synchronize xml data
     */
    public static void loadXml() {
        logger.info("Loading new rule changes ...");
        InputStream xml = new ExtendedResource(FILE_NAME).getAsStream();
        try {
            extendedSGRule = (ExtendedSignalingGatewayRules) unmarshaller.unmarshal(xml);
            rules = extendedSGRule.getApplications()
                    .get(APPLICATION_COUNT)
                    .getRules();
        } catch (Exception e) {
            logger.error("Error loading rules: " + e.getMessage());
            Runtime.getRuntime().halt(1);
        }

    }

    /**
     * @return returns the list of rules defined in the xml
     */
    public static Rules rules() {
        if (rules == null) {
            logger.warn("Please initialize GettingRules in your main");
            initialize();
        }
        return rules;
    }

    @Override
    public void run() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(getFilePath(FILE_NAME));
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    watchService.close();
                } catch (IOException e) {
                    logger.error("WatchService error: " + e.getMessage());
                }
            }));

            WatchKey key;
            while (true) {
                key = watchService.take();
                Thread.sleep(50);
                if (key.pollEvents().stream().anyMatch(event -> event.kind() != StandardWatchEventKinds.OVERFLOW && event.context().toString().equals(FILE_NAME))) {
                    loadXml();
                }
                boolean reset = key.reset();
                if (!reset) {
                    logger.warn("Could not reset the watch key.");
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("WatchService error: " + e.getMessage());
        }
    }

    private String getFilePath(String filename) {
        boolean isConfigPath = System.getProperties().containsKey("mainConfig.path");
        String path = (isConfigPath ? System.getProperty("mainConfig.path") :
                System.getProperty("user.dir"));

        File file = new File(path + "/" + filename);
        if (file.exists()) {
            return path;
        } else {
            return this.getClass().getClassLoader().getResource(filename).getPath();
        }
    }
}
