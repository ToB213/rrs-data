package AIT_2023.module.comm;

import AIT_2023.module.comm.information.MessageClearRequest;
import AIT_2023.module.comm.information.MessageRescueRequest;
import adf.core.agent.communication.standard.bundle.*;
import adf.core.agent.communication.standard.bundle.information.*;
import adf.core.agent.info.*;
import rescuecore2.standard.entities.StandardEntityURN;

import static rescuecore2.standard.entities.StandardEntityURN.*;

import java.util.*;

public class ChannelFB extends AbstractChannel {

  private static final Class[] WHITE_LIST =
      {
          MessageRescueRequest.class,
          MessageClearRequest.class,
          //MessageFireBrigade.class,
          MessageCivilian.class,
      };

  private static final Class[] UNION_LIST =
      {
          //MessageReport.class,
          //CommandScout.class
      };

  private AgentInfo ai;

  public ChannelFB(AgentInfo ai, int[] numbers) {
    super(numbers, true);
    this.ai = ai;
  }

  @Override
  protected boolean applyFilter(StandardMessage message) {
    if (!super.applyFilter(message)) {
      return false;
    }

    final StandardEntityURN urn = this.ai.me().getStandardURN();
    final Class clazz = message.getClass();

    final boolean inWhitelist =
        Arrays.asList(WHITE_LIST).contains(clazz);
    final boolean inUnionlist =
        Arrays.asList(UNION_LIST).contains(clazz);

    return inWhitelist || inUnionlist && urn == FIRE_BRIGADE;
  }
}
