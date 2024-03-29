package com.pixelengine.DataModel;
//2022-01-01
//import com.pixelengine.DTO.ZonalStatDTO;
import com.pixelengine.JRDBHelperForWebservice;
import org.json.JSONObject;


public class JOfftaskResult {
    public JZonalStat2 taskdata ;
    public String productname ;
    public String roiname ;

    //deprecated 2022-9-14
//    public void initWithZonalStatDTO(ZonalStatDTO zsdto)
//    {
//        taskdata = zsdto ;
//        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
//        try{
//            JSONObject jo = new JSONObject(zsdto.getContent());
//            if(taskdata.getMode()==0 ||taskdata.getMode()==1||taskdata.getMode()==2  )
//            {
//                //zs
//                int pid = jo.getInt("pid") ;
//                int rid = jo.getInt("rid") ;
//                String rtype = jo.getString("rtype") ;
//                JProductDisplay pdt = rdb.rdbGetProductDisplayInfo(pid) ;
//                productname = pdt.productname ;
//                roiname = rdb.rdbGetROIInfo( rtype,rid).name ;
//            }else if( taskdata.getMode()==4 )
//            {
//                //co
//                int pid = jo.getInt("inpid") ;
//                productname = rdb.rdbGetProductDisplayInfo(pid).productname ;
//                roiname = String.format("left:%.3f right:%.3f top:%.3f bottom:%.3f",
//                        jo.getDouble("left"),
//                        jo.getDouble("right"),
//                        jo.getDouble("top") ,
//                        jo.getDouble("bottom")) ;
//
//
//            }else if( taskdata.getMode()==5 )
//            {
//                //ex
//                int pid = jo.getInt("inpid") ;
//                productname = rdb.rdbGetProductDisplayInfo(pid).productname ;
//                roiname = String.format("left:%.3f right:%.3f top:%.3f bottom:%.3f",
//                        jo.getDouble("left"),
//                        jo.getDouble("right"),
//                        jo.getDouble("top") ,
//                        jo.getDouble("bottom")) ;
//            }
//        }catch (Exception ex)
//        {
//            System.out.println("initWithZonalStatDTO exception:" + ex.getMessage());
//        }
//    }


    public void initWithZonalStat(JZonalStat2 zs)
    {
        taskdata = zs ;
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        try{
            JSONObject jo = new JSONObject(zs.getContent());
            if(taskdata.getMode()==0 ||taskdata.getMode()==1||taskdata.getMode()==2  )
            {
                //zs
                int pid = jo.getInt("pid") ;
                int rid = jo.getInt("rid") ;
                String rtype = jo.getString("rtype") ;
                JProductDisplay pdt = rdb.rdbGetProductDisplayInfo(pid) ;
                productname = pdt.productname ;
                roiname = rdb.rdbGetROIInfo( rtype,rid).name ;
            }else if( taskdata.getMode()==4 )
            {
                //co
                int pid = jo.getInt("inpid") ;
                productname = rdb.rdbGetProductDisplayInfo(pid).productname ;
                roiname = String.format("left:%.3f right:%.3f top:%.3f bottom:%.3f",
                        jo.getDouble("left"),
                        jo.getDouble("right"),
                        jo.getDouble("top") ,
                        jo.getDouble("bottom")) ;


            }else if( taskdata.getMode()==5 )
            {
                //ex
                int pid = jo.getInt("inpid") ;
                productname = rdb.rdbGetProductDisplayInfo(pid).productname ;
                roiname = String.format("left:%.3f right:%.3f top:%.3f bottom:%.3f",
                        jo.getDouble("left"),
                        jo.getDouble("right"),
                        jo.getDouble("top") ,
                        jo.getDouble("bottom")) ;
            }
        }catch (Exception ex)
        {
            System.out.println("initWithZonalStat exception:" + ex.getMessage());
        }
    }
}
