package flaxbeard.cyberware.common.misc;

import javax.annotation.Nonnull;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.oredict.RecipeSorter;
import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.api.item.IDeconstructable;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.item.ItemBlueprint;

public class BlueprintCraftingHandler implements IRecipe
{
	static
	{
		RecipeSorter.register(Cyberware.MODID + ":blueprintCrafting", BlueprintCraftingHandler.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");
	}
	
	private IRecipe realRecipe;
	
	public BlueprintCraftingHandler()
	{
		this.realRecipe = this;
	}
	
	@Override
	public ResourceLocation getRegistryName()
	{
		return new ResourceLocation(Cyberware.MODID, "blueprintCrafting");
	}
	
	@Override
	public boolean canFit(int width, int height)
	{
		return true;
	}
	
	@Override
	public IRecipe setRegistryName(ResourceLocation name)
	{
		return realRecipe.setRegistryName(name);
	}
	
	@Override
	public Class<IRecipe> getRegistryType()
	{
		return realRecipe.getRegistryType();
	}
	
	@Override
	public boolean matches(@Nonnull InventoryCrafting inventoryCrafting, @Nonnull World world)
	{
		return new BlueprintResult(inventoryCrafting).canCraft;
	}
	
	@Nonnull
	@Override
	public ItemStack getCraftingResult(@Nonnull InventoryCrafting inventoryCrafting)
	{
		return new BlueprintResult(inventoryCrafting).output;
	}
	
	@Nonnull
	@Override
	public ItemStack getRecipeOutput()
	{
		return ItemStack.EMPTY;
	}
	
	@Nonnull
	@Override
	public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventoryCrafting)
	{
		return new BlueprintResult(inventoryCrafting).remaining;
	}
	
	private class BlueprintResult
	{
		private final boolean canCraft;
		private final NonNullList<ItemStack> remaining;
		private final ItemStack output;
		
		private ItemStack ware;
		int wareStack = 0;
		
		public BlueprintResult(InventoryCrafting inv)
		{
			this.ware = ItemStack.EMPTY;
			this.canCraft = process(inv);
			if (canCraft)
			{
				remaining = NonNullList.create();
				remaining.add(ware.copy());
				output = ItemBlueprint.getBlueprintForItem(ware);
			}
			else
			{
				remaining = NonNullList.create();
				output = ItemStack.EMPTY;
			}
		}
		
		private boolean process(InventoryCrafting inventoryCrafting)
		{
			boolean hasBlankBlueprint = false;
			for (int indexSlot = 0; indexSlot < inventoryCrafting.getSizeInventory(); indexSlot++)
			{
				ItemStack stack = inventoryCrafting.getStackInSlot(indexSlot);
				if (!stack.isEmpty())
				{
					if (stack.getItem() instanceof IDeconstructable)
					{
						if (ware.isEmpty())
						{
							ware = stack;
							wareStack = indexSlot;
						}
						else
						{
							return false;
						}
					}
					else if ( stack.getItem() == CyberwareContent.blueprint
					       && ( !stack.hasTagCompound()
					         || !stack.getTagCompound().hasKey("blueprintItem") ) )
					{
						if (!hasBlankBlueprint)
						{
							hasBlankBlueprint = true;
						}
						else
						{
							return false;
						}
					}
					else
					{
						return false;
					}
					
				}
			}
			return !ware.isEmpty() && hasBlankBlueprint;
		}
	}

}
