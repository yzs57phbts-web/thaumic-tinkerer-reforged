package thaumictinkerer.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumictinkerer.kami.KamiFeature;
import thaumictinkerer.awakenedarmor.AwakenedArmorFeature;

@GameTestHolder("bbtt_tt_tests")
@PrefixGameTestTemplate(false)
public final class PackArmorGameTests {
    private static void check(GameTestHelper helper, boolean awakened) {
        var player = TtTestSupport.mockServerPlayer(helper, GameType.SURVIVAL);
        player.setGameMode(GameType.SURVIVAL);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        player.setHealth(1000);
        var items = awakened ? java.util.List.of(AwakenedArmorFeature.COWL.get(), AwakenedArmorFeature.ROBES.get(), AwakenedArmorFeature.LEGGINGS.get(), AwakenedArmorFeature.BOOTS.get())
            : java.util.List.of(KamiFeature.ICHORCLOTH_COWL.get(), KamiFeature.ICHORCLOTH_ROBE.get(), KamiFeature.ICHORCLOTH_LEGGINGS.get(), KamiFeature.ICHORCLOTH_BOOTS.get());
        var slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i=0;i<4;i++) player.setItemSlot(slots[i],new ItemStack(items.get(i)));
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 2, 3.5)));
        for (int tick=0;tick<61;tick++) player.tick();
        player.doTick();
        for (float incoming : new float[]{1, 10, 30, 100}) {
            player.invulnerableTime=0;
            player.setHealth(1000);
            player.hurt(helper.getLevel().damageSources().cactus(),incoming);
            helper.assertTrue(Math.abs((1000-player.getHealth())-incoming*0.15F)<0.001,"Full ichor must absorb 85% at every damage strength");
        }
        player.invulnerableTime=0;player.setHealth(1000);
        player.hurt(helper.getLevel().damageSources().magic(),30);
        helper.assertTrue(Math.abs(1000-player.getHealth()-4.5F)<0.001,"Magic protection missing");
        helper.succeed();
    }

    @GameTest(template="test/platform")
    public static void basicIchor(GameTestHelper helper) { check(helper,false); }

    @GameTest(template="test/platform")
    public static void awakenedIchor(GameTestHelper helper) { check(helper,true); }
}
