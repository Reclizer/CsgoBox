package com.reclizer.csgobox.utils;

public class ColorTools {
    public static  int argbColor(int a,int r,int g,int b){
        int color = (a << 24) | (r << 16) | (b << 8) | g;;
        int a0 = color >> 24;
        int r0 = (color >> 16) & 0xFF;
        int g0 = (color >> 8) & 0xFF;
        int b0 = color & 0xFF;
        return a0 << 24 | r0 << 16 | b0 << 8 | g0;
    }

    public static  int deepColor(int color){

        // 获取每个颜色分量
        int alpha = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        // 减少每个颜色分量的值，使颜色变暗
        red = (int)(red * 0.7);   // 可根据需要调整减小的比例
        green = (int)(green * 0.7);
        blue = (int)(blue * 0.7);

        // 更新颜色值
        color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        return color;
    }



    public static int colorItems(int grade){

        int color=0;
        switch (grade){
            case 1:color=0xff4c70ff;//blue
                break;
            case 2:color=0xff8d5eff;//purple
                break;
            case 3:color=0xffe54af2;//pink
                break;
            case 4:color=0xfff86351;//red
                break;
            case 5:color=0xffffdc1d;//gold
                break;
        }

        return color;
    }
}
