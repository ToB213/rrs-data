package AIT_2023.module.complex.dcop;

import adf.core.agent.communication.MessageManager;
import adf.core.agent.communication.standard.bundle.*;
import adf.core.agent.communication.standard.bundle.information.*;
import adf.core.agent.communication.standard.bundle.centralized.*;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.info.*;
import adf.core.agent.module.ModuleManager;
import adf.core.agent.precompute.PrecomputeData;
import adf.core.component.communication.CommunicationMessage;
import adf.core.component.module.complex.PoliceTargetAllocator;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import static rescuecore2.standard.entities.StandardEntityURN.*;

import rescuecore2.misc.geometry.*;
import es.csic.iiia.bms.*;
import es.csic.iiia.bms.factors.*;
import es.csic.iiia.bms.factors.CardinalityFactor.CardinalityFunction;
import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.*;

public class MaxSumPoliceTargetAllocator extends PoliceTargetAllocator {

  private final static StandardEntityURN URL = POLICE_OFFICE;
  private final static StandardEntityURN AGENT_URL = POLICE_FORCE;
  private final static int ITERATIONS = 100;
  private final static double PENALTY = 300.0;
  private final static EntityID SEARCHING_TASK = new EntityID(-1);

  private final Map<EntityID, EntityID> result = new HashMap<>();
  private Set<EntityID> agents = new HashSet<>();
  private final Set<EntityID> tasks = new HashSet<>();
  private final Set<EntityID> ignored = new HashSet<>();

  private final Map<EntityID, Factor<EntityID>> nodes = new HashMap<>();
  private final BufferedCommunicationAdapter adapter;

  private final Map<EntityID, Double> rates = new HashMap<>();
  private final Set<EntityID> requested = new HashSet<>();

  private final Set<EntityID> received = new HashSet<>();

  public MaxSumPoliceTargetAllocator(
      AgentInfo ai, WorldInfo wi, ScenarioInfo si,
      ModuleManager mm, DevelopData dd) {
    super(ai, wi, si, mm, dd);
    this.adapter = new BufferedCommunicationAdapter();
  }

  @Override
  public Map<EntityID, EntityID> getResult() {
    return this.result;
  }

  @Override
  public PoliceTargetAllocator calc() {
    this.result.clear();
    if (this.agents.isEmpty()) {
      this.initializeAgents();
    }
    if (!this.have2allocate()) {
      return this;
    }

    this.initializeTasks();
    this.initializeFactorGraph();
    for (int i = 0; i < ITERATIONS; ++i) {
      this.nodes.values().stream().forEach(Factor::run);
      this.adapter.execute(this.nodes);
    }

    for (EntityID agent : this.agents) {
      final Factor<EntityID> node = this.nodes.get(agent);
      EntityID task = selectTask((ProxyFactor<EntityID>) node);
      if (task.equals(SEARCHING_TASK)) {
        task = null;
      }
      this.result.put(agent, task);
    }
    //  @ DEBUG
    int n = 0;
    for (EntityID id : this.agents) {
      if (this.result.get(id) != null) {
        ++n;
      }
    }
    System.out.println("POLICE ALLOCATOR -> " + n);
    //  @ END OF DEBUG

    return this;
  }

  @Override
  public PoliceTargetAllocator updateInfo(MessageManager mm) {
    super.updateInfo(mm);
    if (this.getCountUpdateInfo() >= 2) {
      return this;
    }

    this.received.clear();

    final Collection<CommunicationMessage> rmessages =
        mm.getReceivedMessageList(MessageRoad.class);
    for (CommunicationMessage tmp : rmessages) {
      MessageRoad message = (MessageRoad) tmp;
      MessageUtil.reflectMessage(this.worldInfo, message);
    }

    final Collection<CommunicationMessage> pfmessages =
        mm.getReceivedMessageList(MessagePoliceForce.class);
    for (CommunicationMessage tmp : pfmessages) {
      MessagePoliceForce message = (MessagePoliceForce) tmp;
      MessageUtil.reflectMessage(this.worldInfo, message);

      final EntityID id = message.getAgentID();
      this.received.add(id);
      Human pf = (Human) this.worldInfo.getEntity(id);
      pf.undefineX();
      pf.undefineY();
    }

    final Collection<CommunicationMessage> pfcommands =
        mm.getReceivedMessageList(CommandPolice.class);
    for (CommunicationMessage tmp : pfcommands) {
      CommandPolice command = (CommandPolice) tmp;
      if (!command.isBroadcast()) {
        continue;
      }
      if (!command.isTargetIDDefined()) {
        continue;
      }
      if (command.getAction() != CommandPolice.ACTION_CLEAR) {
        continue;
      }

      this.requested.add(command.getTargetID());
    }

    final Collection<CommunicationMessage> repmessages =
        mm.getReceivedMessageList(MessageReport.class);
    for (CommunicationMessage tmp : repmessages) {
      MessageReport message = (MessageReport) tmp;
      if (message.isFromIDDefined()) {
        this.ignored.add(message.getFromID());
      }
    }

    return this;
  }

  @Override
  public PoliceTargetAllocator resume(PrecomputeData pd) {
    super.resume(pd);
    if (this.getCountResume() >= 2) {
      return this;
    }

    final Map<EntityID, Double> areas = new HashMap<>();
    this.worldInfo.getEntitiesOfType(ROAD, HYDRANT)
        .stream()
        .map(Area.class::cast)
        .forEach(a -> areas.put(a.getID(), computeArea(a)));

    final double max = areas.values()
        .stream()
        .max(Double::compare)
        .orElse(1.0);

    for (EntityID id : areas.keySet()) {
      final double area = areas.get(id);
      this.rates.put(id, area / max);
    }

    return this;
  }

  @Override
  public PoliceTargetAllocator preparate() {
    super.preparate();
    if (this.getCountPreparate() >= 2) {
      return this;
    }

    final Map<EntityID, Double> areas = new HashMap<>();
    this.worldInfo.getEntitiesOfType(ROAD, HYDRANT)
        .stream()
        .map(Area.class::cast)
        .forEach(a -> areas.put(a.getID(), computeArea(a)));

    final double max = areas.values()
        .stream()
        .max(Double::compare)
        .orElse(1.0);

    for (EntityID id : areas.keySet()) {
      final double area = areas.get(id);
      this.rates.put(id, area / max);
    }

    return this;
  }

  private static double computeArea(Area area) {
    return GeometryTools2D.computeArea(
        GeometryTools2D.vertexArrayToPoints(area.getApexList()));
  }

  private boolean have2allocate() {
    if (!this.allCentersExists()) {
      return false;
    }
    final int nAgents = this.agents.size();
    if (this.received.size() != nAgents) {
      return false;
    }

    final int lowest = this.worldInfo.getEntityIDsOfType(URL)
        .stream()
        .mapToInt(EntityID::getValue)
        .min().orElse(-1);

    final int me = this.agentInfo.getID().getValue();
    final int time = this.agentInfo.getTime();
    final int ignored = this.scenarioInfo.getKernelAgentsIgnoreuntil();
    return time >= ignored && me == lowest;
  }

  private boolean allCentersExists() {
    final int fss = this.scenarioInfo.getScenarioAgentsFs();
    final int pos = this.scenarioInfo.getScenarioAgentsPo();
    final int acs = this.scenarioInfo.getScenarioAgentsAc();
    return fss > 0 && pos > 0 && acs > 0;
  }

  private void initializeAgents() {
    final Collection<EntityID> tmp =
        this.worldInfo.getEntityIDsOfType(AGENT_URL);
    this.agents = new HashSet<>(tmp);
  }

  private void initializeTasks() {
    this.tasks.clear();

    this.worldInfo.getEntitiesOfType(POLICE_FORCE)
        .stream()
        .map(Human.class::cast)
        .filter(Human::isPositionDefined)
        .map(Human::getPosition)
        .flatMap(this::extractTasks)
        .forEach(this.tasks::add);

    this.tasks.addAll(this.requested);
    this.tasks.removeAll(this.ignored);
  }

  private Stream<EntityID> extractTasks(EntityID position) {
    StandardEntity tmp = this.worldInfo.getEntity(position);
    if (!Area.class.isInstance(tmp)) {
      return null;
    }
    if (!Road.class.isInstance(tmp) || this.ignored.contains(position)) {
      final Area area = (Area) tmp;
      return area.getNeighbours()
          .stream()
          .map(this.worldInfo::getEntity)
          .filter(Road.class::isInstance)
          .map(StandardEntity::getID);
    }

    return Stream.of(position);
  }

  private void initializeFactorGraph() {
    this.initializeVariableNodes(this.agents);
    this.initializeFactorNodes(this.tasks);
    this.connectNodes(this.agents, this.tasks);
  }

  private void initializeVariableNodes(Collection<EntityID> ids) {
    for (EntityID id : ids) {
      final Factor<EntityID> tmp = new BMSSelectorFactor<>();
      final WeightingFactor<EntityID> vnode = new WeightingFactor<>(tmp);
      vnode.setMaxOperator(new Minimize());
      vnode.setIdentity(id);
      vnode.setCommunicationAdapter(this.adapter);
      this.nodes.put(id, vnode);
    }
  }

  private void initializeFactorNodes(Collection<EntityID> ids) {
    for (EntityID id : ids) {
      final CardinalityFactor<EntityID> fnode = new BMSCardinalityFactor<>();
      final CardinalityFunction func = new CardinalityFunction() {
        @Override
        public double getCost(int nActiveVariables) {
          return MaxSumPoliceTargetAllocator
              .this.computePenalty(id, nActiveVariables);
        }
      };
      fnode.setFunction(func);

      fnode.setMaxOperator(new Minimize());
      fnode.setIdentity(id);
      fnode.setCommunicationAdapter(this.adapter);
      this.nodes.put(id, fnode);
    }
  }

  private void connectNodes(
      Collection<EntityID> vnodeids, Collection<EntityID> fnodeids) {
    for (EntityID vnodeid : vnodeids) {
      final List<EntityID> closer = fnodeids
          .stream()
          .sorted((i1, i2) -> {
            final double d1 = this.worldInfo.getDistance(i1, vnodeid);
            final double d2 = this.worldInfo.getDistance(i2, vnodeid);
            return Double.compare(d1, d2);
          })
          .collect(toList());

      for (int i = 0; i < Math.min(3, closer.size()); ++i) {
        final EntityID fnodeid = closer.get(i);
        WeightingFactor<EntityID> vnode =
            (WeightingFactor<EntityID>) this.nodes.get(vnodeid);
        vnode.addNeighbor(fnodeid);

        Factor<EntityID> fnode = this.nodes.get(fnodeid);
        fnode.addNeighbor(vnodeid);

        final double penalty = this.computePenalty(vnodeid, fnodeid);
        vnode.setPotential(fnodeid, penalty);
      }
    }
  }

  private static EntityID selectTask(ProxyFactor<EntityID> proxy) {
    final SelectorFactor<EntityID> selector =
        (SelectorFactor<EntityID>) proxy.getInnerFactor();
    return selector.select();
  }

  private double computePenalty(EntityID agent, EntityID task) {
    if (task.equals(SEARCHING_TASK)) {
      return 0.0;
    }

    final double d = this.worldInfo.getDistance(agent, task);
    return d / (42000.0 / 1.5);
  }

  private double computePenalty(EntityID task, int nAgents) {
    if (task.equals(SEARCHING_TASK)) {
      return 0.0;
    }

    final Road entity = (Road) this.worldInfo.getEntity(task);
    if (nAgents == 0) {
      return PENALTY;
    }

    final double nLeasts = 1.0;
    final double ratio = Math.min((double) nAgents, nLeasts) / nLeasts;

    double rate = this.rates.get(task);

    final boolean isEntrance = entity.getNeighbours()
        .stream()
        .map(this.worldInfo::getEntity)
        .anyMatch(Building.class::isInstance);

    if (isEntrance) {
      rate = 1.5;
    }
    if (this.requested.contains(task)) {
      rate = 2.0;
    }

    return PENALTY * rate * (1.0 - Math.pow(ratio, 2.0));
  }
}
