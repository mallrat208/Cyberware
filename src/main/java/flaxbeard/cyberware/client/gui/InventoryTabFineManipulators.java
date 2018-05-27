package flaxbeard.cyberware.client.gui;

import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.network.CyberwarePacketHandler;
import flaxbeard.cyberware.common.network.GuiPacket;
import micdoodle8.mods.galacticraft.api.client.tabs.AbstractTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class InventoryTabFineManipulators extends AbstractTab
{
	public InventoryTabFineManipulators()
	{
		super(0,0,0, new ItemStack(CyberwareContent.handUpgrades,1,0));
	}
	
	@Override
	public void onTabClicked()
	{
		Minecraft.getMinecraft().player.openGui(Cyberware.INSTANCE, 1, Minecraft.getMinecraft().player.world, 0, 0, 0);
		CyberwarePacketHandler.INSTANCE.sendToServer(new GuiPacket(1, 0, 0, 0));
	}
	
	@Override
	public boolean shouldAddToList()
	{
		EntityPlayerSP playerSP = Minecraft.getMinecraft().player;
		return CyberwareAPI.hasCapability(playerSP) && CyberwareAPI.isCyberwareInstalled(playerSP, new ItemStack(CyberwareContent.handUpgrades,1,0));
	}
}
