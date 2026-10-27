package thaumictinkerer.gear;

import java.util.List;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import thaumictinkerer.ThaumicTinkerer;

@EventBusSubscriber(modid = ThaumicTinkerer.MODID)
public final class BloodSwordEvents {
    public static final float BLOCK_DAMAGE = 3.0F;
    public static final float STRIKE_DAMAGE = 2.0F;

    private BloodSwordEvents() {
    }

    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.PLAYER_ATTACK) || !(source.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(GearFeature.BLOOD_SWORD.get()) || !BloodSwordItem.isActivated(stack)) {
            return;
        }
        List<ItemStack> souls = SoulDrops.soulsFor(event.getEntity());
        if (souls == null) {
            return;
        }
        event.getDrops().clear();
        for (ItemStack soul : souls) {
            ItemEntity drop = new ItemEntity(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), soul);
            drop.setPickUpDelay(10);
            event.getDrops().add(drop);
        }
    }

    @SubscribeEvent
    static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || event.getNewDamage() <= 0) return;
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof LivingEntity attacker && attacker.getMainHandItem().is(GearFeature.BLOOD_SWORD.get())) {
            BloodShield.payPrice(attacker, STRIKE_DAMAGE);
        }
    }
}
