package thaumictinkerer.client.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.screen.ResearchTableScreen;
import thaumcraft.menu.ResearchTableMenu;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.Knowledge;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.client.gateway.GatewayClient;
import thaumictinkerer.client.gateway.WarpGateDestinationsScreen;
import thaumictinkerer.kami.gateway.GatewayFeature;

@EventBusSubscriber(modid = ThaumicTinkerer.MODID, value = Dist.CLIENT)
public final class GithubIssueSmoke {
    private static final BlockPos TABLE = new BlockPos(1, 100, 0);
    private static final BlockPos GATE = new BlockPos(0, 99, 0);
    private static boolean started;
    private static int ticks;
    private static int captures;

    private static void server(Consumer<ServerPlayer> action) {
        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        var id = mc.player.getUUID();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(id)));
    }

    private static void record(String line) {
        try {
            Files.writeString(Path.of(Minecraft.getInstance().gameDirectory.toString(), "github-issue-smoke.txt"),
                line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        System.out.println("GitHub issue smoke: " + line);
    }

    private static void check(boolean condition, String detail) {
        if (!condition) throw new IllegalStateException(detail);
        record("PASS: " + detail);
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), message -> captures++);
    }

    private static void clickAspect(boolean shift) throws ReflectiveOperationException {
        Minecraft mc = Minecraft.getInstance();
        check(mc.screen instanceof ResearchTableScreen, "research screen opened");
        var screen = (ResearchTableScreen) mc.screen;
        var aspects = Knowledge.get(mc.player).aspects().getAspectsSorted();
        int index = aspects.indexOf(Aspect.LIGHT);
        check(index >= 0 && index < 25, "Lux visible in research pool");
        int left = (screen.width - 255) / 2;
        int top = (screen.height - 255) / 2;
        var click = ResearchTableScreen.class.getDeclaredMethod("handleClick", double.class, double.class, boolean.class);
        click.setAccessible(true);
        click.invoke(screen, (double) left + 18 + index / 5 * 16, (double) top + 48 + index % 5 * 16, shift);
        var dragged = ResearchTableScreen.class.getDeclaredField("draggedAspect");
        dragged.setAccessible(true);
        check(shift ? dragged.get(screen) == null : dragged.get(screen) == Aspect.LIGHT,
            shift ? "Shift click combines without dragging" : "ordinary click still drags aspect");
    }

    private static void gateMenuCheck() throws ReflectiveOperationException {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(null);
        var tick = GatewayClient.class.getDeclaredMethod("onClientTick", ClientTickEvent.Post.class);
        tick.setAccessible(true);
        mc.player.input.shiftKeyDown = false;
        tick.invoke(null, new Object[] {null});
        mc.player.input.shiftKeyDown = true;
        tick.invoke(null, new Object[] {null});
        check(mc.screen instanceof WarpGateDestinationsScreen, "gate opens while sneaking");
        mc.setScreen(null);
        for (int index = 0; index < 10; index++) tick.invoke(null, new Object[] {null});
        check(mc.screen == null, "closed gate menu stays closed while Shift held");
        mc.player.input.shiftKeyDown = false;
        tick.invoke(null, new Object[] {null});
        mc.player.input.shiftKeyDown = true;
        tick.invoke(null, new Object[] {null});
        check(mc.screen instanceof WarpGateDestinationsScreen, "releasing and pressing Shift opens gate again");
        mc.player.input.shiftKeyDown = false;
        mc.setScreen(null);
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("thaumictinkerer.githubIssueSmoke")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (!started && mc.getOverlay() == null && mc.screen != null) {
            started = true;
            mc.createWorldOpenFlows().createFreshLevel("github-issues-" + System.currentTimeMillis(),
                new LevelSettings("GitHub issues", GameType.CREATIVE, false, net.minecraft.world.Difficulty.PEACEFUL,
                    true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(12345L, false, false),
                registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
        }
        if (mc.player == null || mc.level == null || mc.getOverlay() != null) return;
        ticks++;
        try {
            if (ticks == 10) server(player -> {
                var level = player.serverLevel();
                for (BlockPos pos : BlockPos.betweenClosed(-5, 99, -5, 5, 99, 5)) level.setBlockAndUpdate(pos, Blocks.STONE_BRICKS.defaultBlockState());
                level.setDayTime(6000);
                player.connection.teleport(0.5, 100, 0.5, 180, 0);
                level.setBlockAndUpdate(TABLE, ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
                var knowledge = Knowledge.get(player);
                knowledge.addResearch("RESEARCHER2");
                knowledge.setAspectPool(Aspect.AIR, 10);
                knowledge.setAspectPool(Aspect.FIRE, 10);
                knowledge.setAspectPool(Aspect.LIGHT, 5);
                Knowledge.sync(player);
            });
            if (ticks == 40) {
                mc.setScreen(null);
                server(player -> thaumcraft.platform.Menus.open(player, new SimpleMenuProvider((id, inventory, ignored) -> new ResearchTableMenu(id, inventory, TABLE), Component.literal("Research shift audit")), TABLE));
            }
            if (ticks == 70) clickAspect(true);
            if (ticks == 100) {
                check(Knowledge.get(mc.player).getAspectPool(Aspect.LIGHT) == 6, "Shift combines when pool already contains Lux");
                check(Knowledge.get(mc.player).getAspectPool(Aspect.AIR) == 9 && Knowledge.get(mc.player).getAspectPool(Aspect.FIRE) == 9, "server consumes both component aspects");
                capture("issues-01-research-existing-pool");
                server(player -> { Knowledge.get(player).setAspectPool(Aspect.LIGHT, 0); Knowledge.sync(player); });
            }
            if (ticks == 130) clickAspect(true);
            if (ticks == 160) {
                check(Knowledge.get(mc.player).getAspectPool(Aspect.LIGHT) == 1, "Shift combines when pool contains zero Lux");
                clickAspect(false);
            }
            if (ticks == 185) {
                mc.player.closeContainer();
                server(player -> {
                    player.serverLevel().setBlockAndUpdate(GATE, GatewayFeature.WARP_GATE.get().defaultBlockState());
                    player.connection.teleport(0.5, 100, 0.5, 180, 0);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GatewayFeature.WARP_GATE.get()));
                });
            }
            if (ticks == 230) gateMenuCheck();
            if (ticks == 260) capture("issues-02-gate-first-person");
            if (ticks == 275) mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            if (ticks == 300) capture("issues-03-gate-inventory");
            if (ticks == 320) {
                check(captures == 3, "three screenshots captured");
                record("ALL CLIENT CHECKS PASSED");
                mc.stop();
            }
        } catch (Exception exception) {
            record("FAIL: " + exception);
            exception.printStackTrace();
            mc.stop();
        }
    }
}
