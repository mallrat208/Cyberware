package flaxbeard.cyberware.common.item;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.mojang.realmsclient.gui.ChatFormatting;

import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.IBlueprint;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.misc.NNLUtil;

public class ItemBlueprint extends Item implements IBlueprint
{
	public ItemBlueprint(String name)
	{
		this.setRegistryName(name);
		GameRegistry.register(this);
		this.setUnlocalizedName(Cyberware.MODID + "." + name);
		
		this.setCreativeTab(Cyberware.creativeTab);
				
		this.setHasSubtypes(true);
		this.setMaxStackSize(1);

		CyberwareContent.items.add(this);
	}
	
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced)
	{
		if (stack.hasTagCompound())
		{
			NBTTagCompound comp = stack.getTagCompound();
			if (comp.hasKey("blueprintItem"))
			{
				GameSettings settings = Minecraft.getMinecraft().gameSettings;
				if (settings.isKeyDown(settings.keyBindSneak))
				{
					ItemStack blueprintItem = new ItemStack(comp.getCompoundTag("blueprintItem"));
					if (!blueprintItem.isEmpty() && CyberwareAPI.canDeconstruct(blueprintItem))
					{
						NonNullList<ItemStack> items = NNLUtil.copyList(CyberwareAPI.getComponents(blueprintItem));
						tooltip.add(I18n.format("cyberware.tooltip.blueprint", blueprintItem.getDisplayName()));
						for (ItemStack item : items)
						{
							if (!item.isEmpty())
							{
								tooltip.add(item.getCount() + " x " + item.getDisplayName());
							}
						}
						return;
					}
				}
				else
				{
					tooltip.add(ChatFormatting.DARK_GRAY + I18n.format("cyberware.tooltip.shift_prompt"));
					return;
				}
			}
		}
		tooltip.add(ChatFormatting.DARK_GRAY + I18n.format("cyberware.tooltip.craft_blueprint"));
	}
	
	@Override
	public void getSubItems(Item item, CreativeTabs tab, NonNullList<ItemStack> list)
	{

		list.add(new ItemStack(this, 1, 1));
	}
	
	public static ItemStack getBlueprintForItem(ItemStack stack)
	{
		if (!stack.isEmpty() && CyberwareAPI.canDeconstruct(stack))
		{
			ItemStack toBlue = stack.copy();
			

			toBlue.setCount(1);
			if (toBlue.isItemStackDamageable())
			{
				toBlue.setItemDamage(0);
			}
			toBlue.setTagCompound(null);
			
			ItemStack ret = new ItemStack(CyberwareContent.blueprint);
			NBTTagCompound tag = new NBTTagCompound();
			NBTTagCompound itemTag = new NBTTagCompound();
			toBlue.writeToNBT(itemTag);
			tag.setTag("blueprintItem", itemTag);
			
			ret.setTagCompound(tag);
			return ret;
		}
		else
		{
			return ItemStack.EMPTY;
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public String getItemStackDisplayName(ItemStack stack)
	{
		if (stack.hasTagCompound())
		{
			NBTTagCompound comp = stack.getTagCompound();
			if (comp.hasKey("blueprintItem"))
			{
				ItemStack blueprintItem = new ItemStack(comp.getCompoundTag("blueprintItem"));
				if (!blueprintItem.isEmpty())
				{
					return I18n.format("item.cyberware.blueprint.not_blank.name", blueprintItem.getDisplayName()).trim();
				}
			}
		}
		return ("" + I18n.format(this.getUnlocalizedNameInefficiently(stack) + ".name")).trim();
	}

	@Override
	public ItemStack getResult(ItemStack stack, NonNullList<ItemStack> craftingItems)
	{
		if (stack.hasTagCompound())
		{
			NBTTagCompound comp = stack.getTagCompound();
			if (comp.hasKey("blueprintItem"))
			{
				ItemStack blueprintItem = new ItemStack(comp.getCompoundTag("blueprintItem"));
				if (!blueprintItem.isEmpty() && CyberwareAPI.canDeconstruct(blueprintItem))
				{
					NonNullList<ItemStack> requiredItems = NNLUtil.copyList(CyberwareAPI.getComponents(blueprintItem));
					for (int i = 0; i < requiredItems.size(); i++)
					{
						ItemStack required = requiredItems.get(i).copy();
						boolean satisfied = false;
						for (ItemStack crafting : craftingItems)
						{
							if (!crafting.isEmpty() && !required.isEmpty())
							{
								if (crafting.getItem() == required.getItem() && crafting.getItemDamage() == required.getItemDamage() && (!required.hasTagCompound() || (ItemStack.areItemStackTagsEqual(required, crafting))))
								{
									required.shrink(crafting.getCount());
								}
								if (required.getCount() <= 0)
								{
									satisfied = true;
									break;
								}
							}
						}
						if (!satisfied) return ItemStack.EMPTY;
					}
					
					return blueprintItem;
				}
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public NonNullList<ItemStack> consumeItems(ItemStack stack, NonNullList<ItemStack> craftingItems)
	{
		if (stack.hasTagCompound())
		{
			NBTTagCompound comp = stack.getTagCompound();
			if (comp.hasKey("blueprintItem"))
			{
				ItemStack blueprintItem = new ItemStack(comp.getCompoundTag("blueprintItem"));
				if (!blueprintItem.isEmpty() && CyberwareAPI.canDeconstruct(blueprintItem))
				{
					NonNullList<ItemStack> requiredItems = NNLUtil.copyList(CyberwareAPI.getComponents(blueprintItem));
					NonNullList<ItemStack> newCrafting = NonNullList.create();
					for (int c = 0; c < craftingItems.size(); c++)
					{
						newCrafting.add(craftingItems.get(c));
					}
					for (int i = 0; i < requiredItems.size(); i++)
					{
						ItemStack required = requiredItems.get(i).copy();
						boolean satisfied = false;
						for (int c = 0; c < newCrafting.size(); c++)
						{
							ItemStack crafting = newCrafting.get(c);
							if (!crafting.isEmpty() && !required.isEmpty())
							{
								if (crafting.getItem() == required.getItem() && crafting.getItemDamage() == required.getItemDamage() && (!required.hasTagCompound() || (ItemStack.areItemStackTagsEqual(required, crafting))))
								{
									int toSubtract = Math.min(required.getCount(), crafting.getCount());
									required.shrink(toSubtract);
									crafting.shrink(toSubtract);
									if (crafting.getCount() <= 0)
									{
										crafting = ItemStack.EMPTY;
									}
									newCrafting.set(c,crafting);
								}
								if (required.getCount() <= 0)
								{
									break;
								}
							}
						}
					}
					
					return newCrafting;
				}
			}
		}
		throw new IllegalStateException("Consuming items when items shouldn't be consumed!");
	}
}
