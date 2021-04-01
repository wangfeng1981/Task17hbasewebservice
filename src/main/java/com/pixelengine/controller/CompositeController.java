package com.pixelengine.controller;

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
            "{if(vals[i]>={{{vmin}}}&&vals[i]<={{{vmax}}}){if(cnt==0){res=vals[i];++cnt}" +
            "else if(vals[i]>res)res=vals[i]}}return res} function minfunc(vals,index,numds)" +
            "{var res={{{filldata}}};var cnt=0;for(var i=0;i<numds;++i)" +
            "{if(vals[i]>={{{vmin}}}&&vals[i]<={{{vmax}}}){if(cnt==0){res=vals[i];++cnt}" +
            "else if(vals[i]<res)res=vals[i]}}return res} function avefunc(vals,index,numds)" +
            "{var res={{{filldata}}};var cnt=0;var sum=0.0;for(var i=0;i<numds;++i)" +
            "{if(vals[i]>={{{vmin}}}&&vals[i]<={{{vmax}}}){sum+=vals[i];++cnt}}if(cnt>0)" +
            "{res=sum/cnt}return res}function main(){var dsarr=pe.DatasetArray('{{{name}}}'," +
            "{{{fromdt}}},{{{todt}}},[{{{iband}}}],-1,-1,0,-1,-1);" +
            "var resds=dsarr.forEachPixel({{{methodfunc}}});return resds;}" ;

    //get user region list
    @CrossOrigin
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
    @CrossOrigin
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

    //task new
    @CrossOrigin
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
                                String method  //合成方法min，max，ave，日后在增加一个累加模式 2021-4-1
    ) {
        RestResult result = new RestResult() ;


        JCompositeParams params = new JCompositeParams() ;
        params.bandindex = Integer.parseInt(bandindex) ;
        params.fromdt = Long.parseLong(fromdt) ;
        params.todt = Long.parseLong(todt) ;
        params.pid = Integer.parseInt(pid) ;
        params.vmin = Double.parseDouble(vmin) ;
        params.vmax  = Double.parseDouble(vmax) ;
        params.filldata = Double.parseDouble(filldata) ;
        params.method = method ;

        //
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        JProduct product = rdb.rdbGetProductForAPI(params.pid) ;
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
        String scriptfilename = FileDirTool.makeDatetimeFilename( WConfig.sharedConfig.tempdir,"co",".js") ;
        boolean writeOk = FileDirTool.writeToFile(scriptfilename , scriptcontent) ;
       if( writeOk==false ){
           result.setState(1);
           result.setMessage("failed to write script file:"+scriptfilename);
           return result ;
       }
       String userFileName = "/" + FilenameUtils.getBaseName(scriptfilename) ;

        //其他参数
        params.scriptfilename = scriptfilename ;
        params.outhtable = WConfig.sharedConfig.userhtable ;
        params.outhfami = WConfig.sharedConfig.userhfami ;
        params.outhpid = rdb.rdbNewEmptyProduct( userFileName , Integer.parseInt(userid)) ;
        params.outhpidblen = WConfig.sharedConfig.userhpidblen ;
        params.outyxblen = WConfig.sharedConfig.useryxblen ;
        params.outhcol = 1 ;
        params.userbound = 0 ;
        params.left = 0 ;
        params.right = 0 ;
        params.top = 0 ;
        params.bottom = 0 ;
        params.zmin = product.minZoom ;
        params.zmax = product.maxZoom ;


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
    @CrossOrigin
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
    @CrossOrigin
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
