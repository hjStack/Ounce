package ounce.market.demo.common.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Issues the CSRF cookie used by the browser frontend. */
@Hidden
@RestController
@RequestMapping("/api/csrf")
public class CsrfController {

    @GetMapping
    public void csrf(CsrfToken csrfToken) {
        // Resolving the token causes CookieCsrfTokenRepository to persist it.
        csrfToken.getToken();
    }
}
