package dev.shadowsoffire.apothic_attributes.packet;

import java.util.List;
import java.util.Optional;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.client.AttributesLibClient;
import dev.shadowsoffire.placebo.network.PayloadHelper;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CritParticleMessage(int entityId) implements CustomPacketPayload {

    public static ResourceLocation ID = ApothicAttributes.loc("crit_particle");

    public static class Provider implements PayloadProvider<CritParticleMessage, IPayloadContext> {

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public CritParticleMessage read(FriendlyByteBuf buf) {
            return new CritParticleMessage(buf.readVarInt());
        }

        @Override
        public void handle(CritParticleMessage msg, IPayloadContext ctx) {
            PayloadHelper.handle(() -> {
                AttributesLibClient.apothCrit(msg.entityId);
            }, ctx);
        }

        @Override
        public List<ConnectionProtocol> getSupportedProtocols() {
            return List.of(ConnectionProtocol.PLAY);
        }

        @Override
        public Optional<PacketFlow> getFlow() {
            return Optional.of(PacketFlow.CLIENTBOUND);
        }

    }

    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.entityId);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

}
