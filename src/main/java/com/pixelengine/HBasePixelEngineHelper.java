package com.pixelengine;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.ColumnRangeFilter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.KeyOnlyFilter;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;

//import org.apache.spark.sql.catalyst.plans.logical.Except;
//import shapeless.ops.nat;


public class HBasePixelEngineHelper {
    public String errorMessage;
    private int MaxDatetimeRecords = 30 ;
    private int PEIgnore = -1 ;

    //返回最近一次调用的错误信息
    public String getErrorMessage() {
        return errorMessage;
    }

    public static Connection hbaseConn = null ;
    public static String zookeeper = null ;

    public static Connection getHBaseConnection() throws IOException {
        if( zookeeper==null )
        {
            zookeeper = WConfig.sharedConfig.zookeeper;
        }
        if( hbaseConn==null || hbaseConn.isClosed() || hbaseConn.isAborted() )
        {
            Configuration conf = HBaseConfiguration.create();
            conf.set("hbase.zookeeper.quorum", zookeeper );//must has this code for hbase.
            hbaseConn = ConnectionFactory.createConnection(conf);
        }
        return hbaseConn ;
    }

    //get one tile data
    public TileData getTileData( long dt, String dsName,int[] bandindices,int z,int y,int x)
    {
        System.out.println("in java getTileData zyx: "+z + "," + y+","+x ) ;
        TileData tiledata = new TileData(1) ;

        tiledata.datetimeArray[0] = dt ;
        tiledata.numds = 1;
        tiledata.x = x ;
        tiledata.y = y ;
        tiledata.z = z ;

        try {
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
            JProductInfo theinfo = rdb.rdbGetProductInfoByName(dsName) ;
            if( theinfo==null ){
                errorMessage="Error : not find info of "+dsName ;
                return null ;
            }

            tiledata.width = theinfo.tileWid;
            tiledata.height = theinfo.tileHei ;
            tiledata.nband = bandindices.length;
            tiledata.dataType = theinfo.dataType;
            int pixelLen = theinfo.getDataTypeByteLen();
            tiledata.tiledataArray[0] = new byte[
                    tiledata.width
                    *tiledata.height
                    *tiledata.nband
                    *pixelLen] ;
            int bandbytesize = tiledata.width*tiledata.height * pixelLen ;
            int lastmysqlpid = -1;
            byte[] lastCellData = null ;
            for(int iband = 0 ; iband < bandindices.length; ++ iband )
            {
                int bandindex = bandindices[iband] ;
                if( bandindex>=0 && bandindex < theinfo.bandNum )
                {
                    int mysqlpidOfBandindex = theinfo.bandPids[iband] ;
                    int newbandindex = theinfo.bandBandIndices[iband];
                    if( mysqlpidOfBandindex == lastmysqlpid && lastCellData!=null ){
                        copyBytes2Bytes(lastCellData, newbandindex , bandbytesize , tiledata.tiledataArray[0], iband );
                    }else{
                        if( mysqlpidOfBandindex == theinfo.pid )
                        {
                            lastCellData = hbaseGetCellData(
                                    theinfo.hTableName,
                                    theinfo.hFamily,
                                    dt,
                                    theinfo.hPidByteNum,
                                    theinfo.hPid,
                                    theinfo.hYXByteNum ,
                                    z,y,x) ;
                            if( lastCellData==null ){
                                errorMessage = "get emtpty cell data.";
                                return null ;
                            }
                        }else{
                            JProductInfo newinfo = rdb.rdbGetProductInfoByMysqlPid(mysqlpidOfBandindex) ;
                            if( newinfo != null )
                            {
                                lastCellData = hbaseGetCellData(
                                        newinfo.hTableName,
                                        newinfo.hFamily,
                                        dt,
                                        newinfo.hPidByteNum,
                                        newinfo.hPid,
                                        newinfo.hYXByteNum ,
                                        z,y,x) ;
                                if( lastCellData==null ){
                                    errorMessage = "get emtpty cell data.";
                                    return null ;
                                }
                            }else{
                                errorMessage = "failed to get band product info by pid of "+mysqlpidOfBandindex ;
                                return null ;
                            }
                        }

                        lastmysqlpid = mysqlpidOfBandindex ;
                        copyBytes2Bytes(lastCellData, newbandindex , bandbytesize , tiledata.tiledataArray[0], iband );
                    }
                }else{
                    //no this bandindex
                    errorMessage = "no bandindex of " + bandindex ;
                    return null ;
                }
            }
            return tiledata;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            errorMessage = "Error : getTileData exception of " + e.getMessage() ;
            return null ;
        }
    }


    //hbase get cell data
    private byte[] hbaseGetCellData( String htablename,String hfami,
                                     long datetime ,
                                     int hpidlen, int hpid ,
                                     int yxlen , int z,int y,int x )
    {
        try{
            Connection conn = getHBaseConnection();
            Table table = conn.getTable(TableName.valueOf(htablename)) ;
            byte[] rowkey1 = WHBaseUtil.GenerateRowkey( hpidlen ,hpid ,yxlen ,z,y,x) ;
            Get get = new Get(rowkey1);
            get.readVersions(1) ;
            get.addColumn(hfami.getBytes() , Bytes.toBytes(datetime) ) ;
            Result result = table.get(get);
            if(null ==result)
            {
                errorMessage = "not find the cell in step1." ;
                return null ;
            }
            if(result.listCells()==null || result.listCells().size()==0)
            {
                errorMessage = "not find the cell in step2." ;
                return null ;
            }
            Cell cell1 = result.listCells().get(0) ;

            byte[] outByteData = CellUtil.cloneValue(cell1) ;
            System.out.println("java debug listCells.get(0) cellvalue.bytesize : " + outByteData.length ) ;
//debug
//            System.out.println("debug write binary file /home/hadoop/test-tiledata.raw") ;
//            OutputStream outputStream = new FileOutputStream("/home/hadoop/test-tiledata.raw");
//            outputStream.write(outByteData);
//            outputStream.close();


            return outByteData;
        }catch (Exception ex)
        {
            errorMessage = "Error : get cell data exception :" + ex.getMessage() ;
            return null;
        }
    }

    //copy band data into new container
    private void copyBytes2Bytes(byte[] source,
                                 int sourceBandIndex,
                                 int bandbytelen,
                                 byte[] target,
                                 int targetbandindex )
    {
        System.arraycopy(source, sourceBandIndex*bandbytelen ,
                target , targetbandindex * bandbytelen ,
                bandbytelen);
    }

    //get filtered cell data
    private ArrayList<DatetimeCellData> getFilteredDatetimeCellData(
            String tableName,
            String fami,
            int pidlen,int hpid,long startdt,long stopdt,
            int filterMon,
            int filterDay,
            int filterHour,
            int filterMinu,
            int filterSec,
            int yxlen,
            int z,
            int y,
            int x) throws IOException {
        ArrayList<DatetimeCellData> dtdatalist = new ArrayList<DatetimeCellData>() ;

        byte[] rowkey1 = WHBaseUtil.GenerateRowkey(pidlen,hpid,yxlen,z,y,x) ;
        FilterList filterList = new FilterList() ;//default must pass all.
        ColumnRangeFilter rangeFilter = new ColumnRangeFilter( Bytes.toBytes(startdt),
                true ,
                Bytes.toBytes(stopdt) ,
                true ) ;
        filterList.addFilter(rangeFilter);
        KeyOnlyFilter keyfilter = new KeyOnlyFilter() ;
        filterList.addFilter(keyfilter);
        Get get = new Get(rowkey1);
        get.readVersions(1) ;
        get.addFamily(fami.getBytes()) ;
        get.setFilter(filterList) ;

        Connection conn = getHBaseConnection();
        Table table = conn.getTable( TableName.valueOf(tableName) ) ;
        Result result = table.get(get);

        if(result != null )
        {
            int findcount = 0;
            while( result.advance() )
            {
                ++findcount;
                if( dtdatalist.size()>= MaxDatetimeRecords ) break ;
                Cell cell1 = result.current() ;
                ByteBuffer bbuff = ByteBuffer.allocate(8) ;
                byte[] qualifier = CellUtil.cloneQualifier(cell1);
                bbuff.put(qualifier) ;
                long datetime1 = bbuff.getLong(0) ;
                if( isPassAllDatetimeFilter(datetime1,filterMon,filterDay,filterHour,
                        filterMinu,filterSec) )
                {
                    Get newget = new Get(rowkey1) ;
                    newget.addColumn(fami.getBytes() ,qualifier) ;
                    newget.readVersions(1);
                    Result newresult = table.get(newget) ;
                    if( newresult.value() != null )
                    {
                        DatetimeCellData dtdata = new DatetimeCellData();
                        dtdata.dt = datetime1;
                        dtdata.data = CellUtil.cloneValue(newresult.listCells().get(0)) ;
                        dtdatalist.add(dtdata) ;
                    }
                }

            }
            System.out.println("getFilteredDatetimeCellData max records:"+MaxDatetimeRecords) ;
            System.out.println("getFilteredDatetimeCellData findcount:" + findcount + " filtered:"+dtdatalist.size() ) ;
        }
        //sort
        if( dtdatalist.size() > 0)
        {
            dtdatalist.sort(new Comparator<DatetimeCellData>() {
                @Override
                public int compare(DatetimeCellData o1, DatetimeCellData o2) {
                    if( o1.dt < o2.dt ) {
                        return -1;
                    }else if( o1.dt > o2.dt ){
                        return 1;
                    }else{
                        return 0;
                    }
                }
            });
        }
        return dtdatalist;
    }

    private boolean isPassAllDatetimeFilter(long dtx, int monf, int dayf, int hourf, int minuf, int secf)
    {
        if( monf!=PEIgnore )
        {
            int monx = (int) ((dtx / 100000000L)%100) ;
            if( monx != monf )
            {
                return false ;
            }
        }
        if( dayf!=PEIgnore )
        {
            int dayx = (int) ((dtx / 1000000L)%100) ;
            if( dayx != dayf )
            {
                return false ;
            }
        }
        if( hourf!=PEIgnore )
        {
            int hx = (int) ((dtx / 10000L)%100) ;
            if( hx != hourf )
            {
                return false ;
            }
        }
        if( minuf!=PEIgnore )
        {
            int minux = (int) ((dtx / 100L)%100) ;
            if( minux != minuf )
            {
                return false ;
            }
        }
        if( secf !=PEIgnore )
        {
            int secx = (int) (dtx % 100L) ;
            if( secx != secf )
            {
                return false ;
            }
        }
        return true ;
    }



    //get tile data array
    public TileData getTileDataArray(long fromdtInclude,long todtInclude ,
                                     String dsName, int[] bandindices ,
                                     int z,int y,int x,
                                     int filterMon,int filterDay,int filterHour,
                                     int filterMinu,int filterSec
                                     )
    {
        System.out.println("in java getTileDataArray zyx: "+z + "," + y+","+x ) ;

        try {
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
            JProductInfo theinfo = rdb.rdbGetProductInfoByName(dsName) ;
            if( theinfo==null ){
                errorMessage="Error : not find info of "+dsName ;
                return null ;
            }
            TileData resultTileData = null ;
            int lastmysqlpid = -1;
            int bandbytesize = theinfo.tileWid*theinfo.tileHei*theinfo.getDataTypeByteLen();
            ArrayList<DatetimeCellData> lastCellDataList = null ;
            for(int iband = 0 ; iband < bandindices.length; ++ iband )
            {
                int bandindex = bandindices[iband] ;
                if( bandindex>=0 && bandindex < theinfo.bandNum )
                {
                    int mysqlpidOfBandindex = theinfo.bandPids[iband] ;
                    int newbandindex = theinfo.bandBandIndices[iband];
                    if( mysqlpidOfBandindex == lastmysqlpid && lastCellDataList!=null && resultTileData!=null ){
                        for(int ids=0;ids<resultTileData.numds;++ids )
                        {
                            copyBytes2Bytes(lastCellDataList.get(ids).data,
                                    newbandindex , bandbytesize , resultTileData.tiledataArray[ids], iband );
                        }
                    }else{
                        if( mysqlpidOfBandindex == theinfo.pid )//pid is mysql-pid
                        {
                            lastCellDataList = this.getFilteredDatetimeCellData(
                                    theinfo.hTableName,
                                    theinfo.hFamily,
                                    theinfo.hPidByteNum,
                                    theinfo.hPid,
                                    fromdtInclude,
                                    todtInclude,
                                    filterMon,
                                    filterDay,
                                    filterHour,
                                    filterMinu,
                                    filterSec,
                                    theinfo.hYXByteNum,
                                    z,y,x) ;
                            if( lastCellDataList==null ){
                                errorMessage = "get emtpty range of cell data.";
                                return null ;
                            }
                        }else{
                            JProductInfo newinfo = rdb.rdbGetProductInfoByMysqlPid(mysqlpidOfBandindex) ;
                            if( newinfo != null )
                            {
                                lastCellDataList = this.getFilteredDatetimeCellData(
                                        newinfo.hTableName,
                                        newinfo.hFamily,
                                        newinfo.hPidByteNum,
                                        newinfo.hPid,
                                        fromdtInclude,
                                        todtInclude,
                                        filterMon,
                                        filterDay,
                                        filterHour,
                                        filterMinu,
                                        filterSec,
                                        newinfo.hYXByteNum,
                                        z,y,x) ;
                                if( lastCellDataList==null ){
                                    errorMessage = "get emtpty range cell data.";
                                    return null ;
                                }
                            }else{
                                errorMessage = "failed to get band product info by pid of "+mysqlpidOfBandindex ;
                                return null ;
                            }
                        }

                        if( resultTileData==null ){
                            resultTileData = new TileData(lastCellDataList.size()) ;
                            resultTileData.height = theinfo.tileHei;
                            resultTileData.width = theinfo.tileWid;
                            resultTileData.nband = bandindices.length;
                            resultTileData.dataType = theinfo.dataType;
                            resultTileData.x = x;
                            resultTileData.y = y;
                            resultTileData.z = z ;
                            for(int ids=0;ids<resultTileData.numds;++ids )
                            {
                                resultTileData.datetimeArray[ids] = lastCellDataList.get(ids).dt;
                            }
                        }

                        lastmysqlpid = mysqlpidOfBandindex ;
                        for(int ids=0;ids<resultTileData.numds;++ids )
                        {
                            if(resultTileData.datetimeArray[ids] != lastCellDataList.get(ids).dt)
                            {
                                System.out.println("Warning : resultTileData.datetimeArray[ids] not equal lastCellDataList.get(ids).dt,");
                                System.out.println("    ids="+ids);
                                System.out.println("    resultTileData.datetimeArray[ids]="+resultTileData.datetimeArray[ids]);
                                System.out.println("    lastCellDataList.get(ids).dt="+lastCellDataList.get(ids).dt);
                            }
                            copyBytes2Bytes(lastCellDataList.get(ids).data,
                                    newbandindex , bandbytesize , resultTileData.tiledataArray[ids], iband );
                        }
                    }
                }else{
                    //no this bandindex
                    errorMessage = "no bandindex of " + bandindex ;
                    return null ;
                }
            }
            return resultTileData;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            errorMessage = "Error : getTileData exception of " + e.getMessage() ;
            return null ;
        }

    }

    //old api , keep for old api use.
    public ColorRamp getColorRamp( String crid ){
        System.out.println("in java getColorRamp colorid: "+crid) ;
        ColorRamp cr = new ColorRamp() ;
        cr.Nodata = -1 ;
        cr.NodataColor = new byte[4] ;
        cr.NodataColor[0] = cr.NodataColor[1]=cr.NodataColor[2]=cr.NodataColor[3]=0 ;
        cr.ivalues = new int[10] ;
        cr.r = new byte[10] ;
        cr.g = new byte[10] ;
        cr.b = new byte[10] ;
        cr.a = new byte[10] ;
        cr.labels = new String[10] ;
        for(int ic = 0 ; ic<10 ; ++ ic )
        {
            cr.ivalues[ic] = ic*25 ;
            cr.r[ic] = 0 ;
            cr.g[ic] = (byte)(ic*25) ;
            cr.b[ic] = 0 ;
            cr.a[ic] = (byte)255 ;
            cr.labels[ic] = "ll" ;
        }
        return cr ;
    }

    //JPeStyle used to substitute ColorRamp
    public JPeStyle getStyle( String styleid ){
        System.out.println("in java getStyle styleid: "+styleid) ;

        JPeStyle style = new JPeStyle() ;
        style.bands = new int[1] ;
        style.bands[0] = 0 ;

        style.nodatacolor = new JPeColorElement() ;

        style.type = "linear" ;

        style.colors = new JPeColorElement[2] ;

        style.colors[0] = new JPeColorElement() ;
        style.colors[0].r = 0 ;
        style.colors[0].g = 0;
        style.colors[0].b = 0;
        style.colors[0].a = (byte)255 ;
        style.colors[0].val = 1 ;

        style.colors[1] = new JPeColorElement() ;
        style.colors[1].r = (byte)255 ;
        style.colors[1].g = (byte)255 ;
        style.colors[1].b = (byte)255 ;
        style.colors[1].a = (byte)255 ;
        style.colors[1].val = 255 ;

        style.vranges = new JPeVRangeElement[2] ;
        style.vranges[0] = new JPeVRangeElement();
        style.vranges[0].minval = 1 ;
        style.vranges[0].maxval = 128 ;

        style.vranges[1] = new JPeVRangeElement();
        style.vranges[1].minval = 127 ;
        style.vranges[1].maxval = 255 ;
        System.out.println("in java , style ok") ;
        return style ;
    }


}

