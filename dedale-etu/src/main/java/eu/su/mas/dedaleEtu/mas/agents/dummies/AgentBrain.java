package eu.su.mas.dedaleEtu.mas.agents.dummies;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AgentBrain {
    @SystemMessage("""
        Tu es un agent explorateur dans le monde de Dedale. 
        Ton but est d'explorer la grille.
        Tu dois répondre uniquement par le nom du nœud où tu veux aller.
        """)
    String decideNextMove(@UserMessage String context);
}