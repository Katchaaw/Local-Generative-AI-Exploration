package eu.su.mas.dedaleEtu.mas.agents.dummies;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;
import java.util.ArrayList;

public class DedaleTools {
    private String nextNodeToVisit = null;
    private List<PendingMessage> messagesQueue = new ArrayList<>();
    private String nextNode = null;
    private boolean pingRequested = false; // Flag pour le Ping

    /** * Structure interne pour stocker le couple (Destinataire, Message)
     */
    public static class PendingMessage {
        public String receivers; // "ALL" ou "Agent1,Agent2"
        public String content;
        public PendingMessage(String r, String c) { this.receivers = r; this.content = c; }
    }

    @Tool("Sends a signal to detect nearby agents and synchronize map data. Use this periodically to share your discovered topology with allies.")
    public String pingNearbyAgents() {
        this.pingRequested = true;
        return "Synchronization signal sent.";
    }

    @Tool("Sends a radio message to communicate with other agents. You can target specific agents or broadcast to everyone. DO NOT use this for map synchronization.")
    public String sendMessage(
            @P("The textual content of the message.") String content,
            @P("Recipient(s): Use 'ALL' for broadcast, or specific names separated by commas (e.g., 'OllamaBot1, OllamaBot3').") String receivers
    ){
        this.messagesQueue.add(new PendingMessage(receivers, content));
        return "Message queued for " + receivers;
    }

    public List<PendingMessage> popMessages() {
        List<PendingMessage> copy = new ArrayList<>(messagesQueue);
        messagesQueue.clear();
        return copy;
    }

    /**
     * Méthode que le LLM appelle tout seul.
     * L'annotation @Tool génère la documentation de la méthode que le LLM va lire.
     * L'annotation @Tool génère la documentation de l'argument de la fonction que le LLM va lire.
     */
    @Tool("Moves the agent to an adjacent neighbor node. Destination must be a valid adjacent node from your current observations.")
    public String executeMove(@P("The unique ID of the target node (e.g., '16').") String nodeId){
        if (this.nextNodeToVisit == null) {
            this.nextNodeToVisit = nodeId;
            return "Movement order received.";
        }
        return "Error: You have already decided to move this turn.";
    }

    /**
     * Méthode utilisée par notre Behaviour pour récupérer le choix de l'IA.
     */
    public String popNextNode(){
        String node = nextNodeToVisit;
        this.nextNodeToVisit = null; // On réinitialise pour le tour suivant
        return node;
    }

    public boolean popPing() {
        boolean p = pingRequested;
        this.pingRequested = false;
        return p;
    }
}