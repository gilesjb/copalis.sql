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

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;



/**
 * Maintains the text of a prepared statement and the mapping of source arguments
 * to indexed parameters
 *
 * @author gilesjb
 */
public class ParameterizedStatement {
	
	private static final char ESCAPE = '$';
	private static final int RADIX = 10;

	private final LinkedList<Integer> indexes = new LinkedList<Integer>();
	private final StringBuilder text = new StringBuilder();
	
	/**
	 * State machine for parsing escapes and parameter references
	 */
	private enum Parser {
		START {
			public Parser readChar(char ch, StringBuilder text, LinkedList<Integer> params) {
				if (ch == ESCAPE) return ESCAPED;
				return append(text, ch, START);
			}
		}, 
		ESCAPED {
			public Parser readChar(char ch, StringBuilder text, LinkedList<Integer> params) {
				if (ch == ESCAPE) {
					return append(text, ch, START);
				}
				if (Character.isDigit(ch)) {
					params.add(Character.digit(ch, RADIX));
					return append(text, '?', PARAM);
				}
				throw new IllegalArgumentException("Illegal escaped character: '" + ch + "'");
			}
			@Override public void end() {
				throw new IllegalArgumentException("Incomplete escape code");
			}
		}, 
		PARAM {
			public Parser readChar(char ch, StringBuilder text, LinkedList<Integer> params) {
				if (ch == ESCAPE) return ESCAPED;
				if (Character.isDigit(ch)) {
					params.add(params.removeLast() * RADIX + Character.digit(ch, RADIX));
					return PARAM;
				}
				return append(text, ch, START);
			}
		};
		
		public abstract Parser readChar(char ch, StringBuilder text, LinkedList<Integer> params);
		
		public void end() {}
		
		static Parser append(StringBuilder text, char ch, Parser ps) {
			text.append(ch);
			return ps;
		}
	}
	
	public ParameterizedStatement(String source) {
		Parser parser = Parser.START;
		for (int i = 0, len = source.length(); i < len; i++) {
			try {
				parser = parser.readChar(source.charAt(i), text, indexes);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("At position " + i + " in \"" + source + '"', e);
			}
		}
		parser.end();
	}
	
	public String text() {
		return text.toString();
	}
	
	public List<Integer> indexes() {
		return Collections.unmodifiableList(indexes);
	}
	
	/**
	 * Creates a PreparedStatement and validates it against a method's parameters
	 * @param connection
	 * @param paramTypes
	 * @return a PreparedStatement created from connection
	 * @throws SQLException 
	 * @throws java.lang.IndexOutOfBoundsException
	 * @throws java.lang.IllegalArgumentException
	 */
	public PreparedStatement prepare(Connection connection, Class<?>... paramTypes) throws SQLException {
		try {
			PreparedStatement stmt = connection.prepareStatement(text());
			validate(stmt.getParameterMetaData(), paramTypes);
			return stmt;
		} catch (SQLException e) {
			throw new SQLException("Error in statement: " + text(), e);
		}
	}
	
	/**
	 * Checks that the parameters from a method can applied to a prepared statement,
	 * using this mapping
	 * @param meta
	 * @param paramTypes
	 * @throws java.lang.IndexOutOfBoundsException
	 * @throws java.lang.IllegalArgumentException
	 */
	public void validate(ParameterMetaData meta, final Class<?>... paramTypes) {
		int i = 0;
		for (int idx : indexes) {
			if (idx < 1 || idx > paramTypes.length) {
				throw new IndexOutOfBoundsException("Statement parameter " + ESCAPE + idx + " is out of range 1.." + paramTypes.length);
			}
			if (meta != null) {
				try {
					String className = meta.getParameterClassName(i + 1);
					
					if (!FieldType.wrapperType(className).isAssignableFrom(FieldType.wrapperType(paramTypes[idx - 1]))) {
						throw new ClassCastException(
								"" + ESCAPE + idx + " refers to " + paramTypes[idx - 1].getCanonicalName() +
								" parameter, but field type is " + className);
					}
				} catch (SQLException e) {} // No parameter data available - can't validate
			}
			i++;
		}
	}
	
	public PreparedStatement setParameters(PreparedStatement stmt, Object[] args) throws SQLException {
		int param = 1;
		for (int idx : indexes) {
			stmt.setObject(param++, args[idx - 1]);
		}
		return stmt;
	}
	
	@Override public String toString() {
		return '"' + text.toString() + "\" " + indexes;
	}
}
