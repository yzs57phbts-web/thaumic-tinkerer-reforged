package thaumictinkerer.kami;

import net.minecraft.world.entity.EquipmentSlot;

public final class IchorArmorProtection {
    private IchorArmorProtection() {
    }

    public static double absorption(EquipmentSlot slot) {
        return 0.0425 * switch (slot) {
            case FEET, HEAD -> 3;
            case LEGS -> 8;
            case CHEST -> 6;
            default -> 0;
        };
    }
}
