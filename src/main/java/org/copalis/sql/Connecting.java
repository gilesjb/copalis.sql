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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.copalis.sql.Session.Command;
import org.copalis.sql.common.Finalizer;
import org.copalis.sql.session.ConnectionWrapper;

/**
 * A factory that instantiates {@link Session} interfaces.
 * The underlying connections are created by delegating to a {@link Connector}
 * <P>
 * This class also contains several static methods for {@code Connecting.to}
 * standard jdbc connection sources
 *  
 * @param <T> the type of {@link Session} that this creates
 * @author gilesjb
 */
public class Connecting<T extends Session> implements Connector {
	
	private final Connector delegate;
	private final ConnectionWrapper<T> wrapper;
	
	private Connecting(Connector delegate, ConnectionWrapper<T> wrapped) {
		this.delegate = delegate;
		this.wrapper = wrapped;
	}

	/**
	 * Creates a {@link Connecting} to the data source,
	 * that creates instances of the desired session interface
	 * 
	 * @param iface a {@link Class} object for an interface derived from {@link Session}
	 * @return a {@link Connecting} that creates instances of iface
	 */
	public <C extends Session> Connecting<C> as(final Class<C> iface) throws DataException {
		if (!iface.isInterface()) throw new IllegalArgumentException(iface.toString() + " is not an interface");

		Connection connection = connect();
		Finalizer handler = new Finalizer();
		try {
			return new Connecting<C>(delegate, new ConnectionWrapper.Generic<C>(iface, connection));
		} catch (Exception e) {
			throw handler.wrap(e);
		} finally {
			handler.close(connection);
		}
	}
	
	public final Connection connect() throws DataException {
		try {
			return delegate.connect();
		} catch (SQLException e) {
			throw DataException.wrap(e);
		}
	}
	
	/**
	 * Opens a {@link Connection} to the database,
	 * and wraps it with an instance of the data connection interface
	 * 
	 * @return a new instance of the data connection interface
	 * @throws DataException
	 */
	public final T open() throws DataException {
		return wrapper.wrap(connect());
	}
	
    /**
     * Opens a {@link Connection} to the database,
     * and wraps it with an instance of the data connection interface
     * 
     * @param iface a {@link Class} object for an interface derived from {@link Session}
     * @return a new instance of the data connection interface
     * @throws DataException
     */
	public final <C extends Session> C open(Class<C> iface) {
	    return as(iface).open();
	}

	/**
	 * Executes a {@link Session.Command}.
	 * 
	 * Creates a database {@link Connection},
	 * wraps it with an instance of the data connection interface,
	 * invokes the {@link Command#execute(Session) execute} method
	 * of the supplied {@link Session.Command},
	 * and finally closes the connection
	 * 
	 * @param command a {@link Session.Command} object that accepts {@link Session}s
	 * created by this {@link Connecting}
	 * @throws DataException wraps any {@link SQLException} that is thrown during execution
	 */
	public final void execute(Session.Command<T> command) throws DataException {
		Connection connection = connect();
		Finalizer handler = new Finalizer();
		try {
			command.execute(open());
		} catch (Exception e) {
			throw handler.wrap(e);
		} finally {
			handler.close(connection);
		}
	}

	/**
	 * Executes a {@link Session.Command} within a transaction.
	 * 
	 * The transaction is rolled back if an exception is thrown during
	 * {@link Command#execute(Session) execute}
	 * 
	 * @param command a {@link Session.Command} object that accepts data connections
	 * created by this {@link Connecting}
	 * @throws DataException wraps any {@link SQLException} that is thrown during execution
	 */
	public final void transact(final Session.Command<T> command) throws DataException {
		execute(new Session.Command<T>() {
			public void execute(T session) throws SQLException {
				Finalizer handler = new Finalizer();
				session.connection().setAutoCommit(false);
				try {
					command.execute(session);
				} catch (Exception e) {
					try {
						throw handler.wrap(e);
					} finally {
						handler.rollback(session.connection());
					}
				} finally {
					handler.setAutoCommit(session.connection(), true);
				}
			}
		});
	}

	/**
	 * Creates a {@link Connecting} that connects to a database by URL.
	 * 
	 * @param url a database URL
	 * @param user the database user
	 * @param password the database user's password
	 * @return a new {@link Connecting}
	 * @see DriverManager#getConnection(String, String, String)
	 */
	public static Connecting<Session> to(final String url, final String user, final String password) {
		return with(new Connector() {
			public Connection connect() throws SQLException {
				return DriverManager.getConnection(url, user, password);
			}
		});
	}

	/**
	 * Creates a {@link Connecting} that connects to a database by URL.
	 * 
	 * @param url a database URL
	 * @return a new {@link Connecting}
	 * @see DriverManager#getConnection(String)
	 */
	public static Connecting<Session> to(final String url) {
		return with(new Connector() {
			public Connection connect() throws SQLException {
				return DriverManager.getConnection(url);
			}
		});
	}

	/**
	 * Creates a {@link Connecting} that connects to a database using a {@link DataSource}.
	 * 
	 * @param datasource a {@link DataSource}
	 * @param user the database user
	 * @param password the database user's password
	 * @return a new {@link Connecting}
	 * @see DataSource#getConnection(String, String)
	 */
	public static Connecting<Session> to(final DataSource datasource, final String user, final String password) {
		return with(new Connector() {
			public Connection connect() throws SQLException {
				return datasource.getConnection(user, password);
			}
		});
	}

	/**
	 * Creates a {@link Connecting} that connects to a database using a {@link DataSource}.
	 * 
	 * @param datasource a {@link DataSource}
	 * @return a new {@link Connecting}
	 * @see DataSource#getConnection()
	 */
	public static Connecting<Session> to(final DataSource datasource) {
		return with(new Connector() {
			public Connection connect() throws SQLException {
				return datasource.getConnection();
			}
		});
	}

	/**
	 * Creates a {@link Connecting} that delegates to a {@link Connector}.
	 * 
	 * @param connector the delegate {@link Connector}
	 * @return a new {@link Connecting}
	 */
	public static Connecting<Session> with(Connector connector) {
		return new Connecting<Session>(connector, new ConnectionWrapper.BasicSession());
	}
}
