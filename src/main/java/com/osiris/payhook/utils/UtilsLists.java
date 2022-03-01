package com.osiris.payhook.utils;

import java.util.List;

public class UtilsLists {
    public static boolean containsIgnoreCase(List<String> stringList, String s){
        for (String s0 :
                stringList) {
            if(s0.equalsIgnoreCase(s)) return true;
        }
        return false;
    }
}
