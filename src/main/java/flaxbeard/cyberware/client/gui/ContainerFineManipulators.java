package flaxbeard.cyberware.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ContainerFineManipulators extends ContainerPlayer
{
	public ContainerFineManipulators(InventoryPlayer playerInventory, boolean localWorld, EntityPlayer player)
	{
		super(playerInventory, localWorld, player);
		
		this.inventorySlots.clear();
		
		craftMatrix = new InventoryCrafting(this, 3, 3);
		
		this.addSlotToContainer(new SlotCrafting(playerInventory.player, this.craftMatrix, this.craftResult, 0, 124, 35));
		
		for (int i = 0; i < 3; ++i)
		{
			for (int j = 0; j < 3; ++j)
			{
				this.addSlotToContainer(new Slot(this.craftMatrix, j + i * 3, 30 + j * 18, 17 + i * 18));
			}
		}
		
		for (int l = 0; l < 3; ++l)
		{
			for (int j1 = 0; j1 < 9; ++j1)
			{
				this.addSlotToContainer(new Slot(playerInventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
			}
		}
		
		for (int i1 = 0; i1 < 9; ++i1)
		{
			this.addSlotToContainer(new Slot(playerInventory, i1, 8 + i1 * 18, 142));
		}
	}
	
	@Override
	public void onContainerClosed(EntityPlayer playerIn)
	{
		super.onContainerClosed(playerIn);
		
		for (int i = 0; i < 9; ++i)
		{
			ItemStack itemstack = this.craftMatrix.removeStackFromSlot(i);
			
			if (!itemstack.isEmpty())
			{
				playerIn.dropItem(itemstack, false);
			}
		}
		
		this.craftResult.setInventorySlotContents(0, ItemStack.EMPTY);
	}
}
