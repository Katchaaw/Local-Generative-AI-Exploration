package eu.su.mas.dedaleEtu.mas.utils;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class MessageFactory {
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

    public static MessageTemplate buildMultiProtocolTemplate(int performative, List <String> protocols){
        if(protocols == null || protocols.isEmpty()){
            return null;
        }
        MessageTemplate final_template = MessageTemplate.MatchProtocol(protocols.getFirst());
        for(int i = 1; i < protocols.size(); i++){
            MessageTemplate protocol = MessageTemplate.MatchProtocol(protocols.get(i));
            final_template = MessageTemplate.or(final_template, protocol);
        }
        MessageTemplate perfo = MessageTemplate.MatchPerformative(performative);
        final_template = MessageTemplate.and(final_template, perfo);
        return  final_template;
    }
}
