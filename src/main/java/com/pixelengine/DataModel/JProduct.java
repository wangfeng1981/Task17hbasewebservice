package com.pixelengine.DataModel;

import com.pixelengine.JRDBHelperForWebservice;

import java.sql.SQLException;
import java.util.ArrayList;


//2021-3
public class JProduct {
    public int pid ;
    public String name,proj;
    public int minZoom,maxZoom,dataType,timeType;
    public String hTableName;
    public int tileWid,tileHei ;
    public String compress ;
    public int styleid , userid ;

    //exteranl
    public ArrayList<JProductBand> bandList = new ArrayList<>() ;
    public JHBaseTable hbaseTable = new JHBaseTable() ;
    public JProductDisplay productDisplay = new JProductDisplay() ;
    public JProductDataItem latestDataItem = new JProductDataItem() ;

    public static int getDataByteLenByDataType(int datatype){
        if( datatype==1 ){
            return 1;
        }else if( datatype==2 || datatype==3 ){
            return 2 ;
        }else if( datatype==4 || datatype==5 || datatype==6 ){
            return 4 ;
        }else if( datatype==7) {
            return 8 ;
        }else{
            return 0 ;
        }
    }
    public int getDataByteLen(){
        return JProduct.getDataByteLenByDataType(this.dataType) ;
    }

    private static ArrayList<JProduct> s_sharedlist ;
    public static ArrayList<JProduct> getSharedList() throws SQLException {
        if( s_sharedlist==null ){
            //load from db
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;

            //1.load from tbProduct
            s_sharedlist = rdb.rdbGetProducts() ;

            return s_sharedlist ;
        }else{
            return s_sharedlist ;
        }
    }
}