package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.utils.MessageFactory;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Comportement à exécution unique ({@link OneShotBehaviour}) dédié à l'expédition asynchrone de messages.
 * <p>
 * Ce comportement encapsule la logique de composition et d'envoi d'un message réseau JADE.
 * </p>
 */
public class SendMsgBehaviour extends OneShotBehaviour {

    @Serial
    private static final long serialVersionUID = 8567689731896717661L;

    /** Charge utile */
    private final Serializable content;

    /** Protocole */
    private final String protocol;

    /** Liste des noms des agents cibles devant intercepter le message. */
    private final List<String> receivers;

    /** Identifiant de conversation unique */
    private final String conversation_id;

    /**
     * Initialise un comportement d'envoi de message.
     *
     * @param myagent         L'instance de l'agent Dédale émetteur.
     * @param conversation_id L'identifiant unique de session.
     * @param msg             La charge utile sérialisable à transmettre sur le réseau.
     * @param protocol        Le protocole réseau.
     * @param receivers       La liste des agents destinataires locaux.
     */
    public SendMsgBehaviour(final AbstractDedaleAgent myagent, String conversation_id,Serializable msg, String protocol, List<String > receivers){
        super(myagent);
        this.conversation_id = conversation_id;
        this.content = msg;
        this.protocol = protocol;
        this.receivers = receivers;
    }

    @Override
    public void action(){
        ACLMessage msg = MessageFactory.buildMsg(this.myAgent.getAID(), receivers, protocol, content, conversation_id);
        ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
    }

}
