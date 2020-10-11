package com.pixelengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Task17HbaseWebserviceApplication {
	/// posts and base url should edit /resources/application.properties




	public static void main(String[] args) {
		System.out.println("params: config.json");
		System.out.println("v1.0 2020-10-10");
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

		SpringApplication.run(Task17HbaseWebserviceApplication.class, args);
	}

}
