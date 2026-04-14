package dev.shadowsoffire.apothic_attributes.payload;

import java.util.List;
import java.util.Optional;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.client.AttributesLibClient;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CritParticlePayload(int entityId) implements CustomPacketPayload {

    public static final Type<CritParticlePayload> TYPE = new Type<>(ApothicAttributes.loc("crit_particle"));

    public static final StreamCodec<FriendlyByteBuf, CritParticlePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, CritParticlePayload::entityId,
        CritParticlePayload::new);

    @Override
    public Type<CritParticlePayload> type() {
        return TYPE;
    }

    public static class Provider implements PayloadProvider<CritParticlePayload> {

        @Override
        public Type<CritParticlePayload> getType() {
            return TYPE;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, CritParticlePayload> getCodec() {
            return CODEC;
        }

        @Override
        public void handleClient(CritParticlePayload msg, IPayloadContext ctx) {
            AttributesLibClient.apothCrit(msg.entityId);
        }

        @Override
        public List<ConnectionProtocol> getSupportedProtocols() {
            return List.of(ConnectionProtocol.PLAY);
        }

        @Override
        public Optional<PacketFlow> getFlow() {
            return Optional.of(PacketFlow.CLIENTBOUND);
        }

        @Override
        public String getVersion() {
            return "1";
        }

    }

}
