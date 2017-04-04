package flaxbeard.cyberware.common.misc;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
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

	@Override
	public boolean matches(InventoryCrafting inv, World worldIn)
	{
		return new BlueprintResult(inv).canCraft;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv)
	{
		return new BlueprintResult(inv).output;
	}

	@Override
	public int getRecipeSize()
	{
		return 0;
	}

	@Override
	public ItemStack getRecipeOutput()
	{
		return ItemStack.EMPTY;
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv)
	{
		return new BlueprintResult(inv).remaining;
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
		
		private boolean process(InventoryCrafting inv)
		{
			boolean hasBlankBlueprint = false;
			for (int i = 0; i < inv.getSizeInventory(); i++)
			{
				ItemStack stack = inv.getStackInSlot(i);
				if (!stack.isEmpty())
				{
					if (stack.getItem() instanceof IDeconstructable)
					{
						if (ware.isEmpty())
						{
							ware = stack;
							wareStack = i;
							break;
						}
						else
						{
							return false;
						}
					}
					else if (stack.getItem() == CyberwareContent.blueprint
							&& (!stack.hasTagCompound()
							|| !stack.getTagCompound().hasKey("blueprintItem")))
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
