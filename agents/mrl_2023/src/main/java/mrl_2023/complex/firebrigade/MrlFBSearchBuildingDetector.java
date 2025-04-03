package mrl_2023.complex.firebrigade;

import adf.core.agent.develop.DevelopData;
import adf.core.agent.info.AgentInfo;
import adf.core.agent.info.ScenarioInfo;
import adf.core.agent.info.WorldInfo;
import adf.core.agent.module.ModuleManager;
import adf.core.component.module.complex.BuildingDetector;
import rescuecore2.worldmodel.EntityID;

/**
 * Created by Peyman on 7/12/2017.
 */
public class MrlFBSearchBuildingDetector extends BuildingDetector {

    public MrlFBSearchBuildingDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
    }

    @Override
    public EntityID getTarget() {
        return null;
    }

    @Override
    public BuildingDetector calc() {
        return null;
    }
}
