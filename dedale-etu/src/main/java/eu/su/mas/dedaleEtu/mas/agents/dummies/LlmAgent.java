package eu.su.mas.dedaleEtu.mas.agents.dummies;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.su.mas.dedale.env.EntityCharacteristics;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.ListenerBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.LlmTestBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
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
    private transient DedaleTools apiTools;
    private MapRepresentation myMap;

    private List<String> inbox = new ArrayList<>();
    private List<String> agentList = new ArrayList<>();

    // Dans LlmAgent.java
    private Set<String> knownEdges = new HashSet<>();

    public Set<String> getKnownEdges() {
        return knownEdges;
    }

    public void registerEdge(String id1, String id2) {
        if (id1.equals(id2)) return;
        String edge = id1.compareTo(id2) < 0 ? id1 + "-" + id2 : id2 + "-" + id1;
        this.knownEdges.add(edge);
    }
    
    /**
     * Méthode d'initialisation appelée lors de la création de l'agent sur la plateforme.
     */
    protected void setup(){
        super.setup();
        final Object[] args = getArguments();

        if (args != null && args.length > 0) {
            EntityCharacteristics ec = (EntityCharacteristics) args[0];

            List<String> userParams = ec.getUserParameters();

            if (userParams != null && !userParams.isEmpty()) {
                this.agentList.addAll(userParams);
            }
        }

        // Configuration du LLM local
        initializeBrain();

        // Ajout du comportement d'exploration (la boucle onTick)
        List<Behaviour> lb= new ArrayList<>();
        lb.add(new LlmTestBehaviour(this));
        lb.add(new ListenerBehaviour(this));
        addBehaviour(new StartMyBehaviours(this,lb));
    }

    /**
     * Configure et instancie la connexion avec le serveur local Ollama
     * utilisant le modèle Llama 3.2:3b.
     */
    private void initializeBrain() {
        dev.langchain4j.model.ollama.OllamaChatModel model =
                dev.langchain4j.model.ollama.OllamaChatModel.builder()
                        .baseUrl("http://127.0.0.1:11434")
                        .modelName("llama3.2:3b")
                        .logRequests(false)
                        .logResponses(false)
                        .build();

        this.apiTools = new DedaleTools(this.getLocalName()); // On crée l'outil

        this.brain = dev.langchain4j.service.AiServices.builder(AgentBrain.class)
                .chatLanguageModel(model)
                .tools(apiTools)
                .build();
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

    public DedaleTools getApiTools() { return apiTools; }
    public void initializeMyMap(){this.myMap = new MapRepresentation(this.getLocalName());}
    public MapRepresentation getMyMap(){return this.myMap;}

    public synchronized void addMessageToInbox(String msg) {
        this.inbox.add(msg);
    }

    public synchronized List<String> fetchInbox() {
        List<String> messages = new ArrayList<>(inbox);
        inbox.clear(); // On vide après lecture
        return messages;
    }

    public List<String> getAgentList() {
        return agentList;
    }
}