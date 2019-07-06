package flaxbeard.cyberware.common.block;

import javax.annotation.Nonnull;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.block.item.ItemBlockCyberware;
import flaxbeard.cyberware.common.block.tile.TileEntityCharger;

public class BlockCharger extends BlockContainer
{

	public BlockCharger()
	{
		super(Material.IRON);
		setHardness(5.0F);
		setResistance(10.0F);
		setSoundType(SoundType.METAL);
		
		String name = "charger";
		
		this.setRegistryName(name);
		ForgeRegistries.BLOCKS.register(this);

		ItemBlock ib = new ItemBlockCyberware(this, "cyberware.tooltip.charger.0","cyberware.tooltip.charger.1");
		ib.setRegistryName(name);
		ForgeRegistries.ITEMS.register(ib);
		
		this.setTranslationKey(Cyberware.MODID + "." + name);

		this.setCreativeTab(Cyberware.creativeTab);
		GameRegistry.registerTileEntity(TileEntityCharger.class, new ResourceLocation(Cyberware.MODID, name));
		
		CyberwareContent.blocks.add(this);
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta)
	{
		return new TileEntityCharger();
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state)
	{
		return EnumBlockRenderType.MODEL;
	}
}
