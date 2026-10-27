package thaumictinkerer.platform;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

public final class PouchStorage {
    private PouchStorage() {
    }

    public static boolean canStore(ItemStack stack) {
        return stack.canFitInsideContainerItems()
            && stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().allMatch(ItemStack::isEmpty)
            && stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).isEmpty();
    }
}
