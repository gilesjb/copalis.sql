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
import java.util.Map;

import org.copalis.sql.Results;
import org.copalis.sql.common.FieldType;

/**
 * @author gilesjb
 *
 */
public class SelectResultSetWrapper<C extends Results> implements ResultSetWrapper<C> {
	
	private final Class<C> type;
	private final Map<Method, ResultsMethodHandler> handlers;
	
	public SelectResultSetWrapper(Class<C> type, ResultSetMetaData meta) throws SQLException {
		if (ResultsProperty.subResults(type).length > 0) {
			throw new IllegalArgumentException("Result type must not have qualifier methods");
		}
		this.type = type;
		this.handlers = new HashMap<Method, ResultsMethodHandler>();
		
		ResultsProperty[] properties = ResultsProperty.properties(type);

		Map<String, Integer> cols = new HashMap<String, Integer>();
		for (int i = 1, c = meta.getColumnCount(); i <= c; i++) {
			cols.put(meta.getColumnLabel(i).toLowerCase(), i);
		}
		
		for (final ResultsProperty property : properties) {
			final Integer i = cols.get(property.name.toLowerCase());
			if (i == null) {
				throw new IllegalArgumentException("No column named: " + property.name);
			}
			
			property.validateTypes(FieldType.forClassName(meta.getColumnClassName(i)));
			
			if (property.getter != null) {
				handlers.put(property.getter, ResultsMethodHandler.getter(i, property.name));
			}
			
			if (property.setter != null) {
				handlers.put(property.setter, ResultsMethodHandler.setter(i));
			}
		}
	}

	public C wrap(ResultSet results) {
		return ResultsProxy.proxy(type, results, handlers);
	}
}
