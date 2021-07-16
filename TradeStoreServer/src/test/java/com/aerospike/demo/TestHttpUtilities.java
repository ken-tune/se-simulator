package com.aerospike.demo;

import org.junit.jupiter.api.*;

import java.text.ParseException;
import java.util.Map;

public class TestHttpUtilities {
    @Test
    public void testGetParametersFromURI() throws ParseException {
        String testString = "abc.com?a=1&b=2&c&d=4";
        Map<String,String>  parametersAsMap =  HttpUtilities.getParametersFromURI(testString);
        Assertions.assertEquals(parametersAsMap.get("a"),"1");
        Assertions.assertEquals(parametersAsMap.get("b"),"2");
        Assertions.assertEquals(parametersAsMap.get("c"),null);
        Assertions.assertEquals(parametersAsMap.get("d"),"4");


    }
}
