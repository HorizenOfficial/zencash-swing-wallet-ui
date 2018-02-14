package com.vaklinov.zcashui;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageUtil {


    private LanguageUtil(){}


    static Locale[] supportedLocale = {
            Locale.US, Locale.ITALY, Locale.GERMANY
    };

    // locale selection in app settings to be implemented
    int chosenLocale = 0;
    Locale defaultLocale = Locale.getDefault();;
    static ResourceBundle rb;

    public void initLocale(){
        List<Locale> locales = Arrays.asList(supportedLocale);
        Locale currentLocale;
        if(locales.contains(defaultLocale)){
            currentLocale = defaultLocale;
        }else{
            currentLocale = Locale.US;
        }
        rb = ResourceBundle.getBundle("messages.zencash", currentLocale);
    }

    public static String getString(String key){
        return rb.getString(key);
    }
}
