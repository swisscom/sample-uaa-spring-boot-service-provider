/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.swisscom.cloud.apc.sample;

import com.jayway.jsonpath.JsonPath;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.*;

@SpringBootApplication
@EnableOAuth2Sso
@RestController
public class Oauth2SampleApplication {

    @RequestMapping("/")
    @ResponseBody
    Authentication home(Principal user) {
        // print user info to show what's available to the app (firstname, lastname, ...)
        return ((OAuth2Authentication) user).getUserAuthentication();
    }

    public static void main(String[] args) {
        String vcapServices = System.getenv().get("VCAP_SERVICES");
        Optional<Map<String, Object>> maybeCredentials = parseOAuth2Credentials(vcapServices);
        System.out.println(maybeCredentials.orElseThrow(() -> new RuntimeException("Oauth2 credentials not found in VCAP_SERVICES")));

        Map<String, Object> credentials = maybeCredentials.get();
        HashMap<String, Object> props = new HashMap<>();

        props.put("security.oauth2.client.clientId", credentials.get("clientId"));
        props.put("security.oauth2.client.clientSecret", credentials.get("clientSecret"));
        props.put("security.oauth2.client.accessTokenUri", credentials.get("tokenEndpoint"));
        props.put("security.oauth2.client.userAuthorizationUri", credentials.get("authorizationEndpoint"));
        props.put("security.oauth2.resource.userInfoUri", credentials.get("userInfoEndpoint"));
        props.put("security.resources.chain.enabled", true);


        new SpringApplicationBuilder()
                .sources(Oauth2SampleApplication.class)
                .properties(props)
                .run(args);

    }

    public static Optional<Map<String, Object>> parseOAuth2Credentials(String vcapServices) {
        if (vcapServices != null) {
            List<Map<String, Object>> services = JsonPath.parse(vcapServices)
                    .read("$.*.[?(@.credentials)]", List.class);

            return services.stream().filter(o -> {
                Collection<String> tags = (Collection<String>) o.get("tags");
                Collection<String> credentialKeys =  ((Map<String, Object>) o.get("credentials")).keySet();
                return (tags != null && tags.contains("oauth2")) || credentialKeys.contains("authorizationEndpoint");
            }).findFirst().map(t -> (Map<String, Object>)t.get("credentials"));

        }
        return Optional.empty();
    }
}
