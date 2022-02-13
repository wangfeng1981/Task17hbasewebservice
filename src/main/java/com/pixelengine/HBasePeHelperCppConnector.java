//com_pixelengine_HBasePeHelperCppConnector
package com.pixelengine;
////////////////////////////////////////////////////////
//
//
//这个接口是java主动调用C++ so的
//update 2022-2-13 1020
//
/////////////////////////////////////////////////////////

//Java离线计算与C++接口。
//// -Djava.library.path="/home/hadoop/IdeaProjects/PixelEngine_TileCompute/pe_tilecompute/HBasePeHelperCppConnector"

public class HBasePeHelperCppConnector {
    static {
        System.loadLibrary("HBasePeHelperCppConnector") ;
    }

    public native String ParseScriptForDsDt(String javaHelperClassName, String script ) ;

    //2022-2-12 Java_com_pixelengine_HBasePeHelperCppConnector_GetDatasetNameArray
    public native String GetDatasetNameArray(String javaHelperClassName, String script ) ;


    public native TileComputeResult RunScriptForTileWithoutRender(String javaHelperClassName, String script, long datetime, int z , int y , int x) ;
    public native TileComputeResult RunScriptForTileWithRender(String javaHelperClassName,String script,String styleJson,long datetime,int z,int y,int x) ;

    public native TileComputeResult RunScriptForTileWithoutRenderWithExtra(String javaHelperClassName, String script, String extraJsonText , int z , int y , int x) ;
    public native TileComputeResult RunScriptForTileWithRenderWithExtra(String javaHelperClassName,String script,String styleJson,String extraJsonText ,int z,int y,int x) ;

    /// 通过检查返回空字符串 “”
    /// if not pass check , return the error message string
    public native String CheckScriptOk( String javaHelperClassName, String script ) ;

    public native String GetVersion() ;

    /// if get style , return the style json string
    /// else return the "" empty string
    public native String RunToGetStyleFromScript(String javaHelperClassName,String script) ;

}



