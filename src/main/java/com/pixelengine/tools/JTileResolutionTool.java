package com.pixelengine.Tools;

public class JTileResolutionTool {
    public static double computeResolution(int level,int tileSize){
        int ntile = (int)Math.pow(2,level) ;
        int xpixelcount = ntile*tileSize ;
        return 360.0/xpixelcount ;
    }
}
