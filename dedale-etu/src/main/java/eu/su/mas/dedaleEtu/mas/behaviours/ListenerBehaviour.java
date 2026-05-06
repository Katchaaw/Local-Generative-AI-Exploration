package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.LlmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.utils.MessageFactory;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.List;

public class ListenerBehaviour extends SimpleBehaviour {
    private LlmAgent agent;
    private MessageTemplate template;
    private final List<String> protocols = List.of("PING","PONG","LLM-CHAT", "SHARE-MAP");

    public ListenerBehaviour(final LlmAgent agent){
        super(agent);
        this.agent = agent;
        this.template = MessageFactory.buildMultiProtocolTemplate(ACLMessage.INFORM,protocols);

    }

    @Override
    public void action(){
        // On boucle tant qu'il y a des messages dans la file
        ACLMessage msgReceived;
        while ((msgReceived = this.myAgent.receive(this.template)) != null) {
            String protocol = msgReceived.getProtocol();
            switch (protocol){
                case "SHARE-MAP" -> shareMapHandler(msgReceived);
                case "LLM-CHAT" -> llmChatHandler(msgReceived);
                case "PING" -> pingHandler(msgReceived);
                case "PONG" -> pongHandler(msgReceived);
            }
        }
        block();
    }


    // TRAITEMENT AUTOMATIQUE (à changer peut être ?)
    private void pongHandler(ACLMessage msg){
        String conv_id = msg.getConversationId();
        String sender_id = msg.getSender().getLocalName();
        SerializableSimpleGraph sg = this.agent.getMyMap().getSerializableGraph();
        this.myAgent.addBehaviour(new SendMsgBehaviour((AbstractDedaleAgent)myAgent, conv_id, sg, "SHARE-MAP", List.of(sender_id)));
    }
    private void pingHandler(ACLMessage msg){
        String conversation_id = msg.getConversationId();
        String sender = msg.getSender().getLocalName();
        List <String> monoList = List.of(sender);
        this.agent.addBehaviour(new SendMsgBehaviour(this.agent,conversation_id, "", "PONG",monoList));
    }

    private void shareMapHandler(ACLMessage msg){
        try {
            this.agent.getMyMap().mergeMap((SerializableSimpleGraph<String, MapRepresentation.MapAttribute>) msg.getContentObject());
        } catch (UnreadableException e) {
            System.out.println("problème de merge");
            throw new RuntimeException(e);
        }
        this.agent.addMessageToInbox("Système : Map mise à jour par " + msg.getSender().getLocalName());
    }
    
    // TRANSMISSION AU LLM (TEXTE)
    private void llmChatHandler(ACLMessage msg){
        String text = "L'agent " + msg.getSender().getLocalName() + " dit : " + msg.getContent();
        this.agent.addMessageToInbox(text);
    }

    @Override
    public boolean done() {
        return false;
    }
}
