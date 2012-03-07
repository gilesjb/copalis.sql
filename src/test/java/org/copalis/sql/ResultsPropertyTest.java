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

import java.sql.ResultSet;

import junit.framework.TestCase;

import org.copalis.sql.results.ResultsProperty;

/**
 * @author gilesjb
 *
 */
public class ResultsPropertyTest extends TestCase {
	
	public interface FooBar extends Results {
		int foo(); void foo(int val);
		String bar(); void bar(String val);
	}
	
	public void testGetProperties() throws Exception {
		ResultsProperty[] properties = ResultsProperty.properties(FooBar.class);
		
		FooBar fb = new FooBar() {
			public String bar() {
				return "bar";
			}
			public void bar(String val) {
				assertEquals(bar(), val);
			}
			public int foo() {
				return 666;
			}
			public void foo(int val) {
				assertEquals(foo(), val);
			}
			public void close() throws DataException {
				throw new UnsupportedOperationException();
			}
			public boolean next() throws DataException {
				throw new UnsupportedOperationException();
			}
			public ResultSet results() {
				throw new UnsupportedOperationException();
			}
		};
		
		assertEquals(2, properties.length);
		
		for (ResultsProperty property : properties) {
			property.setter.invoke(fb, property.getter.invoke(fb));
		}
	}
	
	public interface Wrong extends Results {
		int a(int x, int y);
	}
	
	public void testNonInterface() {
		try {
			ResultsProperty.properties(Wrong.class);
			fail();
		} catch (UnsupportedOperationException e) {
			return;
		}
	}
}
