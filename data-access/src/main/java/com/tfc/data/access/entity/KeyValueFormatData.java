package com.tfc.data.access.entity;

import java.util.LinkedHashSet;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.tfc.data.access.RepositoryFactory;

/**
 * 键值对结构的格式。如Map类型。
 * 
 * @author taofucheng
 * 
 */
public class KeyValueFormatData<K, V> extends AbstractFormatData {
	private static final String prefix = "map";
	private String instanceName = "";
	private Set<Entry<K, V>> entries = new LinkedHashSet<Entry<K, V>>();

	public KeyValueFormatData(String instanceName) {
		this.instanceName = instanceName + System.nanoTime() + random();
	}

	public void put(K key, V value) {
		if (valueClass == null && value != null) {
			valueClass = value.getClass();
		}
		String store = getStoreValue(value);
		boolean ret = RepositoryFactory.save(genarateKey(key), store);
		if (ret) {
			synchronized (entries) {
				entries.add(new Entry<K, V>(this, key));
			}
		}
	}

	public long size() {
		return entries.size();
	}

	@SuppressWarnings("unchecked")
	public V getValue(Object key) {
		if (valueClass == null) {
			return null;
		}
		String value = RepositoryFactory.findValueByKey(genarateKey(key));
		if ("NaN".equals(value)) {
			if (Double.class.isAssignableFrom(valueClass)) {
				return (V) new Double("NaN");
			} else if (Float.class.isAssignableFrom(valueClass)) {
				return (V) new Float("NaN");
			}
		}
		return (V) parseToObject(valueClass, value);
	}

	private String genarateKey(Object key) {
		return prefix + "_" + instanceName + "_" + JSON.toJSONString(key);
	}

	public boolean containsKey(Object key) {
		return RepositoryFactory.findValueByKey(genarateKey(key)) != null;
	}

	public static class Entry<K, V> {
		private KeyValueFormatData<K, V> instance;
		private K key;

		public Entry(KeyValueFormatData<K, V> instance, K perKey) {
			this.key = perKey;
			this.instance = instance;
		}

		public K getKey() {
			return this.key;
		}

		public V getValue() {
			return (V) instance.getValue(key);
		}

		@SuppressWarnings("rawtypes")
		public boolean equals(Object o) {
			if (o instanceof Entry) {
				String oKey = ((Entry) o).instance.instanceName + "_" + ((Entry) o).getKey();
				return oKey.equals(instance.instanceName + "_" + key);
			}
			return false;
		}
	}

	public Set<Entry<K, V>> entrySet() {
		return this.entries;
	}
}
