package com.acs.tgbot;

import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotService.class);

    private final BotConfig botConfig;
    private final ResponsibleRepository responsibleRepository;
    private final ModeratorRepository moderatorRepository;
    private final SubscriptionRepository subscriptionRepository;

    @PostConstruct
    public void init() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(this);
        } catch (TelegramApiException e) {
            logger.error("Error registering bot: ", e);
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            if (messageText.startsWith("/setresponsible")) {
                handleSetResponsibleCommand(update.getMessage());
            } else if (messageText.startsWith("/unsetresponsible")) {
                handleUnsetResponsibleCommand(update.getMessage());
            } else if (messageText.startsWith("/setmoderator")) {
                handleSetModeratorCommand(update.getMessage());
            } else if (messageText.startsWith("/unsetmoderator")) {
                handleUnsetModeratorCommand(update.getMessage());
            } else if (messageText.startsWith("/subscribe")) {
                handleSubscribeCommand(update.getMessage());
            } else if (messageText.startsWith("/unsubscribe")) {
                handleUnsubscribeCommand(update.getMessage());
            } else if (messageText.startsWith("/getmyid")) {
                handleGetMyIdCommand(update.getMessage());
            } else if (messageText.startsWith("/help")) {
                handleHelpCommand(update.getMessage());
            }
        }
    }

    private boolean isAdmin(String telegramId) {
        return telegramId.equals(botConfig.getAdminId());
    }

    private boolean isModeratorOrAdmin(String telegramId) {
        return isAdmin(telegramId) || moderatorRepository.findByTelegramId(telegramId) != null;
    }

    private void handleSetResponsibleCommand(Message message) {
        String userTelegramId = getUserIdFromMessage(message);
        if (!isModeratorOrAdmin(userTelegramId)) {
            sendMessage(message.getChatId().toString(), escapeMarkdown("Только администратор или модератор могут назначать ответственных."));
            return;
        }

        String[] parts = message.getText().split(" ", 3);
        if (parts.length == 3) {
            String customer = parts[1].toUpperCase().trim();
            String responsible = parts[2];
            Responsible existingResponsible = responsibleRepository.findByCustomer(customer);
            if (existingResponsible == null) {
                Responsible newResponsible = new Responsible();
                newResponsible.setCustomer(customer);
                newResponsible.setResponsible(responsible);
                responsibleRepository.save(newResponsible);
            } else {
                existingResponsible.setResponsible(responsible);
                responsibleRepository.save(existingResponsible);
            }
            sendMessage(message.getChatId().toString(), escapeMarkdown("Ответственный за " + parts[1] + " установлен как " + responsible));
        } else {
            sendMessage(message.getChatId().toString(), escapeMarkdown("Неверный формат команды. Используйте /setresponsible {Заказчик} {Тег_Ответственного_В_Телеграм}"));
        }
    }

    private void handleUnsetResponsibleCommand(Message message) {
        String userTelegramId = getUserIdFromMessage(message);
        if (!isModeratorOrAdmin(userTelegramId)) {
            sendMessage(message.getChatId().toString(), escapeMarkdown("Только администратор или модератор могут удалять ответственных."));
            return;
        }

        String[] parts = message.getText().split(" ", 2);
        if (parts.length == 2) {
            String customer = parts[1].toUpperCase().trim();
            Responsible existingResponsible = responsibleRepository.findByCustomer(customer);
            if (existingResponsible != null) {
                responsibleRepository.delete(existingResponsible);
                sendMessage(message.getChatId().toString(), escapeMarkdown("Ответственный за " + parts[1] + " удален."));
            } else {
                sendMessage(message.getChatId().toString(), escapeMarkdown("Ответственный за " + parts[1] + " не найден."));
            }
        } else {
            sendMessage(message.getChatId().toString(), escapeMarkdown("Неверный формат команды. Используйте /unsetresponsible {Заказчик}"));
        }
    }

    private void handleSetModeratorCommand(Message message) {
        String userTelegramId = getUserIdFromMessage(message);
        if (!isAdmin(userTelegramId)) {
            sendMessage(message.getChatId().toString(), escapeMarkdown("Только администратор может назначать модераторов."));
            return;
        }

        String[] parts = message.getText().split(" ", 2);
        if (parts.length == 2) {
            String telegramId = parts[1];
            if (moderatorRepository.findByTelegramId(telegramId) == null) {
                Moderator moderator = new Moderator();
                moderator.setTelegramId(telegramId);
                moderatorRepository.save(moderator);
                sendMessage(message.getChatId().toString(), escapeMarkdown("Модератор " + telegramId + " добавлен."));
            } else {
                sendMessage(message.getChatId().toString(), escapeMarkdown("Модератор " + telegramId + " уже существует."));
            }
        } else {
            sendMessage(message.getChatId().toString(), escapeMarkdown("Неверный формат команды. Используйте /setmoderator {Тег_Модератора_В_Телеграм}"));
        }
    }

    private void handleUnsetModeratorCommand(Message message) {
        String userTelegramId = getUserIdFromMessage(message);
        if (!isAdmin(userTelegramId)) {
            sendMessage(message.getChatId().toString(), escapeMarkdown("Только администратор может удалять модераторов."));
            return;
        }

        String[] parts = message.getText().split(" ", 2);
        if (parts.length == 2) {
            String telegramId = parts[1];
            Moderator existingModerator = moderatorRepository.findByTelegramId(telegramId);
            if (existingModerator != null) {
                moderatorRepository.delete(existingModerator);
                sendMessage(message.getChatId().toString(), escapeMarkdown("Модератор " + telegramId + " удален."));
            } else {
                sendMessage(message.getChatId().toString(), escapeMarkdown("Модератор " + telegramId + " не найден."));
            }
        } else {
            sendMessage(message.getChatId().toString(), escapeMarkdown("Неверный формат команды. Используйте /unsetmoderator {Тег_Модератора_В_Телеграм}"));
        }
    }

    private void handleSubscribeCommand(Message message) {
        String[] parts = message.getText().split(" ", 2);
        if (parts.length == 2) {
            String customer = parts[1].toUpperCase().trim();
            String telegramId = getUserIdFromMessage(message);
            if (subscriptionRepository.findByCustomerAndTelegramId(customer, telegramId) == null) {
                Subscription subscription = new Subscription();
                subscription.setCustomer(customer);
                subscription.setTelegramId(telegramId);
                subscriptionRepository.save(subscription);
                sendMessage(message.getChatId().toString(), escapeMarkdown("Вы подписаны на " + customer));
            } else {
                sendMessage(message.getChatId().toString(), escapeMarkdown("Вы уже подписаны на " + customer));
            }
        } else {
            sendMessage(message.getChatId().toString(), escapeMarkdown("Неверный формат команды. Используйте /subscribe {Заказчик}"));
        }
    }

    private void handleUnsubscribeCommand(Message message) {
        String[] parts = message.getText().split(" ", 2);
        if (parts.length == 2) {
            String customer = parts[1].toUpperCase().trim();
            String telegramId = getUserIdFromMessage(message);
            Subscription subscription = subscriptionRepository.findByCustomerAndTelegramId(customer, telegramId);
            if (subscription != null) {
                subscriptionRepository.delete(subscription);
                sendMessage(message.getChatId().toString(), escapeMarkdown("Вы отписаны от " + customer));
            } else {
                sendMessage(message.getChatId().toString(), escapeMarkdown("Вы не подписаны на " + customer));
            }
        } else {
            sendMessage(message.getChatId().toString(), escapeMarkdown("Неверный формат команды. Используйте /unsubscribe {Заказчик}"));
        }
    }

    private void handleGetMyIdCommand(Message message) {
        String userTelegramId = getUserIdFromMessage(message);
        sendMessage(message.getChatId().toString(), escapeMarkdown("Ваш числовой ID: " + userTelegramId));
    }

    private void handleHelpCommand(Message message) {
        String helpText = "Доступные команды:\n" +
                "/setresponsible {Заказчик} {Тег_Ответственного_В_Телеграм} - Назначить ответственного\n" +
                "/unsetresponsible {Заказчик} - Удалить ответственного\n" +
                "/setmoderator {Тег_Модератора_В_Телеграм} - Назначить модератора (только для администратора)\n" +
                "/unsetmoderator {Тег_Модератора_В_Телеграм} - Удалить модератора (только для администратора)\n" +
                "/subscribe {Заказчик} - Подписаться на заказчика\n" +
                "/unsubscribe {Заказчик} - Отписаться от заказчика\n" +
                "/getmyid - Получить ваш числовой ID\n" +
                "/help - Показать это сообщение";
        sendMessage(message.getChatId().toString(), escapeMarkdown(helpText));
    }


    private String getUserIdFromMessage(Message message) {
        return message.getFrom().getId().toString();
    }

    public void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode(ParseMode.MARKDOWNV2); // Установите режим разметки MarkdownV2
        message.setDisableWebPagePreview(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message: ", e);
        }
    }

    public void sendCustomerMessage(Map<String, Object> customerInfo) {
        StringBuilder messageText = new StringBuilder();
        String customer = ((String) customerInfo.get("Заказчик")).toUpperCase().trim();
        String geo = (String) customerInfo.get("GEO");
        int row = (int) customerInfo.get("Строка");
        String documentLink = (String) customerInfo.get("Cсылка на документ");
        String deadline = customerInfo.get("Дедлайн по исправлениям ПМ").toString();

        // Проверка и форматирование даты
        String formattedDeadline = formatDeadline(deadline);

        // Добавление жирного шрифта к первой строке
        messageText.append("*")
                .append(escapeMarkdown("\uD83C\uDFA7 " + customer + " " + geo + " Строка: " + row))
                .append("*\n")
                .append(escapeMarkdown("\uD83D\uDED1 " + formattedDeadline))
                .append("\n");

        Responsible responsible = responsibleRepository.findByCustomer(customer);
        if (responsible != null) {
            messageText.append(escapeMarkdown("Ответственный: @"))
                    .append(escapeMarkdown(responsible.getResponsible()))
                    .append("\n");
        }
        if (documentLink != null && !documentLink.isEmpty()) {
            messageText.append(escapeMarkdown("Док: "))
                    .append(escapeMarkdown(documentLink))
                    .append("\n");
        }

        String finalMessage = messageText.toString();

        List<String> moderatorChatIds = moderatorRepository.findAll().stream()
                .map(Moderator::getTelegramId)
                .collect(Collectors.toList());
        for (String chatId : moderatorChatIds) {
            sendMessage(chatId, finalMessage);
        }

        // Отправка сообщения подписчикам
        List<Subscription> subscriptions = subscriptionRepository.findByCustomer(customer);
        for (Subscription subscription : subscriptions) {
            sendMessage(subscription.getTelegramId(), finalMessage);
        }

        // Отправка сообщения в группу
        String groupId = botConfig.getGroupId(); // Замените на ID вашей группы
        sendMessage(groupId, finalMessage);
    }

    private String escapeMarkdown(String text) {
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    private String formatDeadline(String deadline) {
        if (deadline == null || deadline.isEmpty() || deadline.length() < 10) {
            return "Неверная дата";
        }
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        SimpleDateFormat outputFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));
        try {
            Date date = inputFormat.parse(deadline);
            return outputFormat.format(date);
        } catch (ParseException e) {
            logger.error("Error parsing date: ", e);
            return "Неверная дата";
        }
    }
}
