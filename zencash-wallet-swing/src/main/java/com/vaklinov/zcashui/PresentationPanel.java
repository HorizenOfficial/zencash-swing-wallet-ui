/************************************************************************************************
 *  _________          _     ____          _           __        __    _ _      _   _   _ ___
 * |__  / ___|__ _ ___| |__ / ___|_      _(_)_ __   __ \ \      / /_ _| | | ___| |_| | | |_ _|
 *   / / |   / _` / __| '_ \\___ \ \ /\ / / | '_ \ / _` \ \ /\ / / _` | | |/ _ \ __| | | || |
 *  / /| |__| (_| \__ \ | | |___) \ V  V /| | | | | (_| |\ V  V / (_| | | |  __/ |_| |_| || |
 * /____\____\__,_|___/_| |_|____/ \_/\_/ |_|_| |_|\__, | \_/\_/ \__,_|_|_|\___|\__|\___/|___|
 *                                                 |___/
 *
 * Copyright (c) 2016 Ivan Vaklinov <ivan@vaklinov.com>
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;

import javax.swing.JPanel;


/**
 * Panel with gradient background etc. for pretty label presentations.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class PresentationPanel 
	extends JPanel 
{
	static final int GRADIENT_EXTENT = 17;

	static final Color  colorBorder = new Color(140, 145, 145);
	static final Color  colorLow    = new Color(250, 250, 250);
	static final Color  colorHigh   = new Color(225, 225, 230);
	static final Stroke edgeStroke  = new BasicStroke(1);

	
	public PresentationPanel()
	{
		this.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
	}
	

	public void paintComponent(Graphics graphics) 
	{
		int h = getHeight();
		int w =  getWidth();
		
		if (h < GRADIENT_EXTENT + 1) 
		{
			super.paintComponent(graphics);
			return;
		}
		
		float percentageOfGradient = (float) GRADIENT_EXTENT / h;
		
		if (percentageOfGradient > 0.49f)
		{
			percentageOfGradient = 0.49f;
		}
		
		Graphics2D graphics2D = (Graphics2D) graphics;
		
		float fractions[] = new float[] 
		{ 
			0, percentageOfGradient, 1 - percentageOfGradient, 1f 
		};
		
		Color colors[] = new Color[] 
		{ 
			colorLow, colorHigh, colorHigh, colorLow 
		};
		
		LinearGradientPaint paint = new LinearGradientPaint(0, 0, 0, h - 1, fractions, colors);
		
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics2D.setPaint(paint);
		graphics2D.fillRoundRect(0, 0, w - 1, h - 1, GRADIENT_EXTENT, GRADIENT_EXTENT);
		graphics2D.setColor(colorBorder);
		graphics2D.setStroke(edgeStroke);
		graphics2D.drawRoundRect(0, 0, w - 1, h - 1, GRADIENT_EXTENT, GRADIENT_EXTENT);
	}

}
