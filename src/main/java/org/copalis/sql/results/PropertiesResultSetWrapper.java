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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.copalis.sql.DataException;
import org.copalis.sql.Results;
import org.copalis.sql.common.FieldType;

/**
 * Maintains information necessary to wrap a <code>ResultSet</code> with a <code>Result</code> interface
 *
 * @author gilesjb
 * @param <T> the <code>Result</code> interface
 */
public class PropertiesResultSetWrapper<T extends Results> implements ResultSetWrapper<T> {

	private interface Validator {
		void validate(ResultSetMetaData meta) throws SQLException;
	}

	private final Class<T> type;
	
	private final ResultsProperty[] properties;
	private final Method[] subResults;
	private final int finalIdx;
	
	private final Map<Method, PropertiesResultSetWrapper<?>> subProxies = new LinkedHashMap<Method, PropertiesResultSetWrapper<?>>();
	private final Map<Method, ResultsMethodHandler> handlers = new HashMap<Method, ResultsMethodHandler>();
	private final Map<Method, ResultsMethodHandler.Factory> factories = new HashMap<Method, ResultsMethodHandler.Factory>();
	private final List<Validator> validators = new LinkedList<Validator>();
	
	public static <C extends Results> PropertiesResultSetWrapper<C> forType(Class<C> type) {
		return new PropertiesResultSetWrapper<C>(type, 1);
	}
	
	/**
	 * Stores property and child info about the type
	 * @param type
	 */
	PropertiesResultSetWrapper(Class<T> type, int idx) {
		
		this.type = type;
		
		this.properties = ResultsProperty.properties(type);
		this.subResults = ResultsProperty.subResults(type);
		
		for (ResultsProperty property : properties) {
			addHandlers(property, idx++);
		}
		
		for (final Method sub : subResults) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
            final PropertiesResultSetWrapper child = new PropertiesResultSetWrapper(sub.getReturnType(), idx);
			subProxies.put(sub, child);
			factories.put(sub, new ResultsMethodHandler.Factory() {
				public ResultsMethodHandler create(ResultSet results) {
					final Results wrapped = child.wrap(results);
					
					return new ResultsMethodHandler() {
						public Object invoke(ResultSet results, Object proxy, Object[] args) throws SQLException {
							return wrapped;
						}

						public String toString(ResultSet results) throws SQLException {
							return ResultsProperty.asName(sub) + ": {" + wrapped.toString() + '}';
						}
					};
				}
			});
			
			validators.add(new Validator() {
				public void validate(ResultSetMetaData meta) throws SQLException {
					child.validate(meta);
				}
			});
			idx = child.finalIdx;
		}
		this.finalIdx = idx;
	}
	
	private void addHandlers(final ResultsProperty property, final int index) {
		validators.add(new Validator() {
			public void validate(ResultSetMetaData meta) throws SQLException {
				property.validateTypes(FieldType.forClassName(meta.getColumnClassName(index)));
			}
		});
		
		property.createMethodHandlers(handlers, index);
	}
	
	/**
	 * Accepts a string which should consist of a partial SQL select statement,
	 * and prepends it with "SELECT <i>fields</i> FROM",
	 * where <i>fields</i> comprise the properties of this proxy
	 * 
	 * @param query a partial SQL select statement,
	 * omitting the SELECT <i>fields</i> FROM clause
	 * @return <b>query</b> prepended with "SELECT <i>fields</i> FROM",
	 * where <i>fields</i> is the comma-delimited list of property names
	 */
	public String getSQL(String query) {
		if (query.toUpperCase().startsWith("SELECT ")) {
			throw new IllegalArgumentException("Query already contain SELECT: " + query);
		}
		
		StringBuilder sql = new StringBuilder("SELECT ");
		propertyNames(sql, "");
		return sql.append(" FROM ").append(query).toString();
	}
	
	private void propertyNames(StringBuilder str, String prefix) {
		String separator = "";
		for (ResultsProperty property : properties) {
			str.append(separator).append(prefix).append(property.name);
			separator = ",";
		}
		for (Map.Entry<Method, PropertiesResultSetWrapper<?>> entry : subProxies.entrySet()) {
			str.append(separator);
			entry.getValue().propertyNames(str, ResultsProperty.asName(entry.getKey()) + '.');
			separator = ",";
		}
	}
	
	/**
	 * Sets up handlers for property and child methods.
	 * @param meta
	 * @return a ResultSetWrapper
	 */
	public ResultSetWrapper<T> validate(ResultSetMetaData meta) {
		try {
			for (Validator validator : validators) {
				validator.validate(meta);
			}
		} catch (SQLException e) {
			throw DataException.wrap(e);
		}
		return this;
	}
	
	/**
	 * Creates a new instance of the T interface
	 * @param results a ResultSet
	 * @return a dynamic proxy implementing the interface
	 */
	public T wrap(final ResultSet results) {
		return ResultsProxy.proxy(type, results, handlers, factories);
	}
}
