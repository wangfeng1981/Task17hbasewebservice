//main.js


//deprecated
function makeScriptHeaderElement(sid,stitle){
	var tid = 'panel2_tab_' + sid;
	var tid2 = 'panel2_con_' + sid;
	var str1='<li class="nav-item" role="presentation"><a class="nav-link" id="'+tid
		+'" data-toggle="tab" href="#'+tid2
		+'" role="tab" aria-controls="'+tid2
		+'" aria-selected="true">'+stitle
		+'</a></li>';
	return str1;
}

//deprecated
function makeEditorDivId(sid){
	return 'panel2_con_'+sid+'_editor' ;
}

//deprecated
function makeScriptContentElement(sid,scontent){
	var tid = 'panel2_tab_' + sid;
	var tid2 = 'panel2_con_' + sid;
	var tid3 = makeEditorDivId(sid) ;
	var str = 
		'<div id="'+tid2+'" class="tab-pane fade active" role="tabpanel" aria-labelledby="'+tid+'" style="height:100%;">'
		+ '<div id="' + tid3 + '" class="editorContainer">'
		+ scontent 
		+ '</div>'
		+'<div class="editorFooter">'
		+'<button class="btn btn-sm editorFooterButton">保存脚本</button>'
		+'<button class="btn btn-sm editorFooterButton">离线任务</button>'
		+'<button class="btn btn-sm btn-primary editorFooterButton">在线运行</button>'
		+'</div>'
		+'</div>';
	return str;
}
 

//添加一个编程图层
function addProgramLayerElement(
	sid,    //脚本id
    stitle, //脚本标题
    scontent, //脚本内容
    tabHeaderContainer, //编辑器tab头容器
    tabContentContainer, //编辑器tab内容容器
    layersContainer,     //图层管理器容器
    map,
    lyrElementsContainer){                //OL地图对象

	//检查重复
	for(var ie =0;ie<lyrElementsContainer.length;++ie){
		if( lyrElementsContainer[ie].sid  === sid ){
			return null ;
		}
	}


	//tab标签DIV
	var tabheadid = 'panel2_tabhead_' + sid;
	var tabcontentid = 'panel2_tabcontent_' + sid;
	var editorid = 'panel2_editor_' + sid;
	var lyrliid = 'lyriid_' + sid;

	var headdiv='<li class="nav-item" role="presentation"><a class="nav-link" id="'+tabheadid
		+'" data-toggle="tab" href="#'+tabcontentid
		+'" role="tab" aria-controls="'+tabcontentid
		+'" aria-selected="true">'+stitle
		+'</a></li>';

	var condiv = 
		'<div id="'+tabcontentid
		+'" class="tab-pane fade active" role="tabpanel" aria-labelledby="'+tabheadid
		+'" style="height:100%;">'
		+ '<div id="' + editorid
		+ '" class="editorContainer">'
		+ scontent 
		+ '</div>'
		+'<div class="editorFooter">'
		+'<button class="btn btn-sm editorFooterButton" onclick="saveScript('+sid
		+')">保存脚本</button>'
		+'<button class="btn btn-sm editorFooterButton" onclick="showOffTaskDialog('+sid
		+')">离线任务</button>'
		+'<button class="btn btn-sm btn-primary editorFooterButton" onclick="submitOnlineTask('+sid
		+')">在线运行</button>'
		+'</div>'
		+'</div>';

	var layerdiv = "<li class='list-group-item layerItem unselected' id='"
		+lyrliid
		+"'><div class='openEye'><i class=' iconfont icon-yanjing'></i></div><div class='layerInfo'><div class='layer_name'>"
		+stitle
		+"</div><div class='layer_control'><span class='control_btn' onclick='alert(3)'><i class=' iconfont icon-guanbi'></i></span></div></div></li>" ;

	$(tabHeaderContainer).append( headdiv );
	$(tabContentContainer).append( condiv ) ;
	$(layersContainer).prepend(layerdiv);

    var editor1 = ace.edit(editorid);
    editor1.session.setMode("ace/mode/javascript");

    var onlinetaskid=0;

    //resolution and matrix
    var tempLevel0Size = 360.0 / 256;
	var resolutions = new Array(14);
	var matrixIds = new Array(14);
	for (var z = 0; z < 14; ++z) {
	  // generate resolutions and matrixIds arrays for this WMTS
	  resolutions[z] = tempLevel0Size / Math.pow(2, z);
	  matrixIds[z] = z;
	}

    //ol layer object
    var wmtsurl = "http://localhost:1234/pe/onlinetask/wmts/"+onlinetaskid+"/" ;
    var tsource = new ol.source.WMTS({
    	url:wmtsurl,
    	layer: onlinetaskid,
    	matrixSet: '250m',
    	format: 'image/png',
    	projection: 'EPSG:4326',
        tileGrid: new ol.tilegrid.WMTS({
          origin: [-180 , 90 ] ,// getTopLeft(projectionExtent),
          resolutions: resolutions,
          matrixIds: matrixIds,
          tileSize: 256
        }),
        dimensions: {
		    'dt': 0,
		},
    	style: 'default',
    	wrapX: false
    });
    var wmtslayer = new ol.layer.Tile({
    	opacity: 1.0,
    	source:tsource, 
    	extent: [-180, -90, 180, 90]
    }) ;

    map.addLayer(wmtslayer) ;


	var obj = {} ;
	obj.sid = sid;
	obj.stitle = stitle;
	obj.scontent = scontent;
	obj.layertitle = stitle ;
	obj.tabheadid = tabheadid;
	obj.tabcontid = tabcontentid;
	obj.editor = editor1; 
	obj.lyrliid = lyrliid ;
	obj.oltid = onlinetaskid;
	obj.wmtslayer = wmtslayer;
	obj.wmtsurl = wmtsurl;

	lyrElementsContainer.push(obj);
	return obj;
}


//提交一个在线计算请求
function submitOnlineTask( sid ){
	var url = "http://192.168.10.178:15900/pe/onlinetask/new" ;
	var scontent="";
	var el = null ;
	for(var ie =0;ie<globalLayerManager.length;++ie){
		if( globalLayerManager[ie].sid  === sid ){
			scontent = globalLayerManager[ie].editor.getValue();	
			console.log("newscript:") ;
			console.log(scontent);		
			globalLayerManager[ie].scontent = scontent ;
			el = globalLayerManager[ie] ;
			break;
		}
	}
	if( scontent !== "" ){
		//save new script first
		var urlupdate = "http://192.168.10.178:15900/pe/scripts/update/" + el.sid;
		$.post(  urlupdate ,{script:scontent} ).done(function(data0){
			console.log("save script ok.") ;
			//submit a online task
			$.post(url , {userid:1, script:scontent }).done(function(data){
				console.log(data) ;
				el.oltid = data.oltid;
				el.wmtsurl = "http://192.168.10.178:15900/pe/onlinetask/wmts/"+data.oltid+"/" ;
				//update layer source.
				el.wmtslayer.getSource().setUrl( el.wmtsurl ) ;
			}) ;
		})  ;
	}
}


//弹出离线配置对话框
function showOffTaskDialog(sid) {
	$( "#offTaskDialog" ).data('sid',sid).dialog( "open" );
}

//离线任务对话框
$( "#offTaskDialog" ).dialog({
    autoOpen: false,
    width: 400,
    buttons: [
        {
            text: "Ok",
            click: function() {
                //选择的图层类型 
                var datetimestr = $("#offtaskdate").val() ;
                console.log("datetimestr:"+ datetimestr);
                console.log("sid:" + $(this).data('sid') ) ;
                submitOffTask( $(this).data('sid') , datetimestr ) ;
                $( this ).dialog( "close" );
            }
        },
        {
            text: "Cancel",
            click: function() {
                $( this ).dialog( "close" );
            }
        }
    ]
});

//提交离线任务
function submitOffTask( sid , datetimestr ){
	//
	var url = "http://192.168.10.178:15900/pe/offlinetask/new" ;
	var scontent="";
	var el = null ;
	for(var ie =0;ie<globalLayerManager.length;++ie){
		if( globalLayerManager[ie].sid  === sid ){
			scontent = globalLayerManager[ie].editor.getValue();	
			console.log("newscript:") ;
			console.log(scontent);		
			globalLayerManager[ie].scontent = scontent ;
			el = globalLayerManager[ie] ;
			break;
		}
	}
	if( scontent !== "" ){
		//new offtask
		$.post(  url ,{script:scontent,datetime:datetimestr,userid:1} ).done(function(data0){
			console.log("off task ok.") ;
		})  ;
	}
}


//保存更新的脚本
function saveScript( sid )
{
	var scontent="";
	var el = null ;
	for(var ie =0;ie<globalLayerManager.length;++ie){
		if( globalLayerManager[ie].sid  === sid ){
			scontent = globalLayerManager[ie].editor.getValue();	
			console.log("newscript:") ;
			console.log(scontent);		
			globalLayerManager[ie].scontent = scontent ;
			el = globalLayerManager[ie] ;
			break;
		}
	}
	if( scontent !== "" ){
		//save new script first
		var urlupdate = "http://192.168.10.178:15900/pe/scripts/update/" + el.sid;
		$.post(  urlupdate ,{script:scontent} ).done(function(data0){
			console.log("save script ok.") ;
		})  ;
	}
}


//感兴趣区绘制
 //draw
var draw; // global so we can remove it later
function addInteraction(typeval) {
    console.log(typeval);
  var value = typeval ;
  if (value === 'Polygon') {
    draw = new ol.interaction.Draw({
      source: drawVectorLayerSource,
      type: typeval,
    });
    map.addInteraction(draw);
  }else if(value==='Box'){
    console.log("use box");
    draw = new ol.interaction.Draw({
      source: drawVectorLayerSource,
      type: value,
      geometryFunction: ol.interaction.Draw.createBox(),  
    });
    map.addInteraction(draw);
  }
}

function onHandClick() {
    map.removeInteraction(draw);
}

function onRectClick() {
    map.removeInteraction(draw);
    addInteraction('Box');
}

function onPolygonClick() {
    map.removeInteraction(draw);
    addInteraction('Polygon');
}


//更新脚本列表
function updateScriptHandler() {
    //test userid=1
    $("#scriptlist").empty();
    $.get("http://192.168.10.178:15900/pe/scripts/user/1", function(result){
        console.log(result);
        var scripts = result.scripts;
        for(var i = 0; i<scripts.length;++ i ){
            $("#scriptlist").append("<li><a onclick='onScriptItemClick("
                + scripts[i].sid 
                + ")'>" + scripts[i].title+"</a></li>") ; 
        }
    });
}

//更新离线任务列表
function updateOffTaskListHandler() {
	//test userid=1
    $("#offtasklist").empty();
    $.get("http://192.168.10.178:15900/pe/offlinetask/list/1", function(result){
        //console.log(result);
        var results = result.results;
        for(var i = 0; i<results.length;++ i ){
        	if( results[i].endTime >= results[i].startTime ){
        		$("#offtasklist").append("<li><a href='#'>离线任务"+results[i].oftid+"(已完成)</a></li>") ; 
        	}else{
        		$("#offtasklist").append("<li><a href='#'>离线任务"+results[i].oftid+"(运行中)</a></li>") ; 
        	}
        }
    });
}

//更新全局产品与用户产品列表
function updateProductListHandler() {
	//test userid=1
    $("#productlist").empty();
    $.get("http://192.168.10.178:15900/pe/product/list/1", function(result){
        //console.log(result);
        var results = result.results;
        for(var i = 0; i<results.length;++ i ){
            $("#productlist").append("<li><a href='#'>产品名称:"+results[i].product.productName 
                +"</a></li>") ; 
        }
    });
}