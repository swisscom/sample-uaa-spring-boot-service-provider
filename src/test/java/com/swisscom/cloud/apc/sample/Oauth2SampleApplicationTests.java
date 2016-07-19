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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;
import java.util.Optional;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Oauth2SampleApplication.class)
@WebAppConfiguration
public class Oauth2SampleApplicationTests {
	@Test
	public void credentialsWithTagsAreParsed(){
		//given
		String credentials = "{\n" +
				"\t\"cf-sso\": [{\n" +
				"\t\t\"name\": \"uaa1\",\n" +
				"\t\t\"label\": \"cf-sso\",\n" +
				"\t\t\"tags\": [\"oauth2\"],\n" +
				"\t\t\"plan\": \"usage\",\n" +
				"\t\t\"credentials\": {\n" +
				"\t\t\t\"clientId\": \"clientId\",\n" +
				"\t\t\t\"clientSecret\": \"clientSecret\",\n" +
				"\t\t\t\"tokenEndpoint\": \"https://cf-sso-uaa.bosh-lite.com/oauth/token\",\n" +
				"\t\t\t\"authorizationEndpoint\": \"https://cf-sso-uaa.bosh-lite.com/oauth/authorize\",\n" +
				"\t\t\t\"userInfoEndpoint\": \"https://cf-sso-uaa.bosh-lite.com/userinfo\",\n" +
				"\t\t\t\"redirectUris\": \"null\",\n" +
				"\t\t\t\"grantTypes\": \"authorization_code,refresh_token\"\n" +
				"\t\t}\n" +
				"\t}]\n" +
				"}";

		//when
		Optional<Map<String,Object>> parsed = Oauth2SampleApplication.parseOAuth2Credentials(credentials);

		//then
		Assert.assertTrue(parsed.isPresent());
		Assert.assertEquals("clientId", parsed.get().get("clientId"));
		Assert.assertEquals("clientSecret", parsed.get().get("clientSecret"));
	}

    @Test
    public void credentialsWithoutTagsAreParsed(){
        //given
        String credentials = "{\n" +
                "\t\"user-provided\": [{\n" +
                "\t\t\"credentials\": {\n" +
                "\t\t\t\"clientId\": \"clientId\",\n" +
                "\t\t\t\"clientSecret\": \"clientSecret\",\n" +
                "\t\t\t\"tokenEndpoint\": \"https://cf-sso-uaa.bosh-lite.com/oauth/token\",\n" +
                "\t\t\t\"authorizationEndpoint\": \"https://cf-sso-uaa.bosh-lite.com/oauth/authorize\",\n" +
                "\t\t\t\"userInfoEndpoint\": \"https://cf-sso-uaa.bosh-lite.com/userinfo\",\n" +
                "\t\t\t\"redirectUris\": \"null\",\n" +
                "\t\t\t\"grantTypes\": \"authorization_code,refresh_token\"\n" +
                "\t\t}\n" +
                "\t}]\n" +
                "}";

        //when
        Optional<Map<String,Object>> parsed = Oauth2SampleApplication.parseOAuth2Credentials(credentials);

        //then
        Assert.assertTrue(parsed.isPresent());
        Assert.assertEquals("clientId", parsed.get().get("clientId"));
        Assert.assertEquals("clientSecret", parsed.get().get("clientSecret"));
    }
}
