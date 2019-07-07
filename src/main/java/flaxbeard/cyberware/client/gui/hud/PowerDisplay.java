package flaxbeard.cyberware.client.gui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.ICyberwareUserData;
import flaxbeard.cyberware.api.hud.HudElementBase;
import flaxbeard.cyberware.client.ClientUtils;
import flaxbeard.cyberware.common.handler.HudHandler;

public class PowerDisplay extends HudElementBase
{

	private static float cache_percentFull = 0;
	private static int cache_power_capacity = 0;
	private static int cache_power_stored = 0;
	private static int cache_power_production = 0;
 	private static int cache_power_consumption = 0;

	public PowerDisplay()
	{
		super("cyberware:power");
		setDefaultX(5);
		setDefaultY(5);
		setHeight(25);
		setWidth(101);
	}

	@Override
	public void renderElement(int x, int y, EntityPlayer entityPlayer, ScaledResolution resolution, boolean isHUDjackAvailable, boolean isConfigOpen, float partialTicks)
	{
		if ( isHidden()
		  || !isHUDjackAvailable ) {
			return;
		}
		
		boolean isRightAnchored = getHorizontalAnchor() == EnumAnchorHorizontal.RIGHT;
		
		if (entityPlayer.ticksExisted % 20 == 0)
		{
			ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityPlayer);
			if (cyberwareUserData == null) return;
			
			cache_percentFull = cyberwareUserData.getPercentFull();
			cache_power_capacity = cyberwareUserData.getCapacity();
			cache_power_stored = cyberwareUserData.getStoredPower();
			cache_power_production = cyberwareUserData.getProduction();
			cache_power_consumption = cyberwareUserData.getConsumption();
		}
		
		if (cache_power_capacity == 0) return;
		
		boolean isLowPower = cache_percentFull <= 0.2F;
		boolean isCriticalPower = cache_percentFull <= 0.05F;
		
		if ( isCriticalPower
		  && entityPlayer.ticksExisted % 4 == 0 )
		{
			return;
		}
		
		float[] colorFloats = CyberwareAPI.getHUDColor();
		int colorHex = CyberwareAPI.getHUDColorHex();
		
		GL11.glPushMatrix();
		GlStateManager.enableBlend();
		
		Minecraft.getMinecraft().getTextureManager().bindTexture(HudHandler.HUD_TEXTURE);
		
		FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
		
		// battery icon
		GlStateManager.pushMatrix();
		
		int uOffset = isLowPower ? 39 : 0;
		int xOffset = isRightAnchored ? (x + getWidth() - 13) : x;
		int yBatterySize = Math.round(21F * cache_percentFull);
		
		if (!isLowPower) GlStateManager.color(colorFloats[0], colorFloats[1], colorFloats[2]);
		
		// battery top part
		ClientUtils.drawTexturedModalRect(xOffset, y, uOffset, 0, 13, 2 + (21 - yBatterySize));
		// battery background
		ClientUtils.drawTexturedModalRect(xOffset, y + 2 + (21 - yBatterySize), 13 + uOffset, 2 + (21 - yBatterySize), 13, yBatterySize + 2);
		// battery foreground
		ClientUtils.drawTexturedModalRect(xOffset, y + 2 + (21 - yBatterySize), 26 + uOffset, 2 + (21 - yBatterySize), 13, yBatterySize + 2);
		GlStateManager.popMatrix();
		
		// storage stats
		String textPowerStorage = cache_power_stored + " / " + cache_power_capacity;
		int xPowerStorage = isRightAnchored ? x + getWidth() - 15 - fontRenderer.getStringWidth(textPowerStorage) : x + 15;
		fontRenderer.drawStringWithShadow(textPowerStorage, xPowerStorage, y + 4, isLowPower ? 0xFF0000 : colorHex);
		
		// progression stats
		String textPowerProgression = "-" + cache_power_consumption + " / +" + cache_power_production;
		int xPowerProgression = isRightAnchored ? x + getWidth() - 15 - fontRenderer.getStringWidth(textPowerProgression) : x + 15;
		fontRenderer.drawStringWithShadow(textPowerProgression, xPowerProgression, y + 14, isLowPower ? 0xFF0000 : colorHex);
		
		GL11.glPopMatrix();
	}
}
