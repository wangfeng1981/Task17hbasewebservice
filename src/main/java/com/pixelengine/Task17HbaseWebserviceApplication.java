package com.pixelengine;

import com.google.gson.Gson;
import com.pixelengine.DataModel.JOfftaskOrderSender;
import com.pixelengine.DataModel.JProduct;
import com.pixelengine.DataModel.WConfig;
import com.pixelengine.controller.OfftaskCollector;
import com.pixelengine.controller.OmcController;
import com.pixelengine.tools.FileDirTool;
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
		System.out.println("v2.15.4 2022-2-6. add script wmts api.") ;
		System.out.println("v2.15.5 2022-2-8. add ExportController.exportNew2 ") ;
		System.out.println("v2.16.0 2022-2-12. add HBasePeHelperCppConnector.GetDatasetNameArray ") ;
		System.out.println("v2.17.1 2022-2-13. update WConfig ") ;
		System.out.println("v2.17.4 2022-2-13. use 0mq for offtask export") ;
		System.out.println("v2.17.5 2022-2-15. use script for export pe data.") ;
		System.out.println("v2.18.1 2022-3-5. uploaded shp/geojson will be converted to hseg.tlv and write into HBase. helper support read roi.hseg.tlv") ;
		System.out.println("v2.18.2 2022-3-19. sys_roi, user_roi中hcol改为int32, the value still use 1.");
		System.out.println("v2.18.3 2022-3-22 change some class package into DataModel.") ;
		System.out.println("v2.19.0 2022-3-27 add statistic methods.") ;
		System.out.println("v2.19.1 2022-4-4 update some java files from task16.") ;
		System.out.println("v2.20.1 2022-4-5 composite new2.") ;
		System.out.println("v2.21.1 2022-4-6 user offtask list.") ;
		System.out.println("v2.22.4 2022-4-9 add one product info api; stat support script product; delete offtask.") ;
		System.out.println("v2.22.4r 2022-4-9") ;
        System.out.println("v2.23.1 2022-4-17 OnlineMapComposer APIs.") ;
		System.out.println("v2.23.3 2022-4-25 bugfixed for wms url.") ;
		System.out.println("v2.24.0.1 2022-5-19 user script staff, pin pixel values.") ;//not commited
		System.out.println("v2.24.0.r 2022-5-19.") ;//release
		System.out.println("v2.24.1.0 2022-5-19. bugfix for pes wmts; use get method for script content.") ;//
		// bugfixed for sdui matching in script wmts.

		System.out.println("v2.24.1.r 2022-5-19") ;//release
		System.out.println("v2.24.2.0 2022-5-20 try wmts legend api.");
		System.out.println("v2.24.3.0 2022-5-24. update tbcategory related codes.");
		//v2.24.3.0
		// update related codes of tbcategory for newly add itype field 2022-5-24
		System.out.println("v2.24.3.1 2022-5-27 newfromtem.");
		System.out.println("v2.24.4.0 2022-5-31 style detail has filename.");
		System.out.println("v2.24.4.1 update HBasePixelEngineHelper, add some debug infos," +
				" later it runs good, I need remove those debug outputs. 2022-6-6");
		System.out.println("v2.24.5.0 update styleEdit with updatetime.") ;//not commited
		System.out.println("v2.24.6.2 helper add getNearestDatetime;product and script wmts add wmtstclog api. 2022-7-3") ;//

		//2022-7-10 v2.24.7.2
		System.out.println("v2.24.7.2 use dt0 and dt1 for data item. 2022-7-10") ;//



		if( args.length != 1 )
		{
			System.out.println("Error : args.length not 1.");
			return ;
		}
		String configfile = args[0] ;
		System.out.println("Info : loading " + configfile) ;
		WConfig.init(configfile);

		JRDBHelperForWebservice.init(
				WConfig.getSharedInstance().connstr,
				WConfig.getSharedInstance().user,
				WConfig.getSharedInstance().pwd);

		//show pixelengine core version
		HBasePeHelperCppConnector cc = new HBasePeHelperCppConnector();
		System.out.println("PixelEngine Versions: " + cc.GetVersion() );


		//try to init v8 2022-1-31 使用空脚本初始化调用一次v8以免后面瓦片计算导致task17崩溃
		System.out.println("try to init v8 ...");
		TileComputeResult notUsedResult = cc.RunScriptForTileWithoutRender(
				"com/pixelengine/HBasePixelEngineHelper",
				"function main(){return null;}", 0,0,0,0) ;
		System.out.println("init v8 done.");

		//load products
		ArrayList<JProduct> productlist = JProduct.getSharedList() ;

		//启动一个0mq线程作为offtask结果搜集器
		OfftaskCollector offtaskCollector = new OfftaskCollector() ;
		offtaskCollector.start();

		//启动离线任务发送者socket
		JOfftaskOrderSender.getSharedInstance() ;//do nothing , only startup the socket.


		//2022-3-27 尝试一些单元测试的代码，用后注释掉
//		{
//			System.out.println("██╗   ██╗███╗   ██╗██╗████████╗    ████████╗███████╗███████╗████████╗");
//			System.out.println("██║   ██║████╗  ██║██║╚══██╔══╝    ╚══██╔══╝██╔════╝██╔════╝╚══██╔══╝");
//			System.out.println("██║   ██║██╔██╗ ██║██║   ██║          ██║   █████╗  ███████╗   ██║   ");
//			System.out.println("██║   ██║██║╚██╗██║██║   ██║          ██║   ██╔══╝  ╚════██║   ██║   ");
//			System.out.println("╚██████╔╝██║ ╚████║██║   ██║          ██║   ███████╗███████║   ██║   ");
//			System.out.println(" ╚═════╝ ╚═╝  ╚═══╝╚═╝   ╚═╝          ╚═╝   ╚══════╝╚══════╝   ╚═╝   ");
//
//			byte[] tlvdata = FileDirTool.readFileAsBytes("/var/www/html/pe/roi/test-1100.geojson.hseg.tlv");
//			TileComputeResult tcr = new TileComputeResult() ;
//			tcr.status=0;
//			tcr.nbands=3 ;
//			tcr.outType= 1;
//			tcr.dataType=1 ;
//			byte[] tempdata = new byte[256*256*3] ;
//			for(int i = 0 ; i<256*256;++i) tempdata[i] = 1 ;
//			for(int i = 256*256 ; i<256*256*2 ; ++ i ) tempdata[i] = 10 ;
//			for(int i = 256*256*2 ; i<256*256*3 ; ++ i ) tempdata[i] = 13 ;
//			tcr.binaryData = tempdata ;
//			tcr.width = 256 ;
//			tcr.height = 256 ;
//			tcr.x = 26 ;
//			tcr.y = 4 ;
//			tcr.z = 5 ;
//			HBasePeHelperCppConnector cctest = new HBasePeHelperCppConnector() ;
//			JStatisticData[] statdataArr = cctest.ComputeStatisticTileComputeResultByHsegTlv("",tcr,tlvdata,0,1,100);
//			System.out.println( (new Gson()).toJson(statdataArr,JStatisticData[].class) ) ;
//		}

		SpringApplication.run(Task17HbaseWebserviceApplication.class, args);
	}

}
