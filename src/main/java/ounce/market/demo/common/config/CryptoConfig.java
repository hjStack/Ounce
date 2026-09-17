package ounce.market.demo.common.config;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

@Configuration
public class CryptoConfig {

    @PostConstruct
    public void registerBouncyCastle() {
        Security.addProvider(new BouncyCastleProvider());
    }
}