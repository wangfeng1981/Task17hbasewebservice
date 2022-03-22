package com.pixelengine.DataModel;
//2022-01-01
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

    private ZContext context = null ;
    private ZMQ.Socket senderSocket=null ;
    public JOfftaskOrderSender(){
        System.out.println("JOfftaskOrderSender start up");
        try{
            context = new ZContext() ;
            senderSocket = context.createSocket(SocketType.PUSH) ;
            String socketStr = WConfig.getSharedInstance().offtask_export_producer ;
            senderSocket.bind(socketStr) ;
        }catch (Exception ex ){
            System.out.println("JOfftaskOrderSender exception:"+ex.getMessage() );
        }
    }


    public void send( JOfftaskOrderMsg msg) {
        System.out.println("JOfftaskOrderSender start sending ...");
        try{
            Gson gson = new Gson() ;
            String data = gson.toJson(msg) ;
            senderSocket.send(data.getBytes()) ;
        }catch (Exception ex ){
            System.out.println("JOfftaskOrderSender send exception:"+ex.getMessage() );
        }
    }
}
