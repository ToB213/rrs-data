package AIT_2023.module.algorithm;

import adf.core.agent.info.*;
import adf.core.component.module.algorithm.Clustering;
import adf.core.component.module.algorithm.StaticClustering;
import adf.core.agent.module.ModuleManager;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.precompute.PrecomputeData;
import adf.core.debug.DefaultLogger;
import org.apache.log4j.Logger;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

import java.util.*;

import static java.util.Comparator.*;
// @ DEBUG {{{
//import com.mrl.debugger.remote.VDClient;
// }}}

public class HierarchicalKmeans extends StaticClustering {

  private Map<EntityID, Integer> assignment = new HashMap<>();
  private KmeansPP clusterer;

  private KmeansPP miniclusterer;

  private int n = 0;

  private StandardEntityURN urn;

  private List<Integer> assignedCluster = new ArrayList<>();

  private Random rand = new Random(this.agentInfo.getID().getValue());

  private Logger logger;

  // @ DEBUG {{{
  // private VDClient vdclient = VDClient.getInstance();
  // }}}


  public HierarchicalKmeans(
      AgentInfo ai, WorldInfo wi, ScenarioInfo si,
      ModuleManager mm, DevelopData dd) {
    super(ai, wi, si, mm, dd);
    this.urn = this.agentInfo.me().getStandardURN();
    this.logger = DefaultLogger.getLogger(agentInfo.me());

    // @ DEBUG {{{
    // this.vdclient.init();
    // }}}
  }

  @Override
  public Clustering calc() {
    return this;
  }

  @Override
  public int getClusterNumber() {
    return this.n;
  }

  @Override
  public int getClusterIndex(StandardEntity entity) {
    return this.getClusterIndex(entity.getID());
  }

  @Override
  public int getClusterIndex(EntityID id) {
    if (!this.assignment.containsKey(id)) {
      return -1;
    }
    return this.assignment.get(id);
  }

  @Override
  public Collection<StandardEntity> getClusterEntities(int i) {
    if (i < 0 || i >= this.n) {
      return null;
    }

    Collection<EntityID> ids = this.getClusterEntityIDs(i);
    Collection<StandardEntity> ret = new ArrayList<>(ids.size());
    for (EntityID id : ids) {
      ret.add(this.worldInfo.getEntity(id));
    }
    return ret;
  }

  @Override
  public Collection<EntityID> getClusterEntityIDs(int i) {
    if (i < 0 || i >= this.n) {
      return null;
    }
    return this.clusterer.getClusterMembers(i);
  }

  private void initN(int i) {
    this.n = i;
  }

  private int getNumber() {
    int number = 0;
    switch (this.urn) {
      case FIRE_BRIGADE:
        number = this.scenarioInfo.getScenarioAgentsFb();
        break;
      case POLICE_FORCE:
        number = this.scenarioInfo.getScenarioAgentsPf();
        break;
      case AMBULANCE_TEAM:
        number = this.scenarioInfo.getScenarioAgentsAt();
        break;
      default:
        number = 0;
    }
    return number;
  }

  private void initClusterer() {
    List<StandardEntity> entities = new ArrayList<>(
        this.worldInfo.getEntitiesOfType(
            HYDRANT,
            BUILDING, GAS_STATION,
            REFUGE,
            POLICE_OFFICE, FIRE_STATION, AMBULANCE_CENTRE));
    entities.sort(comparing(e -> e.getID().getValue()));

    int size = entities.size();
    EntityID[] is = new EntityID[size];
    double[] xs = new double[size];
    double[] ys = new double[size];

    for (int i = 0; i < size; ++i) {
      Area area = (Area) entities.get(i);
      is[i] = area.getID();
      xs[i] = area.getX();
      ys[i] = area.getY();
    }

    this.clusterer = new KmeansPP(is, xs, ys, this.n);
  }

  private void pair() {
    List<StandardEntity> agents = new ArrayList<>(
        this.worldInfo.getEntitiesOfType(this.urn));
    agents.sort(comparing(e -> e.getID().getValue()));
    final int size = (agents.size() > this.n) ? agents.size() : this.n;
    
    int[][] costs = new int[size][size];
    int[] entity = new int[this.n];
    double[] rate = new double[this.n];
    int[] clusterNumber = new int[this.n];
    
    for (int i = 0; i < this.n; i++){
      entity[i] = this.clusterer.getClusterMembers(i).size();
    }

    int entityNumber = Arrays.stream(entity).sum();
    for (int i = 0; i < this.n; i++){
      rate[i] = (double)entity[i] / entityNumber;
      clusterNumber[i] = (int)(rate[i] * (agents.size() - this.n));
      clusterNumber[i] += 1;
    }
    int[] clusterDetermined = clusterNumber;
    int sum = Arrays.stream(clusterNumber).sum();
    if (sum < agents.size()){
      for (int i = 0; i < agents.size() - sum; i++){
        int select = rouletteChoice(clusterDetermined, entity);
        clusterDetermined[select] += 1;
      }
    }
    int index = 0;
    int front = 0;
    int back = clusterDetermined[index];
    while (true){  
      for (int i = front; i < back; i++){
        this.assignedCluster.add(index);
      }
      if (back != agents.size()){
        index++;
        front = back;
        back += clusterDetermined[index];
        continue;
      }
      break;
    }
    List<Collection<EntityID>> final_cluster = new ArrayList<>(this.getNumber());
    int count = 0;
    for(int ind : clusterDetermined) {
      this.hierarchicalCluster(count, ind);
      this.miniclusterer.execute(REP_PRECOMPUTE);
      for (int i = 0; i < ind; i++) {
        final_cluster.add(this.miniclusterer.getClusterMembers(i));
      }
      count++;
    }

    for (int row = 0; row < size; ++row) {
      Human agent = (Human) agents.get(row);
      for (int col = 0; col < size; ++col) {
        costs[row][col] = this.getMinimumCost(final_cluster.get(col), agent);
      }
    }
    int[] result = Hungarian.execute(costs);
    for (int row = 0; row < agents.size(); ++row) {
      EntityID id = agents.get(row).getID();
      this.assignment.put(id, result[row]);
    }
    initN(this.getNumber());
    this.clusterer = new KmeansPP(this.n, final_cluster);
  }

  private final static int REP_PRECOMPUTE = 20;

  private final static String MODULE_NAME =
      "AIT_2023.module.algorithm.KmeansClustering";
  private final static String PD_CLUSTER_N = MODULE_NAME + ".n";
  private final static String PD_CLUSTER_M = MODULE_NAME + ".m";
  private final static String PD_CLUSTER_A = MODULE_NAME + ".a";

  private final static String PD_CLUSTER_C = MODULE_NAME + ".c";

  private String addSuffixToKey(String path) {
    return path + "." + this.urn;
  }

  private String addSuffixToKey(String path, int i) {
    return this.addSuffixToKey(path) + "." + i;
  }

  @Override
  public Clustering precompute(PrecomputeData pd) {
    super.precompute(pd);
    if (this.getCountPrecompute() > 1) {
      return this;
    }

    this.clusterNumber();

    this.pair();

    pd.setInteger(this.addSuffixToKey(PD_CLUSTER_N), this.n);
    for (EntityID agent : this.assignment.keySet()) {
      int i = this.assignment.get(agent);
      Collection<EntityID> cluster = this.clusterer.getClusterMembers(i);
      pd.setEntityIDList(
          this.addSuffixToKey(PD_CLUSTER_M, i),
          new ArrayList<>(cluster));
      pd.setEntityID(this.addSuffixToKey(PD_CLUSTER_A, i), agent);
    }
    return this;
  }

  @Override
  public Clustering resume(PrecomputeData pd) {
    super.resume(pd);
    if (this.getCountResume() > 1) {
      return this;
    }

    this.n = pd.getInteger(this.addSuffixToKey(PD_CLUSTER_N));
    List<Collection<EntityID>> clusters = new ArrayList<>(this.n);
    for (int i = 0; i < this.n; ++i) {
      List<EntityID> cluster =
              pd.getEntityIDList(this.addSuffixToKey(PD_CLUSTER_M, i));
      EntityID agent =
              pd.getEntityID(this.addSuffixToKey(PD_CLUSTER_A, i));

      clusters.add(cluster);
      this.assignment.put(agent, i);
    }

    this.clusterer = new KmeansPP(this.n, clusters);
    // @ DEBUG {{{
    // this.runVisualDebug();
    // }}}
    return this;
  }

  // private final static int REP_PREPARE = 20;

  @Override
  public Clustering preparate() {
    super.preparate();
    if (this.getCountPreparate() > 1) {
      return this;
    }

    this.initN(getNumber());
    this.initClusterer();
    this.clusterer.execute(REP_PRECOMPUTE);

    this.pair();
    return this;
  }

  public void clusterNumber(){
    if(this.getNumber() >= 50){
      initN(50);
    }else{
      initN(this.getNumber());
    }
    this.initClusterer();
    this.clusterer.execute(REP_PRECOMPUTE);
  }

  public int rouletteChoice(int[] assign, int[] entity){
    double sum = 0.0;
    int[] weight = new int[assign.length];
    double[] rate = new double[assign.length];
    for(int i = 0; i < assign.length; i++){
      weight[i] = entity[i] / assign[i];
      sum += weight[i];
    }
    for(int i = 0; i < assign.length; i++){
      rate[i] = weight[i] / sum;
    }
    double randomNum = rand.nextDouble();
    for (int i = 0; i < weight.length; i++){
      randomNum -= rate[i];
      if(randomNum <= 0){
        return i;
      }
    }
    return 0;
  }


  public int getMinimumCost(Collection<EntityID> col, Human agent){
    int min = Integer.MAX_VALUE;
    double x = agent.getX();
    double y = agent.getY();
    for (EntityID e : col){
      final Area area = (Area) this.worldInfo.getEntity(e);
      final double xs = area.getX();
      final double ys = area.getY();
      int cost = (int) (Math.abs(xs - x) + Math.abs(ys - y));
      if (cost < min){
        min = cost;
      }
    }
    return min;
  }

  private void hierarchicalCluster(int index, int number) {
    List<StandardEntity> entities = new ArrayList<>(this.clusterer.getClusterMembers(index)
            .stream()
            .map(this.worldInfo::getEntity)
            .toList());
    entities.sort(comparing(e -> e.getID().getValue()));

    int size = entities.size();
    EntityID[] is = new EntityID[size];
    double[] xs = new double[size];
    double[] ys = new double[size];

    for (int i = 0; i < size; ++i) {
      Area area = (Area) entities.get(i);
      is[i] = area.getID();
      xs[i] = area.getX();
      ys[i] = area.getY();
    }

    this.miniclusterer = new KmeansPP(is, xs, ys, number);
  }

  // @ DEBUG {{{
  // private void runVisualDebug()
  // {
  //     if (this.urn != POLICE_FORCE) return;

  //     EntityID me = this.agentInfo.getID();
  //     Collection<StandardEntity> cluster =
  //         this.getClusterEntities(this.getClusterIndex(me));

  //     ConvexHull convexhull = new ConvexHull();
  //     for (StandardEntity entity : cluster)
  //     {
  //         convexhull.add((Area)entity);
  //     }

  //     ArrayList<Polygon> data = new ArrayList<>(1);
  //     data.add(convexhull.get());
  //     this.vdclient.draw(me.getValue(), "SamplePolygon", data);
  // }
  // }}}
}
