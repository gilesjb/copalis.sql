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
package org.copalis.sql.samples;

import java.math.BigDecimal;

import org.copalis.sql.Results;
import org.copalis.sql.Session;

interface CoffeeSession extends Session {

    /**
     * The create table commands used to set up the example database in memory
     */
    static final String[] TABLES = {
        "create table COFFEES (COF_NAME varchar(32), SUP_ID int, PRICE decimal, SALES int, TOTAL int)",
        "create table SUPPLIERS (" +
                    "SUP_ID int generated always as identity primary key," +
                    "SUP_NAME varchar(40)," +
                    "STREET varchar(40)," +
                    "CITY varchar(40)," +
                    "STATE char(2)," +
                    "ZIP char(5))"
    };

	/**
     * A Results interface for reading COF_NAME, PRICE and SALES fields
     */
    interface Coffees extends Results {
    	@As("COF_NAME") String coffeeName();
    	BigDecimal price();
    	int sales();
    }
    
    /**
     * An extension of Coffees that allows fields to be updated
     */
    interface UpdatableCoffees extends Coffees, Results.Updatable {
        @As("COF_NAME") void coffeeName(String name);
        void price(BigDecimal value);
        void sales(int value);
    }

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
	 * @return results for matching records
	 */
	@Query( "COFFEES, SUPPLIERS " +
			"where SUPPLIERS.SUP_NAME like $1 " +
			"and SUPPLIERS.SUP_ID = COFFEES.SUP_ID")
	Coffees coffeesFromSupplier(String supplier);
	
	/**
	 * Gets an updatable result set of named coffee records
	 * @param name a value of COF_NAME to match against
	 * @return updatable results for matching records
	 */
	@Query("COFFEES where COF_NAME like $1")
	UpdatableCoffees coffeesByName(String name);
	
	@Query("select SALES from COFFEES where COF_NAME = $1")
	Integer coffeeSales(String name);
}