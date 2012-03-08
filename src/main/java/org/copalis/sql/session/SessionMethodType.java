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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import org.copalis.sql.Results;
import org.copalis.sql.Session.Query;
import org.copalis.sql.Session.Update;
import org.copalis.sql.common.FieldType;
import org.copalis.sql.common.Finalizer;
import org.copalis.sql.common.Name;
import org.copalis.sql.common.ParameterizedStatement;
import org.copalis.sql.results.PropertiesResultSetWrapper;
import org.copalis.sql.results.ResultSetWrapper;
import org.copalis.sql.results.SelectResultSetWrapper;

public enum SessionMethodType {
	SIMPLE_SELECT {
		protected SessionMethodHandler.Binder create(Method method, Connection connection) throws SQLException {
			final Query query = method.getAnnotation(Query.class);
			Class<?> ret = method.getReturnType();
			if (query == null || Results.class.isAssignableFrom(ret)) return null;
			
			ParameterizedStatement ps = new ParameterizedStatement(query.value());
			ResultSetMetaData meta = ps.prepare(connection, method.getParameterTypes()).getMetaData();
			if (meta.getColumnCount() != 1 || !ret.isAssignableFrom(FieldType.forClassName(meta.getColumnClassName(1)))) {
				throw new IllegalArgumentException("Illegal query result type");
			}
			return super.queryMethod(method, query, ps, new ResultSetWrapper<Object>() {
				public Object wrap(ResultSet results) {
					Finalizer handler = new Finalizer();
					try {
						return results.next()? results.getObject(1) : null;
					} catch (Exception e) {
						throw handler.wrap(e);
					} finally {
						handler.close(results);
					}
				}
			});
		}
	},
	SELECT {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		protected SessionMethodHandler.Binder create(final Method method, Connection connection) throws SQLException {
			final Query query = method.getAnnotation(Query.class);
			Class<?> ret = method.getReturnType();
			if (query == null || !query.value().toUpperCase().startsWith("SELECT ")) return null;
			
			ParameterizedStatement ps = new ParameterizedStatement(query.value());
			return super.queryMethod(method, query, ps, new SelectResultSetWrapper(
					ret, ps.prepare(connection, method.getParameterTypes()).getMetaData()));
		}
	},
	INFERRED_SELECT {
		@SuppressWarnings("unchecked")
		protected SessionMethodHandler.Binder create(final Method method, Connection connection) throws SQLException {
			Query query = method.getAnnotation(Query.class);
			Class<?> ret = method.getReturnType();
			if (query == null || !Results.class.isAssignableFrom(ret)) return null;
			
			PropertiesResultSetWrapper<?> proxy = PropertiesResultSetWrapper.forType((Class<Results>) ret);
			ParameterizedStatement ps = new ParameterizedStatement(proxy.getSQL(query.value()));
			return super.queryMethod(method, query, ps, 
					proxy.validate(ps.prepare(connection, method.getParameterTypes()).getMetaData()));
		}
	},
	UPDATE {
		protected SessionMethodHandler.Binder create(final Method method, Connection connection) throws SQLException {
			final Update update = method.getAnnotation(Update.class);
			if (update == null) return null;

			final Class<?> ret = method.getReturnType();
			if (ret != void.class && ret != int.class && !update.returnGeneratedKey()) {
				throw new IllegalArgumentException("Illegal return type for Update");
			}
			
			final ParameterizedStatement[] ps = new ParameterizedStatement[update.value().length];
			for (int i = 0; i < ps.length; i++) {
				ps[i] = new ParameterizedStatement(update.value()[i]);
				ps[i].prepare(connection, method.getParameterTypes());
			}
			final String name = name();
			
			return new SessionMethodHandler.Binder() {
				public SessionMethodHandler bind(Connection connection) throws SQLException {
					final PreparedStatement[] stmts = new PreparedStatement[ps.length];
					for (int i = 0; i < ps.length; i++) {
						stmts[i] = connection.prepareStatement(ps[i].text(), update.returnGeneratedKey()?
								Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
					}
					return new SessionMethodHandler() {
						public Object execute(Object[] args) throws SQLException {
							int rows = 0;
							PreparedStatement last = null;
							for (int i = 0; i < ps.length; i++) {
								last = ps[i].setParameters(stmts[i], args);
								rows += last.executeUpdate();
							}
							if (update.returnGeneratedKey()) {
								if (ps.length == 0) return null;
								ResultSet keys = last.getGeneratedKeys();
								if (ret == ResultSet.class) return keys;
								keys.next();
								return keys.getObject(1);
							}
							return rows;
						}
					};
				}
				
				@Override public String toString() {
					return name + ' ' + Name.of(method) + ": " + 
							(ps.length == 1? ps[0].toString() : Arrays.asList(ps).toString());
				}
			};
		}
	};

	protected abstract SessionMethodHandler.Binder create(Method method, Connection connection) throws SQLException;
	
	private SessionMethodHandler.Binder queryMethod(final Method method, final Query query,
			final ParameterizedStatement ps, final ResultSetWrapper<?> wrapper) {
		return new SessionMethodHandler.Binder() {
			public SessionMethodHandler bind(Connection connection) throws SQLException {
				final PreparedStatement stmt = connection.prepareStatement(
						ps.text(), query.type(),
						Results.Updatable.class.isAssignableFrom(method.getReturnType())?
								ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
				stmt.setFetchSize(query.fetchSize());
				return new SessionMethodHandler() {
					public Object execute(Object[] args) throws SQLException {
						return wrapper.wrap(ps.setParameters(stmt, args).executeQuery());
					}
				};
			}
			
			@Override public String toString() {
				return SessionMethodType.this.toString() + ' ' + Name.of(method) + ": " + ps.toString();
			}
		};
	}
	
	@Override public String toString() {
		return name();
	}
	
	public static SessionMethodHandler.Binder forMethod(Method method, Connection connection) throws SQLException {
		for (SessionMethodType gen : values()) {
			try {
				SessionMethodHandler.Binder dm = gen.create(method, connection);
				if (dm != null) return dm;
			} catch (RuntimeException e) {
				throw new RuntimeException(gen.name() + ' ' + Name.of(method) + ": " + e.getMessage(), e);
			} catch (SQLException e) {
				throw new SQLException(gen.name() + ' ' +  Name.of(method) + ": " + e.getMessage(), e);
			}
		}
		throw new IllegalArgumentException(Name.of(method) + ": Not a valid Session method");
	}
}