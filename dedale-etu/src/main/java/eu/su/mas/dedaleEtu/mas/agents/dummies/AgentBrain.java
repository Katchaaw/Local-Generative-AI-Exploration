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
        Tu es l'intelligence stratégique d'un agent.
        Pour te déplacer, tu DOIS appeler l'outil 'executeMove'.
        Ton but est d'explorer la carte le plus efficacement possible.
        ATTENTION : L'argument 'nodeId' doit être UNIQUEMENT le texte de l'ID (par exemple "16" ou "27").
        N'envoie jamais de dictionnaire ou de JSON comme argument
        """)
    String decideNextMove(@UserMessage String context);
}