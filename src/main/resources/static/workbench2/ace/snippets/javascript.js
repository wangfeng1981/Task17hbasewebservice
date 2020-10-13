ace.define("ace/snippets/javascript",["require","exports","module"], function(require, exports, module) {
"use strict";

exports.snippetText = "\n\
# PixelEngine.Dataset (...) {...}\n\
snippet PixelEngine.Dataset\n\
	var ds=PixelEngine.Dataset(${1:'fy4'},${2:20200101000000},${3:[0,1,2]});\n\
# PixelEngine.Dataset (...) {...}\n\
snippet pe.Dataset\n\
	var ds=pe.Dataset(${1:'fy4'},${2:20200101000000},${3:[0,1,2]});\n\
# PixelEngine.DatasetArray (...) {...}\n\
snippet PixelEngine.DatasetArray\n\
	var dsArr=PixelEngine.DatasetArray(${1:'fy4'},${2:20200101000000},${3:20200102000000},${4:[0,1,2]});\n\
# PixelEngine.ColorRamp (...) {...}\n\
snippet PixelEngine.ColorRamp\n\
	var cr=pe.ColorRamp();\n\
	cr.add(0,255,255,255,255,'');\n\
# PixelEngine.ColorRamp (...) {...}\n\
snippet pe.ColorRamp\n\
	var cr=pe.ColorRamp();\n\
	cr.add(0,255,255,255,255,'');\n\
# ColorRamp.add (...) {...}\n\
snippet cr.add\n\
	cr.add(ival,r,g,b,a,'');\n\
# Dataset.renderRGB (...) {...}\n\
snippet dataset.renderRGB\n\
	var dsrgb=dataset.renderRGB(${1:0},${2:1},${3:2},0,5000,0,5000,0,5000);\n\
# Dataset.renderGray (...) {...}\n\
snippet dataset.renderGray\n\
	var dsout=dataset.renderGray(${1:0},0,10000,-1,[0,0,0,0]);\n\
# Dataset.renderPsuedColor (...) {...}\n\
snippet dataset.renderPsuedColor 1\n\
	var dsrgb=dataset.renderPsuedColor(0,0,10000,-9999,[0,0,0,0],pe.ColorRampRainbow,pe.ColorRampNormal,pe.ColorRampDiscrete);\n\
# Dataset.renderPsuedColor (...) {...}\n\
snippet dataset.renderPsuedColor 2\n\
	var dsrgb=dataset.renderPsuedColor(${1:0},colorRamp,pe.ColorRampDiscrete);\n\
\n\
\n\
";
exports.scope = "javascript";

});                (function() {
                    ace.require(["ace/snippets/javascript"], function(m) {
                        if (typeof module == "object" && typeof exports == "object" && module) {
                            module.exports = m;
                        }
                    });
                })();
            