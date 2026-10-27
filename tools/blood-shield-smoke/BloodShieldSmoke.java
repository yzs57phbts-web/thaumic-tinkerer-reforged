package tt_combat_smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumictinkerer.gear.BloodShield;
import thaumictinkerer.gear.GearFeature;

@Mod("tt_combat_smoke")
public final class BloodShieldSmoke {
    @EventBusSubscriber(modid = "tt_combat_smoke", value = Dist.CLIENT)
    public static final class Events {
        private static boolean started;
        private static int ticks;

        private static void server(Consumer<ServerPlayer> action) {
            var mc = Minecraft.getInstance();
            var id = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id)));
        }

        private static void check(boolean condition, String message) {
            if (!condition) throw new IllegalStateException(message);
            System.out.println("BLOOD_SHIELD_SMOKE_PASS " + message);
        }

        private static void capture(String name) throws Exception {
            var mc = Minecraft.getInstance();
            Path directory = mc.gameDirectory.toPath().resolve("evidence");
            Files.createDirectories(directory);
            Screenshot.grab(directory.toFile(), name + ".png", mc.getMainRenderTarget(), message -> {});
        }

        private static void hit(boolean axe) {
            server(player -> {
                var zombie = EntityType.ZOMBIE.create(player.serverLevel());
                zombie.setPos(player.position().add(0, 0, 2));
                if (axe) zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                float before = player.getHealth();
                player.hurt(player.damageSources().mobAttack(zombie), 20);
                check(Math.abs(before - player.getHealth() - 3) < 0.001, "server block price is three magic damage");
                check(player.getOffhandItem().isEmpty(), "server offhand contains no temporary item");
            });
        }

        @SubscribeEvent
        public static void tick(ClientTickEvent.Post event) throws Exception {
            var mc = Minecraft.getInstance();
            mc.options.pauseOnLostFocus = false;
            if (!started && mc.screen != null && mc.getOverlay() == null) {
                started = true;
                mc.options.guiScale().set(1);
                mc.resizeDisplay();
                mc.createWorldOpenFlows().createFreshLevel("blood-shield-" + System.currentTimeMillis(),
                    new LevelSettings("Blood shield", GameType.SURVIVAL, false, net.minecraft.world.Difficulty.NORMAL,
                        true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(54321L, false, false),
                    registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            }
            if (mc.player == null || mc.level == null || mc.getOverlay() != null) return;
            ticks++;
            if (ticks == 10) server(player -> {
                var level = player.serverLevel();
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
                for (BlockPos pos : BlockPos.betweenClosed(-5, 99, -5, 5, 99, 5)) level.setBlockAndUpdate(pos, Blocks.STONE_BRICKS.defaultBlockState());
                level.setDayTime(6000);
                player.connection.teleport(0.5, 100, 0.5, 0, 0);
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                player.setHealth(100);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GearFeature.BLOOD_SWORD.get()));
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            });
            if (ticks == 30 || ticks == 120) {
                mc.options.keyUse.setDown(true);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
            if (ticks == 38) {
                check(BloodShield.visible(mc.player), "first-person shield active");
                check(mc.player.getOffhandItem().isEmpty(), "client offhand remains empty");
                capture("blood-shield-first-person");
            }
            if (ticks == 40) hit(false);
            if (ticks == 46) {
                check(!BloodShield.visible(mc.player), "shield disappeared after one hit");
                mc.options.keyUse.setDown(false);
            }
            if (ticks == 110) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            if (ticks == 128) {
                check(BloodShield.visible(mc.player), "third-person shield active");
                capture("blood-shield-third-person");
            }
            if (ticks == 132) hit(true);
            if (ticks == 140) {
                mc.options.keyUse.setDown(false);
                check(!BloodShield.visible(mc.player), "axe removed shield model");
                check(mc.player.getCooldowns().isOnCooldown(GearFeature.BLOOD_SWORD.get()), "axe cooldown synchronized");
                capture("blood-shield-axe-disabled");
            }
            if (ticks == 160) {
                System.out.println("BLOOD_SHIELD_SMOKE_ALL_PASSED");
                mc.stop();
            }
        }
    }
}
