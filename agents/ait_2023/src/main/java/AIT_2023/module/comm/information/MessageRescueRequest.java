package AIT_2023.module.comm.information;

import adf.core.agent.communication.standard.bundle.StandardMessage;
import adf.core.agent.communication.standard.bundle.StandardMessagePriority;
import adf.core.component.communication.util.BitOutputStream;
import adf.core.component.communication.util.BitStreamReader;
import rescuecore2.worldmodel.EntityID;

public class MessageRescueRequest extends StandardMessage {

    public static final int I_AM_POLICE_FORCE = 0;
    public static final int I_AM_FIRE_BRIGADE = 1;
    public static final int I_AM_AMBULANCE_TEAM = 2;

    private final EntityID positionID;
    private final int agentType;

    private final int SIZE_POSITION_ID = 32;
    private final int SIZE_TYPE_ID = 4;


    public MessageRescueRequest(boolean isRadio, StandardMessagePriority sendingPriority,
                                EntityID positionID, int agentType) {
        super(isRadio, sendingPriority);
        this.positionID  = positionID;
        this.agentType = agentType;
    }

    public MessageRescueRequest(boolean isRadio, int senderID, int ttl, BitStreamReader bitStreamReader) {
        super(isRadio, senderID, ttl, bitStreamReader);
        this.positionID = new EntityID(bitStreamReader.getBits(SIZE_POSITION_ID));
        this.agentType = bitStreamReader.getBits(SIZE_TYPE_ID);
    }

    public EntityID getPositionID() {
        return positionID;
    }

    public int getAgentType() {
        return agentType;
    }
    @Override
    public int getByteArraySize() {
        return this.toBitOutputStream().size();
    }

    @Override
    public byte[] toByteArray() {
        return this.toBitOutputStream().toByteArray();
    }

    @Override
    public BitOutputStream toBitOutputStream() {
        final BitOutputStream bitOutputStream = new BitOutputStream();
        bitOutputStream.writeBits(this.positionID.getValue(), SIZE_POSITION_ID);
        bitOutputStream.writeBits(this.agentType, SIZE_TYPE_ID);
        return bitOutputStream;
    }

    @Override
    public String getCheckKey() {
        return String.format("Rescue Request Position ID : " + this.positionID.getValue());
    }
}
