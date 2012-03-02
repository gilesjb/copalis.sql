/*
 *  Copyright 2012 Giles Burgess
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.copalis.sql.common;

public class FieldType {
	
	public static Class<?> wrapperType(String name) {
		return wrapperType(forClassName(name));
	}
	
	public static Class<?> wrapperType(Class<?> type) {
		if (!type.isPrimitive()) return type;
		if (type == byte.class) return Byte.class;
		if (type == short.class) return Short.class;
		if (type == char.class) return Character.class;
		if (type == int.class) return Integer.class;
		if (type == long.class) return Long.class;
		if (type == float.class) return Float.class;
		if (type == double.class) return Double.class;
		return Void.class;
	}
	
	public static Class<?> forClassName(String name) {
		try {
			return "[B".equals(name)? byte[].class : Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
