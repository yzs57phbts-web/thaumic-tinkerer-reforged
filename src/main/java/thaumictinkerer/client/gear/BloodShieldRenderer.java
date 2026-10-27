package thaumictinkerer.client.gear;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.gear.BloodShield;

/** Temporary vanilla shield model only: it can never be moved, dropped or saved. */
@EventBusSubscriber(modid = ThaumicTinkerer.MODID, value = Dist.CLIENT)
public final class BloodShieldRenderer {
    private static final ItemStack SHIELD = new ItemStack(Items.SHIELD);

    private BloodShieldRenderer() {}

    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null || event.getHand() != InteractionHand.OFF_HAND || !BloodShield.visible(player)) return;
        event.setCanceled(true);
        boolean left = player.getMainArm().getOpposite() == HumanoidArm.LEFT;
        float side = left ? -1 : 1;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(side * (0.56F - 0.14142136F), -0.52F + 0.08F, -0.72F + 0.14142136F);
        Minecraft.getInstance().gameRenderer.itemInHandRenderer.renderItem(player, SHIELD,
            left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            left, pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
    }

    @EventBusSubscriber(modid = ThaumicTinkerer.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void layers(EntityRenderersEvent.AddLayers event) {
            for (var skin : event.getSkins()) {
                PlayerRenderer renderer = event.getSkin(skin);
                if (renderer != null) renderer.addLayer(new Layer(renderer));
            }
        }
    }

    private static final class Layer extends ItemInHandLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        Layer(PlayerRenderer renderer) {
            super(renderer, Minecraft.getInstance().gameRenderer.itemInHandRenderer);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                           float limbSwing, float limbAmount, float partialTick, float age, float yaw, float pitch) {
            if (!BloodShield.visible(player)) return;
            HumanoidArm arm = player.getMainArm().getOpposite();
            renderArmWithItem(player, SHIELD, arm == HumanoidArm.LEFT
                ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                arm, pose, buffers, light);
        }
    }
}
