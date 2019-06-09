package flaxbeard.cyberware.api;

import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.integration.CyberwareMatterOverdriveCheck;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

public class CyberwareSurgeryEvent extends EntityEvent
{
	private final EntityLivingBase entityLiving;
	
	public CyberwareSurgeryEvent(EntityLivingBase entity)
	{
		super(entity);
		entityLiving = entity;
	}
	
	/**
	 * Fired when the Surgery Chamber starts the process of altering an entities installed Cyberware
	 * Cancel to prevent any changes
	 */
	@Cancelable
	public static class Pre extends CyberwareSurgeryEvent
	{
		public Pre(EntityLivingBase entity)
		{
			super(entity);
			if (isAndroid(entity)){
				setCanceled(true);
			}
		}

		private boolean isAndroid(EntityLivingBase entity){
			if (CyberwareConfig.INT_MATTER_OVERDRIVE && Loader.isModLoaded("matteroverdrive") && entity instanceof EntityPlayer){
				return CyberwareMatterOverdriveCheck.isPlayerAndroid((EntityPlayer)entity);
			}
			return false;
		}
	}
	
	/**
	 * Fired when the Surgery Chamber finishes the process of altering an entities installed Cyberware
	 */
	public static class Post extends CyberwareSurgeryEvent
	{
		
		public Post(EntityLivingBase entity)
		{
			super(entity);
		}
	}
}
