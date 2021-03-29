package com.pixelengine;

import com.pixelengine.DataModel.JProduct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.SQLException;
import java.util.ArrayList;

@SpringBootApplication
public class Task17HbaseWebserviceApplication {
	/// posts and base url should edit /resources/application.properties




	public static void main(String[] args) throws SQLException {
		System.out.println("params: config.json");
		System.out.println("v1.0 2020-10-10");
		System.out.println("v2.0 2021-3-24");
		System.out.println("v2.1 2021-3-26");
		System.out.println("v2.2 region api, zonalstat api. 2021-3-27");
		System.out.println("v2.2.2 extra params for zonalstat task, add hFamily,hpidblen,yxblen. 2021-3-27");
		System.out.println("v2.2.3 new zonalstat task.2021-3-29") ;
		if( args.length != 1 )
		{
			System.out.println("Error : args.length not 1.");
			return ;
		}
		String configfile = args[0] ;
		System.out.println("Info : loading " + configfile) ;
		WConfig.init(configfile);

		JRDBHelperForWebservice.init(WConfig.sharedConfig);

		//show pixelengine core version
		HBasePeHelperCppConnector cc = new HBasePeHelperCppConnector();
		System.out.println("pe core version: " + cc.GetVersion() );


		//test debug
		ArrayList<JProduct> productlist = JProduct.getSharedList() ;

		SpringApplication.run(Task17HbaseWebserviceApplication.class, args);
	}

}
