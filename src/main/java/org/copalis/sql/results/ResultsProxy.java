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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.copalis.sql.DataException;
import org.copalis.sql.Results;
import org.copalis.sql.common.Name;
import org.copalis.sql.results.ResultsMethodHandler.Factory;


/**
 * A general class for implementing a Results proxy.
 *
 * @author gilesjb
 */
public class ResultsProxy implements InvocationHandler, Results, Results.Updatable {

	public static <T extends Results> T proxy(Class<T> type, ResultSet results, Map<Method, ResultsMethodHandler> handlers) {
		return type.cast(Proxy.newProxyInstance(
				type.getClassLoader(), new Class<?>[] {type}, new ResultsProxy(
				        results, handlers, Collections.<Method, Factory>emptyMap())));
	}
	
	public static <T extends Results> T proxy(
			Class<T> type, ResultSet results, Map<Method, ResultsMethodHandler> handlers, Map<Method, ResultsMethodHandler.Factory> factories) {
		return type.cast(Proxy.newProxyInstance(
				type.getClassLoader(), new Class<?>[] {type}, new ResultsProxy(results, handlers, factories)));
	}
	
	private final ResultSet results;
	private final Map<Method, ResultsMethodHandler> handlers;
	private final Map<Method, ResultsMethodHandler.Factory> factories;
	
	private ResultsProxy(
			ResultSet results, Map<Method, ResultsMethodHandler> handlers, Map<Method, ResultsMethodHandler.Factory> factories) {
		this.results = results;
		this.handlers = new HashMap<Method, ResultsMethodHandler>(handlers);
		this.factories = factories;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass().isAssignableFrom(Results.Updatable.class)) {
		    return method.invoke(this, args);
		}
		
		try {
			return handler(method).invoke(results, proxy, args);
		} catch (SQLException e) {
			throw DataException.wrap("In method: " + Name.of(method), e);
		}
	}
	
	private ResultsMethodHandler handler(Method method) throws NoSuchMethodException {
		if (handlers.containsKey(method)) return handlers.get(method);
		if (factories.containsKey(method)) {
			ResultsMethodHandler handler = factories.get(method).create(results);
			handlers.put(method, handler);
			return handler;
		}
		throw new NoSuchMethodException(Name.of(method));
	}
	
	public boolean next() {
		try {
			return results.next();
		} catch (SQLException e) {
			throw DataException.wrap(e);
		}
	}

	public void updateRow() {
		try {
			results.updateRow();
		} catch (SQLException e) {
			throw DataException.wrap(e);
		}
	}

	public void close() {
		try {
			results.close();
		} catch (SQLException e) {
			throw DataException.wrap(e);
		}
	}
	
	public ResultSet results() {
		return results;
	}
	
	@Override public String toString() {
		StringBuilder str = new StringBuilder();
		String sep = "";
		
		Set<Method> methods = new HashSet<Method>(handlers.keySet());
		methods.addAll(factories.keySet());

		for (Method method : methods) {
			String text;
			try {
				text = handler(method).toString(results);
			} catch (SQLException e) {
				throw DataException.wrap(e);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			if (text != null) {
				str.append(sep).append(text);
				sep = ", ";
			}
		}

		return str.toString();
	}
}

