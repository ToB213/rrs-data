package AIT_2023.module.comm;

import AIT_2023.module.comm.information.MessageClearRequest;
import AIT_2023.module.comm.information.MessageRescueRequest;
import adf.core.agent.communication.standard.bundle.*;
import adf.core.agent.communication.standard.bundle.information.*;
import java.util.*;

public class VoiceCommunication extends AbstractChannel {

  private static final Class[] WHITE_LIST =
      {
          //MessageRescueRequest.class,
          //MessageClearRequest.class,
          MessageAmbulanceTeam.class,
          MessageFireBrigade.class,
          MessagePoliceForce.class,
          MessageCivilian.class,
      };

  public VoiceCommunication(int[] numbers) {
    super(numbers, false);
  }

  @Override
  protected boolean applyFilter(StandardMessage message) {
    if (!super.applyFilter(message)) {
      return false;
    }

    final Class clazz = message.getClass();
    return Arrays.asList(WHITE_LIST).contains(clazz);
  }
}
