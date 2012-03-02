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
package org.copalis.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.ResultSet;

/**
 * A base interface for query results.
 * Each instance wraps a {@link ResultSet}. 
 * <p>
 * Every method in an interface derived from {@link Results}
 * must be a <i>Getter</i>, <i>Setter</i> or <i>Qualifier</i>.
 * <p>
 * Unless it has an {@link As} annotation,
 * the name of a method indicates the name of the property it refers to.
 * Getters and setters do <i>not</i> use the JavaBean convention
 * of prefixing a property name with {@code get}, {@code set} or {@code is} to indicate the action.
 * Instead, a method's behavior is determined by its return type and parameters:
 * <p>
 * <table>
 * <tr>
 * <th>Method action</th><th>Return type</th><th>Parameters</th>
 * <tr><td>Setter</td><td>{@code void} or <i>declaring-interface</i></td>
 * <td>{@code (}<i>field-type</i>{@code value)}</td></tr>
 * <tr><td>Getter</td><td><i>field-type</i></td><td>{@code ()}</td></tr>
 * <tr><td>Qualifier</td><td><i>{@link Results} interface</i></td><td>{@code ()}</td></tr>
 * </table>
 * <p>
 * A <i>qualifier</i> is a special type of getter that returns another {@link Results} interface.
 * The returned results wrap the same result set,
 * but each field name will implicitly be prefixed with
 * the qualifier's property name and a '.' character.
 * This is useful for referencing fully-qualified field names in a multi-table join.
 * <p>
 * A setter that returns its declaring interface will return the object it was invoked on;
 * this allows chained setter calls.
 * <p>
 * The <i>field-type</i> of a getter must be assignable from the field column class,
 * and a setter's <i>field-type</i> must be assignable to the field column class.
 * Also, if both getter and setter methods are declared for a field,
 * their <i>field-type</i>s must be the same.
 * 
 * @author gilesb
 * @see java.sql.ResultSet
 * @see java.sql.ResultSetMetaData#getColumnClassName(int column)
 */
public interface Results {

	/**
	 * Specifies the property name of a method.
	 * The annotated method must be a member of an interface that extends {@link Results}.
	 * <p>
	 * The property name may correspond to a field name or a field name qualifier.
	 * <p>
	 * If this annotation is not present, the method's property name is the actual method name.
	 * 
	 * @see java.lang.reflect.Method#getName()
	 */
	@Documented @Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
	public @interface As {
		/**
		 * Property name for an annotated method
		 * @return the property name
		 */
	    String value();
	}

	/**
	 * Gets the {@link ResultSet} wrapped by this {@link Results} object
	 * 
	 * @return The result set underlying this interface
	 */
	ResultSet results();
	
	/**
	 * Moves the cursor one row forward by calling {@link ResultSet#next()}
	 * on the underlying result set
	 * 
	 * @return true if the new current row is valid
	 * @throws DataException
	 */
	boolean next() throws DataException;
	
	/**
	 * Updates the database with the new contents of the current row
	 * by calling {@link ResultSet#updateRow()}
	 * on the underlying result set
	 * 
	 * @throws DataException
	 */
	void updateRow() throws DataException;
	
	/**
	 * Closes the underlying {@link ResultSet}
	 * 
	 * @throws DataException
	 */
	void close() throws DataException;
}
