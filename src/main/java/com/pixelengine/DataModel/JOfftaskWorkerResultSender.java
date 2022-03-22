package com.pixelengine.DataModel;
//2022-01-01
import com.google.gson.Gson;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class JOfftaskWorkerResultSender {
    private static JOfftaskWorkerResultSender sharedInstance =null ;
    public static JOfftaskWorkerResultSender getSharedInstance(){
        if( sharedInstance==null ) sharedInstance=new JOfftaskWorkerResultSender() ;
        return sharedInstance;
    }

    private ZContext context =null ;
    private ZMQ.Socket socket = null ;
    public JOfftaskWorkerResultSender() {
        System.out.println("JOfftaskWorkerResultSender startup ...");
        try{
            context = new ZContext() ;
            socket = context.createSocket(SocketType.PUSH) ;
            String socketStr = WConfig.getSharedInstance().offtask_result_collector ;
            socket.connect(socketStr) ;
        }catch (Exception ex ){
            System.out.println("JOfftaskWorkerResultSender exception:"+ex.getMessage() );
        }
    }

    public void send( JOfftaskWorkerResult res ) {
        System.out.println("JOfftaskWorkerResultSender start sending ...");
        try{
            Gson gson = new Gson() ;
            String data = gson.toJson(res) ;
            socket.send(data.getBytes()) ;
        }catch (Exception ex ){
            System.out.println("JOfftaskWorkerResultSender send exception:"+ex.getMessage() );
        }
    }

}
