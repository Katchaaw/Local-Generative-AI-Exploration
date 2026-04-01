package eu.su.mas.dedaleEtu.mas.agents.dummies;

import java.util.ArrayList;
import java.util.List;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.LlmTestBehaviour;
import jade.core.behaviours.Behaviour;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;

public class LlmAgent extends AbstractDedaleAgent {

    private static final long serialVersionUID = -6431752665590433727L;
    private transient AgentBrain brain;
    private String apiKey;

    protected void setup(){
        super.setup();
        final Object[] args = getArguments();
        eu.su.mas.dedale.env.EntityCharacteristics ec = (eu.su.mas.dedale.env.EntityCharacteristics) args[0];

        List<String> userParams = ec.getUserParameters();

        if (userParams != null && !userParams.isEmpty()) {
            this.apiKey = userParams.get(0);
            System.out.println("Clé API récupérée avec succès.");
        } else {
            System.err.println("Attention : Aucune clé API trouvée dans userParameters (gemini.json)");
        }
        
        initializeBrain();
        List<Behaviour> lb=new ArrayList<Behaviour>();
        lb.add(new LlmTestBehaviour(this));
        addBehaviour(new StartMyBehaviours(this,lb));
    }

    private void initializeBrain() {
        if (this.apiKey == null) return;
        
        dev.langchain4j.model.googleai.GoogleAiGeminiChatModel model =
                dev.langchain4j.model.googleai.GoogleAiGeminiChatModel.builder()
                        .apiKey(this.apiKey)
                        .modelName("gemini-2.5-flash")
                        .logRequestsAndResponses(true)
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
    public AgentBrain getBrain() { return brain; }
}