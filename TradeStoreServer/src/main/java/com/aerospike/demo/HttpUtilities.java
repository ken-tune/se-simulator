package com.aerospike.demo;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class HttpUtilities {
    public static class HttpMethods {
        public static final String GET = "GET";
    }

    public static class HttpCodes {
        public static final int OK = 200;
        public static final int CLIENT_REQUEST_ERROR = 400;
        public static final int SERVER_ERROR = 500;
    }

    public static Map<String,String> getParametersFromURI(String URI) throws ParseException{
        Map<String,String> parameterMap = new HashMap<>();
        String allParameters = null;
        if(URI.split("\\?").length > 1){
            allParameters = URI.split("\\?")[1];
        }
        if(allParameters != null){
            String[] parametersArray = allParameters.split("&");
            for(int i=0;i<parametersArray.length;i++){
                String parameter = parametersArray[i];
                String[] parameterParts = parameter.split("=");
                if(parameterParts.length == 2){
                    parameterMap.put(parameterParts[0],parameterParts[1]);
                }
                else if(parameterParts.length == 1){
                    parameterMap.put(parameterParts[0],null);
                }
                else{
                    throw new ParseException(String.format("Can't parse %s - see %s",URI,parameter),i);
                }
            }
        }
        return parameterMap;
    }
}
