package eu.su.mas.dedaleEtu.mas.agents.dummies;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;
import java.util.ArrayList;

/**
 * Registre d'outils (Tool Registry) exposé au SLM via LangChain4j.
 */
public class DedaleTools {

    /** Identifiant du nœud adjacent choisi par l'IA pour le prochain déplacement. */
    private String nextNodeToVisit = null;

    /** File d'attente des messages radio en attente d'expédition. */
    private final List<PendingMessage> messagesQueue = new ArrayList<>();

    /** Indicateur d'activation du signal de ping. */
    private boolean pingRequested = false;

    /** Nom de l'agent pour le suivi des logs. */
    private final String agentName;

    /**
     * Structure de données encapsulant un message radio en attente de traitement.
     */
    public static class PendingMessage {
        public String receivers; // "ALL" ou "Agent1,Agent2" -> ALL seulement, Ollama n'y arrive pas sinon...oskour
        public String content;
        public PendingMessage(String r, String c) { this.receivers = r; this.content = c; }
    }

    /**
     * Initialise le registre d'outils pour un agent donné.
     *
     * @param agentName Le nom de l'agent.
     */
    public DedaleTools(String agentName){
        this.agentName = agentName;
    }

    /**
     * Outil exposé au SLM : Déclenche l'envoi d'un signal PING pour cartographier les environs
     * et initier l'échange asynchrone mutuel de graphes topologiques entre alliés proches.
     *
     * @return Une notification de confirmation lue par le modèle au cours de sa réflexion.
     */
    @Tool("Sends a signal to detect nearby agents and synchronize map data. Use this periodically to share your discovered topology with allies.")
    public String pingNearbyAgents() {
        System.out.println(this.agentName +  ": appel à pingNearbyAgents");
        this.pingRequested = true;
        return "Synchronization signal sent.";
    }

    /**
     * Outil exposé au SLM : Planifie l'envoi d'un message textuel sémantique sur la radio.
     * Intègre un mécanisme de sécurité interdisant l'envoi de multiples messages par tour.
     *
     * @param content Le texte articulé par le modèle (consignes tactiques, alertes).
     * @return Un message de validation du positionnement dans la file d'attente.
     * @throws AssertionError Si l'IA tente de violer le protocole anti-spam de la radio.
     */
    @Tool("Sends a boradcast message to communicate with other agents. DO NOT use this for map synchronization.")
    public String sendMessage(@P("The textual content of the message.") String content){
        System.out.println(agentName +  ": appel à sendMessage. Message envoyé: " + content + " à ");

        if (!this.messagesQueue.isEmpty()) {
            throw new AssertionError("Anti-spam: Only ONE message allowed per turn. Loop broken.");
        }

        this.messagesQueue.add(new PendingMessage("ALL", content));
        return "Message queued successfully !";
    }

    /**
     * Extrait et vide la file d'attente des messages radio accumulés.
     *
     * @return La {@link List} des messages en attente.
     */
    public List<PendingMessage> popMessages() {
        List<PendingMessage> copy = new ArrayList<>(messagesQueue);
        messagesQueue.clear();
        return copy;
    }

    /**
     * Outil exposé au SLM : Enregistre l'ordre de déplacement physique vers un nœud adjacent.
     * Exige l'expression d'un raisonnement logique (Chain-of-Thought) du mouvement.
     *
     * @param nodeId      L'identifiant unique du nœud cible.
     * @param explication La justification en langage naturel de ce choix tactique.
     * @return Un accusé de réception pour le modèle, ou un message d'erreur si un mouvement a déjà été soumis.
     */
    @Tool("Moves the agent to an adjacent node. You MUST provide a short rationale for your choice.")
    public String executeMove(
            @P("The unique ID of the target node (e.g., '16').") String nodeId,
            @P("A 1-sentence explanation of WHY you chose this node.") String explication) {    System.out.println(agentName +  ": appel à executeMove avec l'ID " + nodeId);
        System.out.println(agentName +  ": appel à executeMove vers " + nodeId + " | Raison : " + explication);

        if (this.nextNodeToVisit == null) {
            this.nextNodeToVisit = nodeId;
            return "Movement order received.";
        }
        return "Error: You have already decided to move this turn.";
    }

    /**
     * Outil exposé au SLM : Signale explicitement la fin du tour d'action de l'agent.
     *
     * @return Une chaîne de clôture de contexte.
     */
    @Tool("Call this tool ONLY when you are done with your turn and have no more actions to take.")
    public String finishTurn() {
        System.out.println(agentName +  ": appel à finish");
        return "Turn finalized successfully. Stop generating!.";
    }

    /**
     * Récupère le choix de nœud enregistré par l'IA et réinitialise le registre pour le tour suivant.
     *
     * @return L'identifiant du nœud cible, ou {@code null} si aucun mouvement n'a été décidé.
     */
    public String popNextNode(){
        String node = nextNodeToVisit;
        this.nextNodeToVisit = null; // On réinitialise pour le tour suivant
        return node;
    }

    /**
     * Interroge et réinitialise l'état de la demande de PING.
     *
     * @return {@code true} si un ping a été requis par le modèle au cours du tour, {@code false} sinon.
     */
    public boolean popPing() {
        boolean p = pingRequested;
        this.pingRequested = false;
        return p;
    }
}