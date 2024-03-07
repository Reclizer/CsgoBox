package com.reclizer.csgobox.utils;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class RenderFontTool {


    public  static int drawString(GuiGraphics guiGraphics,Font pFont, FormattedCharSequence pText, float pX, float pY,int ox,int oy,float scale,int pColor) {
        //guiGraphics.pose().last().pose().scale(scale)
        int z=1;
        guiGraphics.pose().pushPose();
        Matrix4f pMatrix=guiGraphics.pose().last().pose();
        //pMatrix.pushPose();
        pMatrix.translate(-ox,-oy,z);
        pMatrix.translate(pX,pY,z);
        //float scale=0.8F;
        pMatrix.scale(scale);
        Vector4f origin = new Vector4f(0, 0, z, 1);
        pMatrix.transform(origin);

        int i = pFont.drawInBatch(pText, (float)0, (float)0, pColor, false, pMatrix, guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
        guiGraphics.pose().popPose();
        //guiGraphics.flushIfUnmanaged();
        return i;
    }

    public  static int drawString(GuiGraphics guiGraphics,Font pFont, String pText, float pX, float pY,int ox,int oy,float scale,int pColor) {
        //guiGraphics.pose().last().pose().scale(scale)
        guiGraphics.pose().pushPose();
        Matrix4f pMatrix=guiGraphics.pose().last().pose();
        //pMatrix.pushPose();
        pMatrix.translate(-ox,-oy,0);
        pMatrix.translate(pX,pY,0);
        //float scale=0.8F;
        pMatrix.scale(scale);
        Vector4f origin = new Vector4f(0, 0, 0, 1);
        pMatrix.transform(origin);

        int i = pFont.drawInBatch(pText, (float)0, (float)0, pColor, false, pMatrix, guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
        guiGraphics.pose().popPose();
        //guiGraphics.flushIfUnmanaged();
        return i;
    }
}
