package flaxbeard.cyberware.common.integration.enderio;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.item.ItemBrainUpgrade;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class EnderIOIntegration
{
    public static final String MOD_ID = "enderio";

    public static void preInit()
    {
        MinecraftForge.EVENT_BUS.register(new EnderIOIntegration());
    }

    @Optional.Method(modid=MOD_ID)
    @SubscribeEvent(priority= EventPriority.HIGHEST)
    public void onTeleportEntity(crazypants.enderio.api.teleport.TeleportEntityEvent event)
    {
        if (!(event.getEntity() instanceof EntityLivingBase)) return;
        EntityLivingBase entityLivingBase = (EntityLivingBase) event.getEntity();
        
        ItemStack itemStackJammer = new ItemStack(CyberwareContent.brainUpgrades, 1, ItemBrainUpgrade.META_ENDER_JAMMER);

        if ( CyberwareAPI.isCyberwareInstalled(entityLivingBase, itemStackJammer)
          && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(entityLivingBase, itemStackJammer)) )
        {
            event.setCanceled(true);
            return;
        }
        
        if (entityLivingBase != null)
        {
            float range = 25F;
            List<EntityLivingBase> entitiesInRange = entityLivingBase.world.getEntitiesWithinAABB(
                    EntityLivingBase.class,
                    new AxisAlignedBB(entityLivingBase.posX - range, entityLivingBase.posY - range, entityLivingBase.posZ - range,
                                      entityLivingBase.posX + entityLivingBase.width + range, entityLivingBase.posY + entityLivingBase.height + range, entityLivingBase.posZ + entityLivingBase.width + range));
            for (EntityLivingBase entityInRange : entitiesInRange)
            {
                if (entityLivingBase.getDistance(entityInRange) <= range)
                {
                    if ( CyberwareAPI.isCyberwareInstalled(entityInRange, itemStackJammer)
                      && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(entityInRange, itemStackJammer)) )
                    {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }
}
