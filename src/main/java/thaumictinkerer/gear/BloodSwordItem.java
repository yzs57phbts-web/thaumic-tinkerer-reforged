package thaumictinkerer.gear;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import thaumcraft.item.gear.Repairable;

public class BloodSwordItem extends Item implements Repairable {
    public BloodSwordItem(Properties properties) {
        super(properties);
    }

    public static boolean isActivated(ItemStack stack) {
        return GearFeature.ACTIVE.has(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return BloodShield.DURATION;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                boolean activated = isActivated(stack);
                GearFeature.ACTIVE.set(stack, activated ? null : Boolean.TRUE);
                player.sendSystemMessage(Component.translatable(activated ? "ttinkerer.ttmisc.bloodSword.deactivateEssentiaHarvest" : "ttinkerer.ttmisc.bloodSword.activateEssentiaHarvest"));
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (hand != InteractionHand.MAIN_HAND || !player.getOffhandItem().isEmpty()
            || player.getCooldowns().isOnCooldown(this)) return net.minecraft.world.InteractionResultHolder.pass(stack);
        BloodShield.begin(player);
        player.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility ability) {
        return ability == net.neoforged.neoforge.common.ItemAbilities.SHIELD_BLOCK || super.canPerformAction(stack, ability);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("ttinkerer.bloodShield.use").withStyle(net.minecraft.ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("ttinkerer.bloodShield.price").withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (entity instanceof Player player && !level.isClientSide()) BloodShield.end(player);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide()) BloodShield.end(player);
        return stack;
    }
}
