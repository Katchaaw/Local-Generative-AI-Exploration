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
 * Agent physique cognitif autonome.
 * <p>
 * Cet agent hybride intègre un SLM comme moteur de raisonnement logique.
 * Il s'appuie sur le framework LangChain4j pour le *Function Calling*
 * et maintient une boîte de réception de messages asynchrones pour
 * collaborer avec ses alliés (partage de cartes et radio).
 * </p>
 */
public class LlmAgent extends AbstractDedaleAgent {

    @Serial
    private static final long serialVersionUID = -6431752665590433727L;

    /** Interface LangChain4j matérialisant le "cerveau" de l'agent */
    private transient AgentBrain brain;
    /** Registre d'outils natifs Java mis à disposition du processus d'inférence du SLM. */
    private transient DedaleTools apiTools;

    /** Représentation topologique interne et persistante du graphe de l'environnement. */
    private MapRepresentation myMap;

    /** Liste des messages textuels accumulés en tâche de fond (Radio). */
    private final List<String> inbox = new ArrayList<>();
    /** Liste des identifiants locaux des agents alliés présents dans la simulation. */
    private final List<String> agentList = new ArrayList<>();

    /** Ensemble des arêtes. */
    private final Set<String> knownEdges = new HashSet<>();

    @Override
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

    @Override
    protected void takeDown(){
        super.takeDown();
    }

    @Override
    protected void beforeMove(){
        super.beforeMove();
    }

    @Override
    protected void afterMove() {
        super.afterMove();
        initializeBrain();
    }

    /**
     * Enregistre une arête.
     * Trie lexicographiquement les identifiants pour éviter les doublons.
     *
     * @param id1 L'identifiant du premier nœud de l'arête.
     * @param id2 L'identifiant du second nœud de l'arête.
     */
    public void registerEdge(String id1, String id2) {
        if (id1.equals(id2)) return;
        String edge = id1.compareTo(id2) < 0 ? id1 + "-" + id2 : id2 + "-" + id1;
        this.knownEdges.add(edge);
    }


    /** Initialise l'infrastructure de représentation de la carte géographique de l'agent. */
    public void initializeMyMap(){this.myMap = new MapRepresentation(this.getLocalName());}

    /**
     * Récupère l'ensemble des arêtes actuellement connues de l'agent.
     *
     * @return Le {@link Set} contenant les id des arêtes.
     */
    public Set<String> getKnownEdges() {
        return knownEdges;
    }

    /**
     * Getter vers l'interface de communication LangChain4j du modèle de langage.
     */
    public AgentBrain getBrain() { return brain; }

    /**
     * Getter vers le registre d'outils de l'API Dédale exposé au modèle.
     *
     * @return L'instance {@link DedaleTools} associée.
     */
    public DedaleTools getApiTools() { return apiTools; }


    /**
     * Getter vers la représentation cartographique de l'environnement de l'agent.
     *
     * @return L'instance {@link MapRepresentation} stockée.
     */
    public MapRepresentation getMyMap(){return this.myMap;}

    /**
     * Récupère la liste des noms textuels locaux de tous les agents de l'escouade.
     *
     * @return La {@link List} des identifiants des alliés.
     */
    public List<String> getAgentList() {
        return agentList;
    }

    /**
     * Ajoute un message textuel (Radio alliée) à la boîte de réception.
     *
     * @param msg Le message formaté à archiver.
     */
    public synchronized void addMessageToInbox(String msg) {
        this.inbox.add(msg);
    }

    /**
     * Récupère l'intégralité des messages accumulés dans la boîte de réception et la vide.
     * Cette vidange garantit que le modèle ne traitera qu'une seule fois chaque message reçu.
     *
     * @return Une copie isolée sous forme de {@link List} des messages reçus.
     */
    public synchronized List<String> fetchInbox() {
        List<String> messages = new ArrayList<>(inbox);
        inbox.clear();
        return messages;
    }

}