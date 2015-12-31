package com.jcity.roads;

import org.junit.*;

public class TypeConversionTest {

	@Test
	public void test() {
		String sByte = "127";
		String sBool1 = "0";
		String sBool2 = "1";
		String sDouble = "0.12345";
		String sInt ="1200.0";
		System.out.println("byte:" + Byte.valueOf(sByte));
		System.out.println("boolean:" + (Integer.valueOf(sBool1) > 0));
		System.out.println("boolean:" + (Integer.valueOf(sBool2) > 0));
		System.out.println("double:" + Double.valueOf(sDouble));
		System.out.println("int:" + Integer.valueOf(sInt));

	}

}
