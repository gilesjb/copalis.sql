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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A base interface for {@link Connection} wrappers.
 * <p/>
 * Every method in an interface derived from {@link Session} should be annotated
 * as either a {@link Query} or {@link Update},
 * and follow the documented requirements for that annotation
 * 
 * @author gilesjb
 * @see java.sql.Connection
 */
public interface Session {

	/**
	 * Specifies a SQL {@code SELECT} statement to be executed
	 * when the annotated method is invoked.
	 * The annotated method must be a member of an interface that extends {@link Session}
	 * <p>
	 * The return type of the method must confirm to one of the following cases:
	 * <ol>
	 * <li>
	 * <h3>Return selected value</h3>
	 * If {@link #value} is a full {@code SELECT} statement with a single-field result set,
	 * the method can return the value directly.
	 * The return type must be assignable from the selected field, eg:
	 * <pre>
	 * {@code @}Query("select SALES from COFFEES where COF_NAME = $1")
	 *    Integer coffeeSales(String name);
	 * </pre>
	 * When invoked, the method will return the {@code SALES} value
	 * from the first selected record,
	 * or {@code null} if the results are empty.
	 * </li>
	 * <li>
	 * <h3>Return Results</h3>
	 * If it is necessary to retrieve multiple records and/or fields,
	 * the method's return type should be an interface that extends {@link Results},
	 * and that contains property methods for fields which
	 * need to be read or updated.
	 * <p>
	 * When the method is invoked,
	 * it will return an instance of its return type
	 * that wraps the selected result set.
	 * 
	 * <h4>Field name inference</h4>
	 * If the statement in {@link #value} does not begin with {@code "SELECT "},
	 * then a {@code "SELECT <field-names> FROM"} clause will be prepended to it,
	 * with field-names inferred from the property names declared in
	 * the {@link Results} return type.
	 * </li>
	 * </ol>
	 * 
	 * @see Results
	 */
	@Documented @Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
	public @interface Query {
		
		/**
		 * The text of the {@code SELECT} statement.
		 * <p>
		 * Statement parameters are indicated with a $ sign followed by one or more numeric digits,
		 * <pre>$1, $2, $3 ...</pre>
		 * The number indicates the 1-based index of the method parameter which will supply
		 * the value for this statement parameter
		 */
	    String value();
	    
	    /**
	     * A result set type, must be one of:
	     * <ul>
	     * <li>{@link java.sql.ResultSet#TYPE_FORWARD_ONLY TYPE_FORWARD_ONLY} (the default)</li>
	     * <li>{@link java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE TYPE_SCROLL_INSENSITIVE}</li>
	     * <li>{@link java.sql.ResultSet#TYPE_SCROLL_SENSITIVE TYPE_SCROLL_SENSITIVE}</li>
	     * </ul>
	     */
	    int type() default ResultSet.TYPE_FORWARD_ONLY;
	    
	    /**
	     * Gives the JDBC driver a hint as to the number of rows that should 
	     * be fetched from the database when more rows are needed.
	     * If the value specified is zero, then the hint is ignored.
	     * The default value is zero.
	     * 
	     * @see java.sql.Statement#setFetchSize(int)
	     */
	    int fetchSize() default 0;
	}
	
	/**
	 * Specifies one or more SQL {@code INSERT}, {@code UPDATE} or {@code DELETE}
	 * commands to be executed when the annotated method is invoked.
	 * The annotated method must be a member of an interface that extends {@link Session}
	 * <p>
	 * The method return type may be {@code void} or {@code int};
	 * if it is {@code int},
	 * the returned value will be the total updated row count
	 */
	@Documented @Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
	public @interface Update {
		
		/**
		 * An array of one or more SQL {@code INSERT}, {@code UPDATE}
		 * or {@code DELETE} commands
		 * <p>
		 * Statement parameters are indicated with a $ sign followed by one or more numeric digits,
		 * <pre>$1, $2, $3 etc</pre>
		 * The number indicates the 1-based index of the method parameter which will supply
		 * the value for this statement parameter
		 */
		String[] value();
		
		/**
		 * Enables the return of auto-generated keys.
		 * This value is ignored for all but INSERT commands.
		 * @return a boolean indicating whether auto-generated keys should be returned
	     * 
	     * @see Connection#prepareStatement(String, int)
		 */
		boolean returnGeneratedKey() default false;
	}

	/**
	 * An interface for operations to be executed with a {@link Session}.
	 * 
	 * @param <C> the {@link Session} interface this accepts
	 */
	public interface Command<C extends Session> {
		/**
		 * The operations to be performed on the {@link Session}
		 *  
		 * @param session a connection for performing queries and updates on the database
		 * @throws SQLException
		 */
		void execute(C session) throws SQLException;
	}

	/**
	 * Gets the JDBC {@link Connection} that this object is wrapping
	 * 
	 * @return a {@link Connection}
	 */
	Connection connection();
	
	/**
	 * Calls {@link Connection#close()} on the underlying connection

	 * @throws DataException
	 */
	void close() throws DataException;
}
