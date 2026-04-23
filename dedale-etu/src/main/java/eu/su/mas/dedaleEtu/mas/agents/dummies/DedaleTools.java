package eu.su.mas.dedaleEtu.mas.agents.dummies;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public class DedaleTools {

    private String nextNodeToVisit = null;

    /**
     * Méthode que le LLM appelle tout seul.
     * L'annotation @Tool génère la documentation de la méthode que le LLM va lire.
     * L'annotation @Tool génère la documentation de l'argument de la fonction que le LLM va lire.
     */
    @Tool("Déplace l'agent vers un noeud voisin.")
    public String executeMove(@P("ID du noeud (exemple: '16')") String nodeId){
        System.out.println(" [API Dédale] Le LLM à appelé la fonction executeMove avec l'ID : " + nodeId);
        this.nextNodeToVisit = nodeId;
        return "Ordre reçu.";
    }

    /**
     * Méthode utilisée par notre Behaviour pour récupérer le choix de l'IA.
     */
    public String popNextNode(){
        String node = nextNodeToVisit;
        this.nextNodeToVisit = null; // On réinitialise pour le tour suivant
        return node;
    }
}
