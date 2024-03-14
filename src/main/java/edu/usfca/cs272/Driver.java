package edu.usfca.cs272;

import java.nio.file.Path;

/**
 * Class responsible for running this project based on the provided command-line
 * arguments. See the README for details.
 *
 * @author Kayvan Zahiri
 * @author CS 272 Software Development (University of San Francisco)
 * @version Spring 2024
 */

public class Driver {
	/**
	 * Main method
	 *
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		ArgumentParser parser = new ArgumentParser(args);
		InvertedIndex indexer = new InvertedIndex();

		if (parser.hasFlag("-text")) {
			Path inputPath = parser.getPath("-text");
			FileBuilder fileBuilder = new FileBuilder(indexer);
			try {
				fileBuilder.buildStructures(inputPath);
			} catch (Exception e) {
				System.out.println("Error building the structures " + inputPath);
			}
		}

		if (parser.hasFlag("-counts")) {
			Path countsPath = parser.getPath("-counts", Path.of("counts.json"));
			try {
				indexer.writeCounts(countsPath);
			} catch (Exception e) {
				System.out.println("Error building the file word counts " + countsPath);
			}
		}

		if (parser.hasFlag("-index")) {
			Path indexPath = parser.getPath("-index", Path.of("index.json"));
			try {
				indexer.writeIndex(indexPath);
			} catch (Exception e) {
				System.out.println("Error building the inverted index " + indexPath);
			}
		}
	}
}
