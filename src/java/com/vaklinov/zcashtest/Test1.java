package com.vaklinov.zcashtest;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Date;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

public class Test1 
{

	public static void main(String[] args) 
		throws IOException
	{
//		JsonObject toArgument = new JsonObject();
//		toArgument.set("address", "111111111111111111");
//		toArgument.set("memo", "222222222222222222222222222");
//		toArgument.set("amount", "\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF");
//		
//		JsonArray toMany = new JsonArray();
//		toMany.add(toArgument);
//
//		System.out.println(toMany.toString().
//		    replace("\"amount\":\"\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF\"", 
//                    "\"amount\":" + new DecimalFormat("#########.00######").format(Double.valueOf("1234567890000"))));
//		
//		
//		final Date startDate = new Date("04 Oct 2016 00:00:00 GMT");
//		System.out.println(startDate.toString());
		
		System.out.println(hexify("\n"));
		
		
	}
	
	
	
	
	private static String hexify(String plain)
		throws IOException
	{
		StringBuilder hexBuf = new StringBuilder();
		int i = 0;
		for (byte c : plain.getBytes("UTF-8"))
		{
			hexBuf.append("(byte)0x");
			String hexChar = Integer.toHexString((int)c);
			if (hexChar.length() < 2)
			{
				hexChar = "0" + hexChar;
			}
			hexBuf.append(hexChar);
			hexBuf.append(",");
			
			if ((i > 0) & (++i % 10 == 0))
			{
				hexBuf.append("\n");
			}
		}
		
		return hexBuf.toString();
	}

}
