package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.Serial;
import java.util.ArrayList;
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

    // Mémoire de l'agent
    private List<String> visitedNodes;

    /**
     * Constructeur du comportement.
     * @param myagent L'agent Dédale auquel ce comportement est attaché.
     */
    public LlmTestBehaviour(final AbstractDedaleAgent myagent) {
        super(myagent, 3000);
        this.visitedNodes = new ArrayList<>();
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

            // Si on arrive sur un nouveau nœud, on l'ajoute à notre historique
            if (!visitedNodes.contains(myPosition.getLocationId())) {
                visitedNodes.add(myPosition.getLocationId());
            }

            // Perception → On récupère les informations sur la position actuelle et les nœuds adjacents.
            List<Couple<Location, List<Couple<Observation, String>>>> lobs = myAgent.observe();

            List<String> allNeighbors = new ArrayList<>();
            List<String> newNeighbors = new ArrayList<>();
            List<String> oldNeighbors = new ArrayList<>();

            for (Couple<Location, List<Couple<Observation, String>>> c : lobs) {
                String id = c.getLeft().getLocationId();
                allNeighbors.add(id);
                // On sépare les voisins connus des voisins inconnus
                if (visitedNodes.contains(id)) oldNeighbors.add(id);
                else newNeighbors.add(id);
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Tu es sur le noeud ").append(myPosition.getLocationId()).append(".\n");

            if (!newNeighbors.isEmpty()) promptBuilder.append("Voisins NON visités (A PRIORISER) : ").append(String.join(", ", newNeighbors)).append(".\n");
            if (!oldNeighbors.isEmpty()) promptBuilder.append("Voisins DEJA visités (A EVITER) : ").append(String.join(", ", oldNeighbors)).append(".\n");

            promptBuilder.append("Choisis un noeud voisin. Réponds UNIQUEMENT par son ID.");

            String prompt = promptBuilder.toString();
            System.out.println(myAgent.getLocalName() + " demande à Ollama...");

            try {
                // On envoi le prompt au LLM local via LangChain4j
                String rawAnswer = agentIA.getBrain().decideNextMove(prompt);
                System.out.println("Le bot suggère : " + rawAnswer);

                String nextNodeId = null;
                String[] tokens = rawAnswer.split("\\W+");

                // On vérifie si la réponse du LLM est un voisin valide
                for (String token : tokens) {
                    if (rawAnswer.contains(token)) {
                        nextNodeId = token;
                        break;
                    }
                }
                if (nextNodeId != null) {
                    // L'agent tente de se déplacer vers le nœud suggéré par l'IA
                    boolean success = myAgent.moveTo(new GsLocation(nextNodeId));
                    if (success) {
                        System.out.println("Déplacement réussi vers " + nextNodeId);
                    }
                    else {
                        System.out.println("Echec du déplacement. L'IA a peut-être donné un ID inexistant.");
                    }
                }
                else {
                    System.out.println("L'IA n'a pas renvoyé un ID valide parmi les voisins. Elle a dit : " + rawAnswer);

                    // Si l'IA bug, on prend un voisin au hasard pour ne pas rester bloqué éternellement
                    if(!allNeighbors.isEmpty()){
                        String fallbackNode = newNeighbors.isEmpty() ? oldNeighbors.getFirst() : newNeighbors.getFirst();
                        System.out.println("Mouvement de secours vers : " + fallbackNode);
                        myAgent.moveTo(new GsLocation(fallbackNode));
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur LLM : " + e.getMessage());
            }
        }
    }
}
