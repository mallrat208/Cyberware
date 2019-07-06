package flaxbeard.cyberware.common.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.client.ClientUtils;
import flaxbeard.cyberware.common.lib.LibConstants;

public class ItemLungsUpgrade extends ItemCyberware
{

	private static final int META_COMPRESSED_OXYGEN       = 0;
	private static final int META_HYPEROXYGENATION_BOOST  = 1;
	
	public ItemLungsUpgrade(String name, EnumSlot slot, String[] subnames)
	{
		super(name, slot, subnames);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onDrawScreenPost(RenderGameOverlayEvent.Post event)
	{
		if (event.getType() == ElementType.AIR)
		{
			EntityPlayer entityPlayer = Minecraft.getMinecraft().player;
			if (CyberwareAPI.isCyberwareInstalled(entityPlayer, new ItemStack(this, 1, META_COMPRESSED_OXYGEN)) && !entityPlayer.isCreative())
			{
				GL11.glPushMatrix();
				ItemStack stack = CyberwareAPI.getCyberware(entityPlayer, new ItemStack(this, 1, META_COMPRESSED_OXYGEN));
				int air = getAir(stack);
				
				Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.ICONS);
				
				ScaledResolution res = event.getResolution();
				GlStateManager.enableBlend();
				int left = res.getScaledWidth() / 2 + 91;
				int top = res.getScaledHeight() - 49 - 8;
				
				float r = 1.0F;
				float b = 1.0F;
				float g = 1.0F;
				
				if (entityPlayer.isInsideOfMaterial(Material.WATER))
				{
					while (air > 0)
					{
						r += 1.0F;
						b -= 0.25F;
						g += 0.25F;
						GL11.glColor3f(r, g, b);
						int drawAir = Math.min(300, air);
						int full = MathHelper.ceil((double)(drawAir - 2) * 10.0D / 300.0D);
						int partial = MathHelper.ceil((double)drawAir * 10.0D / 300.0D) - full;
				
						for (int i = 0; i < full + partial; ++i)
						{
							ClientUtils.drawTexturedModalRect(left - i * 8 - 9, top, (i < full ? 16 : 25), 18, 9, 9);
						}
						
						air -= 300;
						top -= 8;
					}
				}
				
				GL11.glColor3f(1.0F, 1.0F, 1.0F);
				//GlStateManager.disableBlend();
				GL11.glPopMatrix();
			}
		}
	}
	
	private Map<UUID, Boolean> mapIsOxygenPowered = new HashMap<>();
	
	@SubscribeEvent
	public void handleLivingUpdate(CyberwareUpdateEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		if (CyberwareAPI.isCyberwareInstalled(entityLivingBase, new ItemStack(this, 1, META_COMPRESSED_OXYGEN)))
		{
			ItemStack stack = CyberwareAPI.getCyberware(entityLivingBase, new ItemStack(this, 1, META_COMPRESSED_OXYGEN));
			int air = getAir(stack);
			if (entityLivingBase.getAir() < 300 && air > 0)
			{
				int toAdd = Math.min(300 - entityLivingBase.getAir(), air);
				entityLivingBase.setAir(entityLivingBase.getAir() + toAdd);
				CyberwareAPI.getCyberwareNBT(stack).setInteger("air", air - toAdd);
			}
			else if (entityLivingBase.getAir() == 300 && air < 900)
			{
				CyberwareAPI.getCyberwareNBT(stack).setInteger("air", air + 1);
			}
		}
		
		ItemStack test = new ItemStack(this, 1, META_HYPEROXYGENATION_BOOST);
		if (CyberwareAPI.isCyberwareInstalled(entityLivingBase, new ItemStack(this, 1, META_HYPEROXYGENATION_BOOST)))
		{
			if ((entityLivingBase.isSprinting() || entityLivingBase instanceof EntityMob) && !entityLivingBase.isInWater() && entityLivingBase.onGround)
			{
				boolean wasPowered = getIsOxygenPowered(entityLivingBase);

				int ranks = CyberwareAPI.getCyberwareRank(entityLivingBase, test);
				test.setCount(ranks);
				boolean isPowered = entityLivingBase.ticksExisted % 20 == 0
				                  ? CyberwareAPI.getCapability(entityLivingBase).usePower(test, getPowerConsumption(test))
				                  : wasPowered;
				if (isPowered)
				{
					entityLivingBase.moveRelative(0F, 0.0F, .2F * ranks, 0.075F);
				}
				
				mapIsOxygenPowered.put(entityLivingBase.getUniqueID(), isPowered);
			}
		}
	}
	
	private boolean getIsOxygenPowered(EntityLivingBase entityLivingBase)
	{
		if (!mapIsOxygenPowered.containsKey(entityLivingBase.getUniqueID()))
		{
			mapIsOxygenPowered.put(entityLivingBase.getUniqueID(), Boolean.TRUE);
		}
		return mapIsOxygenPowered.get(entityLivingBase.getUniqueID());
	}
	
	@Override
	public int installedStackSize(ItemStack stack)
	{
		return stack.getItemDamage() == META_HYPEROXYGENATION_BOOST ? 3 : 1;
	}

	private int getAir(ItemStack stack)
	{
		NBTTagCompound tagCompound = CyberwareAPI.getCyberwareNBT(stack);
		if (!tagCompound.hasKey("air"))
		{
			tagCompound.setInteger("air", 900);
		}
		return tagCompound.getInteger("air");
	}
	
	@Override
	public int getPowerConsumption(ItemStack stack)
	{
		return stack.getItemDamage() == META_HYPEROXYGENATION_BOOST ? LibConstants.HYPEROXYGENATION_CONSUMPTION * stack.getCount() : 0;
	}
	
	@Override
	protected int getUnmodifiedEssenceCost(ItemStack stack)
	{
		if (stack.getItemDamage() == META_HYPEROXYGENATION_BOOST)
		{
			switch (stack.getCount())
			{
				case 1:
					return 2;
				case 2:
					return 4;
				case 3:
					return 5;
			}
		}
		return super.getUnmodifiedEssenceCost(stack);
	}
}
