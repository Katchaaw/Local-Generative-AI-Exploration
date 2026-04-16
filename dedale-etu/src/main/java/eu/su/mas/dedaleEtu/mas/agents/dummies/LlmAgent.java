package eu.su.mas.dedaleEtu.mas.agents.dummies;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.LlmTestBehaviour;
import jade.core.behaviours.Behaviour;

/**
 * Classe représentant l'agent physique dans la plateforme Dédale.
 * L'agent intègre un LLM Local pour prendre ses décisions.
 */
public class LlmAgent extends AbstractDedaleAgent {

    @Serial
    private static final long serialVersionUID = -6431752665590433727L;

    // Le composant gérant les appels API vers Ollama.
    // Transient car LangChain4j n'est pas sérialisable par JADE lors de la migration d'un conteneur à l'autre.
    private transient AgentBrain brain;

    /**
     * Méthode d'initialisation appelée lors de la création de l'agent sur la plateforme.
     */
    protected void setup(){
        super.setup();
        final Object[] args = getArguments();
        eu.su.mas.dedale.env.EntityCharacteristics ec = (eu.su.mas.dedale.env.EntityCharacteristics) args[0];

        List<String> userParams = ec.getUserParameters();

        // Configuration du LLM local
        initializeBrain();

        // Ajout du comportement d'exploration (la boucle onTick)
        List<Behaviour> lb= new ArrayList<>();
        lb.add(new LlmTestBehaviour(this));
        addBehaviour(new StartMyBehaviours(this,lb));
    }

    /**
     * Configure et instancie la connexion avec le serveur local Ollama
     * utilisant le modèle Llama 3.2:3b.
     */
    private void initializeBrain() {
        dev.langchain4j.model.ollama.OllamaChatModel model =
                dev.langchain4j.model.ollama.OllamaChatModel.builder()
                        .baseUrl("http://localhost:11434")
                        .modelName("llama3.2:3b")        
                        .logRequests(true) // true pour debug, false sinon
                        .logResponses(true)
                        .build();

        this.brain = dev.langchain4j.service.AiServices.create(AgentBrain.class, model);
    }

    protected void takeDown(){
        super.takeDown();
    }

    protected void beforeMove(){
        super.beforeMove();
    }

    protected void afterMove() {
        super.afterMove();
        initializeBrain();
    }

    /**
     * Permet aux comportements (Behaviours) d'accéder à l'interface LangChain4j
     */
    public AgentBrain getBrain() { return brain; }
}