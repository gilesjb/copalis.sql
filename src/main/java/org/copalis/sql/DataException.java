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

import java.sql.SQLException;

/**
 * An unchecked exception for wrapping {@link SQLException}.
 * 
 * @author gilesjb
 * @see SQLException
 */
public class DataException extends RuntimeException {
	private static final long serialVersionUID = -2133067948978213900L;

	private final SQLException cause;
	
	public static DataException wrap(SQLException e) {
		return new DataException(e);
	}
	
	public static DataException wrap(String message, SQLException e) {
		return new DataException(message, e);
	}
	
	@Override public SQLException getCause() {
		return cause;
	};

	private DataException(SQLException ex) {
		super(ex);
		cause = ex;
	}
	
	private DataException(String message, SQLException ex) {
		super(message, ex);
		cause = ex;
	}
}
