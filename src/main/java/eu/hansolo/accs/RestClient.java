/*
 * Copyright (c) 2016 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.accs;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * Created by hansolo on 15.06.16.
 */
public enum RestClient {
    INSTANCE;
    private static final Optional<String> URL = Optional.ofNullable(System.getenv("URL"));
    private              OkHttpClient client;


    // ******************** Constructors **************************************
    RestClient() {
        client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
                                           .writeTimeout(10, TimeUnit.SECONDS)
                                           .readTimeout(30, TimeUnit.SECONDS)
                                           .build();
    }


    // ******************** Public Methods ************************************
    public JSONArray getLocations() {
        if (!URL.isPresent()) return new JSONArray();
        return getJSONArray(URL.get() + "/locations");
    }


    // ******************** Private Methods ***********************************
    private JSONArray getJSONArray(final String URL) {
        try {
            Request   getRequest  = new Request.Builder().url(URL).build();
            Response  getResponse = client.newCall(getRequest).execute();
            JSONArray jsonArray   = (JSONArray) JSONValue.parse(getResponse.body().string());
            return jsonArray;
        } catch (IOException e){

        }
        return new JSONArray();
    }
}
