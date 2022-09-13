package com.pixelengine.controller;

import com.google.gson.Gson;
import com.pixelengine.DataModel.JOfftaskWorkerResult;
import com.pixelengine.DataModel.WConfig;
import com.pixelengine.JRDBHelperForWebservice;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class OfftaskCollector extends Thread {
    public void run() {
        System.out.println("OfftaskCollector start running ...");
        try(ZContext context = new ZContext()){
            ZMQ.Socket collectorSocket = context.createSocket(SocketType.PULL) ;
            String socketStr = WConfig.getSharedInstance().offtask_result_collector ;
            collectorSocket.bind(socketStr) ;
            while(!Thread.currentThread().isInterrupted()){
                byte[] data = collectorSocket.recv(0) ;
                String jsonData = new String(data, ZMQ.CHARSET) ;
                Gson gson = new Gson() ;
                JOfftaskWorkerResult workerResult = gson.fromJson(jsonData, JOfftaskWorkerResult.class) ;
                //更新数据库
                System.out.println("collector receive worker result of ofid:"+workerResult.ofid);
                System.out.println("update db");
                JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
                if( workerResult.status==1 ){
                    //runninng
                    rdb.updateOfftaskState(workerResult.ofid,workerResult.status) ;
                }else{
                    rdb.updateOfftaskByWorkerResult(workerResult) ;
                }

            }

        }catch (Exception ex ){
            System.out.println("OfftaskCollector exception:"+ex.getMessage() );
        }
    }
}
