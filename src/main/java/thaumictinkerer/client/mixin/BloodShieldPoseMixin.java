package thaumictinkerer.client.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumictinkerer.gear.BloodShield;

@Mixin(HumanoidModel.class)
public abstract class BloodShieldPoseMixin {
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightArm;
    @Shadow private void poseBlockingArm(ModelPart arm, boolean right) { throw new AssertionError(); }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void thaumictinkerer$phantomArm(LivingEntity entity, float walk, float walkAmount, float age,
                                          float yaw, float pitch, CallbackInfo callback) {
        if (entity instanceof Player player && BloodShield.visible(player)) {
            // Vanilla poses only the actively used main arm; pose the visual offhand afterwards.
            boolean right = player.getMainArm().getOpposite() == HumanoidArm.RIGHT;
            poseBlockingArm(right ? rightArm : leftArm, right);
        }
    }
}
