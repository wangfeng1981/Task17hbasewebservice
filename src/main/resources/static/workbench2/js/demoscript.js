var demoScript1 =        
		"function computeSingleShortOuput(b1,b2,b3,b4,b5,b6,b7,b8,b9, \r\n"+
		"	b10,b11,b12,b13,b14,t0,t1,p0,p1){  \r\n"+
		"	if( b1 > 0 ){  \r\n"+
		"		let denom = b3 + b2  ; \r\n"+
		"		if( denom !== 0 ){ \r\n"+
		"			let ndvi = parseInt( (b3-b2)*1.0/denom * 10000) ; \r\n"+
		"			return ndvi ; \r\n"+
		"		}else{ \r\n"+
		"			return -9999 ; \r\n"+
		"		} \r\n"+
		"	}else{ \r\n"+
		"		return -9999 ; \r\n"+
		"	} \r\n"+
		"} \r\n";

var demoScript2 =  
		"function computeRGBOuput(b1,b2,b3,b4,b5,b6,b7,b8,b9,  \r\n"+
		"	b10,b11,b12,b13,b14,t0,t1,p0,p1){  \r\n"+
		"	if( b1 > 0 ){ \r\n"+
		"     let low=50; let high=5000; let diff=high-low; \r\n"+
		"	  let r11 = b1 <= low ? low : b1 >= high ? high : b1; \r\n"+
		"	  let r22 = b2 <= low ? low : b2 >= high ? high : b2; \r\n"+
		"	  let r33 = b3 <= low ? low : b3 >= high ? high : b3; \r\n"+
		"	  let red = parseInt( (r33-low)/diff*255 ) ; \r\n"+
		"	  let green = parseInt( (r22-low)/diff*255 ) ; \r\n"+
		"	  let blue = parseInt( (r11-low)/diff*255 ) ; \r\n"+
		"	  return [red,green,blue] ; \r\n"+
		"	}else{ \r\n"+
		"	  return [0,0,0]; \r\n"+
		"	} \r\n"+
		"} \r\n";

var demoScript3 =  
		"function computeRGBAOuput(b1,b2,b3,b4,b5,b6,b7,b8,b9,  \r\n"+
		"	b10,b11,b12,b13,b14,t0,t1,p0,p1){  \r\n"+
		"	if( b1 > 0 ){  \r\n"+
		"     let low=50; let high=5000; let diff=high-low; \r\n"+
		"	  let r11 = b1 <= low ? low : b1 >= high ? high : b1; \r\n"+
		"	  let r22 = b2 <= low ? low : b2 >= high ? high : b2; \r\n"+
		"	  let r33 = b3 <= low ? low : b3 >= high ? high : b3; \r\n"+
		"	  let red = parseInt( (r33-low)/diff*255 ) ; \r\n"+
		"	  let green = parseInt( (r22-low)/diff*255 ) ; \r\n"+
		"	  let blue = parseInt( (r11-low)/diff*255 ) ; \r\n"+
		"	  return [red,green,blue,255] ; \r\n"+
		"	}else{ \r\n"+
		"	  return [0,0,0,0]; \r\n"+
		"	} \r\n"+
		"} \r\n";

var demoColorNdvi = "-3000,255,255,255,0,fill\r\n0,255,255,255,255,0\r\n1000,178,149,105,255,0.1\r\n2000,147,114,63,255,0.2\r\n4000,61,134,0,255,0.4\r\n6000,28,115,0,255,0.6\r\n7000,0,96,0,255,0.8\r\n10000,0,19,0,255,1.0" ;

var demoColorRainbow = "0,215,25,28,255,0\r\n10,232,91,58,255,10\r\n20,249,158,89,255,20\r\n30,254,201,128,255,30\r\n40,255,237,170,255,40\r\n50,237,248,185,255,50\r\n60,199,233,173,255,60\r\n70,157,211,167,255,70\r\n80,100,171,176,255,80\r\n90,43,131,186,255,90" ;

var demoColor2Value = "0,128,128,128,255,0\r\n1,0,0,255,255,1" ;

var demoColor3Value = "-1,255,255,255,0,fill\r\n0,128,128,128,255,0\r\n1,192,115,25,255,1" ;


var demoTimeArray = ["20190802,000000","20190802,001500","20190802,010000","20190802,020000","20190802,024500","20190802,030000","20190802,031500","20190802,040000","20190802,050000","20190802,054500","20190802,060000","20190802,061500","20190802,070000","20190802,080000","20190802,084500","20190802,090000","20190802,091500","20190802,100000","20190802,110000","20190802,114500","20190802,120000","20190802,121500","20190802,130000","20190802,140000","20190802,144500","20190802,150000","20190802,151500","20190802,160000","20190802,170000","20190802,174500","20190802,180000","20190802,181500","20190802,190000","20190802,200000","20190802,204500","20190802,210000","20190802,211500","20190802,220000","20190802,230000","20190802,234500"] ;

demoTimeArray = ["20180723,120059"] ;//debug


var demoScriptPx =        
		"function main(){\r\n"+
		"  var ds=PixelEngine.Dataset(\"fy3d\",20190601000000,[0,1,2]); \r\n" +
		"  return ds.renderPsuedColor(0,0,7000,-9999,[0,0,0,0],PixelEngine.ColorRampRainbow,1,1);\r\n" +
		"} \r\n";

