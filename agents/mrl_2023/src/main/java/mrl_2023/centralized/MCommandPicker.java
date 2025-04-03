package mrl_2023.centralized;

import adf.core.agent.communication.standard.bundle.centralized.CommandAmbulance;
import adf.core.agent.communication.standard.bundle.centralized.CommandFire;
import adf.core.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.info.AgentInfo;
import adf.core.agent.info.ScenarioInfo;
import adf.core.agent.info.WorldInfo;
import adf.core.agent.module.ModuleManager;
import adf.core.component.centralized.CommandPicker;
import adf.core.component.communication.CommunicationMessage;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class MCommandPicker extends CommandPicker
{
    private Collection<CommunicationMessage> messages = new LinkedList<>();
    private Map<EntityID, EntityID> allocations = null;

    private StandardEntityURN urn;

    public MCommandPicker(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);
        this.urn = ai.me().getStandardURN();
    }

    @Override
    public CommandPicker setAllocatorResult(Map<EntityID, EntityID> allocations)
    {
        this.allocations = allocations;
        return this;
    }

    @Override
    public CommandPicker calc()
    {
        if (this.allocations == null) return this;
        this.messages.clear();

        for (EntityID agent : this.allocations.keySet())
        {
            final EntityID task = this.allocations.get(agent);
            final CommunicationMessage command = this.makeCommand(agent, task);
            this.messages.add(command);
        }

        return this;
    }

    private CommunicationMessage makeCommand(EntityID agent, EntityID task)
    {
        if (this.urn == FIRE_STATION)
        {
            final int action = CommandFire.ACTION_AUTONOMY;
            return new CommandFire(true, agent, task, action);
        }

        if (this.urn == AMBULANCE_CENTRE)
        {
            final int action = CommandAmbulance.ACTION_AUTONOMY;
            return new CommandAmbulance(true, agent, task, action);
        }

        if (this.urn == POLICE_OFFICE)
        {
            final int action = CommandPolice.ACTION_AUTONOMY;
            return new CommandPolice(true, agent, task, action);
        }

        return null;
    }

    @Override
    public Collection<CommunicationMessage> getResult()
    {
        return this.messages;
    }
}
