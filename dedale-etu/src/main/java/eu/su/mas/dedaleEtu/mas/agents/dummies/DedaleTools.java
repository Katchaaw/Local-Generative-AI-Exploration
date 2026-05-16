package eu.su.mas.dedaleEtu.mas.agents.dummies;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;
import java.util.ArrayList;

public class DedaleTools {
    private String nextNodeToVisit = null;
    private List<PendingMessage> messagesQueue = new ArrayList<>();
    private String nextNode = null;
    private boolean pingRequested = false; 

    private String agentName; //pour debug...
    
    /** * Structure interne pour stocker le couple (Destinataire, Message)
     */
    public static class PendingMessage {
        public String receivers; // "ALL" ou "Agent1,Agent2" -> ALL seulement, Ollama n'y arrive pas sinon...oskour
        public String content;
        public PendingMessage(String r, String c) { this.receivers = r; this.content = c; }
    }
    
    public DedaleTools(String agentName){
        this.agentName = agentName;
    }

    @Tool("Sends a signal to detect nearby agents and synchronize map data. Use this periodically to share your discovered topology with allies.")
    public String pingNearbyAgents() {
        System.out.println(this.agentName +  ": appel à pingNearbyAgents");
        this.pingRequested = true;
        return "Synchronization signal sent.";
    }

    @Tool("Sends a boradcast message to communicate with other agents. DO NOT use this for map synchronization.")
        public String sendMessage(
            @P("The textual content of the message.") String content
    ){
        System.out.println(agentName +  ": appel à sendMessage. Message envoyé: " + content + " à ");

        if (!this.messagesQueue.isEmpty()) {
            throw new AssertionError("Anti-spam: Only ONE message allowed per turn. Loop broken.");
        }
        

        this.messagesQueue.add(new PendingMessage("ALL", content));
        return "Message queued successfully !";
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
    @Tool("Moves the agent to an adjacent node. You MUST provide a short rationale for your choice.")
    public String executeMove(
            @P("The unique ID of the target node (e.g., '16').") String nodeId,
            @P("A 1-sentence explanation of WHY you chose this node.") String rationale) {    System.out.println(agentName +  ": appel à executeMove avec l'ID " + nodeId);
        System.out.println(agentName +  ": appel à executeMove vers " + nodeId + " | Raison : " + rationale);

        if (this.nextNodeToVisit == null) {
            this.nextNodeToVisit = nodeId;
            return "Movement order received.";
        }
        return "Error: You have already decided to move this turn.";
    }
    @Tool("Call this tool ONLY when you are done with your turn and have no more actions to take.")
    public String finishTurn() {
        System.out.println(agentName +  ": appel à finish");
        return "Turn finalized successfully. Stop generating!.";
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