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
        super(myagent, 10000);
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
                                System.out.println("golem détected debug");
                                observedWumpusNodes.add(neighborId);
                            } else if (!value.equals(myAgent.getLocalName())) {
                                observedTeammates.add(value + " (en " + neighborId + ")");
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
            promptBuilder.append("Tu es l'agent ").append(myAgent.getLocalName()).append(".\n");
            if(this.agentIA.getLocalName().equals("OllamaBot1")){
                System.out.println(agentIA.getLocalName() + " est le chef");
                promptBuilder.append("Tu es le chef, tous les autres agents t'écoutent, donne des ordres pour encercler le golem. Tu seras notifier si un golem passe à cotée").append(".\n");

            }
            else{
                promptBuilder.append("Tu dois suivre les ordres de OllamaBot1 pour réussir à encercler le golem").append(".\n");
            }
            promptBuilder.append("Liste complète des alliés : ").append(agentIA.getAgentList()).append("\n");
            if (!observedTeammates.isEmpty()) {
                promptBuilder.append("ALLIÉS VISIBLES : ").append(String.join(", ", observedTeammates)).append(".\n");
            }
            if (!observedWumpusNodes.isEmpty()) {
                promptBuilder.append("!!! CIBLE EN VUE !!! Golem(s) détecté(s) sur : ").append(String.join(", ", observedWumpusNodes)).append(".\n");
            }
            if (!stenchCounts.isEmpty()) {
                promptBuilder.append("--- ANALYSE DES ODEURS (STENCH) ---\n");
                for (Map.Entry<String, Integer> entry : stenchCounts.entrySet()) {
                    int count = entry.getValue();
                    String node = entry.getKey();
                    if (count > 1) {
                        promptBuilder.append("- Le noeud ").append(node)
                                .append(" sent TRES FORT (").append(count).append(" odeurs).\n");
                    } else {
                        promptBuilder.append("- Le noeud ").append(node).append(" a une odeur suspecte.\n");
                    }
                }
            }
            promptBuilder.append("Tu es sur le noeud ").append(myPosition.getLocationId()).append(".\n");

            List<String> inbox = agentIA.fetchInbox();
            if (!inbox.isEmpty()) {
                promptBuilder.append("RADIO (Messages reçus) :\n");
                for (String msg : inbox){
                    promptBuilder.append("- ").append(msg).append("\n");
                    System.out.println(this.agentIA.getLocalName() + "a reçu comme message: " + msg);
                }
            }

            if (!newNeighbors.isEmpty()) promptBuilder.append("Voisins NON visités (A PRIORISER si il n'y a rien à faire) : ").append(String.join(", ", newNeighbors)).append(".\n");
            if (!oldNeighbors.isEmpty()) promptBuilder.append("Voisins DEJA visités : ").append(String.join(", ", oldNeighbors)).append(".\n");

            // On lui donne la map de ce qu'il a déjà découvert
            promptBuilder.append("Topologie globale découverte (Arêtes) : ").append(String.join(", ", this.agentIA.getKnownEdges())).append(".\n");
            promptBuilder.append("Décide de ton action : bouge pour explorer ou chasser (encercler le golem), et communique avec tes alliés si nécessaire. TU DOIS OBLIGATOIREMENT UTILISER UN OUTIL POUR TE DÉPLACER. INTERDICTION ABSOLUE D'ÉCRIRE DU TEXTE LIBRE. NE GÉNÈRE QUE L'APPEL DE LA FONCTION.\n");
            promptBuilder.append("RÈGLE STRICTE : Tu ne peux te déplacer QUE sur une case adjacente. Choisis UNE SEULE destination parmi cette liste exacte : ").append(String.join(", ", allNeighbors)).append(".\n");
            promptBuilder.append("RÈGLE DE SÉCURITÉ ABSOLUE : Si la mention '!!! CIBLE EN VUE !!!' n'apparaît pas dans tes observations actuelles, le Golem n'est PAS là. Tu as INTERDICTION STRICTE d'en parler, d'imaginer des plans d'encerclement ou de faire semblant de l'avoir vu dans tes messages. Contente-toi d'explorer.\n");
            
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
                DedaleTools tools = agentIA.getApiTools();

                String nextNodeId = tools.popNextNode();
                // --- NOUVEAU CODE : SAUVETAGE REGEX ---
                // Si l'outil n'a pas été appelé proprement, mais qu'on a une réponse texte
                if (nextNodeId == null && rawAnswer != null) {
                    System.out.println("Analyse du texte brut pour forcer l'extraction du mouvement...");
                    for (String neighbor : allNeighbors) {
                        // Cherche si le numéro du voisin apparaît de manière isolée dans le texte
                        // Cela attrapera "executeMove(28)", "noeud 28", '"28"', etc.
                        if (rawAnswer.matches("(?s).*\\b" + neighbor + "\\b.*")) {
                            nextNodeId = neighbor;
                            System.out.println("✅ [Secours Regex] Outil ignoré, mais noeud " + nextNodeId + " trouvé dans le texte !");
                            break; // On prend le premier voisin valide trouvé
                        }
                    }
                }

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
                        System.out.println("Déplacement réussi vers " + nextNodeId);
                    }
                    else {
                        System.out.println("Échec du déplacement. L'IA a peut-être donné un ID inexistant.");
                    }
                }
                else {
                    System.out.println("/!\\ L'IA n'a pas utilisé l'outil correctement ou a proposé un noeud invalide. Elle a dit : " + rawAnswer);

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
