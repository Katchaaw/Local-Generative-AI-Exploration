package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.Serial;
import java.util.List;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.LlmAgent;
import jade.core.behaviours.TickerBehaviour;

/**
 * TickerBehaviour de l'agent IA.
 * Il exécute une boucle Perception-Décision-Action à intervalle régulier.
 * Actuellement, toutes les 3 secondes.
 */
public class LlmTestBehaviour extends TickerBehaviour {

    @Serial
    private static final long serialVersionUID = -7646778536966020439L;

    /**
     * Constructeur du comportement.
     * @param myagent L'agent Dédale auquel ce comportement est attaché.
     */
    public LlmTestBehaviour(final AbstractDedaleAgent myagent) {
        super(myagent, 3000); 
    }

    /**
     * Méthode appelée à chaque tick du timer.
     * Contient la logique complète d'un tour de l'agent.
     */
    @Override
    public void onTick() {
        // Cast des références pour accéder aux méthodes de l'agent
        AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
        LlmAgent agentIA = (LlmAgent) this.myAgent;

        // Localisation
        Location myPosition = myAgent.getCurrentPosition();

        if (myPosition != null) {
            // Perception
            // On récupère les informations sur la position actuelle et les nœuds adjacents.
            List<Couple<Location, List<Couple<Observation, String>>>> lobs = myAgent.observe();

            // Extraction des IDs des voisins pour les donner au LLM
            StringBuilder nodesFound = new StringBuilder();
            for (Couple<Location, List<Couple<Observation, String>>> c : lobs) {
                nodesFound.append(c.getLeft().getLocationId()).append(" ");
            }

            // Construction du prompt
            // TODO : Injecter la mémoire des nœuds visités pour éviter de retourner sur des nœuds inutilement
            String prompt = "Je suis en " + myPosition.getLocationId()
                    + ". Noeuds voisins : " + nodesFound
                    + ". Réponds par l'ID d'un noeud voisin uniquement.";

            System.out.println(myAgent.getLocalName() + " demande à Ollama...");

            try {
                // On envoi le prompt au LLM local via LangChain4j
                // TODO: Améliorer la robustesse.trim() peut poser problème de formatage
                String nextNodeId = agentIA.getBrain().decideNextMove(prompt).trim();
                System.out.println("Le bot suggère : " + nextNodeId);

                // L'agent tente de se déplacer vers le nœud suggéré par l'IA
                boolean success = myAgent.moveTo(new GsLocation(nextNodeId));

                if (success) {
                    System.out.println("Déplacement réussi vers " + nextNodeId);
                } else {
                    System.out.println("Echec du déplacement. L'IA a peut-être donné un ID inexistant.");
                }

            } catch (Exception e) {
                System.err.println("Erreur LLM : " + e.getMessage());
            }
        }
    }
}
