package flaxbeard.cyberware.client.render;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

public class ModelTrenchCoat extends ModelBiped
{
	public ModelRenderer bottomThing;
	
	public ModelTrenchCoat(float modelSize)
	{
		super(modelSize);
		bottomThing = new ModelRenderer(this, 16, 0);
		bottomThing.addBox(-4.0F, 0F, -1.7F, 8, 12, 4, modelSize);
		bottomThing.setRotationPoint(0, 12.0F, 0.0F);
	}
	
	@Override
	public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, @Nullable Entity entity)
	{
		super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity);
		
		bottomThing.setRotationPoint(0, bipedLeftLeg.rotationPointY, bipedLeftLeg.rotationPointZ);
		bottomThing.rotateAngleX = Math.max(bipedLeftLeg.rotateAngleX, bipedRightLeg.rotateAngleX) + 0.055F;
	}
	
	@Override
	public void render(@Nonnull Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale)
	{
		super.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
		GlStateManager.pushMatrix();
		
		if (isChild)
		{
			float factor = 0.5F;
			GlStateManager.scale(factor, factor, factor);
			GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
		}
		else
		{
			if (entity.isSneaking())
			{
				GlStateManager.translate(0.0F, 0.2F, 0.0F);
			}
		}
		bottomThing.render(scale);
		
		GlStateManager.popMatrix();
	}
}
