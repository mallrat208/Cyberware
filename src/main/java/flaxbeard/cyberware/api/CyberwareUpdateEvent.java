package flaxbeard.cyberware.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.EntityEvent;

public class CyberwareUpdateEvent extends EntityEvent
{
	private final EntityLivingBase entityLivingBase;
	
	public CyberwareUpdateEvent(EntityLivingBase entityLivingBase)
	{
		super(entityLivingBase);
		this.entityLivingBase = entityLivingBase;
	}

	public EntityLivingBase getEntityLiving()
	{
		return entityLivingBase;
	}
}
