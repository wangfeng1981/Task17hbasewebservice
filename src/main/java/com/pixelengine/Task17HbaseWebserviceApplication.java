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
		System.out.println("v2.2.4 use tbstyle replace tbStyle 2021-3-30.") ;
		System.out.println("v2.3 update zonalstat, add serialanalyse, wmts use styleid 2021-4-1") ;
		System.out.println("v2.4 update composite controller 2021-4-6") ;
		System.out.println("v2.5 update Area and ROI 2021-4-8") ;
		System.out.println("v2.6 add user login 2021-4-12") ;
		System.out.println("v2.6.9 package version 2021-4-19") ;
		System.out.println("v2.7.1 add data export apis 2021-4-21") ;
		System.out.println("v2.8.2 离线任务mysql参数中统一增加outfilename,outfilenamedb两个字段 2021-4-21") ;
		System.out.println("v2.9 增加静态产品信息接口 2021-4-23");
		System.out.println("v2.9.1 bugfix for zonalstat/remove, use DESC order for zonalstst/userlist2.2021-4-26");
		System.out.println("v2.9.2 数据合成接口增加矩形区域参数.2021-4-29");
		System.out.println("v2.9.3 数据合成接口userbound改为usebound. 2021-4-29 ");
		System.out.println("v2.9.5 增加数据合成任务产品图层信息接口 2021-4-29 ");

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
