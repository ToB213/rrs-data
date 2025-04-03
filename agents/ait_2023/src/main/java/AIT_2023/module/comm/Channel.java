package AIT_2023.module.comm;

import adf.core.agent.communication.standard.bundle.*;
import java.util.*;

public interface Channel {

  public void addWithFilter(StandardMessage message);

  public void sort();

  public List<StandardMessage> getMessages();

  public int[] getNumbers();
}
