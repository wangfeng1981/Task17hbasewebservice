package com.pixelengine.DataModel;

public class JOfftaskWorkerResult {
    public int ofid ;//offtask 主键
    public int state;//0-good, other failed.
    public String resultRelFilepath;//should be a json file , should be relative path under nginx-pedir
}
