package com.pixelengine.controller;

import com.google.gson.Gson;
import com.pixelengine.DAO.ZonalStatDAO;
import com.pixelengine.DTO.ZonalStatDTO;
import com.pixelengine.DataModel.JCompositeParams;
import com.pixelengine.DataModel.JProduct;
import com.pixelengine.DataModel.JZonalStatParams;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.WConfig;
import com.pixelengine.tools.FileDirTool;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/offtask/composite")
public class CompositeController {
    @Autowired
    ZonalStatDAO dao ;

    private final String scriptTemplate="function maxfunc(vals,index,numds)" +
            "{var res={{{filldata}}};var cnt=0;for(var i=0;i<numds;++i)" +
            "{if(vals[i]>={{{vmin}}}&&vals[i]<={{{vmax}}}){if(cnt==0){res=vals[i];++cnt;}" +
            "else if(vals[i]>res)res=vals[i];}}return res;} function minfunc(vals,index,numds)" +
            "{var res={{{filldata}}};var cnt=0;for(var i=0;i<numds;++i)" +
            "{if(vals[i]>={{{vmin}}}&&vals[i]<={{{vmax}}}){if(cnt==0){res=vals[i];++cnt;}" +
            "else if(vals[i]<res)res=vals[i];}}return res;} function avefunc(vals,index,numds)" +
            "{var res={{{filldata}}};var cnt=0;var sum=0.0;for(var i=0;i<numds;++i)" +
            "{if(vals[i]>={{{vmin}}}&&vals[i]<={{{vmax}}}){sum+=vals[i];++cnt;}}if(cnt>0)" +
            "{res=sum/cnt;}return res;}function main(){var dsarr=pe.DatasetArray('{{{name}}}'," +
            "{{{fromdt}}},{{{todt}}},[{{{iband}}}],-1,-1,0,-1,-1);" +
            "var resds=dsarr.forEachPixel({{{methodfunc}}});return resds;}" ;

    //get user region list
    @CrossOrigin(origins = "*")
    @GetMapping("/userlist")
    @ResponseBody
    public RestResult userList(String userid) {
        int imode = 4 ;//co - composite
        List<ZonalStatDTO> rlist = dao.findAllByUserid( Long.parseLong(userid),imode) ;
        RestResult returnT = new RestResult();
        returnT.setState(0);
        returnT.setMessage("");
        returnT.setData(rlist);
        return returnT ;
    }

    //task detail
    @CrossOrigin(origins = "*")
    @GetMapping("/detail")
    @ResponseBody
    public RestResult getDetail(String tid) {
        Optional<ZonalStatDTO> task = dao.findById( Long.parseLong(tid)) ;
        RestResult returnT = new RestResult();
        returnT.setState(0);
        returnT.setMessage("");
        returnT.setData(task);
        return returnT ;
    }



    //添加到图层接口
    @CrossOrigin(origins = "*")
    @GetMapping("/layerinfo")
    @ResponseBody
    public RestResult LayerInfo(String tid) {
        RestResult returnT = new RestResult();
        Optional<ZonalStatDTO> task = dao.findById( Long.parseLong(tid)) ;
        if( task != null )
        {
            String jsontext = task.get().getContent() ;
            Gson gson = new Gson() ;
            JCompositeParams coparams = gson.fromJson(jsontext , JCompositeParams.class);
            if( coparams.outpid>0 )
            {
                JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
                JProduct pdt = rdb.rdbGetOneProductLayerInfoById(coparams.outpid ) ;
                if( pdt==null )
                {
                    returnT.setState(3);
                    returnT.setMessage("product layer info is null.");
                }else
                {
                    pdt.caps = new String[]{"zs","ex","st","dt" } ;
                    returnT.setState(0);
                    returnT.setMessage("");
                    returnT.setData(pdt);
                }

            }else
            {
                returnT.setState(2);
                returnT.setMessage("Invalid pid of " + coparams.outpid + ", the task maybe failed.");
            }
        }else
        {
            returnT.setState(1);
            returnT.setMessage("No task data for tid:"+tid);
        }
        return returnT ;
    }

    //task new 新建一个用户合成产品
    @CrossOrigin(origins = "*")
    @PostMapping("/new")
    @ResponseBody
    public RestResult createNew(
                                String userid,
                                String pid ,
                                String bandindex,
                                String vmin,
                                String vmax,
                                String filldata,
                                String fromdt ,
                                String todt,
                                String method , //合成方法min，max，ave，日后在增加一个累加模式 2021-4-1
                                String usebound , //0-全部范围，1-使用矩形范围
                                String left,    //-180~+180
                                String right,   //-180~+180
                                String top,     //-90~+90
                                String bottom   //-90~+90
    ) {
        RestResult result = new RestResult() ;


        JCompositeParams params = new JCompositeParams() ;
        params.bandindex = Integer.parseInt(bandindex) ;
        params.fromdt = Long.parseLong(fromdt) ;
        params.todt = Long.parseLong(todt) ;
        params.inpid = Integer.parseInt(pid) ;//2021-4-6
        params.vmin = Double.parseDouble(vmin) ;
        params.vmax  = Double.parseDouble(vmax) ;
        params.filldata = Double.parseDouble(filldata) ;
        params.method = method ;

        //
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        JProduct product = rdb.rdbGetProductForAPI(params.inpid ) ;//
        String methodfunc = "min" ;
        if( method.equals("min") ){
            methodfunc = "minfunc" ;
        }else if( method.equals("max") ){
            methodfunc = "maxfunc" ;
        }else{
            methodfunc = "avefunc" ;
        }

        //生产其他参数
        String scriptcontent = scriptTemplate.replace("{{{name}}}" ,product.name )
                .replace("{{{fromdt}}}",  String.valueOf(params.fromdt) )
                .replace("{{{todt}}}",  String.valueOf(params.todt) )
                .replace("{{{iband}}}",  String.valueOf(params.bandindex) )
                .replace("{{{vmin}}}",  String.valueOf(params.vmin) )
                .replace("{{{vmax}}}",  String.valueOf(params.vmax) )
                .replace("{{{filldata}}}",  String.valueOf(params.filldata) )
                .replace("{{{methodfunc}}}",  methodfunc) ;
        String tempDir = WConfig.sharedConfig.pedir + "/temp/" ;
        String scriptfilename = FileDirTool.makeDatetimeFilename( tempDir,"co",".js") ;
        boolean writeOk = FileDirTool.writeToFile(scriptfilename , scriptcontent) ;
       if( writeOk==false ){
           result.setState(1);
           result.setMessage("failed to write script file:"+scriptfilename);
           return result ;
       }
       String userFileName = "/" + FilenameUtils.getBaseName(scriptfilename) ;

        int iusebound = Integer.parseInt(usebound) ;//是否使用矩形感兴趣区 0-no use , 1-use
        double dleft = Double.parseDouble(left);
        double dright = Double.parseDouble(right);
        double dtop = Double.parseDouble(top);
        double dbottom = Double.parseDouble(bottom);

        if( iusebound==1 )
        {
            if( dleft < -180 || dleft > 180 ){
                result.setState(1);
                result.setMessage("left值无效，有效范围-180~180");
                return result ;
            }
            if( dright < -180 || dright > 180 ){
                result.setState(1);
                result.setMessage("right值无效，有效范围-180~180");
                return result ;
            }
            if( dtop < -90 || dtop > 90 ){
                result.setState(1);
                result.setMessage("top值无效，有效范围-90~90");
                return result ;
            }
            if( dbottom<-90 || dbottom > 90 )
            {
                result.setState(1);
                result.setMessage("bottom值无效，有效范围-90~90");
                return result ;
            }

            if( dleft >= dright )
            {
                result.setState(1);
                result.setMessage("left不能大于等于right");
                return result ;
            }

            if( dtop <= dbottom )
            {
                result.setState(1);
                result.setMessage("bottom不能大于等于top");
                return result ;
            }
        }



        //其他参数
        params.scriptfilename = scriptfilename ;
        params.outhtable = WConfig.sharedConfig.userhtable ;
        params.outhfami = WConfig.sharedConfig.userhfami ;
        params.outpid = rdb.rdbNewEmptyUserProduct( userFileName , Integer.parseInt(userid)) ;
        params.outhpid = params.outpid ;//目前让输出产品的hpid与输出的tbproduct.pid一致。2021-4-6
        params.outhpidblen = WConfig.sharedConfig.userhpidblen ;
        params.outyxblen = WConfig.sharedConfig.useryxblen ;
        params.outhcol = 1 ;
        params.usebound = Integer.parseInt(usebound);
        params.left = Double.parseDouble(left);
        params.right = Double.parseDouble(right);
        params.top = Double.parseDouble(top);
        params.bottom = Double.parseDouble(bottom);
        params.zmin = product.minZoom ;
        params.zmax = product.maxZoom ;

        String[] outdirArr = FileDirTool.checkAndMakeCurrentYearDateDir(WConfig.sharedConfig.pedir,"offtask");
        params.outfilename = outdirArr[0] + "co-u" + userid + "-" + FileDirTool.dateTimeString() + ".json" ;
        params.outfilenamedb = outdirArr[1] + "co-u" + userid + "-" + FileDirTool.dateTimeString() + ".json" ;


        ZonalStatDTO task =new ZonalStatDTO() ;
        task.setContent(params.toJson());
        task.setCreatetime( new Date());
        task.setUpdatetime( new Date());
        task.setMessage("");
        task.setResult("");
        task.setStatus(0);
        task.setTag("");
        task.setUid(Long.parseLong(userid));

        task.setMode(4);//0-zs , 1-sk , 2-ls , 4-co

        ZonalStatDTO newtask = dao.save(task) ;

        result.setState(0);
        result.setMessage("");
        result.setData(newtask);
        return result ;
    }


    //task new
    @CrossOrigin(origins = "*")
    @PostMapping("/edittag")
    @ResponseBody
    public RestResult editTag(String tid,
                              String tag ) {
        Optional<ZonalStatDTO> oldtask = dao.findById( Long.parseLong(tid)) ;
        oldtask.get().setTag(tag);
        oldtask.get().setUpdatetime(new Date());
        ZonalStatDTO newtask = dao.save(oldtask.get()) ;
        RestResult returnT = new RestResult();
        returnT.setState(0);
        returnT.setMessage("");
        returnT.setData(newtask);
        return returnT ;
    }

    //task new
    @CrossOrigin(origins = "*")
    @PostMapping("/remove")
    @ResponseBody
    public RestResult editTag(String tid ) {
        Optional<ZonalStatDTO> oldtask = dao.findById( Long.parseLong(tid)) ;
        oldtask.get().setStatus( ZonalStatDTO.STATUS_DELETE );// wait to delete (4)
        oldtask.get().setUpdatetime(new Date());
        ZonalStatDTO newtask = dao.save(oldtask.get()) ;
        RestResult returnT = new RestResult();
        returnT.setState(0);
        returnT.setMessage("");
        returnT.setData(newtask);
        return returnT ;
    }


}
