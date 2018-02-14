package flaxbeard.cyberware.common.integration.tan;

import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import toughasnails.api.config.GameplayOption;
import toughasnails.api.config.SyncedConfig;
import toughasnails.api.stat.capability.IThirst;
import toughasnails.api.temperature.IModifierMonitor;
import toughasnails.api.temperature.IModifierMonitor.Context;
import toughasnails.api.temperature.Temperature;
import toughasnails.api.temperature.TemperatureScale.TemperatureRange;
import toughasnails.api.thirst.ThirstHelper;
import toughasnails.temperature.modifier.TemperatureModifier;

import javax.annotation.Nonnull;

public class CyberwareModifier extends TemperatureModifier
{
	private final Type cyberwareType;
	
	CyberwareModifier(Type type)
	{
		super(Cyberware.MODID + ":" + type.name);
		this.cyberwareType = type;
	}
	
	@Override
	public Temperature applyPlayerModifiers(@Nonnull EntityPlayer entityPlayer, @Nonnull Temperature temperature, @Nonnull IModifierMonitor iModifierMonitor)
	{
		int newTemperatureLevel = temperature.getRawValue();
		
		switch(this.cyberwareType)
		{
			case SWEAT:
				{
					if(CyberwareAPI.isCyberwareInstalled(entityPlayer, this.cyberwareType.getCyberware()))
					{
						boolean needCooling = temperature.getRange() == TemperatureRange.WARM || temperature.getRange() == TemperatureRange.HOT;
						
						if(needCooling && (ThirstHelper.getThirstData(entityPlayer).getThirst() > 0))
						{
							if(SyncedConfig.getBooleanValue(GameplayOption.ENABLE_THIRST))
							{
								IThirst data = ThirstHelper.getThirstData(entityPlayer);
								data.setExhaustion(Math.min(data.getExhaustion() + 0.008F, 40.0F));
							}
							
							newTemperatureLevel += this.cyberwareType.modifier;
						}
						
						iModifierMonitor.addEntry(new Context(this.getId(), Cyberware.MODNAME + ": " + this.cyberwareType.name, temperature, new Temperature(newTemperatureLevel)));
					}
				
					return new Temperature(newTemperatureLevel);
				}
			case BLUBBER:
				{
					if(CyberwareAPI.isCyberwareInstalled(entityPlayer, this.cyberwareType.getCyberware()))
					{
						newTemperatureLevel += this.cyberwareType.modifier;
						iModifierMonitor.addEntry(new Context(this.getId(), Cyberware.MODNAME + ": " + this.cyberwareType.name, temperature, new Temperature(newTemperatureLevel)));
					}
					
					return new Temperature(newTemperatureLevel);
				}
		}
		
		return temperature;
	}
	
	@Override
	public boolean isPlayerSpecific()
	{
		return true;
	}
	
	@Nonnull
	@Override
	public String getId()
	{
		return Cyberware.MODID + this.cyberwareType.name;
	}
	
	enum Type
	{
		SWEAT("Sweat",-4, 0),
		BLUBBER("Blubber",3, 1);
		
		private final String name;
		private final int modifier;
		private final int meta;
		
		Type(String id, int mod, int meta)
		{
			this.name = id;
			this.modifier = mod;
			this.meta = meta;
		}
		
		public ItemStack getCyberware()
		{
			return new ItemStack(ToughAsNailsIntegration.sweat, 1, this.meta);
		}
	}
}
