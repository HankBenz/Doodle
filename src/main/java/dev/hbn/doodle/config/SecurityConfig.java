package dev.hbn.doodle.config;

import java.util.Set;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class SecurityConfig {

	static final String REGISTRATION_ID = "keycloak";

	@Bean
	ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
		return new InMemoryClientRegistrationRepository(keycloakRegistration(properties));
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.cors(Customizer.withDefaults())
			.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
			.oauth2Login(login -> login.defaultSuccessUrl("/swagger-ui.html", true))
			.logout(logout -> logout.logoutSuccessUrl("/"))
			.build();
	}

	private static ClientRegistration keycloakRegistration(OAuth2ClientProperties properties) {
		OAuth2ClientProperties.Registration registration = properties.getRegistration().get(REGISTRATION_ID);
		OAuth2ClientProperties.Provider provider = properties.getProvider().get(REGISTRATION_ID);
		if (registration == null || provider == null || !StringUtils.hasText(provider.getIssuerUri())) {
			throw new IllegalStateException(
					"Set spring.security.oauth2.client registration and provider '" + REGISTRATION_ID + "'");
		}

		String issuer = StringUtils.trimTrailingCharacter(provider.getIssuerUri(), '/');
		String authorizationUri = endpoint(provider.getAuthorizationUri(), issuer, "/protocol/openid-connect/auth");
		String tokenUri = endpoint(provider.getTokenUri(), issuer, "/protocol/openid-connect/token");
		String jwkSetUri = endpoint(provider.getJwkSetUri(), issuer, "/protocol/openid-connect/certs");
		String userInfoUri = endpoint(provider.getUserInfoUri(), issuer, "/protocol/openid-connect/userinfo");
		String userNameAttribute = StringUtils.hasText(provider.getUserNameAttribute())
				? provider.getUserNameAttribute() : "preferred_username";
		String clientName = StringUtils.hasText(registration.getClientName())
				? registration.getClientName() : REGISTRATION_ID;
		String redirectUri = StringUtils.hasText(registration.getRedirectUri())
				? registration.getRedirectUri() : "{baseUrl}/login/oauth2/code/{registrationId}";

		return ClientRegistration.withRegistrationId(REGISTRATION_ID)
			.clientId(registration.getClientId())
			.clientSecret(registration.getClientSecret())
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri(redirectUri)
			.scope(registration.getScope() == null ? Set.of("openid") : registration.getScope())
			.issuerUri(issuer)
			.authorizationUri(authorizationUri)
			.tokenUri(tokenUri)
			.jwkSetUri(jwkSetUri)
			.userInfoUri(userInfoUri)
			.userInfoAuthenticationMethod(AuthenticationMethod.HEADER)
			.userNameAttributeName(userNameAttribute)
			.clientName(clientName)
			.build();
	}

	private static String endpoint(String configured, String issuer, String path) {
		return StringUtils.hasText(configured) ? configured : issuer + path;
	}

}
