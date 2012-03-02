/*
 *  Copyright 2010 Giles Burgess
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
package org.copalis.sql.samples;

import java.math.BigDecimal;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.copalis.sql.Results;
import org.copalis.sql.Session;
import org.copalis.sql.Connecting;

/**
 * A sample based on Sun's
 * <a href="http://java.sun.com/docs/books/tutorial/jdbc/basics/index.html">JDBC Basics</a>
 * tutorial, using an HSQL in-memory database.
 * <p/>
 * The sample demonstrates the following copalisSQL features:
 * <ul>
 * <li>Indexed parameters</li>
 * <li>Returning auto-generated keys</li>
 * <li>Results getters and setters</li>
 * <li>Updates to Results</li>
 * <li>Field name inference</li>
 * </ul>
 * This sample requires HSQL 2.0
 *
 * @author gilesjb
 */
public class CoffeeBreak {

	interface CoffeeSession extends Session {
		/**
		 * Adds a record to the SUPPLIERS table
		 * @param name a value for SUP_NAME
		 * @param street a value for STREET
		 * @param city a value for CITY
		 * @param state a value for STATE
		 * @param zip a value for ZIP
		 * @return the auto-generated SUP_ID value
		 */
		@Update(value = "insert into SUPPLIERS (SUP_NAME, STREET, CITY, STATE, ZIP)" +
					"values ($1, $2, $3, $4, $5)",
				returnGeneratedKey = true)
		int addSupplier(String name, String street, String city, String state, String zip);
		
		/**
		 * Adds a record to the COFFEES table, with SALES and TOTAL set to 0
		 * @param supplier a value for SUP_ID
		 * @param name a value for COF_NAME
		 * @param price a value for PRICE
		 */
		@Update("insert into COFFEES (COF_NAME, SUP_ID, PRICE, SALES, TOTAL)" +
					"values ($2, $1, $3, 0, 0)")
		void addCoffee(int supplier, String name, BigDecimal price);
		
		/**
		 * Finds coffees supplied by a named supplier by performing a join of
		 * COFFEES and SUPPLIERS tables
		 * @param supplier the SUP_NAME to match
		 * @return NamePrice results for matching records
		 */
		@Query( /* select COFFEES.COF_NAME, COFFEES.PRICE, COFFEES.SALES from */
				"COFFEES, SUPPLIERS " +
				"where SUPPLIERS.SUP_NAME like $1 " +
				"and SUPPLIERS.SUP_ID = COFFEES.SUP_ID")
		CoffeeResults coffeesFromSupplier(String supplier);
		
		/**
		 * Gets an updatable result set of named coffee records
		 * @param name a value of COF_NAME to match against
		 * @return updatable NamePrice results for matching records
		 */
		@Query(value = /* select COF_NAME, PRICE from */ "COFFEES where COF_NAME like $1",
				updatable = true)
		CoffeeResults coffeesByName(String name);
		
		@Query("select SALES from COFFEES where COF_NAME = $1")
		Integer coffeeSales(String name);
	}
	
	/**
	 * A Results interface for getting and setting COF_NAME and PRICE fields
	 */
	interface CoffeeResults extends Results {
		@As("COF_NAME") String coffeeName();
		@As("COF_NAME") void coffeeName(String name);
		
		BigDecimal price();
		void price(BigDecimal value);
		
		int sales();
		void sales(int value);
	}
	
	public static void main(String... args) {
		DataSource dataSource = createDataSource();
		
		Connecting.to(dataSource).as(CoffeeSession.class).execute(new Session.Command<CoffeeSession>() {
			public void execute(CoffeeSession session) {
				int idAcme = session.addSupplier("Acme, Inc", "99 Market Street", "Groundsville", "CA", "95199");
				int idSuperior = session.addSupplier("Superior Coffee", "1 Party Place", "Mendocino", "CA", "95460");
				int idHighGround = session.addSupplier("The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966");
				
				session.addCoffee(idAcme, "Columbian", $(7, 99));
				session.addCoffee(idAcme, "Columbian Decaf", $(8, 99));
				session.addCoffee(idSuperior, "French Roast", $(8, 99));
				session.addCoffee(idSuperior, "French Roast Decaf", $(9, 99));
				session.addCoffee(idHighGround, "Espresso", $(9, 99));
				
				// Increase the price of French Roast by $1.50
				CoffeeResults fr = session.coffeesByName("French Roast");
				while (fr.next()) {
					fr.price(fr.price().add($(1, 50)));
					fr.sales(fr.sales() + 1);
					fr.updateRow();
				}
				fr.close();
				
				// Print out the Superior Coffee prices
				CoffeeResults sc = session.coffeesFromSupplier("Superior Coffee");
				while (sc.next()) {
					System.out.println(sc.coffeeName() + " : " + sc.price());
				}
				sc.close();
				
				System.out.println("Sales of Espresso: " + session.coffeeSales("French Roast"));
			}
		});
	}
	
	private static final BigDecimal $(int dollars, int cents) {
		return BigDecimal.valueOf(dollars * 100 + cents, 2);
	}
	
	/**
	 * Creates an HSQL database and some tables
	 * @return a DataSource for the created HSQL DB
	 */
	private static DataSource createDataSource() {
		org.hsqldb.jdbc.JDBCDataSource dataSource = new org.hsqldb.jdbc.JDBCDataSource();
		dataSource.setDatabase("jdbc:hsqldb:mem:test-database");
		dataSource.setUser("sa");
		
		Connecting.to(dataSource).execute(new Session.Command<Session>() {
			public void execute(Session session) throws SQLException {
				session.connection().createStatement().executeUpdate(
						"create table COFFEES (" +
							"COF_NAME varchar(32)," +
							"SUP_ID int," +
							"PRICE decimal," + // HSQL decimal maps to Java BigDecimal
							"SALES int," +
							"TOTAL int)");
				session.connection().createStatement().executeUpdate(
						"create table SUPPLIERS (" +
							"SUP_ID int generated always as identity primary key," +
							"SUP_NAME varchar(40)," +
							"STREET varchar(40)," +
							"CITY varchar(40)," +
							"STATE char(2)," +
							"ZIP char(5))");
			}
		});
		return dataSource;
	}
}
