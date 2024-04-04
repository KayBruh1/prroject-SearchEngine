package edu.usfca.cs272;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * Class responsible for query handling and adding search results
 */
public class QueryFileProcsesor {
	/**
	 * Map to store search results
	 */
	private final Map<String, List<InvertedIndex.SearchResult>> searchResultsMap;

	/**
	 * Inverted index instance for searching
	 */
	private final InvertedIndex indexer;

	/**
	 * SnowballStemmer instance for query processing word stems
	 */
	private final SnowballStemmer stemmer;

	/**
	 * A boolean indicating whether or not to partial search
	 */
	private final boolean partial;

	/**
	 * Constructs a new QueryFileProcsesor with the InvertedIndex
	 *
	 * @param indexer The InvertedIndex instance for searching
	 * @param partial boolean for partial search or not
	 */
	public QueryFileProcsesor(InvertedIndex indexer, boolean partial) {
		this.indexer = indexer;
		this.searchResultsMap = new TreeMap<>();
		this.stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
		this.partial = partial;
	}

	/**
	 * Processes search queries from a path
	 *
	 * @param queryPath The path containing search queries
	 * @throws IOException If an I/O error occurs
	 */
	public void processQueries(Path queryPath) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(queryPath)) {
			String line;
			while ((line = reader.readLine()) != null) {
				processQueries(line);
			}
		}
	}

	/**
	 * Processes a single search query line
	 *
	 * @param queryLine The query line to process
	 */
	public void processQueries(String queryLine) {
		TreeSet<String> query = FileStemmer.uniqueStems(queryLine, stemmer);
		if (query.isEmpty()) {
			return;
		}
		String queryVal = String.join(" ", query);
		if (searchResultsMap.get(queryVal) != null) {
			return;
		}
		List<InvertedIndex.SearchResult> searchResults = indexer.search(query, partial);
		searchResultsMap.put(queryVal, searchResults);
	}

	/**
	 * Checks if search results exist for a query
	 *
	 * @param query The query to check results for
	 * @return True if search results exist, false otherwise
	 */
	public boolean hasSearchResults(String query) {
		/*
		 * TODO: The processing done to a query line before storing the results in your
		 * map (using the joined unique stems as the key, not the original query line)
		 * is not visible outside of the method or to the user of this method.
		 * 
		 * For example a user might search for "44FOUR44 66SIX66" but that is stored as
		 * "four six" in your map. The user has no idea that processing happened to the
		 * query, so they are going to call this method with the unprocessed
		 * "44FOUR44 66SIX66" version of the query line.
		 * 
		 * That means your code needs to do one of the following:
		 * 
		 * 1) Make that process visible via another public method and update the Javadoc
		 * to make it clear it must be called first.
		 * 
		 * 2) Repeat that process to the line before accessing the results in this
		 * method and any other that accesses the map of results by a key. (Therefore,
		 * stem and join "44FOUR44 66SIX66" into "four six" instead before doing the
		 * containsKey.)
		 */

		return searchResultsMap.containsKey(query);
	}

	/**
	 * Returns the total number processed queries
	 *
	 * @return The total number of processed queries
	 */
	public int getTotalQueries() {
		return searchResultsMap.size();
	}

	/**
	 * Returns an unmodifiable set containing the queries for search results
	 *
	 * @return An unmodifiable set containing the queries for search results
	 */
	public Set<String> viewQueryResults() {
		return Collections.unmodifiableSet(searchResultsMap.keySet());
	}

	// TODO Add one to get search results for a query line

	/**
	 * Writes the search results to a JSON file
	 * 
	 * @param resultsPath the output path of the JSON file
	 * @throws IOException if an I/O error occurs
	 */
	public void writeResults(String resultsPath) throws IOException {
		JsonWriter.writeResults(searchResultsMap, resultsPath);
	}

	/**
	 * Returns a string representation of the search results map
	 * 
	 * @return a string representation of the search results map
	 */
	@Override
	public String toString() {
		return searchResultsMap.toString();
	}
}
