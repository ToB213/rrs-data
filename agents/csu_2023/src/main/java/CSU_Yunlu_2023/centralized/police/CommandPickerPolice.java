package CSU_Yunlu_2023.centralized.police;


import adf.core.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.info.AgentInfo;
import adf.core.agent.info.ScenarioInfo;
import adf.core.agent.info.WorldInfo;
import adf.core.agent.module.ModuleManager;
import adf.core.component.centralized.CommandPicker;
import adf.core.component.communication.CommunicationMessage;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class CommandPickerPolice extends CommandPicker {

	    private Collection<CommunicationMessage> messages;
	    private Map<EntityID, EntityID> allocationData;

	    public CommandPickerPolice(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
	        super(ai, wi, si, moduleManager, developData);
	        this.messages = new ArrayList<>();
	        this.allocationData = null;
	    }

	    @Override
	    public CommandPicker setAllocatorResult(Map<EntityID, EntityID> allocationData) {
	        this.allocationData = allocationData;
	        return this;
	    }

	    @Override
	    public CommandPicker calc() {
	        this.messages.clear();
	        if(this.allocationData == null) {
	            return this;
	        }
	        for(EntityID agentID : this.allocationData.keySet()) {
	            StandardEntity agent = this.worldInfo.getEntity(agentID);
	            if(agent != null && agent.getStandardURN() == StandardEntityURN.POLICE_FORCE) {
	                StandardEntity target = this.worldInfo.getEntity(this.allocationData.get(agentID));
	                if(target != null) {
	                    if(target instanceof Area) {
	                        CommandPolice command = new CommandPolice(
	                                true,
	                                agentID,
	                                target.getID(),
	                                CommandPolice.ACTION_AUTONOMY
	                        );
	                        this.messages.add(command);
	                    }
	                }
	            }
	        }
	        return this;
	    }

	    @Override
	    public Collection<CommunicationMessage> getResult() {
	        return this.messages;
	    }


	

}
