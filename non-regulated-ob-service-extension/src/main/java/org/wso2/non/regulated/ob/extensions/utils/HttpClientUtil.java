/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com). All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.non.regulated.ob.extensions.utils;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Minimal HTTP GET client for fetching account data from the banking backend.
 */
public class HttpClientUtil {

    /**
     * Fetches account data from the banking backend.
     *
     * @param accountsRetrieveUrl the endpoint to call
     * @param parameters          query parameters
     * @param headers             extra headers
     * @return the response body, or null if not found
     * @throws IOException        on a request failure
     * @throws URISyntaxException on an invalid URL
     */
    public static String getAccountsFromEndpoint(String accountsRetrieveUrl, Map<String, String> parameters,
                                                   Map<String, String> headers) throws IOException,
            URISyntaxException {

        String retrieveUrl = trimTrailingSlash(accountsRetrieveUrl);
        HttpGet request = buildAccountsRequest(retrieveUrl, parameters, headers);

        try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(request)) {
            if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            return readResponseBody(response);
        }
    }

    /**
     * Removes a trailing slash from a URL.
     *
     * @param url the URL to normalise
     * @return the URL without a trailing slash
     */
    private static String trimTrailingSlash(String url) {

        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * Builds the accounts GET request.
     *
     * @param retrieveUrl the request URL
     * @param parameters  query parameters
     * @param headers     extra headers
     * @return the request
     * @throws URISyntaxException on an invalid URL
     */
    private static HttpGet buildAccountsRequest(String retrieveUrl, Map<String, String> parameters,
                                                 Map<String, String> headers) throws URISyntaxException {

        URIBuilder uriBuilder = new URIBuilder(retrieveUrl);
        parameters.forEach((key, value) -> {
            if (key != null && value != null) {
                uriBuilder.addParameter(key, value);
            }
        });

        HttpGet request = new HttpGet(uriBuilder.build().toString());
        request.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        if (headers == null) {
            return request;
        }

        headers.forEach((key, value) -> {
            if (key != null && value != null) {
                request.addHeader(key, value);
            }
        });
        return request;
    }

    /**
     * Reads a response body as text.
     *
     * @param response the response to read
     * @return the body as a string
     * @throws IOException on a read failure
     */
    private static String readResponseBody(CloseableHttpResponse response) throws IOException {
        return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    }
}
