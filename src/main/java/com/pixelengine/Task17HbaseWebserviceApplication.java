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
		System.out.println("v2.9.6 增加添加预定义区域到我的感兴趣区接口 2021-5-25 ");
		System.out.println("v2.10.0 用户产品表和系统产品表分离，增加预加载图层接口 2021-5-29 ");
		System.out.println("v2.10.2 使用最小测试环境，不连接HBase，统一使用占位图片返回 2021-6-7 ");
		System.out.println("v2.11.0 我的区域倒排序，增加数据导出标题，删除用户产品表相关内容 2021-6-7 ");
		System.out.println("v2.11.0h h for use HBase data.2021-6-16");
		System.out.println("v2.11.3h h 修改了全部产品接口，xyz图层增加自定义变量.2021-6-23");
		System.out.println("v2.12.0 task7config.json can config local or hbase. 2021-8-2.") ;//not finished, very sleepy.
		System.out.println("v2.12.1 use local tile data. 2021-8-22.") ;//
		System.out.println("v2.13.0 2021-11-28.") ;
		System.out.println("v2.14.0 2022-1-11. add datetime api for getting lowerequal datetime.") ; //版本号a表示开发中，不能发布，一旦完成去掉a
		//1.pe系统直接产品的wmts服务，增加datetime自定义Dimension字段，该字段值给dataitem表中该产品对应hcol最大的值，如果dataitem表里没有记录，给0.
		//2.pe系统直接产品的wmts服务，增加styleid自定义Dimension字段，styleid从数据库读取

		System.out.println("v2.14.1 2022-1-31. init v8 in the app startup.") ;
		//v2.14.1 Task17调用后先启动v8运行个空脚本，避免瓦片计算程序直接崩溃。

		System.out.println("v2.15.0 2022-2-2. 使用RoiController替换之前的感兴趣区接口.") ;
		System.out.println("v2.15.1 2022-2-3. upload shp ROI.") ;
		System.out.println("v2.15.2 2022-2-3. upload geojson ROI, delete ROI.") ;
		System.out.println("v2.15.3 2022-2-5. add get script api.") ;



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
		//try to init v8 2022-1-31
		System.out.println("try to init v8 ...");
		TileComputeResult notUsedResult = cc.RunScriptForTileWithoutRender("com/pixelengine/HBasePixelEngineHelper", "function main(){return null;}", 0,0,0,0) ;
		System.out.println("init v8 done.");

		//test debug
		ArrayList<JProduct> productlist = JProduct.getSharedList() ;

		SpringApplication.run(Task17HbaseWebserviceApplication.class, args);
	}

}
