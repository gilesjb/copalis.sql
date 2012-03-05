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
@SuppressWarnings("serial")
public class CoffeeBreak {
    
    private static final DataSource DATA_SOURCE = new org.hsqldb.jdbc.JDBCDataSource() {{
        setDatabase("jdbc:hsqldb:mem:test-database");
        setUser("sa");
    }};
    
    static {
        Connecting.to(DATA_SOURCE).execute(new Session.Command<Session>() {
            public void execute(Session session) throws SQLException {
                for (String update : CoffeeSession.TABLES) {
                    session.connection().createStatement().executeUpdate(update);
                }
            }
        });
    }

	public static void main(String... args) {
		Connecting.to(DATA_SOURCE).as(CoffeeSession.class).execute(new Session.Command<CoffeeSession>() {
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
				CoffeeSession.UpdatableCoffees fr = session.coffeesByName("French Roast");
				while (fr.next()) {
					fr.price(fr.price().add($(1, 50)));
					fr.sales(fr.sales() + 1);
					fr.updateRow();
				}
				fr.close();
				
				// Print out the Superior Coffee prices
				CoffeeSession.Coffees sc = session.coffeesFromSupplier("Superior Coffee");
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
}
