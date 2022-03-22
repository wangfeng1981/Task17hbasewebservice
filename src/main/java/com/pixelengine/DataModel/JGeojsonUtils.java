package com.pixelengine.DataModel;
//2022-3-22 1406

import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.*;

import java.util.List;

public class JGeojsonUtils {
    public class WExtent {
        public double left,right,top,bottom ; // in longlat
    }

    public boolean validateExtent(WExtent e){
        if( e.left >= -180 && e.left<=180 &&
            e.right>=-180 && e.left<=180 &&
                e.top >= -90 && e.top <= 90 &&
                e.bottom>=-90 && e.bottom<=90 )
        {
            if( e.left < e.right && e.top > e.bottom ){
                return true ;
            }
        }
        return false ;
    }

    public WExtent computeFeatureExtent(Feature f){
        WExtent e = new WExtent() ;
        e.left = 99999999 ;
        e.right = -99999999 ;
        e.top = -99999999 ;
        e.bottom = 99999999 ;

        Geometry geo0 = (Geometry)f.getGeometry() ;
        if( geo0 instanceof Polygon)
        {
            List<List<LngLatAlt>> listList = ((Polygon)geo0).getCoordinates() ;
            for( int il = 0 ; il < listList.size();++il )
            {
                for(int i = 0 ; i < listList.get(il).size(); ++ i ){
                    e.left = Math.min(e.left , listList.get(il).get(i).getLongitude() ) ;
                    e.right = Math.max(e.right , listList.get(il).get(i).getLongitude() ) ;
                    e.top = Math.max(e.top , listList.get(il).get(i).getLatitude() ) ;
                    e.bottom = Math.min(e.bottom , listList.get(il).get(i).getLatitude() ) ;
                }
            }
            if( validateExtent(e) ) return e ;
            System.out.println("computeGeoJsonExtent bad extent values.");
            return null ;
        }else if(geo0 instanceof MultiPolygon ){
            List<List<List<LngLatAlt>>> listListList = ((MultiPolygon)geo0).getCoordinates() ;
            for(int iLL = 0 ; iLL < listListList.size(); ++ iLL )
            {
                List<List<LngLatAlt>> listList = listListList.get(iLL) ;
                for( int il = 0 ; il < listList.size();++il )
                {
                    for(int i = 0 ; i < listList.get(il).size(); ++ i ){
                        e.left = Math.min(e.left , listList.get(il).get(i).getLongitude() ) ;
                        e.right = Math.max(e.right , listList.get(il).get(i).getLongitude() ) ;
                        e.top = Math.max(e.top , listList.get(il).get(i).getLatitude() ) ;
                        e.bottom = Math.min(e.bottom , listList.get(il).get(i).getLatitude() ) ;
                    }
                }
            }
            if( validateExtent(e) ) return e ;
            System.out.println("computeGeoJsonExtent bad extent values.");
            return null ;
        }else{
            System.out.println("computeGeoJsonExtent not Polygon or MultiPolygon.");
            return null ;
        }
    }

    public WExtent computeGeoJsonExtent( String geojsonFilepath )
    {
        WExtent e = new WExtent() ;

        String geojsonText = WTextFile.readFileAsString(geojsonFilepath) ;
        if( geojsonText==null ){
            System.out.println("computeGeoJsonExtent geojsonText is null ");
            return null;
        }

        try{
            GeoJsonObject object = new ObjectMapper().readValue(geojsonText, GeoJsonObject.class);
            if (object instanceof Feature) {
                return computeFeatureExtent( (Feature)object ) ;
            }else if( object instanceof FeatureCollection)
            {
                Feature firstFeature =  ((FeatureCollection) object).getFeatures().get(0) ;
                return computeFeatureExtent( firstFeature ) ;
            }else{
                System.out.println("computeGeoJsonExtent unsupported geojson type.(not Feature or FeatureCollection)");
                return null ;
            }
        }catch (Exception ex){
            System.out.println("computeGeoJsonExtent exception:"+ex.getMessage());
            return null ;
        }
    }

    public static WExtent extent(String geojsonFilepath){
        JGeojsonUtils u = new JGeojsonUtils() ;
        return u.computeGeoJsonExtent(geojsonFilepath);
    }
}
