package com.pixelengine.DataModel;
//2021-3
//2022-01-01
//2022-7-8

public class JProductDataItem {
    public int fid, pid ;
    public long hcol ,dt0,dt1;
    public double left,right,top,bottom ;
    public long createTime , updateTime ;
    public String showVal,realVal ;
    public void convertShowValRealVal(int timetype)
    {
        realVal = String.valueOf(hcol) ;
        showVal = realVal ;
        int ymd = (int)(dt0/1000000L) ;
        int hms = (int)(dt0%1000000L) ;
        int year = ymd/10000 ;
        int month = (ymd%10000)/100 ;
        int day = ymd%100 ;
        int hour = hms/10000 ;
        int minu = (hms%10000)/100 ;
        int sec = hms%100 ;
        if( timetype==1 ){
            //seconds
            showVal = String.format("%04d年%02d月%02d日 %02d:%02d:%02d",year,month,day,hour,minu,sec) ;
        }else if( timetype==2 ){
            //minutes
            showVal = String.format("%04d年%02d月%02d日 %02d:%02d",year,month,day,hour,minu) ;
        }else if( timetype==3 ){
            //hours
            showVal = String.format("%04d年%02d月%02d日 %02d时",year,month,day,hour) ;
        }else if( timetype==4 ){
            //days
            showVal = String.format("%04d年%02d月%02d日",year,month,day) ;
        }else if( timetype==5 ){
            //month
            showVal = String.format("%04d年%02d月",year,month) ;
        }else if( timetype==6 ){
            //season
            if( month==3 ){
                showVal = String.format("%04d年春季",year) ;
            }else if( month==6 ){
                showVal = String.format("%04d年夏季",year) ;
            }else if( month==9 ){
                showVal = String.format("%04d年秋季",year) ;
            }else if(month==12){
                showVal = String.format("%04d年冬季",year) ;
            }
        }else if( timetype==7 ){
            //year
            showVal = String.format("%04d年",year) ;
        }else if( timetype==11 ){
            //five days
            showVal = String.format("%04d年%02d月%02d日候",year,month,day) ;
        }
        else if( timetype==12 ){
            //8days
            showVal = String.format("%04d年%02d月%02d日八天",year,month,day) ;
        }
        else if( timetype==13 ){
            //10days
            if(day==1){
                showVal = String.format("%04d年%02d月上旬",year,month) ;
            }else if( day==11 ){
                showVal = String.format("%04d年%02d月中旬",year,month) ;
            }else{
                showVal = String.format("%04d年%02d月下旬",year,month) ;
            }
        }else if( timetype==14 ){
            //16days
            showVal = String.format("%04d年%02d月%02d日十六天",year,month,day) ;
        }
    }
}
