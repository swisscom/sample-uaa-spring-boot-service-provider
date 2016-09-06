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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.*;

@SpringBootApplication
@Controller
public class Oauth2SampleApplication {
    @Autowired
    private Environment env;

    @RequestMapping("/")
    @ResponseBody
    public String home(Principal user)  {
        // print user info to show what's available to the app (firstname, lastname, ...)
        return userInfoAsJson(user) + "<hr/> <a href=\"/logout\">Logout</a>";
    }

    private String userInfoAsJson(Principal user) {
        Authentication authentication = ((OAuth2Authentication) user).getUserAuthentication();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(authentication);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // for logout, we clear the local session and then redirect to the UAA's logout endpoint.
    // The UAA will then initiate its logout (i.e. by triggering SAML SLO) and redirect us back to the URL on our side
    // which we specified with the redirect= query param.
    @RequestMapping(value="/logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        destroyLocalSession(request, response);
        return "redirect:" + buildLogoutRedirectUrl();
    }

    private void destroyLocalSession(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null){
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
    }

    private String buildLogoutRedirectUrl() {
        String logoutEndpoint = env.getProperty("sample.oauth2.logoutEndpoint");
        String clientId = env.getProperty("security.oauth2.client.clientId");
        String afterLogoutRedirectUrl = env.getProperty("sample.oauth2.afterLogoutRedirectUrl");

        try {
            return logoutEndpoint + "?redirect=" + URLEncoder.encode(afterLogoutRedirectUrl, "UTF-8") +
                    "&client_id=" + URLEncoder.encode(clientId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping("/logged_out")
    @ResponseBody
    public String loggedOut() {
        return "Logged out. <a href=\"/\">Log in again</a>";
    }

    public static void main(String[] args) {
        String vcapServices = System.getenv().get("VCAP_SERVICES");
        String afterLogoutRedirectUrl = System.getenv().get("AFTER_LOGOUT_URL");
        Optional<Map<String, Object>> maybeCredentials = parseOAuth2Credentials(vcapServices);
        System.out.println(maybeCredentials.orElseThrow(() -> new RuntimeException("Oauth2 credentials not found in VCAP_SERVICES")));

        Map<String, Object> credentials = maybeCredentials.get();
        HashMap<String, Object> props = new HashMap<>();

        props.put("security.oauth2.client.clientId", credentials.get("clientId"));
        props.put("security.oauth2.client.clientSecret", credentials.get("clientSecret"));
        props.put("security.oauth2.client.accessTokenUri", credentials.get("tokenEndpoint"));
        props.put("security.oauth2.client.userAuthorizationUri", credentials.get("authorizationEndpoint"));
        props.put("security.oauth2.resource.userInfoUri", credentials.get("userInfoEndpoint"));
        props.put("sample.oauth2.logoutEndpoint", credentials.get("logoutEndpoint"));
        props.put("sample.oauth2.afterLogoutRedirectUrl", afterLogoutRedirectUrl);
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
