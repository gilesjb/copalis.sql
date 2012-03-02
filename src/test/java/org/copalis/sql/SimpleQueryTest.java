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

import junit.framework.TestCase;

/**
 * @author gilesjb
 *
 */
public class SimpleQueryTest extends TestCase {

	interface AddressSession extends Session {
		@Update("insert into ADDRESSES (NAME, STREET, CITY, STATE, ZIP) values ($1, $2, $3, $4, $5)")
		int addr(String name, String street, String city, String state, String zip);

		@Query("select ZIP from ADDRESSES where NAME=$1") String zipCode(String name);
		@Query("ADDRESSES where NAME=$1") Addresses forName(String name);
		@Query("select * from ADDRESSES where NAME=$1") Addresses allForName(String name);
		@Query("select count(*) from ADDRESSES") Integer recordCount();
	}
	
	interface Addresses extends Results {
		String name();
		String street();
		String city();
		String state();
		String zip();
	}
	
	private Connecting<Session> connector;
	private AddressSession session;
	
	@Override protected void setUp() throws Exception {
		Class.forName("org.hsqldb.jdbc.JDBCDriver");
		connector = Connecting.to("jdbc:hsqldb:mem:test-database", "sa", "");
		
		Session conn = connector.open();
		conn.connection().createStatement().executeUpdate(
				"create table ADDRESSES (" +
					"NAME varchar(255)," +
					"STREET varchar(255)," +
					"CITY varchar(255)," +
					"STATE varchar(255)," +
					"ZIP varchar(255))");
		conn.close();
		
		session = connector.as(AddressSession.class).open();
		session.addr("Smith", "100 Main", "Centerville", "WA", "98000");
		session.addr("Vasquez", "500 E 1st", "Exopolis", "IL", "55000");
		session.addr("Ramone", "700 University Way", "Minneapolis", "MN", "22800");
	}
	
	@Override protected void tearDown() throws Exception {
		session.connection().createStatement().execute("shutdown");
		session.close();
	};
	
	public void testZip() {
		assertEquals("98000", session.zipCode("Smith"));
	}

	public void testMissingZip() {
		assertNull(session.zipCode("Hoskins"));
	}
	
	public void testResults() throws SQLException {
		Addresses addrs = session.forName("Ramone");
		assertTrue(addrs.next());
		assertEquals("Minneapolis", addrs.city());
		assertFalse(addrs.next());
	}
	
	public void testCount() {
		assertEquals(3, session.recordCount().intValue());
		session.addr("Romex", "1100 Base 2", "Renton", "WA", "98111");
		assertEquals(4, session.recordCount().intValue());
	}
	
	public void testFail() {
		try {
			connector.as(AddressSession.class).transact(new Session.Command<AddressSession>() {
				public void execute(AddressSession session) throws SQLException {
					session.close();
					session.recordCount();
					System.out.println("Foo");
				}
			});
			fail();
		} catch (DataException e) {}
	}
}
