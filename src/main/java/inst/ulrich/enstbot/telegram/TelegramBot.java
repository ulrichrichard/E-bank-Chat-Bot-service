package inst.ulrich.enstbot.telegram;

import inst.ulrich.enstbot.agents.AIAgent;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Value("${telegram.api.key}")
    private String telegramBotToken;
    private AIAgent aiAgent;

    public TelegramBot(AIAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    // Méthode qui s'exécute juste après le constructeur
    @PostConstruct
    public void registerTelegramBot() {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update telegramRequest) {
        try {
            // Vérifier que la requête contient bien un message avec du texte
            if (!telegramRequest.hasMessage() || !telegramRequest.getMessage().hasText()) return;
            String messageText = telegramRequest.getMessage().getText();
            // Avant d'envoyer la question à l'Agent AI
            Long chatId = telegramRequest.getMessage().getChatId();

            // Le cas des images (traitement)

            List<PhotoSize> photos = telegramRequest.getMessage().getPhoto();
            // S'il n'y a pas de text pour l'image
            List<Media> mediaList = new ArrayList<>();
            String caption = null;
            if (photos != null) {
                caption = telegramRequest.getMessage().getCaption();

                if (caption == null) caption="What do you see in this image";

                for (PhotoSize ps : photos) {
                    String fileId = ps.getFileId();
                    GetFile getFile = new GetFile();
                    getFile.setFileId(fileId);
                    File file = execute(getFile);
                    String filePath = file.getFilePath();
                    String textUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;
                    URL fileUrl = new URL(textUrl);
                    mediaList.add(Media.builder()
                            .id(fileId)
                            .mimeType(MimeTypeUtils.IMAGE_PNG)
                            .data(new UrlResource(fileUrl))
                            .build());
                }
            }

            String query = messageText != null ? messageText : caption;
            UserMessage userMessage = UserMessage.builder()
                    .text(query)
                    .media(mediaList)
                    .build();

            // ////////////////////////
            // Indiquer à l'utilisateur que le bot écrit ("typing...")
            sendTypingQuestion(chatId);
            // Conversion du chatId en String pour servir de conversationId
            String conversationId = String.valueOf(chatId);
            // ////////////////////////////////////////////////////

            // Appel à l'AIAgent avec les 2 paramètres requis
            String answer = aiAgent.askAgent(new Prompt(userMessage), conversationId);
            // Envoi de la réponse sur Telegram
            sendTextMessage(chatId, answer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return "ENSETAIBot";
    }

    @Override
    public String getBotToken() {
        return telegramBotToken;
    }

    private void sendTextMessage(long chatId, String text) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
        execute(sendMessage);
    }

    private void sendTypingQuestion(long chatId) throws TelegramApiException {
        SendChatAction sendChatAction = new SendChatAction();
        sendChatAction.setChatId(String.valueOf(chatId));
        sendChatAction.setAction(ActionType.TYPING);
        execute(sendChatAction);
    }
}
