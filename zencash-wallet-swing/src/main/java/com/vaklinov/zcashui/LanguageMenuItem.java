package com.vaklinov.zcashui;

import javax.swing.*;
import java.util.Locale;

public class LanguageMenuItem extends JRadioButtonMenuItem {

    private Locale locale;

    public LanguageMenuItem(String text, Icon icon, Locale locale) {
        super(text,icon);
        this.locale = locale;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }
}
