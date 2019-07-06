
package flaxbeard.cyberware.common.block;

import javax.annotation.Nullable;

import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.common.block.tile.TileEntitySurgery;

public class BlockSurgeryTable extends BlockBed
{
	
	public BlockSurgeryTable()
	{
		
		String name = "surgeryTable";
		
		this.setRegistryName(name);
		ForgeRegistries.BLOCKS.register(this);
		
		this.setTranslationKey(Cyberware.MODID + "." + name);

		GameRegistry.registerTileEntity(TileEntitySurgery.class, new ResourceLocation(Cyberware.MODID, name));
		
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer entityPlayer, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if (worldIn.isRemote)
		{
			return true;
		}
		else
		{
			if (state.getValue(PART) != BlockBed.EnumPartType.HEAD)
			{
				pos = pos.offset(state.getValue(FACING));
				state = worldIn.getBlockState(pos);

				if (state.getBlock() != this)
				{
					return true;
				}
			}

			if (worldIn.provider.canRespawnHere() && worldIn.getBiome(pos) != Biomes.HELL)
			{
				if (state.getValue(OCCUPIED))
				{
					EntityPlayer entityplayer = this.getPlayerInBed(worldIn, pos);

					if (entityplayer != null)
					{
						entityPlayer.sendMessage(new TextComponentTranslation("tile.bed.occupied"));
						return true;
					}

					state = state.withProperty(OCCUPIED, Boolean.FALSE);
					worldIn.setBlockState(pos, state, 4);
				}

				EntityPlayer.SleepResult entityplayer$sleepresult = entityPlayer.trySleep(pos);

				if (entityplayer$sleepresult == EntityPlayer.SleepResult.OK)
				{
					state = state.withProperty(OCCUPIED, Boolean.TRUE);
					worldIn.setBlockState(pos, state, 4);
					return true;
				}
				else
				{
					if (entityplayer$sleepresult == EntityPlayer.SleepResult.NOT_POSSIBLE_NOW)
					{
						entityPlayer.sendMessage(new TextComponentTranslation("tile.bed.noSleep"));
					}
					else if (entityplayer$sleepresult == EntityPlayer.SleepResult.NOT_SAFE)
					{
						entityPlayer.sendMessage(new TextComponentTranslation("tile.bed.notSafe"));
					}

					return true;
				}
			}
			else
			{
				worldIn.setBlockToAir(pos);
				BlockPos blockpos = pos.offset(state.getValue(FACING).getOpposite());

				if (worldIn.getBlockState(blockpos).getBlock() == this)
				{
					worldIn.setBlockToAir(blockpos);
				}

				worldIn.newExplosion(null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, 5.0F, true, true);
				return true;
			}
		}
	}

/*	@Nonnull
	public Item getItemDropped(IBlockState state, Random rand, int fortune)
	{
		return state.getValue(PART) == BlockBed.EnumPartType.HEAD ? null : CyberwareContent.surgeryTableItem;
	}

	public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state)
	{
		return new ItemStack(CyberwareContent.surgeryTableItem);
	}*/

	@Nullable
	private EntityPlayer getPlayerInBed(World worldIn, BlockPos pos)
	{
		for (EntityPlayer entityplayer : worldIn.playerEntities)
		{
			if (entityplayer.isPlayerSleeping() && entityplayer.getPosition().equals(pos))
			{
				return entityplayer;
			}
		}

		return null;
	}
	
	@Override
	public boolean isBed(IBlockState state, IBlockAccess world, BlockPos pos, Entity player)
	{
		return true;
	}
	
	@SubscribeEvent
	public void handleSleep(PlayerSleepInBedEvent event)
	{

	}
}
