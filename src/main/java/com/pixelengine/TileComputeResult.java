package com.pixelengine;

public class TileComputeResult {
    public int status ; // 0 is ok
    public String log ; // error or log messages
    public int outType; //0-dataset , 1-png
    public int dataType; // 1-byte, 2-u16 , 3-i16 , 4-u32 , 5-i32 , 6-f32, 7-f64
    public int width,height,nbands;
    public byte[] binaryData ;
    public int z,y,x;
}

