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

    /** 
     * Structure interne pour stocker le couple (Destinataire, Message)
     */
    public static class PendingMessage {
        public String receivers; // "ALL" ou "Agent1,Agent2"
        public String content;
        public PendingMessage(String r, String c) { this.receivers = r; this.content = c; }
    }
    
    @Tool("Envoie un signal pour détecter les agents aux alentours et synchroniser la carte (A envoyer périodiquement).")
    public String pingNearbyAgents() {
        this.pingRequested = true;
        return "Signal de synchronisation envoyé.";
    }
    @Tool("Envoie un message. Tu peux cibler des agents précis ou tout le monde. Cette fonction te permet de communiquer avec les autres agents. NE PAS PING ICI")
    public String sendMessage(
            @P("Le contenu du message") String content,
            @P("Destinataires : 'ALL' pour broadcast, ou noms séparés par virgules (ex: 'Agent1, Agent3')") String receivers
    ){
        this.messagesQueue.add(new PendingMessage(receivers, content));
        return "Message enregistré pour " + receivers;
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
    @Tool("Déplace l'agent vers un noeud voisin.")
    public String executeMove(@P("ID du noeud (exemple: '16')") String nodeId){
        if (this.nextNodeToVisit == null) {
            this.nextNodeToVisit = nodeId;
            return "Ordre reçu.";
        }
        return "Erreur : Tu as déjà décidé de bouger ce tour-ci.";
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
