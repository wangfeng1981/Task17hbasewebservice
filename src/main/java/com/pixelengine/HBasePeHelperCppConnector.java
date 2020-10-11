//com_pixelengine_HBasePeHelperCppConnector
package com.pixelengine;

//Java离线计算与C++接口。
//// -Djava.library.path="/home/hadoop/IdeaProjects/PixelEngine_TileCompute/pe_tilecompute/HBasePeHelperCppConnector"

public class HBasePeHelperCppConnector {
    static {
        System.loadLibrary("HBasePeHelperCppConnector") ;
    }

    public native String ParseScriptForDsDt(String javaHelperClassName, String script ) ;

    public native TileComputeResult RunScriptForTileWithoutRender(String javaHelperClassName, String script, long datetime, int z , int y , int x) ;    
    public native TileComputeResult RunScriptForTileWithRender(String javaHelperClassName,String script,String styleJson,long datetime,int z,int y,int x) ;

    public native String CheckScriptOk( String javaHelperClassName, String script ) ;

    public native String GetVersion() ;

    public native String RunToGetStyleFromScript(String javaHelperClassName,String script) ;

}



