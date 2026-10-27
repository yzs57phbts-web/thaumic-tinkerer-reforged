package thaumictinkerer.gear;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumictinkerer.ThaumicTinkerer;

/** A shield action and a client-only model; no temporary inventory item exists. */
@EventBusSubscriber(modid = ThaumicTinkerer.MODID)
public final class BloodShield {
    public static final int DURATION = 40;
    public static final int COOLDOWN = 60;
    public static final int AXE_COOLDOWN = 100;
    private static final ResourceKey<DamageType> PRICE = ResourceKey.create(Registries.DAMAGE_TYPE, ThaumicTinkerer.id("blood_price"));
    private static final Map<Player, State> ACTIVE = new WeakHashMap<>();

    private static final class State {
        boolean consumed;
    }

    private BloodShield() {}

    public static boolean visible(Player player) {
        return player.isAlive() && player.isUsingItem()
            && player.getUsedItemHand() == InteractionHand.MAIN_HAND
            && player.getUseItem().is(GearFeature.BLOOD_SWORD.get())
            && player.getMainHandItem().is(GearFeature.BLOOD_SWORD.get())
            && player.getOffhandItem().isEmpty()
            && player.getTicksUsingItem() < DURATION
            && !player.getCooldowns().isOnCooldown(GearFeature.BLOOD_SWORD.get());
    }

    public static void begin(Player player) {
        if (!player.level().isClientSide()) ACTIVE.put(player, new State());
    }

    public static void end(Player player) {
        State state = ACTIVE.remove(player);
        if (state != null && !state.consumed) {
            player.getCooldowns().addCooldown(GearFeature.BLOOD_SWORD.get(), COOLDOWN);
        }
    }

    public static void payPrice(LivingEntity entity, float amount) {
        // Preserve magic mitigation, but another hit's invulnerability must not waive the price.
        var type = entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(PRICE);
        entity.hurt(new DamageSource(type), amount);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void block(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || !player.getUseItem().is(GearFeature.BLOOD_SWORD.get())) return;
        State state = ACTIVE.get(player);
        if (state == null || state.consumed || !visible(player)) {
            event.setBlocked(false);
            return;
        }
        // Vanilla supplies the frontal check, raise delay, bypass tags and piercing-arrow rules.
        if (!event.getBlocked() || event.getBlockedDamage() <= 0) return;
        state.consumed = true;
        boolean axe = event.getDamageSource().getDirectEntity() instanceof LivingEntity attacker && attacker.canDisableShield();
        player.getCooldowns().addCooldown(GearFeature.BLOOD_SWORD.get(), axe ? AXE_COOLDOWN : COOLDOWN);
        event.setShieldDamage(0);
        payPrice(player, BloodSwordEvents.BLOCK_DAMAGE);
        // Keep the use item until vanilla has processed axe disabling and shield knockback.
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        State state = ACTIVE.get(player);
        if (state != null && (state.consumed || !visible(player))) {
            end(player);
            if (player.getUseItem().is(GearFeature.BLOOD_SWORD.get())) player.stopUsingItem();
        }
    }
}
