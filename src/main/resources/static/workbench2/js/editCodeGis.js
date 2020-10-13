
$(function(){


// var editor = ace.edit("editor");
// editor.setTheme("ace/theme/monokai");//编辑器样式
// editor.getSession().setMode("ace/mode/javascript");
// //启用提示菜单
// ace.require("ace/ext/language_tools");
// editor.setOptions({
//     enableBasicAutocompletion: true,
//     enableSnippets: true,
//     enableLiveAutocompletion: true
// });
// //字体大小
// editor.setFontSize(18);
// console.log(editor)

/*
    //初始化对象
    editor = ace.edit("editor");

    //设置风格和语言（更多风格和语言，请到github上相应目录查看）
    theme = "xcode";
    language = "c_cpp";
    editor.setTheme("ace/theme/" + theme);
    editor.session.setMode("ace/mode/" + language);

    //字体大小
    editor.setFontSize(18);

    //设置只读（true时只读，用于展示代码）
    editor.setReadOnly(false);

    //自动换行,设置为off关闭
    editor.setOption("wrap", "free")

    //启用提示菜单
    ace.require("ace/ext/language_tools");
    editor.setOptions({
        enableBasicAutocompletion: false,
        enableSnippets: false,
        enableLiveAutocompletion: false
    });
*/



 

 
 
    /*创建图层控制dom*/
    var data=[
        {id:"layerId1","layerName":"layer1","layerInfo":"1111111111111"},
        {id:"layerId2","layerName":"layer2","layerInfo":"2222222222222"},
        {id:"layerId3","layerName":"layer3","layerInfo":"3333333333"},
        {id:"layerId4","layerName":"layer4","layerInfo":"4444444444"},
        {id:"layerId5","layerName":"layer5","layerInfo":"55555555"}
    ]

 
 






$(".openEye").click(function(){

    console.log($(this))
    if( $(this).find("i").hasClass("icon-yanjing")){
        $(this).find("i").removeClass("icon-yanjing").addClass("icon-yanjing-xiexian")

        // $(this).parent().css("cssText", "background-color:#cccccc !important;");
        $(this).parent().removeClass("unselected").addClass("selected")
    }else{
    $(this).find("i").removeClass("icon-yanjing-xiexian").addClass("icon-yanjing")
        $(this).parent().removeClass("selected").addClass("unselected")
        // $(this).parent().css("cssText", "background-color:#5e605e !important;");

    }



})




})//入口函数










































