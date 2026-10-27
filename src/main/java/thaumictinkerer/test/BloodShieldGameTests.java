package thaumictinkerer.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumictinkerer.gear.BloodShield;
import thaumictinkerer.gear.GearFeature;
import thaumictinkerer.kami.KamiFeature;
import thaumictinkerer.kami.tool.AwakenedToolsFeature;

@GameTestHolder("thaumictinkerer")
@PrefixGameTestTemplate(false)
public final class BloodShieldGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var player = TtTestSupport.mockServerPlayer(h, GameType.SURVIVAL);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        player.setHealth(100);
        player.invulnerableTime = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GearFeature.BLOOD_SWORD.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.setYRot(0);
        player.setYHeadRot(0);
        return player;
    }

    private static void ticks(ServerPlayer player, int count) {
        for (int i = 0; i < count; i++) {
            player.tick();
            player.doTick();
        }
    }

    private static void raise(ServerPlayer player) {
        player.getMainHandItem().getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        ticks(player, 6);
    }

    private static Zombie attacker(GameTestHelper h, ServerPlayer player, boolean front, boolean axe) {
        Zombie attacker = EntityType.ZOMBIE.create(h.getLevel());
        attacker.setPos(player.position().add(0, 0, front ? 2 : -2));
        if (axe) attacker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        return attacker;
    }

    @GameTest(template = "test/platform", batch = "blood_shield")
    public static void frontHitCostsThreeAndOnlyBlocksOnce(GameTestHelper h) {
        var player = player(h);
        raise(player);
        h.assertTrue(player.isBlocking(), "shield must finish raising");
        player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().copy());
        h.assertTrue(BloodShield.visible(player), "an equivalent inventory stack copy canceled the shield");
        var attacker = attacker(h, player, true, false);
        player.hurt(h.getLevel().damageSources().mobAttack(attacker), 20);
        h.assertTrue(Math.abs(player.getHealth() - 97) < 0.001, "block must replace 20 damage with 3 magic");
        h.assertTrue(player.getOffhandItem().isEmpty(), "phantom must never create an inventory shield");
        player.invulnerableTime = 0;
        player.hurt(h.getLevel().damageSources().mobAttack(attacker), 20);
        h.assertTrue(Math.abs(player.getHealth() - 77) < 0.001, "second hit must pass through");
        ticks(player, 1);
        h.assertFalse(player.isUsingItem(), "consumed shield remains raised");
        ticks(player, 61);
        h.assertFalse(player.getCooldowns().isOnCooldown(GearFeature.BLOOD_SWORD.get()), "ordinary cooldown exceeds three seconds");
        h.succeed();
    }

    @GameTest(template = "test/platform", batch = "blood_shield")
    public static void rearHitAndRaiseDelayDoNotChargeBlood(GameTestHelper h) {
        var player = player(h);
        player.getMainHandItem().getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        player.hurt(h.getLevel().damageSources().mobAttack(attacker(h, player, true, false)), 7);
        h.assertTrue(player.getHealth() == 93, "instant protection bypasses the vanilla raise delay");
        ticks(player, 6);
        player.invulnerableTime = 0;
        player.hurt(h.getLevel().damageSources().mobAttack(attacker(h, player, false, false)), 7);
        h.assertTrue(player.getHealth() == 86, "rear hit must not trigger a blood price or block");
        h.assertFalse(player.getCooldowns().isOnCooldown(GearFeature.BLOOD_SWORD.get()), "unblocked hits consumed the shield");
        h.succeed();
    }

    @GameTest(template = "test/platform", batch = "blood_shield")
    public static void axeDisablesForFiveSeconds(GameTestHelper h) {
        for (var axe : java.util.List.of(Items.IRON_AXE, KamiFeature.ICHORIUM_AXE.get(), AwakenedToolsFeature.AWAKENED_AXE.get())) {
            var player = player(h);
            raise(player);
            var attacker = attacker(h, player, true, false);
            attacker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(axe));
            player.hurt(h.getLevel().damageSources().mobAttack(attacker), 12);
            h.assertTrue(player.getHealth() == 97, "axe block charged the wrong price");
            h.assertFalse(player.isUsingItem(), "axe disabling did not lower the shield");
            ticks(player, 61);
            h.assertTrue(player.getCooldowns().isOnCooldown(GearFeature.BLOOD_SWORD.get()), "axe cooldown ended like an ordinary block");
            ticks(player, 40);
            h.assertFalse(player.getCooldowns().isOnCooldown(GearFeature.BLOOD_SWORD.get()), "axe cooldown exceeds five seconds");
        }
        h.succeed();
    }

    @GameTest(template = "test/platform", batch = "blood_shield")
    public static void occupiedOffhandPreventsActivationAndCancelsShield(GameTestHelper h) {
        var player = player(h);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
        player.getMainHandItem().getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        h.assertFalse(player.isUsingItem(), "occupied offhand allowed a phantom shield");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        raise(player);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
        player.hurt(h.getLevel().damageSources().mobAttack(attacker(h, player, true, false)), 7);
        h.assertTrue(player.getHealth() == 93, "shield stayed active after occupying offhand");
        ticks(player, 1);
        h.assertFalse(player.isUsingItem(), "offhand change did not cancel use");
        h.assertTrue(player.getOffhandItem().is(Items.TORCH), "shield replaced the real offhand item");
        h.succeed();
    }

    @GameTest(template = "test/platform", batch = "blood_shield")
    public static void timeoutAndReleaseCostNoHealthButHaveCooldown(GameTestHelper h) {
        var player = player(h);
        raise(player);
        ticks(player, 35);
        h.assertFalse(player.isUsingItem(), "shield lasted over two seconds");
        h.assertTrue(player.getHealth() == 100, "idle shield consumed health");
        h.assertTrue(player.getCooldowns().isOnCooldown(GearFeature.BLOOD_SWORD.get()), "timeout missed cooldown");
        var other = player(h);
        raise(other);
        other.releaseUsingItem();
        h.assertTrue(other.getHealth() == 100, "releasing consumed health");
        h.assertTrue(other.getCooldowns().isOnCooldown(GearFeature.BLOOD_SWORD.get()), "release missed cooldown");
        h.succeed();
    }

    @GameTest(template = "test/platform", batch = "blood_shield")
    public static void magicBypassesShieldAndPlayersDoNotShareState(GameTestHelper h) {
        var first = player(h);
        var second = player(h);
        raise(first);
        raise(second);
        first.hurt(h.getLevel().damageSources().magic(), 6);
        h.assertTrue(first.getHealth() == 94, "magic was blocked");
        // A previous injury's window cannot waive the blood price.
        first.hurt(h.getLevel().damageSources().mobAttack(attacker(h, first, true, false)), 20);
        second.hurt(h.getLevel().damageSources().mobAttack(attacker(h, second, true, false)), 20);
        h.assertTrue(first.getHealth() == 91 && second.getHealth() == 97, "players shared damage state or invulnerability waived price");
        h.succeed();
    }

    @GameTest(template = "test/platform", batch = "blood_shield")
    public static void axesHaveSwordDamagePlusThreeAndAxeSpeed(GameTestHelper h) {
        var player = player(h);
        for (var pair : java.util.List.of(
            java.util.List.of(KamiFeature.ICHORIUM_SWORD.get(), KamiFeature.ICHORIUM_AXE.get()),
            java.util.List.of(AwakenedToolsFeature.AWAKENED_SWORD.get(), AwakenedToolsFeature.AWAKENED_AXE.get()))) {
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(pair.getFirst()));
            player.doTick();
            double sword = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(pair.getLast()));
            player.doTick();
            double axe = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            h.assertTrue(sword == 10 && axe == 13, "expected sword 10 HP and axe 13 HP");
            h.assertTrue(player.getAttributeValue(Attributes.ATTACK_SPEED) == 1, "axe must attack once per second");
            ticks(player, 25);
            player.setOnGround(true);
            var target = attacker(h, player, true, false);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            target.setHealth(100);
            h.assertTrue(player.getAttackStrengthScale(0.5F) >= 0.99F, "axe attack is not fully charged");
            player.attack(target);
            h.assertTrue(Math.abs(target.getHealth() - 87) < 0.001, "fully charged axe must deal 13 HP");
        }
        h.succeed();
    }

    @GameTest(template = "test/platform", batch = "blood_shield")
    public static void shieldCooldownStillAllowsSwordAttacks(GameTestHelper h) {
        var player = player(h);
        raise(player);
        player.hurt(h.getLevel().damageSources().mobAttack(attacker(h, player, true, false)), 20);
        ticks(player, 16);
        h.assertTrue(player.getCooldowns().isOnCooldown(GearFeature.BLOOD_SWORD.get()), "test must attack during shield cooldown");
        var target = attacker(h, player, true, false);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setHealth(100);
        player.setOnGround(true);
        float healthBeforeAttack = player.getHealth();
        player.attack(target);
        h.assertTrue(Math.abs(target.getHealth() - 89) < 0.001, "cooldown prevented the sword's 11 HP attack");
        h.assertTrue(Math.abs(healthBeforeAttack - player.getHealth() - 2) < 0.001, "attack must retain its two magic damage price");
        h.succeed();
    }

    @GameTest(template = "test/platform", batch = "blood_shield")
    public static void realOffhandShieldKeepsItsNormalBehavior(GameTestHelper h) {
        var player = player(h);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        var result = player.getMainHandItem().getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.PASS, "blade swallowed the offhand interaction");
        player.getOffhandItem().getItem().use(player.level(), player, InteractionHand.OFF_HAND);
        ticks(player, 6);
        player.hurt(h.getLevel().damageSources().mobAttack(attacker(h, player, true, false)), 7);
        h.assertTrue(player.getHealth() == 100, "ordinary shield incorrectly charged the phantom's blood price");
        h.assertFalse(player.getCooldowns().isOnCooldown(GearFeature.BLOOD_SWORD.get()), "ordinary shield used phantom cooldown");
        h.succeed();
    }
}
