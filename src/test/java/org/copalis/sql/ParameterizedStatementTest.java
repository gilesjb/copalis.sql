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
import java.util.Arrays;

import junit.framework.TestCase;

import org.copalis.sql.common.ParameterizedStatement;

public class ParameterizedStatementTest extends TestCase {

	public void testValid() {
		ParameterizedStatement stmt = new ParameterizedStatement("some text $$ $$$$ $111$22");
		
		assertEquals("some text $ $$ ??", stmt.text());
		assertEquals(Arrays.asList(111, 22), stmt.indexes());
	}
	
	public void testIncomplete() {
		try {
			new ParameterizedStatement("$$ $");
			fail();
		} catch (IllegalArgumentException e) {}
	}
	
	public void testBadEscape() {
		try {
			new ParameterizedStatement("$$ $5 $?  ");
			fail();
		} catch (IllegalArgumentException e) {}
	}
	
	public void testBadIndex() throws SQLException {
		try {
			new ParameterizedStatement("$10 $1").validate(null, new Class<?>[] {});
			fail();
		} catch (IndexOutOfBoundsException e) {}
	}
}
