package AIT_2023.centralized;

import adf.core.agent.info.*;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.precompute.PrecomputeData;
import adf.core.agent.module.ModuleManager;
import adf.core.agent.action.Action;
import adf.core.agent.action.common.*;
import adf.core.agent.action.fire.ActionRefill;
import adf.core.agent.communication.MessageManager;
import adf.core.agent.communication.standard.bundle.centralized.*;
import adf.core.component.centralized.CommandExecutor;
import adf.core.component.extaction.ExtAction;
import adf.core.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

import rescuecore2.worldmodel.EntityID;
import java.util.*;

import com.fasterxml.jackson.databind.ser.std.StdKeySerializers.Default;

public class AITCommandExecutorFire extends CommandExecutor<CommandFire> {

  private static final int ACTION_UNKNOWN = -1;
  private static final int ACTION_REST = CommandAmbulance.ACTION_REST;
  private static final int ACTION_MOVE = CommandAmbulance.ACTION_MOVE;
  private static final int ACTION_RESCUE = CommandAmbulance.ACTION_RESCUE;
  private static final int ACTION_AUTONOMY = CommandAmbulance.ACTION_AUTONOMY;

  private EntityID target;
  private int type;
  private EntityID commander;

  private ExtAction extaction;
  private ExtAction actionFireRescue;
  private PathPlanning pathPlanning;

  public AITCommandExecutorFire(
      AgentInfo ai, WorldInfo wi, ScenarioInfo si,
      ModuleManager mm, DevelopData dd) {
    super(ai, wi, si, mm, dd);
    this.type = ACTION_UNKNOWN;

    this.extaction = mm.getExtAction("AIT.FB.CommandExecutorFire.ExtActionFireFighting");

    this.actionFireRescue = mm.getExtAction("AIT.FB.CommandExecutorFire.ExtActionFireRescue");

    this.pathPlanning = mm.getModule("AIT.FB.CommandExecutorFire.PathPlanning");
  }

  @Override
  public CommandExecutor setCommand(CommandFire command) {
    final EntityID me = this.agentInfo.getID();
    if (!command.isToIDDefined()) {
      return this;
    }
    if (!me.equals(command.getToID())) {
      return this;
    }

    this.target = command.getTargetID();
    this.type = command.getAction();
    this.commander = command.getSenderID();
    return this;
  }

  @Override
  public CommandExecutor precompute(PrecomputeData pd) {
    super.precompute(pd);
    if (this.getCountPrecompute() >= 2) {
      return this;
    }

    this.extaction.precompute(pd);
    this.actionFireRescue.precompute(pd);
    this.pathPlanning.precompute(pd);
    return this;
  }

  @Override
  public CommandExecutor resume(PrecomputeData pd) {
    super.resume(pd);
    if (this.getCountResume() >= 2) {
      return this;
    }

    this.extaction.resume(pd);
    this.actionFireRescue.resume(pd);
    this.pathPlanning.resume(pd);
    return this;
  }

  @Override
  public CommandExecutor preparate() {
    super.preparate();
    if (this.getCountPreparate() >= 2) {
      return this;
    }

    this.extaction.preparate();
    this.actionFireRescue.preparate();
    this.pathPlanning.preparate();
    return this;
  }

  @Override
  public CommandExecutor updateInfo(MessageManager mm) {
    super.updateInfo(mm);
    if (this.getCountUpdateInfo() >= 2) {
      return this;
    }

    this.extaction.updateInfo(mm);
    this.actionFireRescue.updateInfo(mm);
    this.pathPlanning.updateInfo(mm);

    if (this.target != null && this.isCommandCompleted()) {
      mm.addMessage(new MessageReport(true, true, true, this.target));
      this.target = null;
      this.type = ACTION_UNKNOWN;
      this.commander = null;
    }

    return this;
  }

  @Override
  public CommandExecutor calc() {
    this.result = null;
    if (this.target == null) {
      return this;
    }
    if (this.type == ACTION_UNKNOWN) {
      return this;
    }

    if (this.type == ACTION_RESCUE) {
      this.actionFireRescue.setTarget(this.target);
      this.actionFireRescue.calc();
      this.result = this.actionFireRescue.getAction();
    } else {
      this.extaction.setTarget(this.target);
      this.extaction.calc();
      this.result = this.extaction.getAction();
    }
    return this;
  }

  private boolean isCommandCompleted() {
    if (this.needIdle()) {
      return false;
    }

    //this.extaction.setTarget(this.target);
    //this.extaction.calc();
    //final Action action = this.extaction.getAction();
    //return this.isEmptyAction(action);

    final Set<EntityID> changes =
        this.worldInfo.getChanged().getChangedEntities();
    if (!changes.contains(this.target)) {
      return false;
    }

    final StandardEntity entity = this.worldInfo.getEntity(this.target);
    if (!Building.class.isInstance(entity)) {
      return true;
    }
    final Building building = (Building) entity;

    return !building.isOnFire();
  }

  private boolean isEmptyAction(Action action) {
    if (action == null) {
      return true;
    }
    if (action instanceof ActionRest) {
      return true;
    }
    if (action instanceof ActionMove) {
      final ActionMove move = (ActionMove) action;
      final int ax = (int) this.agentInfo.getX();
      final int ay = (int) this.agentInfo.getY();
      final int mx = move.getPosX();
      final int my = move.getPosY();
      return ax == mx && ay == my;
    }
    return false;
  }

  private boolean needIdle() {
    final int time = this.agentInfo.getTime();
    final int ignored = this.scenarioInfo.getKernelAgentsIgnoreuntil();
    return time < ignored;
  }
}
