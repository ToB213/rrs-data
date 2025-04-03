package mrl_2023.comm;

import adf.core.agent.communication.standard.bundle.StandardMessage;

import java.util.List;

public interface Channel
{
    public void addWithFilter(StandardMessage message);
    public void sort();
    public List<StandardMessage> getMessages();
    public int[] getNumbers();
}
