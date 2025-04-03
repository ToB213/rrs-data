package AIT_2023.module.complex.fb;

import AIT_2023.module.comm.information.MessageClearRequest;
import adf.core.agent.communication.MessageManager;
import adf.core.agent.communication.standard.bundle.StandardMessageBundle;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.info.AgentInfo;
import adf.core.agent.info.ScenarioInfo;
import adf.core.agent.info.WorldInfo;
import adf.core.agent.module.ModuleManager;
import adf.core.agent.precompute.PrecomputeData;
import adf.core.component.module.complex.FireTargetAllocator;
import rescuecore2.worldmodel.EntityID;

import java.util.HashMap;
import java.util.Map;

public class AITFireTargetAllocator extends FireTargetAllocator {

  private boolean hasRegisterRequest = false;

  public AITFireTargetAllocator(AgentInfo ai, WorldInfo wi, ScenarioInfo si,
      ModuleManager moduleManager, DevelopData developData) {
    super(ai, wi, si, moduleManager, developData);
  }

  @Override
  public FireTargetAllocator resume(PrecomputeData precomputeData) {
    super.resume(precomputeData);
    return this;
  }

  @Override
  public FireTargetAllocator preparate() {
    super.preparate();
    return this;
  }

  @Override
  public Map<EntityID, EntityID> getResult() {
    return new HashMap<>();
  }

  @Override
  public FireTargetAllocator calc() {
    return this;
  }

  @Override
  public FireTargetAllocator updateInfo(MessageManager mm) {
    super.updateInfo(mm);

    // MessageRequestをMessageManagerに登録
    if (!this.hasRegisterRequest) {
      final var index = new StandardMessageBundle().getMessageClassList().size() + 1;
      mm.registerMessageClass(index, MessageClearRequest.class);
      this.hasRegisterRequest = true;
    }

    return this;
  }
}
