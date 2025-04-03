package mrl_2023.complex;

import adf.core.agent.develop.DevelopData;
import adf.core.agent.info.AgentInfo;
import adf.core.agent.info.ScenarioInfo;
import adf.core.agent.info.WorldInfo;
import adf.core.agent.module.ModuleManager;
import adf.core.component.module.complex.PoliceTargetAllocator;
import rescuecore2.worldmodel.EntityID;

import java.util.Map;

public class MrlPoliceTargetAllocator extends PoliceTargetAllocator {


    public MrlPoliceTargetAllocator(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
    }

    @Override
    public Map<EntityID, EntityID> getResult() {
        return null;
    }

    @Override
    public PoliceTargetAllocator calc() {
        return this;
    }
}
