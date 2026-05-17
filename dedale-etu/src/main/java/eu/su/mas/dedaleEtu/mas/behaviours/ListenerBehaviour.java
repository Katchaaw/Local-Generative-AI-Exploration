package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableNode;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.LlmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.utils.MessageFactory;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.Serial;
import java.util.List;

/**
 * Comportement d'écoute permanent et asynchrone pour les agents cognitifs.
 * <p>
 * Ce comportement intercepte en tâche de fond tous les messages entrants correspondants
 * aux protocoles de l'escouade (PING, PONG, SHARE-MAP, LLM-CHAT). Il aiguille ensuite
 * la charge utile vers les handlers appropriés.
 * </p>
 */
public class ListenerBehaviour extends SimpleBehaviour {

    @Serial
    private static final long serialVersionUID = 2821623934189883785L;

    /** Instance de l'agent cognitif piloté par ce comportement. */
    private final LlmAgent agent;

    /** Filtre logique JADE permettant de n'intercepter que les messages pertinents. */
    private final MessageTemplate template;

    /** Liste des protocoles réseau supportés par l'escouade d'agents. */
    private static final List<String> protocols = List.of("PING","PONG","LLM-CHAT", "SHARE-MAP");

    /**
     * Initialise le comportement d'écoute asynchrone et configure le template multiprotocole.
     *
     * @param agent L'instance de l'agent cognitif {@link LlmAgent} associé.
     */
    public ListenerBehaviour(final LlmAgent agent){
        super(agent);
        this.agent = agent;
        this.template = MessageFactory.buildMultiProtocolTemplate(ACLMessage.INFORM,protocols);
    }

    @Override
    public void action(){
        ACLMessage msgReceived;
        while ((msgReceived = this.myAgent.receive(this.template)) != null) {
            String protocol = msgReceived.getProtocol();
            switch (protocol){
                case "SHARE-MAP" -> shareMapHandler(msgReceived);
                case "LLM-CHAT" -> llmChatHandler(msgReceived);
                case "PING" -> pingHandler(msgReceived);
                case "PONG" -> pongHandler(msgReceived);
                default -> System.err.println("[" + this.agent.getLocalName() + "] Protocole inconnu intercepté : " + protocol);
            }
        }
        block();
    }


    /**
     * Traite la réception d'un signal PONG.
     * Répond automatiquement en instanciant un comportement d'envoi pour partager
     * le sous-graphe topologique actuel de l'agent avec l'émetteur du PONG.
     *
     * @param msg Le message ACL contenant le signal PONG.
     */
    private void pongHandler(ACLMessage msg){
        String conv_id = msg.getConversationId();
        String sender_id = msg.getSender().getLocalName();
        SerializableSimpleGraph<String, MapRepresentation.MapAttribute> sg = this.agent.getMyMap().getSerializableGraph();
        this.myAgent.addBehaviour(new SendMsgBehaviour((AbstractDedaleAgent)myAgent, conv_id, sg, "SHARE-MAP", List.of(sender_id)));
    }

    /**
     * Traite la réception d'un signal PING.
     * Répond immédiatement de manière asynchrone par un signal PONG vers l'émetteur
     * pour confirmer sa présence sur le réseau.
     *
     * @param msg Le message ACL contenant le signal PING.
     */
    private void pingHandler(ACLMessage msg){
        String conversation_id = msg.getConversationId();
        String sender = msg.getSender().getLocalName();
        List <String> monoList = List.of(sender);
        this.agent.addBehaviour(new SendMsgBehaviour(this.agent,conversation_id, "", "PONG",monoList));
    }

    /**
     * Traite la réception d'une mise à jour cartographique (SHARE-MAP).
     * Désérialise le graphe partagé par un allié, réalise la fusion de cartes
     * via la méthode {@code mergeMap} et notifie sémantiquement l'agent dans son Inbox.
     *
     * @param msg Le message ACL contenant le sous-graphe sérialisé.
     * @throws RuntimeException Si le contenu de l'objet est illisible ou corrompu.
     */
    private void shareMapHandler(ACLMessage msg) {
        try {
            SerializableSimpleGraph<String, MapRepresentation.MapAttribute> receivedGraph =
                    (SerializableSimpleGraph<String, MapRepresentation.MapAttribute>) msg.getContentObject();

            // Fusion des connaissances
            this.agent.getMyMap().mergeMap(receivedGraph);

            for (SerializableNode<String, MapRepresentation.MapAttribute> node : receivedGraph.getAllNodes()) {
                String nodeId = node.getNodeId();

                for (String neighborId : receivedGraph.getEdges(nodeId)) {
                    this.agent.registerEdge(nodeId, neighborId);
                }
            }

        } catch (UnreadableException e) {
            System.out.println("problème de merge");
            throw new RuntimeException(e);
        }
        this.agent.addMessageToInbox("Map updated by: " + msg.getSender().getLocalName());
    }

    /**
     * Traite la réception d'un message émis par le LLM d'un allié (LLM-CHAT).
     * Nettoie la charge utile et l'ajoute à la boîte de réception synchrone de l'agent
     * afin qu'elle apparaisse dans la section RADIO du prompt au tour suivant.
     *
     * @param msg Le message ACL contenant le texte sémantique de l'allié.
     */
    private void llmChatHandler(ACLMessage msg){
        try {
            String messageClean = (String) msg.getContentObject();

            String text = "The agent" + msg.getSender().getLocalName() + " says : " + messageClean;

            this.agent.addMessageToInbox(text);

        } catch (UnreadableException e) {
            System.err.println("Erreur de lecture du message LLM-CHAT");
            this.agent.addMessageToInbox("The agent says " + msg.getSender().getLocalName() + " says (raw) : " + msg.getContent());
        }
    }

    @Override
    public boolean done() {
        return false;
    }
}
