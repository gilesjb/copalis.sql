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

import com.sun.tools.example.debug.bdi.MethodNotFoundException;

/**
 * A general class for implementing a Results proxy.
 *
 * @author gilesjb
 */
public class ResultsProxyInvoker implements InvocationHandler, Results {

	public interface Handler {
		Object invoke(ResultSet results, Object proxy, Object[] args) throws SQLException;
		String toString(ResultSet results) throws SQLException;
		
		public interface Factory {
			Handler create(ResultSet results);
		}
	}
	
	public static class Getter implements Handler {
		private final String name;
		private final int column;
		
		public Getter(String name, int column) {
			this.name = name;
			this.column = column;
		}
		
		public Object invoke(ResultSet results, Object proxy, Object[] args) throws SQLException {
			return results.getObject(column);
		}
		
		public String toString(ResultSet results) throws SQLException {
			return name + ": " + results.getObject(column);
		}
	}
	
	public static class Setter implements Handler {
		private final int column;
		
		public Setter(int column) {
			this.column = column;
		}

		public Object invoke(ResultSet results, Object proxy, Object[] args) throws SQLException {
			results.updateObject(column, args[0]);
			return proxy;
		}
		
		public String toString(ResultSet results) {
			return null;
		}
	}
	
	public static <T extends Results> T proxy(Class<T> type, ResultSet results, Map<Method, Handler> handlers) {
		Map<Method, Handler.Factory> factories = Collections.emptyMap();
		return type.cast(Proxy.newProxyInstance(
				type.getClassLoader(), new Class<?>[] {type}, new ResultsProxyInvoker(results, handlers, factories)));
	}
	
	public static <T extends Results> T proxy(
			Class<T> type, ResultSet results, Map<Method, Handler> handlers, Map<Method, Handler.Factory> factories) {
		return type.cast(Proxy.newProxyInstance(
				type.getClassLoader(), new Class<?>[] {type}, new ResultsProxyInvoker(results, handlers, factories)));
	}
	
	private final ResultSet results;
	private final Map<Method, Handler> handlers, lazy;
	private final Map<Method, Handler.Factory> factories;
	
	private ResultsProxyInvoker(
			ResultSet results, Map<Method, Handler> handlers, Map<Method, Handler.Factory> factories) {
		this.results = results;
		this.handlers = handlers;
		this.lazy = new HashMap<Method, Handler>();
		this.factories = factories;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass().isAssignableFrom(Results.class)) return method.invoke(this, args);
		
		try {
			return handler(method).invoke(results, proxy, args);
		} catch (SQLException e) {
			throw DataException.wrap("In method: " + Name.of(method), e);
		}
	}
	
	private Handler handler(Method method) throws MethodNotFoundException {
		if (handlers.containsKey(method)) return handlers.get(method);
		if (lazy.containsKey(method)) return lazy.get(method);
		if (factories.containsKey(method)) {
			Handler handler = factories.get(method).create(results);
			lazy.put(method, handler);
			return handler;
		}
		throw new MethodNotFoundException(Name.of(method));
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
			} catch (MethodNotFoundException e) {
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

