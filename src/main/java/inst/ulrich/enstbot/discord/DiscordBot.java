package inst.ulrich.enstbot.discord;

import com.zgamelogic.discord.annotations.DiscordController;
import com.zgamelogic.discord.annotations.DiscordMapping;
import inst.ulrich.enstbot.agents.AIAgent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordController
public class DiscordBot {
    private AIAgent aiAgent;

    public DiscordBot(AIAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @DiscordMapping
    private void perform(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String query = event.getMessage().getContentRaw();
        String conversationId = event.getChannel().getId();

        // 1. Envoyer un message initial d'attente
        event.getChannel().sendMessage("Thinking... 🧠").queue(sentMessage -> {
            StringBuilder fullResponse = new StringBuilder();

            // 2. S'abonner au Flux et éditer le message par blocs/paliers
            aiAgent.askAgentStream(query, conversationId)
                    .doOnNext(chunk -> {
                        fullResponse.append(chunk);
                        // Astuce : On évite de spammer l'API Discord à chaque token (rate limits)
                        // On peut éditer périodiquement si le contenu a bien grandi
                    })
                    .doOnComplete(() -> {
                        // 3. Mise à jour finale du message une fois le stream terminé
                        String finalContent = fullResponse.toString();
                        if (finalContent.length() > 2000) {
                            finalContent = finalContent.substring(0, 1996) + "...";
                        }
                        sentMessage.editMessage(finalContent).queue();
                    })
                    .subscribe(); // Déclenche l'exécution réactive
        });
    }
}
