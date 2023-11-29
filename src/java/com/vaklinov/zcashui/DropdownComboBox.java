/************************************************************************************************
 *   ____________ _   _  _____          _      _____ _    _ _______          __   _ _      _   
 *  |___  /  ____| \ | |/ ____|        | |    / ____| |  | |_   _\ \        / /  | | |    | |  
 *     / /| |__  |  \| | |     __ _ ___| |__ | |  __| |  | | | |  \ \  /\  / /_ _| | | ___| |_ 
 *    / / |  __| | . ` | |    / _` / __| '_ \| | |_ | |  | | | |   \ \/  \/ / _` | | |/ _ \ __|
 *   / /__| |____| |\  | |___| (_| \__ \ | | | |__| | |__| |_| |_   \  /\  / (_| | | |  __/ |_ 
 *  /_____|______|_| \_|\_____\__,_|___/_| |_|\_____|\____/|_____|   \/  \/ \__,_|_|_|\___|\__|
 *             
 * Copyright (c) 2023 Horizen Foundation                          
 * Copyright (c) 2016-2021 Zen Blockchain Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 **********************************************************************************/
package com.vaklinov.zcashui;

import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;


/**
 * Combo Box that is aware of whether is is dropped down.
 */
public class DropdownComboBox<E> 
    extends JComboBox<E>
{
    protected boolean isMenuDown;
    
    public DropdownComboBox(E[] list) 
    {
        super(list);
        
        this.isMenuDown = false;
        this.initDownListener();
    }    
    
    
    public boolean isMenuDown()
    {
        return this.isMenuDown;
    }
    
    
    private void initDownListener()
    {
        this.addPopupMenuListener(new PopupMenuListener() 
        {    
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) 
            {
                DropdownComboBox.this.isMenuDown = true;
            }
            
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) 
            {
                DropdownComboBox.this.isMenuDown = false;
            }
            
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) 
            {
                DropdownComboBox.this.isMenuDown = false;
            }
        });   
    }    
}
