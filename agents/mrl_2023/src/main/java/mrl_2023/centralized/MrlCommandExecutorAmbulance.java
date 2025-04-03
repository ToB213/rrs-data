package mrl_2023.centralized;

import adf.core.agent.communication.MessageManager;
import adf.core.agent.communication.standard.bundle.centralized.CommandAmbulance;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.info.AgentInfo;
import adf.core.agent.info.ScenarioInfo;
import adf.core.agent.info.WorldInfo;
import adf.core.agent.module.ModuleManager;
import adf.core.agent.precompute.PrecomputeData;
import adf.core.component.centralized.CommandExecutor;
import adf.core.component.extaction.ExtAction;
import adf.core.component.module.algorithm.PathPlanning;
import rescuecore2.worldmodel.EntityID;

public class MrlCommandExecutorAmbulance extends CommandExecutor<CommandAmbulance> {
    private static final int ACTION_UNKNOWN = -1;
    private static final int ACTION_REST = CommandAmbulance.ACTION_REST;
    private static final int ACTION_MOVE = CommandAmbulance.ACTION_MOVE;
    private static final int ACTION_RESCUE = CommandAmbulance.ACTION_RESCUE;
    private static final int ACTION_LOAD = CommandAmbulance.ACTION_LOAD;
    private static final int ACTION_UNLOAD = CommandAmbulance.ACTION_UNLOAD;
    private static final int ACTION_AUTONOMY = CommandAmbulance.ACTION_AUTONOMY;

    private PathPlanning pathPlanning;

    private ExtAction actionTransport;
    private ExtAction actionExtMove;

    private int type;
    private EntityID target;
    private EntityID commanderID;

    public MrlCommandExecutorAmbulance(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
    }

    @Override
    public CommandExecutor setCommand(CommandAmbulance command) {
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
