package com.pixelengine.controller;
//deprecated 2022-9-14. use OffTaskController.
import com.google.gson.Gson;
import com.pixelengine.DataModel.JZonalStat2;
import com.pixelengine.DataModel.JCompositeParams;
import com.pixelengine.DataModel.JProduct;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.DataModel.WConfig;
import com.pixelengine.tools.FileDirTool;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/offtask/composite")
public class CompositeController {


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
        RestResult returnT = new RestResult();
        returnT.setState(1);
        returnT.setMessage("deprecated");
        return returnT ;
    }

    //task detail
    @CrossOrigin(origins = "*")
    @GetMapping("/detail")
    @ResponseBody
    public RestResult getDetail(String tid) {
        RestResult returnT = new RestResult();
        returnT.setState(1);
        returnT.setMessage("deprecated");
        return returnT ;
    }



    //添加到图层接口
    @CrossOrigin(origins = "*")
    @GetMapping("/layerinfo")
    @ResponseBody
    public RestResult LayerInfo(String tid) {
        RestResult returnT = new RestResult();
        returnT.setState(1);
        returnT.setMessage("deprecated");
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
        RestResult returnT = new RestResult();
        returnT.setState(1);
        returnT.setMessage("deprecated");
        return returnT ;
    }


    //task new
    @CrossOrigin(origins = "*")
    @PostMapping("/edittag")
    @ResponseBody
    public RestResult editTag(String tid,
                              String tag ) {
        RestResult returnT = new RestResult();
        returnT.setState(1);
        returnT.setMessage("deprecated");
        return returnT ;
    }

    //task new
    @CrossOrigin(origins = "*")
    @PostMapping("/remove")
    @ResponseBody
    public RestResult editTag(String tid ) {
        RestResult returnT = new RestResult();
        returnT.setState(1);
        returnT.setMessage("deprecated");
        return returnT ;
    }


}
