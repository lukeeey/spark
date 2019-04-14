/*
 * This file is part of socketmaster, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.spark.util;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.io.IOException;

/**
 * Utility for interacting with socketmaster
 */
public class SocketmasterClient extends AbstractHttpClient {

    private static final MediaType PLAIN_TYPE = MediaType.parse("text/plain; charset=utf-8");

    /** The socketmaster host */
    private final String host;
    /** The client user agent */
    private final String userAgent;

    /**
     * Creates a new socketmaster client instance
     *
     * @param host the socketmaster host
     * @param userAgent the client user agent string
     */
    public SocketmasterClient(OkHttpClient okHttpClient, String host, String userAgent) {
        super(okHttpClient);
        this.host = host;
        this.userAgent = userAgent;
    }

    public WebSocket newSocket(WebSocketListener listener) throws IOException {
        Request createRequest = new Request.Builder()
                .url("https://" + this.host + "/create")
                .header("User-Agent", this.userAgent)
                .post(RequestBody.create(PLAIN_TYPE, new byte[0]))
                .build();

        String websocketAddress;
        try (Response response = makeHttpRequest(createRequest)) {
            websocketAddress = response.header("Location");
        }

        if (websocketAddress == null) {
            throw new IllegalStateException("No address returned from initial request");
        }

        return connect(websocketAddress, listener);
    }

    public WebSocket connect(String address, WebSocketListener listener) {
        Request websocketRequest = new Request.Builder()
                .url(address)
                .header("User-Agent", this.userAgent)
                .build();

        return connect(websocketRequest, listener);
    }

    public WebSocket connect(Request request, WebSocketListener listener) {
        return this.okHttp.newWebSocket(request, listener);
    }

}
