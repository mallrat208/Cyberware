package flaxbeard.cyberware.common.integration;

import matteroverdrive.entity.android_player.AndroidPlayer;
import matteroverdrive.entity.player.MOPlayerCapabilityProvider;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.player.EntityPlayer;

public class CyberwareMatterOverdriveCheck {
    public static boolean isPlayerAndroid(EntityPlayer player){
        AndroidPlayer androidPlayer = MOPlayerCapabilityProvider.GetAndroidCapability(player);
        if (androidPlayer != null && androidPlayer.isAndroid()){
            return true;
        }
        return false;
    }
}
