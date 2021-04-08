package com.pixelengine;

import com.google.gson.Gson;

public class JPeStyle {
    public int[] bands ;
    public String type; //discrete, linear, exact , gray , rgb , rgba
    JPeColorElement[] colors;
    JPeColorElement   nodatacolor;
    JPeVRangeElement[] vranges ;
}
