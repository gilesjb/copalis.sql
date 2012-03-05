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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.copalis.sql.Session;

/**
 * An interface for a factory that creates {@link Session} wrappers for {@link Connection}s
 *
 * @author gilesjb
 */
public interface ConnectionWrapper<X extends Session> {
	
	X wrap(Connection connection);

	public class BasicSession implements ConnectionWrapper<Session> {
		public Session wrap(Connection connection) {
			return new SessionProxy(connection);
		}
	}
	
	public class Generic<T extends Session> implements ConnectionWrapper<T> {
		private final Map<Method, SessionMethod.Binder> methods = new HashMap<Method, SessionMethod.Binder>();
		private final Class<T> type;
		
		public Generic(Class<T> type, Connection connection) throws SQLException {
			this.type = type;
			for (Method method : type.getMethods()) {
				if (method.getDeclaringClass() != Session.class) {
					methods.put(method, SessionMethodType.forMethod(method, connection));
				}
			}
		}
		
		public T wrap(Connection connection) {
			return SessionProxy.proxy(type, methods, connection);
		}
	}
}
