package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.Serial;
import java.util.*;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.DedaleTools;
import eu.su.mas.dedaleEtu.mas.agents.dummies.LlmAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

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
    private LlmAgent agentIA;

    /**
     * Constructeur du comportement.
     * @param myagent L'agent Dédale auquel ce comportement est attaché.
     */
    public LlmTestBehaviour(final AbstractDedaleAgent myagent) {
        super(myagent, 3);
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
        agentIA = (LlmAgent) this.myAgent;

        // Initialisation de la carte
        if (agentIA.getMyMap() == null){
            agentIA.initializeMyMap();
            this.myMap = agentIA.getMyMap();
        }

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
            Map<String, Integer> stenchCounts = new HashMap<>();
            List<String> observedWumpusNodes = new ArrayList<>();
            List<String> observedTeammates = new ArrayList<>();


            List<String> allNeighbors = new ArrayList<>(); // Tous les voisins qui se trouvent immédiatement autour de l'agent à l'instant T (distance = 1).
            List<String> newNeighbors = new ArrayList<>(); // Voisins adjacents que l'agent n'a jamais visités
            List<String> oldNeighbors = new ArrayList<>(); // Voisins adjacents que l'agent a déjà visités

            for (Couple<Location, List<Couple<Observation, String>>> c : lobs) {
                String neighborId = c.getLeft().getLocationId();
                allNeighbors.add(neighborId);
                List<Couple<Observation, String>> nodeObservations = c.getRight();

                for (Couple<Observation, String> o : nodeObservations) {
                    Observation type = o.getLeft();
                    String value = o.getRight();
                    switch (type) {
                        case STENCH: {
                            stenchCounts.put(neighborId, stenchCounts.getOrDefault(neighborId, 0) + 1);
                            break;
                        }
                        case AGENTNAME: {
                            if (value.equalsIgnoreCase("Wumpus")) {
                                observedWumpusNodes.add(neighborId);
                            } else if (!value.equals(myAgent.getLocalName())) {
                                observedTeammates.add(value + " (at " + neighborId + ")");
                            }
                            break;
                        }
                    }
                }
                // Ajout à la carte
                this.myMap.addNewNode(neighborId); // Ajout du nœud
                if (!myId.equals(neighborId)) {
                    this.myMap.addEdge(myId, neighborId); // Ajout de l'arête

                    // Ajout à notre set d'arêtes pour le LLM (Tri alphabétique pour éviter de doubler A-B | B-A)
                    agentIA.registerEdge(myId, neighborId);
                }

                // On sépare les voisins connus des voisins inconnus
                if (visitedNodes.contains(neighborId)) oldNeighbors.add(neighborId);
                else newNeighbors.add(neighborId);
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("You are agent ").append(myAgent.getLocalName()).append(".\n");
            promptBuilder.append("You are on node ").append(myPosition.getLocationId()).append(".\n");
            promptBuilder.append("Full list of allies: ").append(agentIA.getAgentList()).append("\n");
            promptBuilder.append("VISIBLE ALLIES: ").append(".\n");

            if (!observedTeammates.isEmpty()) {
                promptBuilder.append("VISIBLE ALLIES: ").append(String.join(", ", observedTeammates)).append(".\n");
            }
            else{
                promptBuilder.append("No visible ally ").append(".\n");
            }
            if (!observedWumpusNodes.isEmpty()) {
                promptBuilder.append("!!! TARGET IN SIGHT !!! Golem(s) detected on: ").append(String.join(", ", observedWumpusNodes)).append(".\n");
                promptBuilder.append("Warn allies immediately via 'sendMessage' and make a plan to block it. ").append(".\n");

            }
            if (!stenchCounts.isEmpty()) {
                promptBuilder.append("--- STENCH ANALYSIS ---\n");
                for (Map.Entry<String, Integer> entry : stenchCounts.entrySet()) {
                    int count = entry.getValue();
                    String node = entry.getKey();
                    if (count > 1) {
                        promptBuilder.append("- Node ").append(node)
                                .append(" smells VERY STRONG (").append(count).append(" odors).\n");
                    } else {
                        promptBuilder.append("- Node ").append(node).append(" has a suspicious smell.\n");
                    }
                }
            }
            List<String> inbox = agentIA.fetchInbox();
            if (!inbox.isEmpty()) {
                promptBuilder.append("RADIO (Messages received):\n");
                for (String msg : inbox){
                    promptBuilder.append("- ").append(msg).append("\n");
                    System.out.println(this.agentIA.getLocalName() + " received message: " + msg);
                }
            }

            if (!newNeighbors.isEmpty()) promptBuilder.append("UNVISITED neighbors (PRIORITIZE if there is nothing to do): ").append(String.join(", ", newNeighbors)).append(".\n");
            if (!oldNeighbors.isEmpty()) promptBuilder.append("ALREADY visited neighbors: ").append(String.join(", ", oldNeighbors)).append(".\n");

            // On lui donne la map de ce qu'il a déjà découvert
            promptBuilder.append("Global topology discovered (Edges): ").append(String.join(", ", this.agentIA.getKnownEdges())).append(".\n");
            promptBuilder.append("Decide on your action: move to explore or hunt (encircle the golem), and communicate with your allies if necessary. YOU MUST MANDATORILY USE A TOOL TO MOVE. ABSOLUTE PROHIBITION TO WRITE FREE TEXT. ONLY GENERATE THE FUNCTION CALL.\n");
            promptBuilder.append("STRICT RULE: You can ONLY move to an adjacent node. You can ONLY MOVE ONCE THIS TURN AND CANT BUFFER YOUR NEXT MOVES. Choose ONLY ONE destination from this exact list: ").append(String.join(", ", allNeighbors)).append(".\n");
            promptBuilder.append("ABSOLUTE SECURITY RULE: If the mention '!!! TARGET IN SIGHT !!!' does not appear in your current observations, the Golem is NOT there. You are STRICTLY FORBIDDEN from talking about it, imagining encirclement plans, or pretending to have seen it in your messages. Just stick to exploring.\n");

            String prompt = promptBuilder.toString();
            System.out.println(myAgent.getLocalName() + " requesting LLM...");

            try {
                // Chronomètre pour études de temps
                long startTime = System.currentTimeMillis();

                // On envoi le prompt au LLM local via LangChain4j
                String rawAnswer = agentIA.getBrain().decideNextMove(prompt);

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                System.out.println("LLM response time: " + duration + " ms");
                DedaleTools tools = agentIA.getApiTools();

                String nextNodeId = tools.popNextNode();

                List<DedaleTools.PendingMessage> msgs = tools.popMessages();
                for (DedaleTools.PendingMessage m : msgs) {
                    sendLlmMessage(m);
                }
                if (tools.popPing()) {
                    agentIA.addBehaviour(new SendMsgBehaviour((AbstractDedaleAgent) myAgent, "", "", "PING", agentIA.getAgentList()));
                }

                if (nextNodeId != null && allNeighbors.contains(nextNodeId)) {
                    // L'agent tente de se déplacer vers le nœud suggéré par l'IA
                    boolean success = myAgent.moveTo(new GsLocation(nextNodeId));
                    if (success) {
                        System.out.println("Move successful to " + nextNodeId);
                    }
                    else {
                        System.out.println("Move failed. The AI might have provided a non-existent ID.");
                    }
                }
                else {
                    System.out.println("/!\\ The AI did not use the tool correctly or proposed an invalid node. It said: " + rawAnswer);

                    // Si l'IA bug, on prend un voisin au hasard pour ne pas rester bloqué éternellement
                    if(!allNeighbors.isEmpty()){
                        String fallbackNode = newNeighbors.isEmpty() ? oldNeighbors.get(0) : newNeighbors.get(0);
                        System.out.println("Emergency fallback move to: " + fallbackNode);
                        myAgent.moveTo(new GsLocation(fallbackNode));
                    }
                }
            } catch (Exception e) {
                System.err.println("LLM Error: " + e.getMessage());
            }
        }
    }

    private void sendLlmMessage(DedaleTools.PendingMessage m) {
        if (m == null || m.content == null || m.content.isEmpty()) return;

        List<String> finalReceivers = new ArrayList<>();

        if (m.receivers == null || m.receivers.equalsIgnoreCase("ALL")) {
            finalReceivers = this.agentIA.getAgentList();
        } else {
            String[] parts = m.receivers.split(",");
            for (String p : parts) {
                String cleanName = p.trim(); // Supprime les espaces avant/après
                if (!cleanName.isEmpty()) {
                    finalReceivers.add(cleanName);
                }
            }
        }

        if (!finalReceivers.isEmpty()) {
            String convId = "chat-" + System.currentTimeMillis();
            // On ajoute le comportement d'envoi avec la liste propre
            myAgent.addBehaviour(new SendMsgBehaviour(
                    (AbstractDedaleAgent) myAgent,
                    convId,
                    m.content,
                    "LLM-CHAT",
                    finalReceivers
            ));
        }
    }
}