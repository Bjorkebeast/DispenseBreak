package com.bjorkebeast.dispensebreaker;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.OptionalDispenseBehavior;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraftforge.common.ForgeHooks;

import java.util.List;

public class ExtendedDispenseBehavior extends OptionalDispenseBehavior {

    public static void register( ToolItem tool ){

        DispenserBlock.registerDispenseBehavior( tool.asItem(), new ExtendedDispenseBehavior() );
    }

    public ExtendedDispenseBehavior() {
        super();
    }

    private boolean canHarvestBlock( World world, BlockPos pos, BlockState state, ItemStack stack ){

        if (state.getMaterial().isToolNotRequired()){

            return true;
        }

        return ForgeHooks.canToolHarvestBlock( world, pos, stack );
        // return tool.canHarvestBlock( state );
    }

    @SuppressWarnings("deprecation")
    protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
        World world = source.getWorld();

        if (world.isRemote()) {

            return stack;
        }

        this.successful = false;
        BlockPos blockpos = source.getBlockPos().offset(source.getBlockState().get(DispenserBlock.FACING));
        BlockState state = world.getBlockState( blockpos );

        if ( !this.canHarvestBlock( world, blockpos, state, stack ) ) {

            return stack;
        }

        // Get the drops
        List<ItemStack> drops = state.getDrops( (new LootContext.Builder((ServerWorld)world))
                        .withRandom(world.rand).withParameter(LootParameters.POSITION, blockpos)
                        .withParameter(LootParameters.TOOL, stack )
        );

        // Remove the block
        world.removeBlock( blockpos, false );

        // Spawn the drops
        ItemEntity item = new ItemEntity( world,  blockpos.getX(), blockpos.getY(), blockpos.getZ() );
        for( ItemStack drop : drops ){
            item.entityDropItem( drop );
        }

        // Add the item damage
        if (stack.attemptDamageItem(1, world.rand, (ServerPlayerEntity)null)) {
            stack.setCount(0);
        }

        this.successful = true;
        return stack;
    }

}
