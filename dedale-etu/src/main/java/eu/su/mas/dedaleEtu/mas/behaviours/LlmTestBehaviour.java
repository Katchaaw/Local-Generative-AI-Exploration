package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.LlmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.TickerBehaviour;

/**
 * TickerBehaviour de l'agent IA.
 * Il exécute une boucle Perception-Décision-Action à intervalle régulier.
 * Actuellement, toutes les trois secondes.
 */
public class LlmTestBehaviour extends TickerBehaviour {

    @Serial
    private static final long serialVersionUID = -7646778536966020439L;

    // Mémoire de l'agent
    private List<String> visitedNodes;

    // La topologie / carte Dédale
    private MapRepresentation myMap;
    // On garde une trace simplifiée des arêtes (A-B) pour alléger le prompt du LLM
    private Set<String> knownEdges;

    /**
     * Constructeur du comportement.
     * @param myagent L'agent Dédale auquel ce comportement est attaché.
     */
    public LlmTestBehaviour(final AbstractDedaleAgent myagent) {
        super(myagent, 3000);
        this.visitedNodes = new ArrayList<>();
        this.knownEdges = new HashSet<>();
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

        // Initialisation de la carte
        if (this.myMap == null) this.myMap = new MapRepresentation(this.myAgent.getLocalName());


        // Localisation
        Location myPosition = myAgent.getCurrentPosition();

        if (myPosition != null) {
            String myId = myPosition.getLocationId();

            // Si on arrive sur un nouveau nœud, on l'ajoute à notre historique
            if (!visitedNodes.contains(myId)) visitedNodes.add(myId);

            // MAJ de la topologie (marque le nœud comme visité/fermé)
            this.myMap.addNode(myId, MapRepresentation.MapAttribute.closed);

            // Perception → On récupère les informations sur la position actuelle et les nœuds adjacents.
            List<Couple<Location, List<Couple<Observation, String>>>> lobs = myAgent.observe();


            List<String> allNeighbors = new ArrayList<>(); // Tous les voisins qui se trouvent immédiatement autour de l'agent à l'instant T (distance = 1).
            List<String> newNeighbors = new ArrayList<>(); // Voisins adjacents que l'agent n'a jamais visités
            List<String> oldNeighbors = new ArrayList<>(); // Voisins adjacents que l'agent a déjà visités

            for (Couple<Location, List<Couple<Observation, String>>> c : lobs) {
                String neighborId = c.getLeft().getLocationId();
                allNeighbors.add(neighborId);

                // Ajout à la carte
                this.myMap.addNewNode(neighborId); // Ajout du nœud
                if (!myId.equals(neighborId)) {
                    this.myMap.addEdge(myId, neighborId); // Ajout de l'arête

                    // Ajout à notre set d'arêtes pour le LLM (Tri alphabétique pour éviter de doubler A-B | B-A)
                    String edge = myId.compareTo(neighborId) < 0 ? myId + "-" + neighborId : neighborId + "-" + myId;
                    this.knownEdges.add(edge);
                }

                // On sépare les voisins connus des voisins inconnus
                if (visitedNodes.contains(neighborId)) oldNeighbors.add(neighborId);
                else newNeighbors.add(neighborId);
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Tu es sur le noeud ").append(myPosition.getLocationId()).append(".\n");

            if (!newNeighbors.isEmpty()) promptBuilder.append("Voisins NON visités (A PRIORISER) : ").append(String.join(", ", newNeighbors)).append(".\n");
            if (!oldNeighbors.isEmpty()) promptBuilder.append("Voisins DEJA visités : ").append(String.join(", ", oldNeighbors)).append(".\n");

            // On lui donne la map de ce qu'il a déjà découvert
            promptBuilder.append("Topologie globale découverte (Arêtes) : ").append(String.join(", ", knownEdges)).append(".\n");
            promptBuilder.append("Analyse la topologie et choisis le meilleur noeud voisin pour continuer l'exploration. Réponds UNIQUEMENT par son ID.");

            String prompt = promptBuilder.toString();
            System.out.println(myAgent.getLocalName() + " demande à Ollama...");

            try {
                // Chronomètre pour études de temps
                long startTime = System.currentTimeMillis();

                // On envoi le prompt au LLM local via LangChain4j
                String rawAnswer = agentIA.getBrain().decideNextMove(prompt);

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                System.out.println("Temps de réponse LLM : " + duration + " ms");
                System.out.println("Le bot suggère : " + rawAnswer);

                String nextNodeId = null;
                String[] tokens = rawAnswer.split("\\W+");

                // On vérifie si la réponse du LLM est un voisin valide
                for (String token : tokens) {
                    if (allNeighbors.contains(token)) {
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
                        System.out.println("Échec du déplacement. L'IA a peut-être donné un ID inexistant.");
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
