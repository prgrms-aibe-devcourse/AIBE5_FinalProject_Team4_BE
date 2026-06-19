package com.closetnangam.be.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

class OAuth2ClientPropertiesTest {

    @Test
    void kakaoUsesPostClientAuthenticationMethodInTestProfile() {
        OAuth2ClientProperties properties = loadOAuth2ClientProperties(
                yamlProperties(new ClassPathResource("application-test.yml")));

        assertKakaoClientAuthenticationMethod(properties);
    }

    @Test
    void kakaoUsesPostClientAuthenticationMethodInProdTemplate() {
        OAuth2ClientProperties properties = loadOAuth2ClientProperties(
                yamlProperties(new FileSystemResource("src/main/resources/application-prod.yml.example")));

        assertKakaoClientAuthenticationMethod(properties);
    }

    private void assertKakaoClientAuthenticationMethod(OAuth2ClientProperties properties) {
        OAuth2ClientProperties.Registration kakao = properties.getRegistration().get("kakao");

        assertThat(kakao).isNotNull();
        assertThat(kakao.getClientAuthenticationMethod())
                .isEqualTo("client_secret_post");
    }

    private OAuth2ClientProperties loadOAuth2ClientProperties(Properties properties) {
        Map<String, String> source = properties.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> String.valueOf(entry.getValue())
                ));

        return new Binder(new MapConfigurationPropertySource(source))
                .bind("spring.security.oauth2.client", OAuth2ClientProperties.class)
                .get();
    }

    private Properties yamlProperties(org.springframework.core.io.Resource resource) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(resource);
        return yaml.getObject();
    }
}
