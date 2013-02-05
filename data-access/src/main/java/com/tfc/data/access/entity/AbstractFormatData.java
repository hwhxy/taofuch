package com.tfc.data.access.entity;

import com.alibaba.fastjson.JSON;

/**
 * 抽象的格式。主要为具体的格式提供一些公共的方法。
 * 
 * @author taofucheng
 * 
 */
public class AbstractFormatData {
	protected Class<?> valueClass = null;
	protected static final char[] chs = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

	protected static String random() {
		// StringBuffer sb = new StringBuffer();
		// for (int i = 0; i < 5; i++) {
		// sb.append(chs[RandomUtils.nextInt(62)]);
		// }
		// return sb.toString();
		return "";
	}

	protected String getStoreValue(Object value) {
		String store = JSON.toJSONString(value);
		if (valueClass != null && Number.class.isAssignableFrom(valueClass)) {
			// 如果是数字，则使用String的方式存储
			store = String.valueOf(value);
		}
		return store;
	}

	protected Object parseToObject(Class<?> targetElementClass, String value) {
		if (value != null) {
			if (value.startsWith("{") && value.endsWith("}")) {
				return JSON.parseObject(value, targetElementClass);
			} else if (value.startsWith("[") && value.endsWith("]")) {
				return JSON.parseArray(value, targetElementClass);
			} else if (!value.startsWith("\"") && !value.endsWith("\"")) {
				// 这就是数字啊！
				try {
					return targetElementClass.getConstructor(String.class).newInstance(value);
				} catch (Exception e) {
				}
			}
			try {
				return JSON.parseObject(value, targetElementClass);
			} catch (Exception e) {
				return value;
			}
		}
		if (targetElementClass != null && Number.class.isAssignableFrom(targetElementClass)) {
			return 0;
		}
		return null;
	}

	public static void main(String[] args) {
		int[][] x = new int[2][3];
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 3; j++) {
				x[i][j] = j;
			}
		}
		System.err.println(x[0].length);
		System.out.println(random());
	}

}