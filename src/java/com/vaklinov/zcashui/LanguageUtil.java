package com.vaklinov.zcashui;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageUtil {


    private LanguageUtil(){}

    private static LanguageUtil instance;

    // locale selection in app settings to be implemented
    int chosenLocale = 0;

    private ResourceBundle rb;

    public static LanguageUtil instance(){
        if(instance == null){
            instance = new LanguageUtil();
            instance.loadBundle();
        }
        return instance;
    }


    static Locale[] supportedLocale = {
            Locale.US, Locale.ITALY, Locale.GERMANY
    };



    private void loadBundle(){
        Locale defaultLocale = Locale.getDefault();
        List<Locale> locales = Arrays.asList(supportedLocale);
        Locale currentLocale;
        if(locales.contains(defaultLocale)){
            currentLocale = defaultLocale;
        }else{
            currentLocale = Locale.US;
        }
        rb = ResourceBundle.getBundle("messages.zencash", currentLocale);
    }

    public String getString(String key){
        return rb.getString(key);
    }
}
