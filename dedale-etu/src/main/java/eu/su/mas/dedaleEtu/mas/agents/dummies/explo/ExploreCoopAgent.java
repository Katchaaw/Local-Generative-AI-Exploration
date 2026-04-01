package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedale.env.EntityCharacteristics;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;

import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

import jade.core.behaviours.Behaviour;

/**
 * <pre>
 * ExploreCoop agent. 
 * Basic example of how to "collaboratively" explore the map
 *  - It explore the map using a DFS algorithm and blindly tries to share the topology with the agents within reach.
 *  - The shortestPath computation is not optimized
 *  - Agents do not coordinate themselves on the node(s) to visit, thus progressively creating a single file. It's bad.
 *  - The agent sends all its map, periodically, forever. Its bad x3.
 *  - You should give him the list of agents'name to send its map to in parameter when creating the agent.
 *   Object [] entityParameters={"Name1","Name2};
 *   ag=createNewDedaleAgent(c, agentName, ExploreCoopAgent.class.getName(), entityParameters);
 *
 * It stops when all nodes have been visited.
 *
 *
 *  </pre>
 *
 * @author hc
 *
 */


public class ExploreCoopAgent extends AbstractDedaleAgent {

	private static final long serialVersionUID = -7969469610241668140L;
	private MapRepresentation myMap;


	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *
	 */
	protected void setup(){

		super.setup();

		//get the parameters added to the agent at creation (if any)
		final Object[] args = getArguments();
		List<String> list_agentNames = new ArrayList<>();

		if (args != null && args.length > 0) {
			EntityCharacteristics ec = (EntityCharacteristics) args[0];

			List<String> userParams = ec.getUserParameters();

			if (userParams != null && !userParams.isEmpty()) {
				list_agentNames.addAll(userParams);
			}
		}

		List<Behaviour> lb=new ArrayList<Behaviour>();

		/************************************************
		 *
		 * ADD the behaviours of the Dummy Moving Agent
		 *
		 ************************************************/

		lb.add(new ExploCoopBehaviour(this,this.myMap,list_agentNames));



		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */


		addBehaviour(new StartMyBehaviours(this,lb));

		System.out.println("the  agent "+this.getLocalName()+ " is started");

	}


	/**
	 * This method is automatically called after doDelete()
	 */
	protected void takeDown(){
		super.takeDown();
	}

	protected void beforeMove(){
		super.beforeMove();
		//System.out.println("I migrate");
	}

	protected void afterMove(){
		super.afterMove();
		//System.out.println("I migrated");
	}

}
