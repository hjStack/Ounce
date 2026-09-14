package ounce.market.demo.common.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageUrlResolver {

    @Value("${app.cdn.base-url}")
    private String cdnBaseUrl;   // https://<배포ID>.cloudfront.net


    public String toUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        if (key.startsWith("http://") || key.startsWith("https://")) {
            return key;
        }

        return cdnBaseUrl.replaceAll("/$", "") + "/" + key.replaceFirst("^/", "");
    }

}