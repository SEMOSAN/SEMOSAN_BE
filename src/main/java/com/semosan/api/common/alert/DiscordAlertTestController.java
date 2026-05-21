package com.semosan.api.common.alert;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
public class DiscordAlertTestController {

    @GetMapping("/test/discord-alert")
    public void testDiscordAlert() {
        throw new NullPointerException("discord alert test");
    }
}
