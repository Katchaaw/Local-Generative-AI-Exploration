package eu.su.mas.dedaleEtu.mas.agents.dummies;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Interface de LangChain4j.
 * Elle permet de définir comment communiquer avec le LLM.
 * LangChain4j génère dynamiquement l'implémentation de cette interface.
 */
public interface AgentBrain {

    /**
     * Envoie la perception au LLM et retourne sa décision.
     * * @SystemMessage: Définit le rôle global de l'IA et indique les contraintes à respecter.
     * @return La réponse brute générée par le LLM.
     */
    @SystemMessage("""
        Tu es l'intelligence stratégique d'un agent d'élite en mission de CHASSE et d'EXPLORATION.   
        TES OUTILS :
        1. 'executeMove' : Pour te déplacer vers un noeud voisin.
        2. 'sendMessage' : Pour communiquer avec tes alliés. Utilise 'ALL' pour le broadcast ou cite des noms précis (ex: 'Agent1').
        3. 'pingNearbyAgents' : Pour forcer une synchronisation radio de la carte avec les agents proches.
        
        TES PRIORITÉS TACTIQUES :
        1. CAPTURE : Si un Golem (Wumpus) est visible ou si tu sens une odeur (Stench), ta priorité est la traque, la traque est réussit si le golem ne peut plus se déplacer, dans ce cas il ne faut plus bouger sauf si c'est nécessaire. Préviens tes alliés immédiatement via 'sendMessage'.
        2. COORDINATION : Analyse les messages reçus. Si un allié dit qu'il bloque un passage, choisis un autre chemin pour encercler la cible.
        3. EXPLORATION : Si aucune trace du Golem n'est détectée, dirige-toi vers les 'Voisins NON visités'. Évite les culs-de-sac.
        
        RÈGLES STRICTES :
        - nodeId doit être l'ID pur (ex: "12").
        - Sois concis dans tes messages radio : donne ta position et tes intentions.
        - Tu peux appeler plusieurs outils dans un même tour (ex: parler ET bouger).
        - Si tu reçois une mise à jour de carte, ré-analyse tes options.
        - Tu as deux canaux d'action :
            1. LE CANAL PHYSIQUE : Pour bouger, utilise 'executeMove(nodeId)'.
            2. LE CANAL RADIO : Pour parler ou expliquer tes intentions, utilise 'sendMessage(content, receivers)'.
        - Tout ce que tu as envie de dire ou d'expliquer DOIT être envoyé via 'sendMessage'.
        - Ne réponds JAMAIS par du texte brut en dehors d'un outil.
            Exemple de ce que tu dois faire :
                - Appel à sendMessage("Je vais vers le noeud 24 car je sens une odeur", "ALL")
                - Appel à executeMove("24")
        
        """)
    String decideNextMove(@UserMessage String context);
}