package com.vaklinov.zcashui;

import javax.swing.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Language Utility resource bundle loader.
 *
 * @author aballaci <aballaci@gmail.com>
 */
public class LanguageUtil {

    private static final String PREFERRED_LOCALE_FILE_NAME = "language_preferences.txt";

    private static final String RESOURCE_BUNDLE_FILE_NAME = "messages.zencash";

    private static final Locale DEFAULT_LOCALE = Locale.US;


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
        Locale currentLocale = getUsersPreferredLocale();
        rb = ResourceBundle.getBundle(RESOURCE_BUNDLE_FILE_NAME, currentLocale);
        Log.info("Loading locale: " + currentLocale.toString());
    }

    public String getString(String key){
        try {
            return rb.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public  String getString(String key, Object... params  ) {
        try {
            return MessageFormat.format(rb.getString(key), params);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public void updatePreferredLanguage(Locale locale) {
        try {
            File languagePrefsFile = new File(OSUtil.getSettingsDirectory(),PREFERRED_LOCALE_FILE_NAME );
            try (PrintWriter printWriter = new PrintWriter(new FileWriter(languagePrefsFile))) {
                    printWriter.println(locale.getCountry());
            }
            
        } catch (IOException e) {
            Log.error("Saving Preferred Locale Failed!!!!", e);
        }
    }

    public Locale getUsersPreferredLocale() {
        File languagePrefsFile;
        try {
            languagePrefsFile = new File(OSUtil.getSettingsDirectory(),PREFERRED_LOCALE_FILE_NAME);

        if (!languagePrefsFile.exists()) {
            return DEFAULT_LOCALE;
        }
            BufferedReader bufferedReader = new BufferedReader(new FileReader(languagePrefsFile));
            String country = bufferedReader.readLine().trim();
            bufferedReader.close();
            return supportedLocale.get(country);
        } catch (FileNotFoundException e) {
            Log.error("Loading Language file Failed!!!!", e);
            return DEFAULT_LOCALE;
        } catch (IOException e) {
            Log.error("Loading Language file Failed!!!!", e);
            return DEFAULT_LOCALE;
        }
    }

}
