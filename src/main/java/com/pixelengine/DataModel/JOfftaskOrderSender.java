package com.pixelengine.DataModel;
//2022-01-01
//2022-4-5  add 0mq sender for cppspark service.
//2022-9-9 setsendtimeout

import com.google.gson.Gson;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class JOfftaskOrderSender {

    private static JOfftaskOrderSender sharedInstance = null ;
    public static JOfftaskOrderSender getSharedInstance(){
        if( sharedInstance==null ){
            sharedInstance = new JOfftaskOrderSender() ;
        }
        return sharedInstance ;
    }

    private ZContext context = null ;//for export
    private ZMQ.Socket senderSocket=null ;//for export

    private ZContext contextCppSpark = null ;//for cpp spark
    private ZMQ.Socket senderSocketCppSpark = null ;//for cpp spark

    public JOfftaskOrderSender(){
        System.out.println("JOfftaskOrderSender start up");
        try{
            context = new ZContext() ;
            senderSocket = context.createSocket(SocketType.PUSH) ;
            String socketStr = WConfig.getSharedInstance().offtask_export_producer ;
            senderSocket.bind(socketStr) ;
            senderSocket.setSendTimeOut(2000);
            contextCppSpark = new ZContext( );
            senderSocketCppSpark = contextCppSpark.createSocket(SocketType.PUSH) ;
            String socketStr2 = WConfig.getSharedInstance().offtask_cppspark_order_recv_socket ;
            senderSocketCppSpark.bind(socketStr2) ;
            senderSocketCppSpark.setSendTimeOut(2000) ;
        }catch (Exception ex ){
            System.out.println("JOfftaskOrderSender exception:"+ex.getMessage() );
        }
    }


    public boolean send( JOfftaskOrderMsg msg) {
        System.out.println("JOfftaskOrderSender start sending ...");
        try{
            Gson gson = new Gson() ;
            String data = gson.toJson(msg) ;
            boolean isok=false;
            if( msg.mode == 5 ){
                isok=senderSocket.send(data.getBytes()) ;//export
            }else{
                isok=senderSocketCppSpark.send(data.getBytes()) ;
            }
            System.out.println("sending done:"+isok);
            return isok ;
        }catch (Exception ex ){
            System.out.println("JOfftaskOrderSender send exception:"+ex.getMessage() );
            return false ;
        }
    }
}
