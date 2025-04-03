package AIT_2023.module.complex.pf;

import AIT_2023.module.comm.information.MessageClearRequest;
import adf.core.agent.communication.MessageManager;
import adf.core.agent.communication.standard.bundle.StandardMessageBundle;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.info.AgentInfo;
import adf.core.agent.info.ScenarioInfo;
import adf.core.agent.info.WorldInfo;
import adf.core.agent.module.ModuleManager;
import adf.core.agent.precompute.PrecomputeData;
import adf.core.component.module.complex.PoliceTargetAllocator;
import rescuecore2.worldmodel.EntityID;

import java.util.HashMap;
import java.util.Map;

public class AITPoliceTargetAllocator extends PoliceTargetAllocator {

  private boolean hasRegisterRequest = false;

  public AITPoliceTargetAllocator(AgentInfo ai, WorldInfo wi, ScenarioInfo si,
      ModuleManager moduleManager, DevelopData developData) {
    super(ai, wi, si, moduleManager, developData);
  }

  @Override
  public PoliceTargetAllocator resume(PrecomputeData precomputeData) {
    super.resume(precomputeData);
    if (this.getCountResume() >= 2) {
      return this;
    }
    return this;
  }

  @Override
  public PoliceTargetAllocator preparate() {
    super.preparate();
    if (this.getCountPrecompute() >= 2) {
      return this;
    }
    return this;
  }

  @Override
  public Map<EntityID, EntityID> getResult() {
    return new HashMap<>();
  }

  @Override
  public PoliceTargetAllocator calc() {
    return this;
  }

  @Override
  public PoliceTargetAllocator updateInfo(MessageManager mm) {
    super.updateInfo(mm);

    // MessageRequestをMessageManagerに登録
    if (!this.hasRegisterRequest) {
      final var index = new StandardMessageBundle().getMessageClassList().size() + 1;
      mm.registerMessageClass(index, MessageClearRequest.class);
      this.hasRegisterRequest = true;
    }

    if (this.getCountUpdateInfo() >= 2) {
      return this;
    }
    return this;
  }
}
