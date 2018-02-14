package com.vaklinov.zcashui;

import javax.swing.*;
import java.util.Locale;
import java.util.ResourceBundle;

public class ZenFrame extends JFrame {

    public ZenFrame(){
        super();
        initLocale();
        this.setTitle(getString("label.main.frame.title"));
    }

    static Locale[] localesSupported = {
            Locale.US, Locale.FRANCE, Locale.GERMANY
    };

    int localeChoosen = 0;
    Locale localeCurrent;
    ResourceBundle rb;

    public void initLocale(){
        localeCurrent = localesSupported[localeChoosen];
        this.setLocale(localeCurrent);
        rb = ResourceBundle.getBundle("messages.zencash", localeCurrent);
    }

    public String getString(String key){
        return rb.getString(key);
    }
}
