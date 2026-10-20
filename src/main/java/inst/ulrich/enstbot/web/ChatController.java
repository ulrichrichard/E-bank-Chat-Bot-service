package inst.ulrich.enstbot.web;

import inst.ulrich.enstbot.agents.AIAgent;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {
    private AIAgent aiAgent;

    public ChatController(AIAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    // Communication blocante sur LLM
    // produces = MediaType.TEXT_PLAIN_VALUE => la reponse envoyer doit être un texte
    @GetMapping(value = "/chat",  produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(@RequestParam(name = "query") String query,
                       @RequestParam(name = "conversationId", defaultValue = "default") String conversationId) {
        return aiAgent.askAgent(new Prompt(query), conversationId);
    }

    // Communication non blocante sur LLM
    @GetMapping(value = "/chatStream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chatStream(@RequestParam(name = "query") String query,
                                   @RequestParam(name = "conversationId", defaultValue = "default") String conversationId) {
        return aiAgent.askAgentStream(query, conversationId);
    }
}
