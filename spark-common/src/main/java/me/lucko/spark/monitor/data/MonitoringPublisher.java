/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.monitor.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.util.BytebinClient;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

public class MonitoringPublisher<S> implements DataListener {
    private static final Gson GSON = new Gson();
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final SparkPlatform<S> platform;
    private final MonitoringManager monitoringManager;

    private WebSocket socket = null;
    private BytebinClient.Content payload = null;

    public MonitoringPublisher(SparkPlatform<S> platform, MonitoringManager monitoringManager) {
        this.platform = platform;
        this.monitoringManager = monitoringManager;
        this.monitoringManager.addDataListener(this);
    }

    @Override
    public void onDataCollection(long time, JsonObject data) {
        WebSocket socket = this.socket;
        if (socket == null) {
            return;
        }
        this.platform.runAsync(() -> {
            JsonObject payload = new JsonObject();
            payload.add("time", new JsonPrimitive(time));
            payload.add("data", data);

            socket.send(GSON.toJson(payload));
        });
    }

    public String publish() throws IOException {
        WebSocket socket = this.socket;
        if (socket == null) {
            throw new IllegalStateException("Socket not setup");
        }

        JsonObject payload = new JsonObject();
        payload.add("type", new JsonPrimitive("monitoring"));
        payload.add("data", this.monitoringManager.export());
        payload.add("socket", new JsonPrimitive(socket.request().url().toString().replace("https://", "wss://")));

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(byteOut), StandardCharsets.UTF_8)) {
            GSON.toJson(payload, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] buf = byteOut.toByteArray();

        if (this.payload != null) {
            SparkPlatform.BYTEBIN_CLIENT.modifyContent(this.payload, buf, JSON_TYPE);
        } else {
            this.payload = SparkPlatform.BYTEBIN_CLIENT.postContent(buf, JSON_TYPE, true);
        }

        return this.payload.key();
    }

    public void ensureSocketSetup(S sender) throws IOException {
        if (this.socket == null) {
            if (sender != null) {
                this.platform.sendPrefixedMessage(sender, "&7Creating a new socket, please wait...");
            }

            try {
                this.socket = SparkPlatform.SOCKETMASTER_CLIENT.newSocket(new SocketListener());
            } catch (IOException e) {
                if (sender != null) {
                    this.platform.sendPrefixedMessage(sender, "&cAn error occurred whilst creating a new socket.");
                }
                e.printStackTrace();
                throw e;
            }
        }
    }

    private void closeSocket() {
        if (this.socket != null) {
            try {
                this.socket.close(1001, "error previously occurred");
            } catch (Exception e) {
                // ignored
            }
            this.socket = null;
        }
    }

    private void attemptReconnection() {
        if (this.socket != null) {
            Request initialRequest = this.socket.request();
            this.socket = SparkPlatform.SOCKETMASTER_CLIENT.connect(initialRequest, new SocketListener());
        }
    }

    private final class SocketListener extends WebSocketListener {
        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            new SocketException("Socket closing - " + code + " - " + reason).printStackTrace();
            closeSocket();
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            new SocketException("Socket closed - " + code + " - " + reason).printStackTrace();
            closeSocket();
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            new SocketException("Socket failure - attempting to reconnect", t).printStackTrace();
            attemptReconnection();
        }
    }

    public static final class SocketException extends RuntimeException {
        SocketException(String message) {
            super(message);
        }

        SocketException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
