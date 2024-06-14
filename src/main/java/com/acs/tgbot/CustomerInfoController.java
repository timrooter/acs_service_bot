package com.acs.tgbot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import static com.acs.tgbot.SwaggerConfig.BEARER_KEY_SECURITY_SCHEME;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CustomerInfoController {

    @Autowired
    private TelegramBotService telegramBotService;

    @Autowired
    private BotConfig botConfig;

    @PostMapping("/process")
    @Operation(security = {@SecurityRequirement(name = BEARER_KEY_SECURITY_SCHEME)})

    public String processCustomerInfo(@RequestBody List<Map<String, Object>> customerInfoList) {
        for (Map<String, Object> info : customerInfoList) {
            String message = formatMessage(info);
            telegramBotService.sendMessage(botConfig.getAdminId(), message);
        }
        return "Processed";
    }

    private String formatMessage(Map<String, Object> info) {
        StringBuilder message = new StringBuilder();
        info.forEach((key, value) -> message.append(key).append(": ").append(value).append("\n"));
        return message.toString();
    }
}
