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
package org.copalis.sql.results;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.copalis.sql.Results;
import org.copalis.sql.Results.As;
import org.copalis.sql.common.FieldType;
import org.copalis.sql.common.Name;

/**
 * Represents a property declared in an interface with setter and/or getter methods,
 * where a getter declaration is of the form
 * <code><i>type method_name</i>();</code>
 * and a setter declaration is of the form
 * <code>void <i>method_name</i>(<i>type</i> value);</code>
 * <p>
 * The existence of a setter and/or getter method will result in the creation
 * of a <code>Property</code> with name <code><i>method_name</i></code>,
 * unless a <code>@Name</code> annotation is used,
 * in which case the annotion value is taken as the property name
 * 
 * @see org.copalis.sql.Results.As
 */
public class ResultsProperty {
	
	private enum Type {
		INHERITED,
		GETTER {
			@Override public ResultsProperty property(String name, Method method, ResultsProperty existing) {
				return new ResultsProperty(name, method, existing == null? null : existing.setter());
			}
		},
		SETTER {
			@Override public ResultsProperty property(String name, Method method, ResultsProperty existing) {
				return new ResultsProperty(name, existing == null? null : existing.getter(), method);
			}
		},
		QUALIFIER;
		
		ResultsProperty property(String name, Method method, ResultsProperty existing) {
			return existing;
		}
		
		static Type of(Method method) {
			if (method.getDeclaringClass().isAssignableFrom(Results.class)) return INHERITED;

			int args = method.getParameterTypes().length;
			Class<?> returns = method.getReturnType();
			boolean isResults = Results.class.isAssignableFrom(returns);
			
			if (args == 0 && returns != void.class && !isResults) return GETTER;
			if (args == 1 && (returns == void.class || isResults)) return SETTER;
			if (args == 0 && returns.isInterface() && isResults) return QUALIFIER;

			throw new IllegalArgumentException(Name.of(method) + " is not a valid Results method");
		}
	}

	private final Method getter, setter;
	private final String name;
	
	public Method getter() {
		return getter;
	}
	
	public void validateTypes(Class<?> type) {
		final Class<?> boxed = FieldType.wrapperType(type);
		
		if (getter != null) {
			Class<?> gets = getter.getReturnType();
			if (!FieldType.wrapperType(gets).isAssignableFrom(boxed)) {
				throw new ClassCastException(Name.of(getter) + " return type incompatible with \"" + name + 
						"\" type: " + Name.of(type));
			}
		}
		if (setter != null) {
			Class<?> sets = setter.getParameterTypes()[0];
			if (!boxed.isAssignableFrom(FieldType.wrapperType(sets))) {
				throw new ClassCastException(Name.of(setter) + " parameter type incompatible with \"" + name + 
						"\" type: " + Name.of(type));
			}
		}
	}
	
	public Method setter() {
		return setter;
	}
	
	public String name() {
		return name;
	}
	
	ResultsProperty(String name, Method getter, Method setter) {
		this.name = name;
		if (getter != null && setter != null && getter.getReturnType() != setter.getParameterTypes()[0]) {
			throw new IllegalArgumentException("Getter for \"" + name + "\" must return same type that setter accepts");
		}
		this.getter = getter;
		this.setter = setter;
	}
	
	public static ResultsProperty[] properties(Class<? extends Results> type) {
		Map<String, ResultsProperty> map = new HashMap<String, ResultsProperty>();
		
		for (Method method : type.getMethods()) {
			String name = asName(method);
			ResultsProperty property = Type.of(method).property(name, method, map.get(name));
			if (property != null) map.put(name, property);
		}
		
		return map.values().toArray(new ResultsProperty[0]);
	}
	
	public static Method[] subResults(Class<? extends Results> type) {
		List<Method> methods = new LinkedList<Method>();
		
		for (Method method : type.getMethods()) {
			if (Type.of(method) == Type.QUALIFIER) {
				methods.add(method);
			}
		}
		
		return methods.toArray(new Method[0]);
	}
	
	public static String asName(Method method) {
		As annot = method.getAnnotation(As.class);
		return annot != null? annot.value() : method.getName();
	}
}
