package mrl_2023.extaction.fb;

import adf.core.agent.action.Action;
import adf.core.agent.action.ambulance.ActionUnload;
import adf.core.agent.action.common.ActionMove;
import adf.core.agent.action.common.ActionRest;
import adf.core.agent.action.fire.ActionRescue;
import adf.core.agent.communication.MessageManager;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.info.AgentInfo;
import adf.core.agent.info.ScenarioInfo;
import adf.core.agent.info.WorldInfo;
import adf.core.agent.module.ModuleManager;
import adf.core.agent.precompute.PrecomputeData;
import adf.core.component.extaction.ExtAction;
import adf.core.component.module.algorithm.PathPlanning;
import mrl_2023.MRLConstants;
import mrl_2023.complex.Honey;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static rescuecore2.standard.entities.StandardEntityURN.BLOCKADE;

public class ActionFireRescue extends ExtAction {

    private PathPlanning pathPlanning;
    private int thresholdRest;
    private int kernelTime;
    private EntityID target;
    private ExtAction actionExtMove;
    private StandardEntity assigned;
     private StandardEntity fe_target;



    private static final boolean debug = false;

    public ActionFireRescue(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo,
                            ModuleManager moduleManager, DevelopData developData) {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        this.target = null;
        this.thresholdRest = developData.getInteger("ActionTransport.rest", 100);
        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("DefaultExtActionTransport.PathPlanning",
                        "adf.impl.module.algorithm.DijkstraPathPlanning");
                this.actionExtMove = moduleManager.getExtAction("DefaultTacticsFireBrigade.ExtActionMove", "adf.impl.extaction.DefaultExtActionMove");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("DefaultExtActionTransport.PathPlanning",
                        "adf.impl.module.algorithm.DijkstraPathPlanning");
                this.actionExtMove = moduleManager.getExtAction("DefaultTacticsFireBrigade.ExtActionMove", "adf.impl.extaction.DefaultExtActionMove");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("DefaultExtActionTransport.PathPlanning",
                        "adf.impl.module.algorithm.DijkstraPathPlanning");
                this.actionExtMove = moduleManager.getExtAction("DefaultTacticsFireBrigade.ExtActionMove", "adf.impl.extaction.DefaultExtActionMove");
                break;
        }

    }



    public ExtAction precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    public ExtAction resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        this.pathPlanning.resume(precomputeData);
        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    public ExtAction preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        this.pathPlanning.preparate();
        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    public ExtAction updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.pathPlanning.updateInfo(messageManager);
        return this;
    }

    @Override
    public ExtAction setTarget(EntityID target) {
        this.target = null;//？？？
        if (target != null) {
            StandardEntity entity = this.worldInfo.getEntity(target);
            if (entity instanceof Human || entity instanceof Area) {
                this.target = target;
                return this;
            }
        }
        return this;
    }

    @Override
    public ExtAction calc() {

        this.result = null;
        FireBrigade me = (FireBrigade) this.agentInfo.me();
        if ((this.result == null) && this.needRest(me)) {

            EntityID areaID = this.convertArea(this.target);
            ArrayList<EntityID> targets = new ArrayList<>();
            if (areaID != null) {
                targets.add(areaID);
            }
            this.result = this.calcRefugeAction(me, this.pathPlanning, targets, false);
            if(result == null) {

            }

        }
        if ((this.result == null) && this.target != null) {

            this.result = this.calcRescue(me, this.pathPlanning, this.target);
        }



        return this;
    }

    private Action calcRescue(FireBrigade agent, PathPlanning pathPlanning, EntityID target) {
        StandardEntity targetEntity = this.worldInfo.getEntity(target);
        if (targetEntity == null) {

            return null;
        }
        EntityID agentPosition = agent.getPosition();
        if (targetEntity instanceof Human) {
            Human human = (Human) targetEntity;
            if (!human.isPositionDefined()) {
                return null;
            }
            if (human.isHPDefined() && human.getHP() == 0 ) {
                return null;
            }
            EntityID targetPosition = worldInfo.getPosition(human).getID();


            if (agentPosition.getValue() == targetPosition.getValue()) {
                if (human.isBuriednessDefined() && human.getBuriedness() > 0) {
                    return new ActionRescue(human);
                }
            } else {
                List<EntityID> path = pathPlanning.setFrom(agentPosition).setDestination(targetPosition).calc().getResult();

                if (path != null && path.size() > 0) {
                    Action action = getMoveAction(path);

                    return action;
                }
            }
            return null;
        }
        if (targetEntity.getStandardURN() == BLOCKADE) {
            Blockade blockade = (Blockade) targetEntity;
            if (blockade.isPositionDefined()) {//Assign to targetEntity
                assigned = this.worldInfo.getEntity(blockade.getPosition());
                if (MRLConstants.Assigned_Unpositon){

                    fe_target = assigned ;

                }
                targetEntity = this.worldInfo.getEntity(blockade.getPosition());
            }
        }
        if (targetEntity instanceof Area) {
            List<EntityID> path = pathPlanning.getResult(agentPosition, targetEntity.getID());
            if (path != null && path.size() > 0) {
                this.result = getMoveAction(path);
            }
        }
        return null;
    }
    public int FOOD_NUMBER;
    public ArrayList<Honey> foodSources;
    public Random rand;
    public void sendOnlookerBees() {
        int i = 0;
        int t = 0;
        int neighborBeeIndex = 0;
        Honey currentBee = null;
        Honey neighborBee = null;

        while(t < FOOD_NUMBER) {
            currentBee = foodSources.get(i);
            if(rand.nextDouble() < currentBee.getSelectionProbability()) {
                t++;

                neighborBeeIndex = getExclusiveRandomNumber(FOOD_NUMBER-1, i);
                neighborBee = foodSources.get(neighborBeeIndex);
                sendToWork(currentBee, neighborBee);
            }
            i++;
            if(i == FOOD_NUMBER) {
                i = 0;
            }
        }
    }

    public void sendToWork(Honey currentBee, Honey neighborBee) {
        int newValue = 0;
        int tempValue = 0;
        int tempIndex = 0;
        int prevConflicts = 0;
        int currConflicts = 0;
        int parameterToChange = 0;
        StandardEntity pre_Worked;

        //get number of conflicts
        prevConflicts = currentBee.getConflicts();

        //The parameter to be changed is determined randomly
        parameterToChange = getRandomNumber(0, MAX_LENGTH-1);

        /*v_{ij}=x_{ij}+\phi_{ij}*(x_{kj}-x_{ij})
        solution[param2change]=Foods[i][param2change]+(Foods[i][param2change]-Foods[neighbour][param2change])*(r-0.5)*2;
        */
        tempValue = currentBee.getNectar(parameterToChange);
        newValue = (int)(tempValue+(tempValue - neighborBee.getNectar(parameterToChange))*(rand.nextDouble()-0.5)*2);

        //trap the value within upper bound and lower bound limits
        if(newValue < 0) {
            newValue = 0;
        }
        if(newValue > MAX_LENGTH-1) {
            newValue = MAX_LENGTH-1;
        }

        //get the index of the new value
        tempIndex = currentBee.getIndex(newValue);

        //swap
        currentBee.setNectar(parameterToChange, newValue);
        currentBee.setNectar(tempIndex, tempValue);
        currentBee.computeConflicts();

        currConflicts = currentBee.getConflicts();

        //greedy selection
        if(prevConflicts < currConflicts) {						//No improvement
            currentBee.setNectar(parameterToChange, tempValue);
            currentBee.setNectar(tempIndex, newValue);
            currentBee.computeConflicts();
            //currentBee.setTrials(currentBee.getTrials() + 1);
        } else {
            pre_Worked = fe_target;
            //improved solution
            //currentBee.setTrials(0);
        }

    }
    private boolean needRest(Human agent) {
        int hp = agent.getHP();
        int damage = agent.getDamage();
        if (hp == 0 || damage == 0) {
            return false;
        }
        int activeTime = (hp / damage) + ((hp % damage) != 0 ? 1 : 0);
        if (this.kernelTime == -1) {
            try {
                this.kernelTime = this.scenarioInfo.getKernelTimesteps();
            } catch (NoSuchConfigOptionException e) {
                this.kernelTime = -1;
            }
        }
        return damage >= this.thresholdRest || (activeTime + this.agentInfo.getTime()+20) < this.kernelTime;
    }

    private EntityID convertArea(EntityID targetID) {
        StandardEntity entity = this.worldInfo.getEntity(targetID);
        if (entity == null) {
            return null;
        }
        if (entity instanceof Human) {
            Human human = (Human) entity;
            if (human.isPositionDefined()) {
                EntityID position = human.getPosition();
                if (this.worldInfo.getEntity(position) instanceof Area) {
                    return position;
                }
            }
        } else if (entity instanceof Area) {
            return targetID;
        } else if (entity.getStandardURN() == BLOCKADE) {
            Blockade blockade = (Blockade) entity;
            if (blockade.isPositionDefined()) {
                return blockade.getPosition();
            }
        }
        return null;
    }
    public int MAX_LENGTH; 		/*Be optimized AGJ ++ */

    public int getRandomNumber(int low, int high) {
        return (int)Math.round((high - low) * rand.nextDouble() + low);
    }
    private Action calcRefugeAction(Human human, PathPlanning pathPlanning, Collection<EntityID> targets,
                                    boolean isUnload) {
        EntityID position = human.getPosition();
        Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE);
        int size = refuges.size();
        if (refuges.contains(position)) {
            return isUnload ? new ActionUnload() : new ActionRest();
        }
        List<EntityID> firstResult = null;
        while (refuges.size() > 0) {
            pathPlanning.setFrom(position);
            pathPlanning.setDestination(refuges);
            List<EntityID> path = pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                if (firstResult == null) {//？？？
                    firstResult = new ArrayList<>(path);
                    if (targets == null || targets.isEmpty()) {
                        break;
                    }
                }
                EntityID refugeID = path.get(path.size() - 1);
                pathPlanning.setFrom(refugeID);
                pathPlanning.setDestination(targets);
                List<EntityID> fromRefugeToTarget = pathPlanning.calc().getResult();
                if (fromRefugeToTarget != null && fromRefugeToTarget.size() > 0) {
                    return getMoveAction(path);
                }
                refuges.remove(refugeID);
                if (size == refuges.size()) {
                    break;
                }
                size = refuges.size();
            } else {
                break;
            }
        }
        return firstResult != null ? getMoveAction(firstResult) : null;
    }
    public int getExclusiveRandomNumber(int high, int except) {
        boolean done = false;
        int getRand = 0;

        while(!done) {
            getRand = rand.nextInt(high);
            if(getRand != except){
                done = true;
            }
        }

        return getRand;
    }
    private Action getMoveAction(List<EntityID> path) {
        if (path != null && path.size() > 0) {
            ActionMove moveAction = (ActionMove) actionExtMove.setTarget(path.get(path.size() - 1)).calc().getAction();
            return moveAction;
        }
        return null;
    }
}