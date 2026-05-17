package eu.su.mas.dedaleEtu.mas.utils;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Fichier dédié à l'abstraction, la création et le filtrage des messages.
 * <p>
 * Centralise les primitives de construction de messages asynchrones et
 * les templates nécessaires à la fusion de cartes et à la coordination.
 * </p>
 */
public class MessageFactory {

    /**
     * Construit un message ACL de type {@code INFORM}.
     * Automatise la conversion des Strings des noms des agents en identifiants locaux (AID).
     *
     * @param senderId        L'identifiant unique (AID) de l'agent émetteur.
     * @param receivers       La liste des noms locaux des agents destinataires.
     * @param protocol        Le protocole réseau qualifiant la nature du message.
     * @param content         L'objet sérialisable constituant le corps du message.
     * @param conversation_id L'identifiant unique de session pour le suivi conversationnel asynchrone.
     * @return Un objet {@link ACLMessage}
     */
    public static ACLMessage buildMsg(AID senderId, List<String> receivers, String protocol, Serializable content, String conversation_id){
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setProtocol(protocol);
        msg.setSender(senderId);
        msg.setConversationId(conversation_id);

        for (String agentName : receivers) {
            msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
        }

        try {
            msg.setContentObject(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return msg;
    }

    /**
     * Compose un MessageTemplate permettant à un agent d'écouter plusieurs protocoles en simultané,
     * restreints à une performative précise.
     * <p>
     * Construit dynamiquement une arborescence booléenne de type : <br>
     * {@code (Protocol1 OR Protocol2 OR ... ProtocolN) AND Performative}.
     * </p>
     *
     * @param performative Le type de message attendu (ex: {@link ACLMessage#INFORM}).
     * @param protocols    La liste des protocoles à intercepter en tâche de fond.
     * @return Un {@link MessageTemplate} pour l'interception non-bloquante,
     * ou {@code null} si la liste de protocoles est vide.
     */
    public static MessageTemplate buildMultiProtocolTemplate(int performative, List <String> protocols){
        if(protocols == null || protocols.isEmpty()){
            return null;
        }

        MessageTemplate final_template = MessageTemplate.MatchProtocol(protocols.getFirst());

        // Construction de la chaîne de protocoles acceptés
        for(int i = 1; i < protocols.size(); i++){
            MessageTemplate protocol = MessageTemplate.MatchProtocol(protocols.get(i));
            final_template = MessageTemplate.or(final_template, protocol);
        }

        // (AND)
        MessageTemplate perfo = MessageTemplate.MatchPerformative(performative);
        final_template = MessageTemplate.and(final_template, perfo);

        return final_template;
    }
}
