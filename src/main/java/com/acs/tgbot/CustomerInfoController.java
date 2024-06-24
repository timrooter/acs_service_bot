package com.acs.tgbot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Customer Info Controller", description = "API для управления информацией о клиентах")
public class CustomerInfoController {

    @Autowired
    private TelegramBotService telegramBotService;

    @Autowired
    private BotConfig botConfig;

    @PostMapping("/process")
    @Operation(summary = "Process customer info and send messages via Telegram bot")
    public String processCustomerInfo(@RequestBody List<Map<String, Object>> customerInfoList) {
        for (Map<String, Object> info : customerInfoList) {
            telegramBotService.sendCustomerMessage(info);
        }
        return "Processed";
    }
}
