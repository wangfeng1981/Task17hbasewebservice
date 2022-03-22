package com.pixelengine.DataModel;
//created 2022-2-13 1200 by wf
import com.google.gson.Gson;
import com.pixelengine.HBasePeHelperCppConnector;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.TileComputeResult;
import com.pixelengine.Tools.JScriptTools;
import com.pixelengine.Tools.JTileRangeTool;
import com.pixelengine.Tools.JTileResolutionTool;
import org.apache.hadoop.hbase.util.Bytes;
import org.gdal.gdal.Dataset ;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;

import java.io.File;

public class WOrderWorker {
    private byte[] filldataBufferByte = null ;//byte
    private int[] filldataBufferInt = null ;//int16 uint16 int32
    private float[] filldataBufferFloat = null ;//float
    private double[] filldataBufferDouble = null ;//uint32 double

    private void fillNodataValue( Dataset outds , int ixtileOffset, int iytileOffset, int tileSize , int nbands, int outdsDataType, double fillvalue)
    {
        int atilesize = tileSize*tileSize ;
        if( outdsDataType==1 ){
            if( filldataBufferByte == null ) {
                filldataBufferByte = new byte[tileSize*tileSize] ;
                for(int i = 0 ; i<atilesize;++i ) filldataBufferByte[i] = (byte)fillvalue ;
            }
            for(int ib = 0 ; ib<nbands;++ib ){
                outds.GetRasterBand(ib+1).WriteRaster(tileSize*ixtileOffset,tileSize*iytileOffset, tileSize,tileSize, filldataBufferByte) ;
            }
        }else if( outdsDataType==2 || outdsDataType==3 || outdsDataType==5 )
        {
            if( filldataBufferInt == null ){
                filldataBufferInt = new int[tileSize*tileSize] ;
                for(int i = 0 ; i<atilesize;++i ) filldataBufferInt[i] = (int)fillvalue ;
            }
            for(int ib = 0 ; ib<nbands;++ib ){
                outds.GetRasterBand(ib+1).WriteRaster(tileSize*ixtileOffset,tileSize*iytileOffset, tileSize,tileSize, 5, filldataBufferInt ) ;
            }
        }else if( outdsDataType==6 )
        {
            if( filldataBufferFloat==null ){
                filldataBufferFloat = new float[tileSize*tileSize] ;
                for(int i = 0 ; i<atilesize;++i ) filldataBufferFloat[i] = (float)fillvalue ;
            }
            for(int ib = 0 ; ib<nbands;++ib ){
                outds.GetRasterBand(ib+1).WriteRaster(tileSize*ixtileOffset,tileSize*iytileOffset, tileSize,tileSize, 6, filldataBufferFloat ) ;
            }
        }else if( outdsDataType==4 || outdsDataType==7 )
        {
            if( filldataBufferDouble==null ){
                filldataBufferDouble = new double[tileSize*tileSize] ;
                for(int i = 0 ; i<atilesize;++i ) filldataBufferDouble[i] = fillvalue ;
            }
            for(int ib = 0 ; ib<nbands;++ib ){
                outds.GetRasterBand(ib+1).WriteRaster(tileSize*ixtileOffset,tileSize*iytileOffset, tileSize,tileSize, 7, filldataBufferDouble ) ;
            }
        }
    }

    private void fillTileResultData( Dataset outds , int ixtileOffset, int iytileOffset, int tileSize , int nbands, int outdsDataType, TileComputeResult tileRes)
    {
        int pixelByteLen = 1 ;
        if( outdsDataType<2 ) pixelByteLen=1 ;
        else if( outdsDataType<4 ) pixelByteLen = 2 ;
        else if( outdsDataType<7 ) pixelByteLen = 4 ;
        else if( outdsDataType==7 ) pixelByteLen = 8 ;
        for(int ib = 0 ; ib<nbands;++ib ){
            int bandByteOffset = ib * tileSize * tileSize * pixelByteLen ;
            byte[] buffer = Bytes.copy(tileRes.binaryData , bandByteOffset , tileSize * tileSize*pixelByteLen ) ;
            outds.GetRasterBand(ib+1).WriteRaster(tileSize*ixtileOffset,tileSize*iytileOffset, tileSize,tileSize, outdsDataType , buffer) ;
        }
    }


    public WResult processOneOrder(WConfig config, JExportOrder order ){
        WResult r = new WResult() ;
        r.state = 9 ;

        try{
            ///////////////////////////////////////////////////////////////////
            //
            //通过geojson计算四角范围
            System.out.println("compute geojson extent ");
            String geojsonFile = config.pedir + order.geojsonRelFilepath ;
            JGeojsonUtils.WExtent extent = JGeojsonUtils.extent(geojsonFile );
            if( extent==null ){
                throw new Exception("failed to compute geojson extent.") ;
            }
            System.out.println("geojson extent l,r t,b:"+extent.left+","+extent.right+" "+extent.top+","+extent.bottom);

            ///////////////////////////////////////////////////////////////////
            //
            //init pe
            HBasePeHelperCppConnector cc = new HBasePeHelperCppConnector();


            ///////////////////////////////////////////////////////////////////
            //
            //read script text
            String scriptFilePath = config.pedir + order.scriptRelFilepath ;
            System.out.println("read script text "+scriptFilePath) ;
            String scriptText = WTextFile.readFileAsString(scriptFilePath) ;
            if(scriptText==null) {
                throw new Exception("read scriptText failed");
            }

            ///////////////////////////////////////////////////////////////////
            //
            //计算数据集zlevel
            System.out.println("get dsname array for compute zlevel");
            String dsNameArrJsonText = cc.GetDatasetNameArray("com/pixelengine/HBasePixelEngineHelper",
                    scriptText) ;
            if( dsNameArrJsonText==null ){
                throw new Exception("HBasePeHelperCppConnector.GetDatasetNameArray failed") ;
            }
            Gson gson = new Gson();
            JDsNameArrayResult dsnamesResult = gson.fromJson(dsNameArrJsonText , JDsNameArrayResult.class) ;
            if( dsnamesResult.status != 0 ){
                throw new Exception("bad JDsNameArrayResult, " + dsnamesResult.error) ;
            }
            if( dsnamesResult.data.length == 0 ){
                throw new Exception("None valid dsname in script");
            }
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
            int maxZoom = -1 ;
            //int maxDataByteLen = 0 ;
            for(int ids = 0 ; ids < dsnamesResult.data.length ; ++ ids ){
                JProduct pdt =rdb.rdbGetProductInfoByName( dsnamesResult.data[ids] ) ;
                if( pdt==null ){
                    throw new Exception("Undefined Product of "+dsnamesResult.data[ids] );
                }
                maxZoom = Math.max( maxZoom , pdt.maxZoom) ;
                //maxDataByteLen = Math.max(maxDataByteLen,pdt.getDataByteLen()) ;
            }
            if( maxZoom<0 ){
                throw new Exception("Invalid maxZoom "+maxZoom);
            }
            System.out.println("maxZoom:"+maxZoom) ;
            //set maxZoom 15 , about 4.29m , xtilenum:32768
            if( maxZoom>15 ){
                maxZoom = 15 ;
                System.out.println("adjust maxZoom to :"+maxZoom) ;
            }

            ///////////////////////////////////////////////////////////////////
            //
            //计算瓦片x，y范围
            final int tileSize = 256 ;
            final double maxPixelByteSizeInMB = 100 ;
            System.out.println("compute tile range");
            JTileRangeTool.TileXYRange tileRange = JTileRangeTool.computeTileRangeByLonglatExtent(
                    extent.left,
                    extent.right,
                    extent.top,
                    extent.bottom,
                    maxZoom,
                    tileSize) ;
            if( tileRange==null ){
                throw new Exception("failed to compute tileRange");
            }
            System.out.println("tile range xmin,xmax ymin,ymax:"
                + tileRange.xmin + "," + tileRange.xmax + " "
                    + tileRange.ymin+","+tileRange.ymax
            );


            //计算0号瓦片，获得结果数据类型与波段数目,如果计算0号瓦片失败，也是报异常不再进行后面计算了
            System.out.println("compute zero tile (0,0,0)");
            String extraText = "{\"datetime\":"+order.datetime+"}" ;
            String scriptWithSDUI = JScriptTools.assembleScriptWithSDUI( scriptText , order.sdui ) ;
            TileComputeResult zeroTileResult = cc.RunScriptForTileWithoutRenderWithExtra(
                    "com/pixelengine/HBasePixelEngineHelper",
                    scriptWithSDUI,
                    extraText,
                    0,0,0
            ) ;
            if(zeroTileResult==null ){
                throw new Exception("zeroTileResult is null") ;
            }
            if( zeroTileResult.status!=0 ){
                throw new Exception("compute zeroTileResult failed") ;
            }
            System.out.println("zeroTileResult ok, dataType:"+zeroTileResult.outType+", nband:"+zeroTileResult.nbands);


            ///////////////////////////////////////////////////////////////////
            //
            //根据最大限量从新计算x，y范围，从config.json中读取
            int tileXNum = tileRange.xmax - tileRange.xmin + 1 ;
            int tileYNum = tileRange.ymax - tileRange.ymin + 1 ;
            double oneTileByteLenInKB = tileSize*tileSize*zeroTileResult.getDataByteLen()*zeroTileResult.nbands / 1024.0 ;
            double totalByteLenInMB = (oneTileByteLenInKB * tileXNum / 1024.0) * tileYNum ;
            if( totalByteLenInMB > maxPixelByteSizeInMB ){
                throw new Exception("request data size exceed max data size "+maxPixelByteSizeInMB + " MB");
            }
            System.out.println("unziped data size:"+totalByteLenInMB+" MB");


            ///////////////////////////////////////////////////////////////////
            //
            //计算分辨率
            double theReso = JTileResolutionTool.computeResolution(maxZoom , tileSize);
            System.out.println("the resolution:"+theReso);


            ///////////////////////////////////////////////////////////////////
            //
            //在存储中打开结果文件
            String outTiffFilepath = (config.pedir + order.resultRelFilepath).replace(".json",".tif")  ;
            String outTiffRelFilepath = order.resultRelFilepath.replace(".json",".tif") ;
            String tempOutTifFilepath = outTiffFilepath.replace(".tif" , "_tiled.tif") ;//未裁剪数据
            System.out.println("create output tif file " + outTiffFilepath );
            int outxsize = (tileRange.xmax-tileRange.xmin +1)*tileSize ;
            int outysize = (tileRange.ymax-tileRange.ymin +1)*tileSize ;
            if( outxsize<=0 || outysize<=0 ){
                throw new Exception("bad x,y size: "+outxsize+","+outysize);
            }
            Driver driver = gdal.GetDriverByName("GTIFF") ;
            if( driver==null ){
                throw new Exception("gdal has no GTIFF driver available");
            }
            String[] options = new String[1] ;
            options[0] = "COMPRESS=Deflate" ;
            Dataset outputds = driver.Create(tempOutTifFilepath, outxsize,outysize,zeroTileResult.nbands,zeroTileResult.dataType,options ) ;
            if( outputds==null ){
                throw new Exception("gdal create output dataset failed");
            }
            double[] geoTrans = new double[6] ;
            geoTrans[0] = tileRange.left ;
            geoTrans[1] =  theReso;
            geoTrans[2] = 0 ;
            geoTrans[3] = tileRange.top ;
            geoTrans[4] = 0 ;
            geoTrans[5] = -theReso ;
            outputds.SetGeoTransform(geoTrans) ;
            outputds.SetProjection("GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]");

            //设置无效值
            for(int ib = 0 ; ib<zeroTileResult.nbands;++ib ){
                outputds.GetRasterBand(ib+1).SetNoDataValue( order.fillvalue) ;
            }


            ///////////////////////////////////////////////////////
            //
            //开始瓦片循环，调用v8计算，每个瓦片计算结果写入geotiff
            int numGoodTile = 0 ;
            int numBadTile = 0 ;
            int tileCount = (tileRange.xmax-tileRange.xmin +1)* (tileRange.ymax-tileRange.ymin +1) ;
            int tileCounter = 0 ;
            int per0 = -1 ;
            for(int iytile = tileRange.ymin ; iytile <= tileRange.ymax; ++ iytile)
            {
                for(int ixtile = tileRange.xmin ; ixtile <= tileRange.xmax; ++ ixtile )
                {
                    ++ tileCounter ;
                    TileComputeResult tileRes = cc.RunScriptForTileWithoutRenderWithExtra(
                            "com/pixelengine/HBasePixelEngineHelper",
                            scriptWithSDUI,
                            extraText,
                            maxZoom,iytile,ixtile
                    ) ;
                    if(tileRes==null || tileRes.status!=0 ){
                        //填充无效值
                        fillNodataValue(outputds , ixtile - tileRange.xmin ,
                                iytile - tileRange.ymin ,
                                tileSize ,
                                zeroTileResult.nbands ,
                                zeroTileResult.dataType ,
                                order.fillvalue
                        );
                        ++numBadTile ;
                    }else{
                        //有效计算
                        fillTileResultData(outputds , ixtile-tileRange.xmin ,
                                iytile-tileRange.ymin ,
                                tileSize , zeroTileResult.nbands ,
                                zeroTileResult.dataType ,
                                tileRes );
                        ++numGoodTile ;
                    }
                    int per1 = tileCounter*100 / tileCount ;
                    if( per1!=per0){
                        per0 = per1 ;
                        System.out.print( per0 + "% ");
                    }
                }
            }
            System.out.println( " All tile compute done. good:"+numGoodTile+", bad:"+numBadTile);

            ///////////////////////////////////////////////////////
            //
            //关闭文件
            System.out.println("close outputds.");
            outputds.FlushCache();
            outputds.delete();

            ///////////////////////////////////////////////////////
            //
            //geojson裁剪
            System.out.println("start geojson cliping");
            String clipCmd = config.gdalwarp + " -co \"COMPRESS=DEFLATE\" "
                    + " -srcnodata " + order.fillvalue
                    + " -dstnodata " + order.fillvalue
                    + " -cutline " + config.pedir + order.geojsonRelFilepath
                    + " -crop_to_cutline "
                    + tempOutTifFilepath
                    + " "
                    + outTiffFilepath ;
            System.out.println("clip command:" + clipCmd);
            Runtime.getRuntime().exec(clipCmd).waitFor() ;

            ///////////////////////////////////////////////////////
            //
            //检查裁剪结果是否存在
            System.out.println("check clip result tif exists");
            File clipResultFile = new File( outTiffFilepath ) ;
            if( clipResultFile.exists()==false ){
                throw new Exception("clip result tif is not available") ;
            }else{
                System.out.println("clip result file exists.");
            }

            //结束返回
            System.out.println("order process done.");
            r.data = outTiffRelFilepath ;//保存tif相对路径
            r.state = 0 ;//everything ok.
            return r;
        }catch (Exception ex )
        {
            r.state = 99 ;//everything ok.
            r.message = "WOrderWorker.processOneOrder exception:"+ex.getMessage() ;
            return r;
        }
    }
}
