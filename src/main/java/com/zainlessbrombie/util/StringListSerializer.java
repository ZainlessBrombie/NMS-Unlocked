package com.zainlessbrombie.util;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Arrays;

/* Created by 'ZainlessBrombie'.
 * You may use, modify and distribute this code freely.
 */

public abstract class StringListSerializer {
	
	private static <T> String doAToString(T o) {
		return o == null ? null : o.toString();
	}

	public static <T> String parseMapToString(Map <String,T> map) {
		return parseMapToString(map,StringListSerializer::doAToString);
	}

	public static <T> String parseMapToString(Map <String,T> map, ValueToString <T> converter) {
		List <String> backList = new LinkedList <String> ();
		for(Map.Entry <String,T> entry : map.entrySet()) {
			backList.add(entry.getKey());
			backList.add(entry.getValue() == null ? null : converter.stringValue(entry.getValue()));
		}
		return parseListToString(backList);
	}

	public static Map <String,String> parseStringToMap(String str, Map <String,String> putInto) {
		List <String> raw = parseStringToList(str);
		if(raw.size() % 2 != 0) throw new IllegalArgumentException("A map must contain the same amount of keys and values");
		for (int i = 0; i < raw.size(); i += 2)
			putInto.put(raw.get(i),raw.get(i + 1));
		return putInto;
	}

	public static Map<String,String> parseStringToMap(String str) {
		return parseStringToMap(str,new HashMap<String,String>());
	}

	public static <T> Map <String,T> parseStringToMap(String str, StringToValue <T> converter, Map <String,T> putInto) {
		List <String> raw = parseStringToList(str);
		if(raw.size() % 2 != 0) throw new IllegalArgumentException("A map must contain the same amount of keys and values");
		for(int i = 0; i < raw.size(); i += 2)
			putInto.put(raw.get(i), converter.value(raw.get(i + 1)) );
		return putInto;
	}


	public static List <String> parseStringToList(String str) {
		return parseStringToList(str,new LinkedList<String>());
	}

	public static List <String> parseStringToList(String str, List <String> putInto) {
		try {
			List <Integer> startingAddresses = new LinkedList<Integer>();
			List <Integer> lengths = new LinkedList<Integer>();

			int address = 0;
			int startingLocation = 0;
			int parsed = 0;
			int i = 0;
			while(str.charAt(i) != '|') {
				if(str.charAt(i) == ';') {
					parsed = Integer.parseInt(str.substring(startingLocation,i));
					lengths.add(parsed);
					startingAddresses.add(address);
					if(parsed >= 0) address = address + parsed;
					startingLocation = i + 1;
				}
				i++;
			}

			String stringSection = str.substring(i + 1, str.length());

			ListIterator<Integer> addressIterator = startingAddresses.listIterator();
			ListIterator<Integer> lengthsIterator = lengths.listIterator();
			int len = 0;
			while(addressIterator.hasNext()) {
				if((len = lengthsIterator.next()) == -1) {
					addressIterator.next();
					putInto.add(null);
					continue;
				}
				address = addressIterator.next();
				putInto.add(stringSection.substring(address,address + len));
			}

			return putInto;

		} catch(Exception e) {
			throw new IllegalArgumentException("The passed list could not be parsed");
		}
	}

	public static <T> List <T> parseStringToList(String str, StringToValue <T> converter) {
		return parseStringToList(str,converter,new LinkedList<T>());
	}

	public static <T> List <T> parseStringToList(String str, StringToValue <T> converter, List <T> putInto) {
		try {
			List <Integer> startingAddresses = new LinkedList<Integer>();
			List <Integer> lengths = new LinkedList<Integer>();

			int address = 0;
			int startingLocation = 0;
			int parsed = 0;
			int i = 0;
			while(str.charAt(i) != '|') {
				if(str.charAt(i) == ';') {
					parsed = Integer.parseInt(str.substring(startingLocation,i));
					lengths.add(parsed);
					startingAddresses.add(address);
					if(parsed >= 0) address = address + parsed;
					startingLocation = i + 1;
				}
				i++;
			}

			String stringSection = str.substring(i + 1, str.length());

			ListIterator<Integer> addressIterator = startingAddresses.listIterator();
			ListIterator<Integer> lengthsIterator = lengths.listIterator();
			int len = 0;
			while(addressIterator.hasNext()) {
				if((len = lengthsIterator.next()) == -1) {
					addressIterator.next();
					putInto.add(null);
					continue;
				}
				address = addressIterator.next();
				putInto.add(converter.value(stringSection.substring(address,address + len)));
			}

			return putInto;

		} catch(Exception e) {
			throw new IllegalArgumentException("The passed list could not be parsed");
		}
	}

	public static <T> String parseListToString(Iterable<T> list,ValueToString <T> converter) {
		StringBuilder builder = new StringBuilder();
		StringBuilder contentBuilder = new StringBuilder();
		String str;
		for(T obj : list) {
			if(obj != null) {
				str = converter.stringValue(obj);
				if(str != null) {
					builder.append(str.length());
					contentBuilder.append(str);
				}
			}
			else builder.append(-1);
			builder.append(';');
		}
		builder.append('|');
		builder.append(contentBuilder);
		return builder.toString();
	}

	public static <T> String parseListToString(Iterable<T> list) {
		return parseListToString(list, StringListSerializer::doAToString);
	}

	public static <T> String parseListToString(T [] list, ValueToString <T> converter) {
		return parseListToString(Arrays.asList(list),converter); //arrays.aslist returns an immutable list, backed by the array so the impact on performance is size independent
	}

	@SuppressWarnings("unchecked")
	public static <T> String parseListToStringUsingConverter(ValueToString <T> converter, T ... list) {
		return parseListToString(Arrays.asList(list),converter);
	}

	@SuppressWarnings("unchecked")
	public static <T> String parseListToString(T ... list) {
		return parseListToString(Arrays.asList(list),StringListSerializer::doAToString); //arrays.aslist returns an immutable list, backed by the array so the impact on performance is size independent
	}



	private interface StringToValue <T> {
		public T value(String asString);
	}

	private interface ValueToString <T> {
		public String stringValue(T of);
	}
}
