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

package com.paic.esg.impl.app.handler;

import com.paic.esg.network.layers.DiameterLayer;
import org.jdiameter.api.Request;
import org.jdiameter.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

public class RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    private static final Long EXPIRE_AFTER = 30000L;
    public static ConcurrentMap<String, Map<String, Object>> requests = new ConcurrentHashMap<>();

    private static RequestHandler instance = null;
    private static DiameterLayer diameter = null;

    private static RequestHandler getInstance(DiameterLayer diameterLayer) {
        if (instance == null) {
            instance = new RequestHandler();
            diameter = diameterLayer;
        }
        return instance;
    }

    public static void initialize(DiameterLayer diameterLayer) {
        RequestHandler.getInstance(diameterLayer);
    }

    public RequestHandler() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    logger.debug(String.format("Executing task at [%s]", new Date()));
                    int sessionLength = requests.size();
                    if (removeIf(f -> ((long) f.getValue().get("dateExpire")) < System.currentTimeMillis())) {
                        logger.info(String.format("Deleting '%d' out of '%d' sessions",
                                (sessionLength - requests.size()), sessionLength));
                    }
                } catch (Exception e) {
                    logger.error("Exception caught while aging sessions! ", e);
                }
            }
        }, 0, 60000);
    }

    private boolean removeIf(Predicate<? super Map.Entry<String, Map<String, Object>>> filter) {
        Objects.requireNonNull(filter);
        boolean removed = false;
        final Iterator<Map.Entry<String, Map<String, Object>>> requestEntryIterator = requests.entrySet().iterator();
        while (requestEntryIterator.hasNext()) {
            final Map.Entry<String, Map<String, Object>> item = requestEntryIterator.next();
            if (filter.test(item)) {
                requestEntryIterator.remove();
                try {
                    Session session = diameter.getSession(((Request) item.getValue().get("request")).getSessionId());
                    session.release();
                } catch (Exception e) {
                    logger.error("Exception caught while sessions release! ", e);
                }
                removed = true;
            }
        }
        return removed;
    }

    public static void addRequest(Request request) {
        String key = getKey(request.getSessionId(), request.getEndToEndIdentifier());
        if (!requests.containsKey(key))
            requests.put(key, new HashMap<String, Object>() {{
                put("request", request);
                put("dateExpire", System.currentTimeMillis() + EXPIRE_AFTER);
            }});
    }

    public static Request getRequest(String sessionId, long endToEndIdentifier) {
        Request request = null;
        String key = getKey(sessionId, endToEndIdentifier);
        if (requests.containsKey(key)) {
            //request = (Request) requests.remove(sessionId).get("request");
            request = (Request) requests.get(key).get("request");
            if (request == null) {
                logger.error(String.format("Recovered null value for sessionId '%s' from request cache", sessionId));
            }
        } else {
            logger.error(String.format("Unable to find sessionId '%s' in request cache!", sessionId));
        }

        return request;
    }

    private static String getKey(String sessionId, Long endToEndIdentifier) {
        return String.format("%s:%s", sessionId, endToEndIdentifier);
    }
}