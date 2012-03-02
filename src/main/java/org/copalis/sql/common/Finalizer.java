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
import java.sql.ResultSet;
import java.sql.SQLException;

import org.copalis.sql.DataException;

/**
 * @author gilesjb
 *
 */
public class Finalizer {
	
	private SQLException exception = null;
	private boolean active = true;
	
	public RuntimeException wrap(Exception except) {
		try {
			throw except;
		} catch (DataException ex) {
			exception = ex.getCause();
			return ex;
		} catch (SQLException ex) {
			exception = ex;
			return DataException.wrap(ex);
		} catch (RuntimeException ex) {
			active = false;
			return ex;
		} catch (Exception ex) {
			active = false;
			return new RuntimeException(ex);
		}
	}
	
	interface Operation {
		void run() throws SQLException;
	}
	
	public void operate(Operation oper)	{
		if (active) {
			try {
				oper.run();
			} catch (SQLException ex) {
				if (exception != null) exception.setNextException(ex);
				exception = ex;
			}
		}
	}
	
	public void close(final ResultSet results) {
		operate(new Operation() {
			public void run() throws SQLException {
				results.close();
			}});
	}
	
	public void close(final Connection connection) {
		operate(new Operation() {
			public void run() throws SQLException {
				connection.close();
			}});
	}
	
	public void rollback(final Connection connection) {
		operate(new Operation() {
			public void run() throws SQLException {
				connection.rollback();
			}});
	}

	public void setAutoCommit(final Connection connection, final boolean commit) {
		operate(new Operation() {
			public void run() throws SQLException {
				connection.setAutoCommit(commit);
			}});
	}
}
