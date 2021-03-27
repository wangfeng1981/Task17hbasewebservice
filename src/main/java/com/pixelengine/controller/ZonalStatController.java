package com.pixelengine.controller;


import com.pixelengine.DAO.ZonalStatDAO;
import com.pixelengine.DTO.RegionDTO;
import com.pixelengine.DTO.StyleDTO;
import com.pixelengine.DTO.ZonalStatDTO;
import com.pixelengine.DataModel.JProduct;
import com.pixelengine.DataModel.JZonalStatParams;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.JRDBHelperForWebservice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/offtask/zonalstat")
public class ZonalStatController {
    @Autowired
    ZonalStatDAO dao ;

    //get user region list
    @CrossOrigin
    @GetMapping("/userlist")
    @ResponseBody
    public RestResult userList(String userid) {
        List<ZonalStatDTO> rlist = dao.findAllByUserid( Long.parseLong(userid)) ;
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
    public RestResult createNew(String rtype,
                                String rid,
                                String userid,
                                String pid ,
                                String bandindex,
                                String vmin,
                                String vmax,
                                String datetime ) {

        JZonalStatParams params = new JZonalStatParams() ;
        params.bandindex = Integer.parseInt(bandindex) ;
        params.dt = Long.parseLong(datetime) ;
        params.pid = Integer.parseInt(pid) ;
        params.rid = Long.parseLong(rid) ;
        params.rtype = rtype ;
        params.vmin = Double.parseDouble(vmin) ;
        params.vmax  = Double.parseDouble(vmax) ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;

        JProduct pdt = rdb.rdbGetProductForAPI(params.pid) ;
        params.regfile = rdb.rdbGetGeoJsonFilePath(params.rtype,params.rid) ;
        params.zlevel = pdt.maxZoom ;
        params.hTableName = pdt.hTableName ;
        params.hPid = pdt.bandList.get(params.bandindex).hPid ;
        params.bsqIndex = pdt.bandList.get(params.bandindex).bsqIndex ;
        params.dataType = pdt.dataType ;

        ZonalStatDTO task =new ZonalStatDTO() ;
        task.setContent(params.toJson());
        task.setCreatetime( new Date());
        task.setUpdatetime( new Date());
        task.setMessage("");
        task.setResult("");
        task.setStatus(0);
        task.setTag("");
        task.setUid(Long.parseLong(userid));

        ZonalStatDTO newtask = dao.save(task) ;

        RestResult returnT = new RestResult();
        returnT.setState(0);
        returnT.setMessage("");
        returnT.setData(newtask);
        return returnT ;
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
