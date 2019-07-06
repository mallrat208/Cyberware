package flaxbeard.cyberware.common.block;

import java.util.Random;

import javax.annotation.Nonnull;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.block.item.ItemComponentBox;
import flaxbeard.cyberware.common.block.tile.TileEntityComponentBox;

public class BlockComponentBox extends BlockContainer
{
	public static final PropertyDirection FACING = BlockHorizontal.FACING;
	public ItemBlock ib;
	
	public BlockComponentBox()
	{
		super(Material.IRON);
		setHardness(5.0F);
		setResistance(10.0F);
		setSoundType(SoundType.METAL);
		
		String name = "component_box";
		
		this.setRegistryName(name);
		ForgeRegistries.BLOCKS.register(this);

		ib = new ItemComponentBox(this);
		ib.setRegistryName(name);
		ForgeRegistries.ITEMS.register(ib);
		
		this.setTranslationKey(Cyberware.MODID + "." + name);

		this.setCreativeTab(Cyberware.creativeTab);
		GameRegistry.registerTileEntity(TileEntityComponentBox.class, new ResourceLocation(Cyberware.MODID, name));
		
		CyberwareContent.items.add(ib);
		
		this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
	}
	
	private static final AxisAlignedBB ns = new AxisAlignedBB(4F / 16F, 0F, 1F / 16F, 12F / 16F, 10F / 16F, 15F / 16F);
	private static final AxisAlignedBB ew = new AxisAlignedBB(1F / 16F, 0F, 4F / 16F, 15F / 16F, 10F / 16F, 12F / 16F);
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
		EnumFacing face = state.getValue(FACING);
		if (face == EnumFacing.NORTH || face == EnumFacing.SOUTH)
		{
			return ew;
		}
		else
		{
			return ns;
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(IBlockState state)
	{
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullCube(IBlockState state)
	{
		return false;
	}
	
	@Nonnull
	@Override
	public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
	{
		return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	}
	
	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta)
	{
		return new TileEntityComponentBox();
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state)
	{
		return EnumBlockRenderType.MODEL;
	}
	
	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
	{
		worldIn.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
		if (stack.hasDisplayName())
		{
			TileEntity tileentity = worldIn.getTileEntity(pos);

			if (tileentity instanceof TileEntityComponentBox)
			{
				((TileEntityComponentBox) tileentity).setCustomInventoryName(stack.getDisplayName());
			}
		}
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("contents"))
		{
			TileEntity tileentity = worldIn.getTileEntity(pos);

			if (tileentity instanceof TileEntityComponentBox)
			{
				((TileEntityComponentBox) tileentity).slots.deserializeNBT(stack.getTagCompound().getCompoundTag("contents"));
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(int meta)
	{
		EnumFacing enumfacing = EnumFacing.byIndex(meta);

		if (enumfacing.getAxis() == EnumFacing.Axis.Y)
		{
			enumfacing = EnumFacing.NORTH;
		}

		return this.getDefaultState().withProperty(FACING, enumfacing);
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		return state.getValue(FACING).getIndex();
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState withRotation(IBlockState state, Rotation rot)
	{
		return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
	{
		return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState()
	{
		return new BlockStateContainer(this, FACING);
	}
	
	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer entityPlayer, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		TileEntity tileentity = worldIn.getTileEntity(pos);
		
		if (tileentity instanceof TileEntityComponentBox)
		{
			/*
			if (entityPlayer.isCreative() && entityPlayer.isSneaking())
			{
				TileEntityScanner scanner = ((TileEntityScanner) tileentity);
				scanner.ticks = CyberwareConfig.SCANNER_TIME - 200;
			}
			*/
			if (entityPlayer.isSneaking())
			{
				TileEntityComponentBox box = (TileEntityComponentBox) tileentity;
				ItemStack toDrop = this.getStack(box);
				
				if (entityPlayer.inventory.mainInventory.get(entityPlayer.inventory.currentItem).isEmpty())
				{
					entityPlayer.inventory.mainInventory.set(entityPlayer.inventory.currentItem, toDrop);
				}
				else
				{
					if (!entityPlayer.inventory.addItemStackToInventory(toDrop))
					{
						InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), toDrop);
					}
				}
				box.doDrop = false;
				worldIn.setBlockToAir(pos);
			}
			else
			{
				entityPlayer.openGui(Cyberware.INSTANCE, 5, worldIn, pos.getX(), pos.getY(), pos.getZ());
			}
		}
		
		return true;
		
	}
	
	private ItemStack getStack(TileEntityComponentBox box)
	{
		ItemStack stackToDrop = new ItemStack(ib);

		NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setTag("contents", box.slots.serializeNBT());
		stackToDrop.setTagCompound(tagCompound);
		
		if (box.hasCustomName())
		{
			stackToDrop = stackToDrop.setStackDisplayName(box.getName());
		}
		return stackToDrop;
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
	{ 
		TileEntity tileentity = worldIn.getTileEntity(pos);

		if (tileentity instanceof TileEntityComponentBox && !worldIn.isRemote)
		{
			TileEntityComponentBox box = (TileEntityComponentBox) tileentity;
			if (box.doDrop)
			{
				ItemStack stackToDrop = getStack(box);
				InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stackToDrop);
			}
		}
		
		super.breakBlock(worldIn, pos, state);
	}
	
	@Override
	public int quantityDropped(Random random)
	{
		return 0;
	}

}
