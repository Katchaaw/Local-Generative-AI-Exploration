package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.utils.MessageFactory;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.Serializable;
import java.util.List;

public class SendMsgBehaviour extends OneShotBehaviour {
    Serializable content;
    String protocol;
    List<String> receivers;
    String conversation_id;

    private static final long serialVersionUID = 8567689731896717661L;


    public SendMsgBehaviour(final AbstractDedaleAgent myagent, String conversation_id,Serializable msg, String protocol, List<String > receivers){
        super(myagent);
        this.conversation_id = conversation_id;
        this.content = msg;
        this.protocol = protocol;
        this.receivers = receivers;
        if(msg instanceof String) System.out.println((String) msg);
    }

    @Override
    public void action(){
        ACLMessage msg = MessageFactory.buildMsg(this.myAgent.getAID(), receivers, protocol, content, conversation_id);
        ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
    }

}
