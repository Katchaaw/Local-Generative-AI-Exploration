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
     * * @SystemMessage: Définit le rôle global de l'IA.
     * Il indique les contraintes à respecter.
     * * @param context Les observations actuelles de l'agent.
     * @return La réponse brute générée par le LLM.
     */
    @SystemMessage("""
        Tu es un agent explorateur dans le monde de Dedale.
        Ton but est d'explorer la grille.
        Tu dois répondre uniquement par le nom du nœud où tu veux aller.
        """)
    String decideNextMove(@UserMessage String context);
}