package fr.adrien1106.reframed.mixin;

import fr.adrien1106.reframed.block.ReFramedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static fr.adrien1106.reframed.block.ReFramedEntity.BLOCKSTATE_KEY;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(
        method = "writeNbtToBlockEntity",
        at = @At("HEAD")
    )
    private static void placeBlockWithOffHandCamo(World world, PlayerEntity player, BlockPos pos, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Check if this already has block entity data
        NbtComponent existingData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        if (existingData != null) return;

        // Check if player has valid items in both hands
        if (player == null
            || player.getOffHandStack().isEmpty()
            || player.getMainHandStack().isEmpty()
            || !(player.getMainHandStack().getItem() instanceof BlockItem frame)
            || !(frame.getBlock() instanceof ReFramedBlock)
            || !(player.getOffHandStack().getItem() instanceof BlockItem block)
            || block.getBlock() instanceof BlockEntityProvider
            || (world.getBlockState(pos).contains(Properties.LAYERS) && world.getBlockState(pos).get(Properties.LAYERS) > 1)
            || !Block.isShapeFullCube(block.getBlock().getDefaultState().getCollisionShape(world, pos))
        ) return;

        // Add the camo block state to the stack's component data
        NbtCompound nbt = new NbtCompound();
        nbt.put(BLOCKSTATE_KEY + 1, NbtHelper.fromBlockState(block.getBlock().getDefaultState()));
        stack.apply(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.DEFAULT, comp -> comp.apply(n -> n.copyFrom(nbt)));

        player.getOffHandStack().decrement(1);
    }

}
