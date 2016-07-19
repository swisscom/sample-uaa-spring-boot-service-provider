# Sample OAuth2 spring boot app for CloudFoundry
Demo app that authenticates its users against an OAuth2 provider.
It uses Spring boot and Spring security (based on https://github.com/spring-guides/tut-spring-boot-oauth2).
It is primarily designed to run on CloudFoundry, since it expects the OAuth2 configuration to be passed in `VCAP_SERVICES.

## How it works
When the app is accessed by an unauthenticated user, it redirects the user to the OAuth2 provider (standard OAuth2 `Authorization code` flow).
When the user logs in on the OAuth2 provider, he is redirected back to the app.
The app completes the OAuth2 flow and receives the user's info (using the Userinfo endpoint), i.e. its attributes like firstname, lastname.

It was tested with [UAA](https://github.com/cloudfoundry/uaa) acting as OAuth2 provider, but should work fine with other providers (i.e. Facebook, Github etc.).

## Configure, deploy and test
The app expects a `VCAP_SERVICES` env variable containing a service with a tag `oauth2` or a `credentials` with a key `authorizationEndpointUrl`,
so make sure it is set when running the app.

When deploying to CloudFoundry, one can use a [User provided service instance](https://docs.cloudfoundry.org/devguide/services/user-provided.html) to achieve this.

Full example:
```
# compile & push the app
mvn clean package
cf push oauth2-simple --random-route --no-start -p target/oauth2-simple-0.0.1-SNAPSHOT.jar

# Now create the user provided service which will be provided to the app in VCAP_SERVICES.
# The client specified here must be created manually beforehand on the OAuth2 provider.
CREDENTIALS='{"authorizationEndpoint": "<uaa-url>/oauth/authorize", "tokenEndpoint": "<uaa-url>/oauth/token", "userInfoEndpoint": "<uaa-url>/userinfo", "clientId": "<client-id>", "clientSecret": "<client-secret>"}'
cf create-user-provided-service OAUTH2-CLIENT -p $CREDENTIALS

# Bind & start the app to make the service instance available
cf bind-service oauth2-simple OAUTH2-CLIENT
cf start oauth2-simple
```

Now access the app in your browser, which redirects you to the UAA/OAuth2 provider you specified in the configuration.
After login, the user attributes passed to the app will be shown:
```
{
  "details":
  {
    "user_id":"e814ddc8-1b28-422f-b34a-1aa0c97beb31",
    "user_name":"jdoe",
    "given_name":"John",
    "family_name":"Doe",
    "email":"john.doe@example.com",
    "name":"John Doe"
  },
  "authorities":[{"authority":"ROLE_USER"}],
  "authenticated":true,
  "principal":"e814ddc8-1b28-422f-b34a-1aa0c97beb31",
  "credentials":"N/A",
  "name":"e814ddc8-1b28-422f-b34a-1aa0c97beb31"
}
```

## Other applications
A similar example for Ruby applications can be found here:
https://github.com/cloudfoundry/omniauth-uaa-oauth2/blob/master/examples/config.ru