package AIT_2023.module.complex.fb;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.naming.ContextNotEmptyException;

import AIT_2023.module.algorithm.*;

import static java.util.Comparator.*;

import adf.core.agent.communication.MessageManager;
import adf.core.agent.communication.standard.bundle.information.MessageCivilian;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.info.*;
import adf.core.agent.module.ModuleManager;
import adf.core.agent.precompute.PrecomputeData;
import adf.core.component.module.algorithm.Clustering;
import adf.core.component.module.complex.BuildingDetector;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.*;
import rescuecore2.standard.entities.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

import rescuecore2.worldmodel.EntityID;

public class AITBuildingDetector extends BuildingDetector {

  private final Clustering clustering;
  private final Clustering dispersible;
  private final Random random = new Random();
  private EntityID result = null;

  // Debug
  // private final VDClient vdclient = VDClient.getInstance();
  // /Debug

  public AITBuildingDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si,
      ModuleManager mm, DevelopData dd) {
    super(ai, wi, si, mm, dd);
    this.clustering = moduleManager.getModule("AIT.FB.BuildingDetector.Clustering");
    this.dispersible = mm.getModule("AIT.FB.BuildingDetector.Dispersible");
    this.registerModule(this.clustering);
    this.registerModule(this.dispersible);
    this.random.setSeed(ai.getID().getValue());

    // Debug
    // this.vdclient.init("localhost", 1099);
    // /Debug
  }

  @Override
  public BuildingDetector precompute(PrecomputeData precomputeData) {
    super.precompute(precomputeData);
    if (this.getCountPrecompute() > 1) {
      return this;
    }
    return this;
  }

  @Override
  public BuildingDetector resume(PrecomputeData precomputeData) {
    super.resume(precomputeData);
    if (this.getCountResume() > 1) {
      return this;
    }
    this.preparate();
    return this;
  }

  @Override
  public BuildingDetector preparate() {
    super.preparate();
    if (this.getCountPreparate() > 1) {
      return this;
    }
    this.clustering.calc();
    this.dispersible.calc();
    return this;
  }

  @Override
  public EntityID getTarget() {
    return this.result;
  }

  @Override
  public BuildingDetector updateInfo(MessageManager messageManager) {
    super.updateInfo(messageManager);
    if (this.getCountUpdateInfo() > 1) {
      return this;
    }
    // if (this.result != null)
    // {
    // Building building = (Building) this.worldInfo.getEntity(this.result);
    // if (building.isOnFire())
    // {
    // messageManager.addMessage(new MessageBuilding(true, building));
    // }
    // }
    return this;
  }

  @Override
  public BuildingDetector calc() {
    Set<EntityID> activeAgentIDs = new HashSet<>(
        this.getAgentIDsHavingWater(
            this.worldInfo.getEntityIDsOfType(FIRE_BRIGADE)));
    activeAgentIDs.add(this.agentInfo.getID());

    this.result =
        this.getTargetIDInFireCluster(new ArrayList<>(activeAgentIDs));
    if (this.result != null) {
      this.out("TARGET #" + this.result);
    }
    return this;
  }

  private EntityID getTargetIDInFireCluster(List<EntityID> activeAgentIDs) {
    if (activeAgentIDs.isEmpty()) {
      return null;
    }

    this.clustering.calc();
    int clusterSize = this.clustering.getClusterNumber();
    double minDist = Double.MAX_VALUE;
    List<EntityID> closeCluster = new ArrayList<>();
    for (int i = 0; i < clusterSize; ++i) {
      List<EntityID> cluster = new ArrayList<>(this.clustering.getClusterEntityIDs(i));
      Point2D center = this.getClusterCenterPoint(cluster);

      double dist = this.getDistance(
          new Point2D(this.agentInfo.getX(), this.agentInfo.getY()), center);
      if (dist < minDist) {
        minDist = dist;
        closeCluster = cluster;
      }
    }

    // Debug
    // Collection<Polygon> datas = new ArrayList<>();
    // for (int i = 0; i < clusterSize; ++i)
    // {
    // List<EntityID> cluster =
    // new ArrayList<>(this.clustering.getClusterEntityIDs(i));
    // ConvexHull convexhull = new ConvexHull();
    // cluster.forEach(id -> {
    // Area area = (Area) this.worldInfo.getEntity(id);
    // convexhull.add(area);
    // });
    // convexhull.compute();
    // datas.add(convexhull.get());
    // }
    // this.vdclient.drawAsync(
    // this.agentInfo.getID().getValue(),
    // "ClusterConvexhull",
    // (Serializable) datas);
    // /Debug

    ConvexHull convexHull = new ConvexHull();
    for (EntityID id : closeCluster) {
      StandardEntity entity = this.worldInfo.getEntity(id);
      if (!(entity instanceof Area)) {
        continue;
      }
      Area area = (Area) entity;
      convexHull.add(area);
    }
    convexHull.compute();
    List<EntityID> candidateBuildingIDs = this.getPerimeterEntityIDsOfCluster(
        closeCluster, convexHull.getApexes());

    // Debug
    // this.vdclient.drawAsync(
    // this.agentInfo.getID().getValue(),
    // "SamplePolygon",
    // (Serializable)  Arrays.asList(convexHull.get()));
    // /Debug

    return this.getTargetInNaturalOrderOfFireyness(activeAgentIDs, candidateBuildingIDs);
  }

  private List<EntityID> getBuildingIDsOnFire(Collection<EntityID> buildingIDs) {
    List<EntityID> result = new ArrayList<>();
    for (EntityID id : buildingIDs) {
      StandardEntity entity = this.worldInfo.getEntity(id);
      if (!(entity instanceof Building)) {
        continue;
      }
      Building building = (Building) entity;
      if (building.isOnFire()) {
        result.add(building.getID());
      }
    }
    return result;
  }

  private List<EntityID> getAgentIDsHavingWater(Collection<EntityID> agentIDs) {
    List<EntityID> result = new ArrayList<>(agentIDs);
    for (EntityID id : agentIDs) {
      StandardEntity entity = this.worldInfo.getEntity(id);
      if (!(entity instanceof FireBrigade)) {
        continue;
      }
      FireBrigade agent = (FireBrigade) entity;
      if (agent.getWater() == 0) {
        result.remove(agent.getID());
      }
    }
    return result;
  }

  private EntityID getCloseBuildingID(
      List<EntityID> agentIDs, List<EntityID> buildingIDs) {
    EntityID myId = this.agentInfo.getID();
    final int i = this.dispersible.getClusterIndex(myId);
    final Collection<EntityID> group = this.dispersible.getClusterEntityIDs(i);

    // if no fires this agent found in the group, add fires to candidates
    Collection<EntityID> fires = this.worldInfo
        .getEntitiesOfType(BUILDING).stream()
        .filter(e -> e instanceof Building)
        .map(Building.class::cast)
        .filter(Building::isOnFire)
        .map(Building::getID)
        .collect(Collectors.toList());
    if (Collections.disjoint(group, fires)) {
      group.addAll(fires);
      //this.out("ADD "+group+" AS CANDIDATES");
    }

    Optional<EntityID> ret = buildingIDs.stream()
        .filter(id -> group.contains(id))
        .sorted(comparing(id -> this.worldInfo.getDistance(myId, id)))
        .findFirst();
    return ret.orElse(null);
  }

  private EntityID getTargetInNaturalOrderOfFireyness(
      List<EntityID> agentIDs, List<EntityID> buildingIDs) {
    List<EntityID> heatingBuildingIDs = new ArrayList<>();
    List<EntityID> burningBuildingIDs = new ArrayList<>();
    List<EntityID> infernoBuildingIDs = new ArrayList<>();

    for (EntityID id : buildingIDs) {
      StandardEntity entity = this.worldInfo.getEntity(id);
      if (!(entity instanceof Building)) {
        continue;
      }
      Building building = (Building) entity;
      int fieryness = building.getFieryness();
      if (fieryness == 1) {
        heatingBuildingIDs.add(entity.getID());
      }
      if (fieryness == 2) {
        burningBuildingIDs.add(entity.getID());
      }
      if (fieryness == 3) {
        infernoBuildingIDs.add(entity.getID());
      }
    }

    if (!heatingBuildingIDs.isEmpty()) {
      EntityID ret = this.getCloseBuildingID(agentIDs, heatingBuildingIDs);
      if (ret == null) {
        return heatingBuildingIDs.get(this.random.nextInt(heatingBuildingIDs.size()));
      }
      return ret;
    }
    if (!burningBuildingIDs.isEmpty()) {
      EntityID ret = this.getCloseBuildingID(agentIDs, burningBuildingIDs);
      if (ret == null) {
        return burningBuildingIDs.get(this.random.nextInt(burningBuildingIDs.size()));
      }
      return ret;
    }
    if (!infernoBuildingIDs.isEmpty()) {
      EntityID ret = this.getCloseBuildingID(agentIDs, infernoBuildingIDs);
      if (ret == null) {
        return infernoBuildingIDs.get(this.random.nextInt(infernoBuildingIDs.size()));
      }
      return ret;
    }

    return null;
  }

  private List<EntityID> getPerimeterEntityIDsOfCluster(
      List<EntityID> clusterEntityIDs, Point2D[] clusterApexes) {
    List<EntityID> ret = new ArrayList<>();
    for (EntityID entityID : clusterEntityIDs) {
      StandardEntity entity = this.worldInfo.getEntity(entityID);
      if (!(entity instanceof Building)) {
        continue;
      }
      Building building = (Building) entity;
      int[] entityApexes = building.getApexList();
      for (int i = 0; i < entityApexes.length - 1; i += 2) {
        boolean isPerimeter = false;
        Point2D entityApex = new Point2D(entityApexes[i], entityApexes[i + 1]);
        for (int j = 0; j < clusterApexes.length; ++j) {
          if (this.isNearlyPoint(entityApex, clusterApexes[j])) {
            isPerimeter = true;
            break;
          }
        }
        if (isPerimeter) {
          ret.add(entityID);
          break;
        }
      }
    }

    // Debug
    // List<Integer> buildingIDs = ret.stream()
    // .map(id -> this.worldInfo.getEntity(id))
    // .filter(Building.class::isInstance)
    // .map(Building.class::cast)
    // .map(Building::getID)
    // .map(EntityID::getValue)
    // .collect(Collectors.toList());
    // this.vdclient.drawAsync(
    // this.agentInfo.getID().getValue(),
    // "SampleBuilding",
    // (Serializable) buildingIDs);
    // /Debug

    return ret;
  }

  private Point2D getClusterCenterPoint(List<EntityID> cluster) {
    double x = 0;
    double y = 0;
    for (EntityID id : cluster) {
      Pair<Integer, Integer> location = this.worldInfo.getLocation(id);
      x += location.first();
      y += location.second();
    }
    return new Point2D(x / cluster.size(), y / cluster.size());
  }

  private double getDistance(Point2D point1, Point2D point2) {
    return Math.hypot(point2.getX() - point1.getX(), point2.getY() - point1.getY());
  }

  private boolean isNearlyPoint(Point2D p1, Point2D p2) {
    double dist = GeometryTools2D.getDistance(p1, p2);
    if (GeometryTools2D.nearlyZero(dist)) {
      return true;
    }
    return false;
  }

  private void out(String str) {
    String ret;
    ret = "🚒  [" + String.format("%10d", this.agentInfo.getID().getValue()) + "]";
    ret += " BUILDING-DETECTOR ";
    ret += "@" + String.format("%3d", this.agentInfo.getTime());
    ret += " -> ";
    System.out.println(ret + str);
  }
}
