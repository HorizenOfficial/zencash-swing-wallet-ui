package com.vaklinov.zcashui;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class LanguageUtil {


    private LanguageUtil(){
        supportedLocale = new HashMap();
        supportedLocale.put(Locale.US.getCountry(), Locale.US);
        supportedLocale.put(Locale.ITALY.getCountry(), Locale.ITALY);
        supportedLocale.put(Locale.GERMANY.getCountry(), Locale.GERMANY);
    }

    private static LanguageUtil instance;

    private Map<String, Locale>  supportedLocale;

    private ResourceBundle rb;

    public static LanguageUtil instance(){
        if(instance == null){
            instance = new LanguageUtil();
            instance.loadBundle();
        }
        return instance;
    }





    private void loadBundle(){

        Locale currentLocale = getUsersPrferedLocale();
        rb = ResourceBundle.getBundle("messages.zencash", currentLocale);
        Log.info("Loading locale: " + currentLocale.toString());
        Enumeration<String> keys = rb.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            Log.info(key + " : "  +  rb.getString(key));
        }
    }

    public String getString(String key){
        return rb.getString(key);
    }

    public  String getString(String key, Object... params  ) {
        try {
            return MessageFormat.format(rb.getString(key), params);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    private Locale getUsersPrferedLocale(){
        Preferences prefs = Preferences.userNodeForPackage(LanguageUtil.class);
        String country = prefs.get("country", Locale.US.getCountry());
        return supportedLocale.get(country);
    }

    public void updatePreferedLanguage(Locale locale){
        Preferences prefs = Preferences.userNodeForPackage(LanguageUtil.class);
        prefs.put("country", locale.getCountry());
    }

}
