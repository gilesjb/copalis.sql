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

import java.math.BigDecimal;
import java.sql.SQLException;

import junit.framework.TestCase;

/**
 * @author gilesjb
 *
 */
public class CoffeeBreakTest extends TestCase {

	interface CoffeeSession extends Session {
		@Update(value = "insert into SUPPLIERS (NAME, STREET, CITY, STATE, ZIP)" +
					"values ($1, $2, $3, $4, $5)",
				returnGeneratedKey = true)
		int addSupplier(String name, String street, String city, String state, String zip);
		
		@Update("insert into COFFEES (NAME, SUP_ID, PRICE, SALES, TOTAL)" +
					"values ($2, $1, $3, 0, 0)")
		void addCoffee(int supplier, String name, BigDecimal price);
		
		@Query( "COFFEES, SUPPLIERS " +
				"where SUPPLIERS.NAME like $1 " +
				"and SUPPLIERS.SUP_ID = COFFEES.SUP_ID")
		CoffeeResults coffeesFromSupplier(String supplier);
		
		@Query("COFFEES where NAME like $1")
		CoffeeResults coffeesByName(String name);
		
		@Query("select SALES from COFFEES where NAME = $1")
		Integer coffeeSales(String name);
		
		@Query( "COFFEES, SUPPLIERS where COFFEES.SUP_ID = SUPPLIERS.SUP_ID")
		Names names(BigDecimal price);
	}
	
	/**
	 * A Results interface for getting and setting COF_NAME and PRICE fields
	 */
	interface CoffeeResults extends Results {
		@As("NAME") String coffeeName();
		BigDecimal price();
		int sales();
	}
	
	interface Names extends Results {
		@As("COFFEES.NAME") String coffeeName();
		@As("SUPPLIERS.NAME") String supplierName();
	}

	private Connecting<Session> connector;
	private CoffeeSession coffeeSession;

	@Override protected void setUp() throws SQLException {
		org.hsqldb.jdbc.JDBCDataSource dataSource = new org.hsqldb.jdbc.JDBCDataSource();
		dataSource.setDatabase("jdbc:hsqldb:mem:test-database");
		dataSource.setUser("sa");
		
		connector = Connecting.to(dataSource);
		
		Session session = connector.open();
		session.connection().createStatement().executeUpdate(
				"create table COFFEES (" +
				"NAME varchar(32)," +
				"SUP_ID int," +
				"PRICE decimal," + // HSQL decimal maps to Java BigDecimal
				"SALES int," +
		"TOTAL int)");
		session.connection().createStatement().executeUpdate(
				"create table SUPPLIERS (" +
				"SUP_ID int generated always as identity primary key," +
				"NAME varchar(40)," +
				"STREET varchar(40)," +
				"CITY varchar(40)," +
				"STATE char(2)," +
		"ZIP char(5))");
		session.close();
		
		coffeeSession = connector.as(CoffeeSession.class).open();
		int idAcme = coffeeSession.addSupplier("Acme, Inc", "99 Market Street", "Groundsville", "CA", "95199");
		int idSuperior = coffeeSession.addSupplier("Superior Coffee", "1 Party Place", "Mendocino", "CA", "95460");
		int idHighGround = coffeeSession.addSupplier("The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966");
		
		coffeeSession.addCoffee(idAcme, "Columbian", BigDecimal.valueOf(799, 2));
		coffeeSession.addCoffee(idAcme, "Columbian Decaf", BigDecimal.valueOf(899, 2));
		coffeeSession.addCoffee(idSuperior, "French Roast", BigDecimal.valueOf(899, 2));
		coffeeSession.addCoffee(idSuperior, "French Roast Decaf", BigDecimal.valueOf(999, 2));
		coffeeSession.addCoffee(idHighGround, "Espresso", BigDecimal.valueOf(999, 2));
	}

	@Override protected void tearDown() throws Exception {
		Session session = connector.open();
		session.connection().createStatement().execute("shutdown");
		session.close();
	};
	
	public void testJoin() {
		
	}
}
