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

import javax.sql.DataSource;

import org.copalis.sql.Results;
import org.copalis.sql.Session;
import org.copalis.sql.Connecting;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

/**
 * Example that uses MySQL to query data from public ucsc genome db.
 * 
 * @author gilesjb
 */
public class GenomeQueries {
	
	interface VisiGene extends Session {
		
		@Query("visiGene.submissionSource ORDER BY id")
		Sources querySources();
		
		@Query("visiGene.submissionSource WHERE id=$1")
		Sources querySourceById(int id);
		
		@Query("visiGene.submissionSet subs, visiGene.submissionSource source, visiGene.journal journal" + 
				" WHERE subs.name=$1 AND source.id=subs.submissionSource AND journal.id=subs.journal")
		SubmissionJoin queryNamedSubmission(String name);
	}
	
	/**
	 * Return type for queries that SELECT id,name,acknowledgement
	 */
	interface Sources extends Results {
		int id();
		String name();
		String acknowledgement();
	}
	
	/**
	 * Return type for queries that SELECT id,name,year,pubUrl
	 */
	interface Submissions extends Results {
		int id();
		String name();
		int year();
		byte[] pubUrl();
	}
	
	/**
	 * Return type for queries that SELECT id,name,url
	 */
	interface Journals extends Results {
		int id();
		String name();
		String url();
	}
	
	/**
	 * Return type for queries that
	 * SELECT source.id,source.name,source.journal,
	 * subs.id,subs.name,subs.year,subs.pubUrl,
	 * journal.id,journal.name,journal.url
	 */
	interface SubmissionJoin extends Results {
		Sources source();
		@As("subs") Submissions submission();
		Journals journal();
	}

	public static void main(String... args) {
		// Create the DataSource
		DataSource dataSource = getDataSource();
		
		// Connect and perform queries
		Connecting.to(dataSource).as(VisiGene.class).execute(new Session.Command<VisiGene>() {
			public void execute(VisiGene session) {

				System.out.println("List of all sources:");
				Sources src = session.querySources();
				
				while (src.next()) {
					System.out.format("%d %s, acknowledgement:%s\n",
							src.id(), src.name(), src.acknowledgement());
				}
				src.close();
				
				System.out.println("\nSearch for a specific submission set:");
				SubmissionJoin sj = session.queryNamedSubmission("jax94492");
				
				if (sj.next()) {
					System.out.format("%s %d, journal:%s, source:%s, url:%s\n",
							sj.submission().name(), sj.submission().year(), sj.journal().name(),
							sj.source().name(), new String(sj.submission().pubUrl()));
				}
				sj.close();
			}
		});
	}
	
	private static DataSource getDataSource() {
		MysqlDataSource ds = new MysqlDataSource();
		ds.setServerName("genome-mysql.cse.ucsc.edu");
		ds.setPort(3306);
		ds.setUser("genomep");
		ds.setPassword("password");
		return ds;
	}
}
