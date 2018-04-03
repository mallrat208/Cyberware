package flaxbeard.cyberware.common.integration.enderio;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.common.CyberwareContent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class EnderIOIntegration
{
    public static final String MOD_ID = "enderio";

    public static void preInit()
    {
        MinecraftForge.EVENT_BUS.register(new EnderIOIntegration());
    }

    @Optional.Method(modid=MOD_ID)
    @SubscribeEvent(priority= EventPriority.HIGHEST, receiveCanceled=false)
    public void onTeleportEntity(crazypants.enderio.api.teleport.TeleportEntityEvent event)
    {
        if (event.getEntity() instanceof EntityLivingBase)
        {
            EntityLivingBase te = (EntityLivingBase) event.getEntity();
            ItemStack jam = new ItemStack(CyberwareContent.brainUpgrades, 1, 1);

            if (CyberwareAPI.isCyberwareInstalled(te, jam) && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(te, jam)))
            {
                event.setCanceled(true);
                return;
            }
            if (te != null)
            {
                float range = 25F;
                List<EntityLivingBase> test = te.world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(te.posX - range, te.posY - range, te.posZ - range, te.posX + te.width + range, te.posY + te.height + range, te.posZ + te.width + range));
                for (EntityLivingBase e : test)
                {
                    if (te.getDistance(e) <= range)
                    {
                        if (CyberwareAPI.isCyberwareInstalled(e, jam) && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(e, jam)))
                        {
                            event.setCanceled(true);
                            return;
                        }
                    }
                }
            }
        }
    }
}
