package flaxbeard.cyberware.common.handler;

import java.util.List;
import java.util.Stack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.ICyberwareUserData;
import flaxbeard.cyberware.api.hud.CyberwareHudDataEvent;
import flaxbeard.cyberware.api.hud.CyberwareHudEvent;
import flaxbeard.cyberware.api.hud.IHudElement;
import flaxbeard.cyberware.api.hud.IHudElement.EnumAnchorHorizontal;
import flaxbeard.cyberware.api.hud.IHudElement.EnumAnchorVertical;
import flaxbeard.cyberware.api.hud.NotificationInstance;
import flaxbeard.cyberware.api.item.IHudjack;
import flaxbeard.cyberware.client.KeyBinds;
import flaxbeard.cyberware.client.gui.GuiHudConfiguration;
import flaxbeard.cyberware.client.gui.hud.MissingPowerDisplay;
import flaxbeard.cyberware.client.gui.hud.NotificationDisplay;
import flaxbeard.cyberware.client.gui.hud.PowerDisplay;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.CyberwareContent;

public class HudHandler
{
	public static final HudHandler INSTANCE = new HudHandler();
	
	// http://stackoverflow.com/a/16206356/1754640
	private static class NotificationStack<T> extends Stack<T>
	{
		private int maxSize;

		public NotificationStack(int size)
		{
			super();
			this.maxSize = size;
		}

		@Override
		public T push(T object)
		{
			while (this.size() >= maxSize)
			{
				this.remove(0);
			}
			return super.push(object);
		}
	}
	
	public static void addNotification(NotificationInstance notification)
	{
		notifications.push(notification);
	}
	
	public static final ResourceLocation HUD_TEXTURE = new ResourceLocation(Cyberware.MODID + ":textures/gui/hud.png");
	public static Stack<NotificationInstance> notifications = new NotificationStack<>(5);
	
	private static PowerDisplay powerDisplay = new PowerDisplay();
	private static MissingPowerDisplay missingPowerDisplay = new MissingPowerDisplay();
	private static NotificationDisplay notificationDisplay = new NotificationDisplay();
	
	static
	{
		notificationDisplay.setHorizontalAnchor(EnumAnchorHorizontal.LEFT);
		notificationDisplay.setVerticalAnchor(EnumAnchorVertical.BOTTOM);
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void addHudElements(CyberwareHudEvent event)
	{
		if (event.isHudjackAvailable())
		{
			event.addElement(powerDisplay);
			event.addElement(missingPowerDisplay);
			event.addElement(notificationDisplay);
		}
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void saveHudElements(CyberwareHudDataEvent event)
	{
		event.addElement(powerDisplay);
		event.addElement(missingPowerDisplay);
		event.addElement(notificationDisplay);
	}
	
	private int lastTickExisted = 0;
	private double lastVelX = 0;
	private double lastVelY = 0;
	private double lastVelZ = 0;
	private double lastLastVelX = 0;
	private double lastLastVelY = 0;
	private double lastLastVelZ = 0;
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onDrawScreenPost(RenderTickEvent event)
	{
		if (event.phase != Phase.END) return;
		
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP entityPlayerSP = mc.player;
		if (entityPlayerSP == null) return;
		
		ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityPlayerSP);
		if (cyberwareUserData == null) return;
		
		GlStateManager.pushMatrix();
		float floatAmt = 0.0F;
		boolean isHUDjackAvailable = false;
		
		List<ItemStack> listHUDjackItems = cyberwareUserData.getHudjackItems();
		for (ItemStack stack : listHUDjackItems)
		{
			if (((IHudjack) CyberwareAPI.getCyberware(stack)).isActive(stack))
			{
				isHUDjackAvailable = true;
				if (CyberwareConfig.ENABLE_FLOAT)
				{
					if (CyberwareAPI.getCyberware(stack) == CyberwareContent.eyeUpgrades)
					{
						floatAmt = CyberwareConfig.HUDLENS_FLOAT;
					}
					else
					{
						floatAmt = CyberwareConfig.HUDJACK_FLOAT;
					}
				}
				break;
			}
		}
		
		double accelLastY = lastVelY - lastLastVelY;
		double accelY = entityPlayerSP.motionY - lastVelY;
		double accelPitch = accelLastY + (accelY - accelLastY) * (event.renderTickTime + entityPlayerSP.ticksExisted - lastTickExisted) / 2F;
		
		double pitchCameraMove = floatAmt * ((entityPlayerSP.prevRenderArmPitch + (entityPlayerSP.renderArmPitch - entityPlayerSP.prevRenderArmPitch) * event.renderTickTime) - entityPlayerSP.rotationPitch);
		double yawCameraMove   = floatAmt * ((entityPlayerSP.prevRenderArmYaw   + (entityPlayerSP.renderArmYaw   - entityPlayerSP.prevRenderArmYaw  ) * event.renderTickTime) - entityPlayerSP.rotationYaw  );
		
		GlStateManager.translate(yawCameraMove, pitchCameraMove + accelPitch * 50F * floatAmt, 0);
		
		if (entityPlayerSP.ticksExisted > lastTickExisted + 1)
		{
			lastTickExisted = entityPlayerSP.ticksExisted;
			lastLastVelX = lastVelX;
			lastLastVelY = lastVelY;
			lastLastVelZ = lastVelZ;
			lastVelX = entityPlayerSP.motionX;
			lastVelY = entityPlayerSP.motionY;
			lastVelZ = entityPlayerSP.motionZ;
		}
		
		ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
		CyberwareHudEvent hudEvent = new CyberwareHudEvent(scaledResolution, isHUDjackAvailable);
		MinecraftForge.EVENT_BUS.post(hudEvent);
		List<IHudElement> hudElements = hudEvent.getElements();
		isHUDjackAvailable = hudEvent.isHudjackAvailable();
		
		for (IHudElement hudElement : hudElements)
		{
			if (hudElement.getHeight() + GuiHudConfiguration.getAbsoluteY(scaledResolution, hudElement) <= 3)
			{
				GuiHudConfiguration.setYFromAbsolute(scaledResolution, hudElement, 0 - hudElement.getHeight() + 4);
			}
			
			if (GuiHudConfiguration.getAbsoluteY(scaledResolution, hudElement) >= scaledResolution.getScaledHeight() - 3)
			{
				GuiHudConfiguration.setYFromAbsolute(scaledResolution, hudElement, scaledResolution.getScaledHeight() - 4);
			}
			
			if (hudElement.getWidth() + GuiHudConfiguration.getAbsoluteX(scaledResolution, hudElement) <= 3)
			{
				GuiHudConfiguration.setXFromAbsolute(scaledResolution, hudElement, 0 - hudElement.getWidth() + 4);
			}
			
			if (GuiHudConfiguration.getAbsoluteX(scaledResolution, hudElement) >= scaledResolution.getScaledWidth() - 3)
			{
				GuiHudConfiguration.setXFromAbsolute(scaledResolution, hudElement, scaledResolution.getScaledWidth() - 4);
			}
			
			hudElement.render(entityPlayerSP, scaledResolution, isHUDjackAvailable, mc.currentScreen instanceof GuiHudConfiguration, event.renderTickTime);
		}
		
		// Display a prompt to the user to open the radial menu if they haven't yet
		if ( cyberwareUserData.getActiveItems().size() > 0
		  && !cyberwareUserData.hasOpenedRadialMenu() )
		{
			String textOpenMenu = I18n.format("cyberware.gui.open_menu", KeyBinds.menu.getDisplayName());
			FontRenderer fontRenderer = mc.fontRenderer;
			fontRenderer.drawStringWithShadow(textOpenMenu, scaledResolution.getScaledWidth() - fontRenderer.getStringWidth(textOpenMenu) - 5, 5, CyberwareAPI.getHUDColorHex());
		}
		
		GlStateManager.popMatrix();
	}
}