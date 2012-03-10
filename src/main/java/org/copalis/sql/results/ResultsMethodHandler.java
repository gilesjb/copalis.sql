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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A handler that executes a user-declared Results method
 *
 * @author gilesjb
 */
public interface ResultsMethodHandler {

    Object invoke(ResultSet results, Object proxy, Object[] args) throws SQLException;
	String toString(ResultSet results) throws SQLException;
	
	public interface Factory {
		ResultsMethodHandler create(ResultSet results);
	}
}