package mrl_2023.centralized;


import adf.core.agent.communication.MessageManager;
import adf.core.agent.communication.standard.bundle.centralized.CommandScout;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.info.AgentInfo;
import adf.core.agent.info.ScenarioInfo;
import adf.core.agent.info.WorldInfo;
import adf.core.agent.module.ModuleManager;
import adf.core.agent.precompute.PrecomputeData;
import adf.core.component.centralized.CommandExecutor;
import adf.core.component.module.algorithm.PathPlanning;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;

public class MrlCommandExecutorScout extends CommandExecutor<CommandScout> {
    private static final int ACTION_UNKNOWN = -1;
    private static final int ACTION_SCOUT = 1;

    private PathPlanning pathPlanning;

    private int type;
    private Collection<EntityID> scoutTargets;
    private EntityID commanderID;

    public MrlCommandExecutorScout(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
    }

    @Override
    public CommandExecutor setCommand(CommandScout command) {
        return this;
    }

    @Override
    public CommandExecutor updateInfo(MessageManager messageManager){
        super.updateInfo(messageManager);
        return this;
    }

    @Override
    public CommandExecutor precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        return this;
    }

    @Override
    public CommandExecutor resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        return this;
    }

    @Override
    public CommandExecutor preparate() {
        super.preparate();
        return this;
    }

    @Override
    public CommandExecutor calc() {
        this.result = null;
        return this;
    }
}
