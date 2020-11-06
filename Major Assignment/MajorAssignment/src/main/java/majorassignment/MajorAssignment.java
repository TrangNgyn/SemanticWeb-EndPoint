/*-----------------------------------------------------------------------
Student number: 6166994
Student name: Trang Nguyen
Based on the lecture in week 5 by Dr Davoud Mougouei
-----------------------------------------------------------------------*/

package majorassignment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.util.FileManager;

public class MajorAssignment {

	public static void main(String[] args) {
		// File paths
		String OWL_PATH = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main"
				+ File.separator + "resources" + File.separator + "ontologies" + File.separator + "ValuesMapOntology_RDF.owl";
		String PATH_DATA_LOCAL = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main"
				+ File.separator + "resources" + File.separator + "data" + File.separator + "temp.ttl";
		String PATH_DATA_REMOTE = "http://dig.csail.mit.edu/2008/webdav/timbl/foaf.rdf";
		
		// -------- Local query file paths -------- //
		String PATH_QUERY_SELECT = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main"
				+ File.separator + "resources" + File.separator + "queries" + File.separator + "query_select.sparql";
		String PATH_QUERY_ASK = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main"
				+ File.separator + "resources" + File.separator + "queries" + File.separator + "query_ask.sparql";
		String PATH_QUERY_DESCRIBE = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main"
				+ File.separator + "resources" + File.separator + "queries" + File.separator + "query_describe.sparql";
		String PATH_QUERY_CONSTRUCT = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main"
				+ File.separator + "resources" + File.separator + "queries" + File.separator + "query_construct.sparql";
		
		String ENDPOINT = "http://localhost:3030/test/query";
		// select data source
		String data = OWL_PATH;
		// select query form file or directly enter
		String query_select = QueryFactory.read(PATH_QUERY_SELECT).toString();
		String query_ask = QueryFactory.read(PATH_QUERY_ASK).toString();
		String query_describe = QueryFactory.read(PATH_QUERY_DESCRIBE).toString();
		String query_construct = QueryFactory.read(PATH_QUERY_CONSTRUCT).toString();
		
		try {
			// ------ Execute local data and queries ------ //
			//Model model = ModelFactory.createDefaultModel();
			// read data
			//model.read(data);		
			//model.write(System.out, "Turtle");
			// read query
			//executeQuery(query, false, model, "");
			
			// ------ Execute remote data and queries at endpoint ------ //			
			System.out.println("\nSELECT query results:\n");
			executeQuery(query_select, true, null, ENDPOINT);
			System.out.println("\n====================================");
			
			System.out.print("\nASK query results: ");
			executeQuery(query_ask, true, null, ENDPOINT);
			System.out.println("\n====================================");
			
			System.out.println("\nDESCRIBE query results:\n");
			executeQuery(query_describe, true, null, ENDPOINT);
			System.out.println("\n====================================");
			
			System.out.println("\nCONSTRUCT query results:\n");
			executeQuery(query_construct, true, null, ENDPOINT);
			System.out.println("\n====================================");
			
		} catch (Exception ex) {
			System.out.println("Faild to load Data/Query: " + ex.toString());
		}
		
	}
	
	// --------------------------------------------------------------------------------------
	public static void executeQuery(String query, boolean remote, Model model, String endPoint) {

			if (query.contains("SELECT")) {
				if (remote) {
					select_remote(query, endPoint);
				}else {
					select_local(query, model);
				}
			} else if (query.contains("CONSTRUCT")) {
				Model graph = null;
				if (remote) {
					graph = construct_remote(query, endPoint);
					//select_local("SELECT * {?s ?p ?o .}", graph);
					// Write a model in Turtle syntax, default style (pretty printed)
					RDFDataMgr.write(System.out, graph, Lang.TURTLE);
				}else {
					graph = construct_local(query, model);
					//select_local("SELECT * {?s ?p ?o .}", graph);
					// Write a model in Turtle syntax, default style (pretty printed)
					RDFDataMgr.write(System.out, graph, Lang.TURTLE);
				}
			} else if (query.contains("ASK")) {
				if(remote)
					System.out.println(ask_remote(query, endPoint));
				else
					System.out.println(ask_local(query, model));
			} else if (query.contains("DESCRIBE")) {
				if(remote) {
					Model graph = describe_remote(query, endPoint);
					//select_local("SELECT * {?s ?p ?o .}", graph);
					// Write a model in Turtle syntax, default style (pretty printed)
					RDFDataMgr.write(System.out, graph, Lang.TURTLE);
				}else {
					Model graph = describe_local(query, model);
					//select_local("SELECT * {?s ?p ?o .}", graph);
					// Write a model in Turtle syntax, default style (pretty printed)
					RDFDataMgr.write(System.out, graph, Lang.TURTLE);
				}
				
			}		

	}

	// --------------------------------------------------------------------------------------
	// Function for executing the query at the endpoint
	public static ResultSet select_remote(String query_string, String ENDPOINT) {
		ResultSet results = null;
		Query query = QueryFactory.create(query_string);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(ENDPOINT, query);
		try {
			results = qexec.execSelect();
		} finally {
			if (results != null) //if there is at least one result found
				System.out.println(ResultSetFormatter.asText(results, query));
			else
				System.out.println("no result were found!");
			qexec.close();
		}
		return results;
	}
	
	// or create a temporary model for executing local query
	public static ResultSet select_local(String query_string, Model model) {
		ResultSet results = null;
		Query query = QueryFactory.create(query_string);
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		try {
			results = qexec.execSelect();
		} finally {
			if (results != null) //if there is at least one result found
				System.out.println(ResultSetFormatter.asText(results, query));
			else
				System.out.println("no result were found!");
			qexec.close();
		}
		return results;
	}

	// -----------------------------------------------------------------------------------------
	
	public static Model construct_remote(String query_string, String ENDPOINT) {
		Model graph = null;
		Query query = QueryFactory.create(query_string);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(ENDPOINT, query);
		try {
			graph = qexec.execConstruct();
		} finally {
			qexec.close();
		}
		return graph;
	}
		
	public static Model construct_local(String query_string, Model model) {
		Model graph = null;
		Query query = QueryFactory.create(query_string);
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		
		try {
			graph = qexec.execConstruct();
		} finally {
			qexec.close();
		}
		return graph;
	}

	// ------------------------------------------------------------------------------------------
	public static Boolean ask_remote(String query_string, String ENDPOINT) {
		Boolean results = null;
		Query query = QueryFactory.create(query_string);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(ENDPOINT, query);
		try {
			results = qexec.execAsk();
		} finally {
			qexec.close();
		}
		return results;
	}
	
	public static Boolean ask_local(String query_string, Model model) {
		Boolean results = null;
		Query query = QueryFactory.create(query_string);
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		try {
			results = qexec.execAsk();
		} finally {
			qexec.close();
		}
		return results;
	}

	// ------------------------------------------------------------------------------------------
	
	public static Model describe_remote(String query_string, String ENDPOINT) {
		Model results = null;
		Query query = QueryFactory.create(query_string);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(ENDPOINT, query);
		try {
			results = qexec.execDescribe();
		} finally {
			qexec.close();
		}
		return results;
	}
	
	public static Model describe_local(String query_string, Model model) {
		Model results = null;
		Query query = QueryFactory.create(query_string);
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		try {
			results = qexec.execDescribe();
		} finally {
			qexec.close();
		}
		return results;
	}
}