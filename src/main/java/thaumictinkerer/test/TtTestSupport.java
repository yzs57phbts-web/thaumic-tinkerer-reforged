package thaumictinkerer.test;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class TtTestSupport {
    private TtTestSupport() {
    }

    public static ServerPlayer mockServerPlayer(GameTestHelper helper, GameType type) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "tt-test-player");
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile,
            net.minecraft.server.level.ClientInformation.createDefault());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player,
            net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        player.setGameMode(type);
        player.moveTo(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1))));
        for (int tick = 0; tick < 61; tick++) {
            player.tick();
        }
        return player;
    }
}
