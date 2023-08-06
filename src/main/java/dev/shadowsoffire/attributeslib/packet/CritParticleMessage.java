package dev.shadowsoffire.attributeslib.packet;

import java.util.function.Supplier;

import dev.shadowsoffire.attributeslib.client.AttributesLibClient;
import dev.shadowsoffire.placebo.network.MessageHelper;
import dev.shadowsoffire.placebo.network.MessageProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class CritParticleMessage {

    protected final int entityId;

    public CritParticleMessage(int entityId) {
        this.entityId = entityId;
    }

    public static class Provider implements MessageProvider<CritParticleMessage> {

        @Override
        public Class<CritParticleMessage> getMsgClass() {
            return CritParticleMessage.class;
        }

        @Override
        public void write(CritParticleMessage msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.entityId);
        }

        @Override
        public CritParticleMessage read(FriendlyByteBuf buf) {
            return new CritParticleMessage(buf.readInt());
        }

        @Override
        public void handle(CritParticleMessage msg, Supplier<Context> ctx) {
            MessageHelper.handlePacket(() -> {
                AttributesLibClient.apothCrit(msg.entityId);
            }, ctx);
        }

    }

}
