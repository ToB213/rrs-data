package AIT_2023.module.complex.at;

import java.util.*;
import java.util.stream.Collectors;

import AIT_2023.module.comm.information.MessageClearRequest;
import AIT_2023.module.comm.information.MessageRescueRequest;
import AIT_2023.module.complex.common.EntityFilter;
import AIT_2023.module.complex.common.LifeXpectancy;
import AIT_2023.module.complex.common.Stucked;
import adf.core.agent.communication.standard.bundle.StandardMessageBundle;
import adf.core.agent.communication.standard.bundle.StandardMessagePriority;
import adf.core.agent.communication.standard.bundle.information.MessageAmbulanceTeam;
import adf.core.agent.communication.standard.bundle.information.MessageCivilian;
import adf.core.component.module.complex.HumanDetector;
import adf.core.component.communication.CommunicationMessage;
import adf.core.component.module.algorithm.*;
import adf.core.agent.info.*;
import adf.core.agent.module.ModuleManager;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.communication.MessageManager;
import rescuecore2.worldmodel.AbstractEntity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

import static java.util.Comparator.*;

public class AITAmbulanceTeamDetector extends HumanDetector {

  /**
   * 移動経路を決定するパスプランニングモジュール
   */
  private PathPlanning pathPlanner;
  /**
   * 各エージェントの担当範囲を決定するクラスタリングモジュール
   */
  private Clustering clusterer;
  /**
   * クラスタリングモジュール（未使用）
   */
  private Clustering stuckedHumans;

  /**
   * 視覚から各建物にいる市民を認識して，その市民がRescueタスクなのか，Loadタスクなのかを判定するモジュール
   */
  private CivilianMessages civMessages;
  /**
   * 計算結果．救助対象のEntityID
   */
  private EntityID result = null;
  /**
   * 自分のクラスターの範囲内にあるEntityIDの集合
   */
  private Set<EntityID> cluster = new HashSet<>();

  /**
   * 他エージェントから送信された市民のうち，救助対象となるもの
   * 使っていない
   */
  private Set<EntityID> loadCivTask = new HashSet<>();

  /**
   * 他エージェントから送信された市民のうち，救助対象とならないもの
   * 使っていない
   */
  private Set<EntityID> nonTarget = new HashSet<>();

  /**
   * 無視すべき市民のIDの集合
   * 他エージェントから送信された古い情報により，搬送対象と誤認されている
   */
  private Set<EntityID> ignoreTasks = new HashSet<>();

  /**
   * エージェントの移動速度の平均値
   */
  private static final double AGENT_CAN_MOVE = 40000.0;

  /**
   * 通信用の定義
   */
  private final boolean VOICE = false;
  private final boolean RADIO = true;
  final private int HELP_BURIED = 5;
  final private int HELP_BLOCKADE = 6;

  private boolean hasRegisterRequest = false;

  public AITAmbulanceTeamDetector(
          AgentInfo ai, WorldInfo wi, ScenarioInfo si,
          ModuleManager mm, DevelopData dd) {
    super(ai, wi, si, mm, dd);

    this.pathPlanner = mm.getModule("AIT.AT.HumanDetector.PathPlanning");
    this.registerModule(this.pathPlanner);

    this.clusterer = mm.getModule("AIT.AT.HumanDetector.Clustering");
    this.registerModule(this.clusterer);

    this.stuckedHumans = mm.getModule("AIT.AT.HumanDetector.StuckHumans");
    this.registerModule(this.stuckedHumans);

    this.civMessages = mm.getModule("AIT.AT.HumanDetector.CivilianMessages");
    this.registerModule(this.civMessages);
  }

  /**
   * 救助対象のEntityIDを返す．
   * @return 救助対象のEntityID
   */
  @Override
  public EntityID getTarget() {
    return this.result;
  }

  /**
   * エージェントの内部情報更新のためのメソッド.メッセージの送受信，クラスタの初期化をおこなう．
   * @param mm 　メッセージマネージャ
   * @return HumanDetectorメソッド
   */
  @Override
  public HumanDetector updateInfo(MessageManager mm) {
    super.updateInfo(mm);
    if (this.getCountUpdateInfo() > 1) {
      return this;
    }

    // MessageRequestをMessageManagerに登録
    if (!this.hasRegisterRequest) {
      final var index = new StandardMessageBundle().getMessageClassList().size() + 1;
      mm.registerMessageClass(index, MessageClearRequest.class);
      this.hasRegisterRequest = true;
    }

    if (this.cluster.isEmpty()) {
      this.initCluster();
    }

    this.receiveMessage(mm);
    this.sendMessage(mm);

    this.updateIgnoreTasks();
    this.updateTask();
    return this;
  }

  /**
   * 搬送対象の決定をおこなう．
   * 市民を乗せている時はその市民のEntityIDを返す．
   * そうでないときは知覚しているうち，もっとも近い市民を返す．
   * そうでなければメッセージによるものも含め，もっとも近い市民を返す．
   * @return calcメソッド
   */
  @Override
  public HumanDetector calc() {
    this.civMessages.calc();

    final Human onboard = this.agentInfo.someoneOnBoard();
    if (onboard != null) {
      this.result = onboard.getID();
      return this;
    }

    // そのステップに知覚したエンティティを優先
    // そのステップに受信したメッセージを含む
    this.result = this.worldInfo.getChanged().getChangedEntities()
            .stream()
            .map(this.worldInfo::getEntity)
            .filter(Civilian.class::isInstance)
            .map(AbstractEntity::getID)
            .filter(this::loadTarget)
            .filter(e -> !this.worldInfo.getPosition(e).getStandardURN().equals(REFUGE))
            .filter(e -> !this.ignoreTasks.contains(e))
            .sorted(Comparator.comparingInt(e -> this.worldInfo.getDistance(e, this.agentInfo.getID())))
            .findFirst()
            .orElse(null);

    if (this.result != null) {
      return this;
    }

    // それ以前のステップで知覚したエンティティも対象にする
    this.result = this.worldInfo.getEntityIDsOfType(CIVILIAN)
            .stream()
            .filter(this::loadTarget)
            .filter(e -> !this.worldInfo.getPosition(e).getStandardURN().equals(REFUGE))
            .filter(e -> !this.ignoreTasks.contains(e))
            .sorted(Comparator.comparingInt(e -> this.worldInfo.getDistance(e, this.agentInfo.getID())))
            .findFirst()
            .orElse(null);

    return this;
  }

  /**
   * メッセージを送信する．
   * 自分が瓦礫に埋まっている時，近くのPFに対してACTION_CLEARを有線・無線で要請する．
   * 自分が埋没している時，近くのFBに対してACTION_CLEARを有線・無線で要請する．
   * @param mm メッセージマネージャ
   */
  private void sendMessage(MessageManager mm) {
    final EntityFilter filter = new EntityFilter(this.worldInfo, this.agentInfo);
    // 知覚範囲内のBLOCKADEをすべて取得
    final Set<Blockade> blockades = filter.getBlockades();
    final Stucked stucked = new Stucked(blockades, this.agentInfo.getX(), this.agentInfo.getY());
    // 自身の位置ID
    final EntityID positionID = this.agentInfo.getPosition();
    // 自身のStandardEntity
    final AmbulanceTeam me = (AmbulanceTeam) this.worldInfo.getEntity(this.agentInfo.getID());

    // 自身がBLOCKADEに埋まっているとき
    if (stucked.isStucked()) {
      mm.addMessage(new MessageClearRequest(RADIO, StandardMessagePriority.HIGH, positionID, MessageClearRequest.I_AM_AMBULANCE_TEAM));
      mm.addMessage(new MessageClearRequest(VOICE, StandardMessagePriority.HIGH, positionID, MessageClearRequest.I_AM_AMBULANCE_TEAM));
      return;
    }
    // 自身が埋没しているとき
    if (this.isMyselfBuried()) {
      mm.addMessage(new MessageAmbulanceTeam(RADIO, me, HELP_BURIED, this.agentInfo.getID()));
      mm.addMessage(new MessageAmbulanceTeam(VOICE, me, HELP_BURIED, this.agentInfo.getID()));
      return;
    }
  }

  /**
   * 自分が埋没しているか．
   * @return 埋没している場合，True
   */
  private boolean isMyselfBuried() {
    Human me = (Human) this.worldInfo.getEntity(this.agentInfo.getID());
    if (!(this.agentInfo.getPositionArea() instanceof Building)) return false;
    if (!me.isBuriednessDefined()) return false;
    return me.getBuriedness() != 0;
  }

  /**
   * クラスターを計算し，自分の領域のEntityIDsをidsに格納する．
   */
  private void initCluster() {
    this.clusterer.calc();

    final EntityID me = this.agentInfo.getID();
    final int index = this.clusterer.getClusterIndex(me);
    final Collection<EntityID> ids =
            this.clusterer.getClusterEntityIDs(index);
    this.cluster.addAll(ids);
  }

  /**
   * エンティティが搬送対象かどうかを判定する
   * エンティティが市民かつ，HPが0より大きい，埋没度0，ダメージが0より大きい場合，搬送対象と判定される
   * また，市民の生存時間が移動時間よりも小さい場合，搬送対象ではないと判定する
   * @param e 判定したいエンティティのID
   * @return エンティティが搬送対象ならtrue，そうでないならfalse
   */
  private boolean loadTarget(EntityID e) {
    final Human h = (Human) this.worldInfo.getEntity(e);
    if (!(h instanceof Civilian)) {
      return false;
    }
    //救助対象から一番近いrefugeのEntityID
    final EntityID refuge = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE)
            .stream()
            .min(comparing(i -> this.worldInfo.getDistance(e, i)))
            .orElse(null);

    final LifeXpectancy lx = new LifeXpectancy((Human) this.worldInfo.getEntity(e));
    final int ttl = lx.getLifeXpectancy();

    final int d1 = (int) Math.ceil(this.worldInfo.getDistance(this.agentInfo.getPosition(), e) / AGENT_CAN_MOVE);
    final int d2 = (int) Math.ceil(this.worldInfo.getDistance(e, refuge) / AGENT_CAN_MOVE);

    //市民の生存時間が移動時間よりも小さいか判定
    if(ttl < d1 + d2){
      return false;
    }

    final int hp = h.getHP();
    final int buried = h.getBuriedness();
    final int damage = h.getDamage();

    return (hp > 0 && buried == 0 && damage > 0);
  }

  /**
   * 救急隊に対するメッセージのみを抽出し取得します．
   *
   * @param mm メッセージの取得元となるメッセージマネージャ
   */
  private void receiveMessage(MessageManager mm) {
    final List<CommunicationMessage> civMes =
            mm.getReceivedMessageList(MessageCivilian.class);
    for (CommunicationMessage tmp : civMes) {
      final MessageCivilian message = (MessageCivilian) tmp;
      this.handleMessage(message);
    }
  }

  /**
   * 救急隊に対するMessageCivilianを扱う
   * @param msg 救急隊に対するMessageCivilian
   */
  private void handleMessage(MessageCivilian msg) {
    final EntityID targetID = msg.getAgentID();

    if (this.loadTarget(targetID)) {
      this.loadCivTask.add(targetID);
    } else {
      this.nonTarget.add(targetID);
      this.loadCivTask.remove(targetID);
    }
  }

  /**
   * this.loadCivTaskとthis.nonTargetを更新する
   */
  private void updateTask(){
    this.loadCivTask.removeAll(this.ignoreTasks);
    this.nonTarget.removeAll(this.ignoreTasks);

    Iterator<EntityID> i = this.loadCivTask.iterator();
    while (i.hasNext()) {
      final EntityID e = i.next();
      if (!this.loadTarget(e)) {
        this.nonTarget.add(e);
        i.remove();
      }
    }
    i = this.nonTarget.iterator();
    while (i.hasNext()) {
      final EntityID e = i.next();
      if (this.loadTarget(e)) {
        this.loadCivTask.add(e);
        i.remove();
      }
    }
  }

  /**
   * this.ignoreTasksを更新する
   */
  private void updateIgnoreTasks() {
    if (!(this.agentInfo.getPositionArea() instanceof Building)) {
      return;
    }

    if (this.agentInfo.getPositionArea() instanceof Refuge) {
      return;
    }

    // エージェントと同じ建物にいるはずの市民のID
    final Set<EntityID> inSameBuildingCivilianIDs = this.worldInfo.getEntitiesOfType(CIVILIAN)
            .stream()
            .map(Human.class::cast)
            .filter(e -> e.getPosition().equals(this.agentInfo.getPosition()))
            .map(StandardEntity::getID)
            .collect(Collectors.toSet());

    if (inSameBuildingCivilianIDs.isEmpty()) {
      return;
    }

    // 知覚したエージェントを削除
    inSameBuildingCivilianIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());

    // 古い情報が残っているHumanのIDをthis.ignoreTasksに追加
    this.ignoreTasks.addAll(inSameBuildingCivilianIDs);
  }
}
