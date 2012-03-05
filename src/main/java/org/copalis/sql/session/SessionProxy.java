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
package org.copalis.sql.session;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.copalis.sql.DataException;
import org.copalis.sql.Session;
import org.copalis.sql.common.Name;

public class SessionProxy implements InvocationHandler, Session {
	private final Connection connection;
	private final Map<Method, SessionMethod.Binder> methods;

	private final Map<Method, SessionMethod> executors = 
			new HashMap<Method, SessionMethod>();
	
	public static <C extends Session> C proxy(
			Class<C> type, Map<Method, SessionMethod.Binder> methods, Connection connection) {
		return type.cast(Proxy.newProxyInstance(
				SessionProxy.class.getClassLoader(), new Class<?>[] {type}, new SessionProxy(
						connection, methods)));
	}
	
	public SessionProxy(Connection connection) {
		this(connection, null);
	}
	
	public SessionProxy(Connection connection, Map<Method, SessionMethod.Binder> methods) {
		this.connection = connection;
		this.methods = methods;
	}
	
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass().isAssignableFrom(Session.class)) {
			return method.invoke(this, args);
		} else {
			SessionMethod exec = executors.get(method);
			try {
				if (exec == null) {
					exec = methods.get(method).bind(connection);
					executors.put(method, exec);
				}
				return exec.execute(args);
			} catch (SQLException e) {
				throw DataException.wrap("In method: " + Name.of(method), e);
			}
		}
	}
	
	public Connection connection() {
		return connection;
	}
	
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			throw DataException.wrap(e);
		}
	}
	
	@Override public String toString() {
		return methods.values().toString();
	}
}