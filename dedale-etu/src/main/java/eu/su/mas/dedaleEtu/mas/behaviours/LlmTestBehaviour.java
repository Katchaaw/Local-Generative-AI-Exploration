package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.LlmAgent;
import jade.core.behaviours.TickerBehaviour;

public class LlmTestBehaviour extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    public LlmTestBehaviour(final AbstractDedaleAgent myagent) {
        super(myagent, 3000); // On agit toutes les 3 secondes
    }

    @Override
    public void onTick() {
        AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
        LlmAgent agentIA = (LlmAgent) this.myAgent;

        // 1. RÉCUPÉRER LA POSITION (Type Location)
        Location myPosition = myAgent.getCurrentPosition();

        if (myPosition != null) {
            // 2. OBSERVER (La fameuse liste de couples complexe)
            List<Couple<Location, List<Couple<Observation, String>>>> lobs = myAgent.observe();

            // On prépare une version texte pour Gemini
            StringBuilder nodesFound = new StringBuilder();
            for (Couple<Location, List<Couple<Observation, String>>> c : lobs) {
                nodesFound.append(c.getLeft().getLocationId()).append(" ");
            }

            // 3. RÉCHLÉCHIR (Appel à Gemini)
            String prompt = "Je suis en " + myPosition.getLocationId()
                    + ". Noeuds voisins : " + nodesFound.toString()
                    + ". Réponds par l'ID d'un noeud voisin uniquement.";

            System.out.println(myAgent.getLocalName() + " demande à Gemini...");

            try {
                String nextNodeId = agentIA.getBrain().decideNextMove(prompt).trim();
                System.out.println("Gemini suggère : " + nextNodeId);

                // 4. AGIR (Utilisation de GsLocation pour le moveTo)
                // Comme dans ton exemple : moveTo(new GsLocation(nextNodeId))
                boolean success = myAgent.moveTo(new GsLocation(nextNodeId));

                if (success) {
                    System.out.println("Déplacement réussi vers " + nextNodeId);
                } else {
                    System.out.println("Echec du déplacement. L'IA a peut-être donné un ID inexistant.");
                }

            } catch (Exception e) {
                System.err.println("Erreur Gemini : " + e.getMessage());
            }
        }
    }
}
