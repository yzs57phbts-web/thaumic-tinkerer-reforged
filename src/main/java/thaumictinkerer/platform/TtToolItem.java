package thaumictinkerer.platform;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.platform.TcToolMaterial;

public class TtToolItem extends Item {
    private final int enchantmentValue;
    private final boolean axe;

    public TtToolItem(Properties properties, TcToolMaterial material) {
        this(properties, material, false);
    }

    public TtToolItem(Properties properties, TcToolMaterial material, boolean axe) {
        super(properties);
        enchantmentValue = material.enchantability();
        this.axe = axe;
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity defender, LivingEntity attacker) {
        return axe || super.canDisableShield(stack, shield, defender, attacker);
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }
}
