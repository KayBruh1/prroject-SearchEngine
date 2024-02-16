package edu.usfca.cs272;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Class responsible for running this project based on the provided command-line
 * arguments. See the README for details.
 *
 * @author Kayvan Zahiri
 * @author CS 272 Software Development (University of San Francisco)
 * @version Spring 2024
 */
public class Driver {
    /** TreeMap storing word counts for each file */
	static TreeMap<String, Integer> fileWordCounts = new TreeMap<>();
	
	/** TreeMap storing inverted index for files and word positions */
	static TreeMap<String, TreeMap<String, List<Integer>>> invertedIndex = new TreeMap<>();
	
    /** Path to input text files */
    static String inputPath;

    /** Path to write word counts JSON file */
    static String countsPath;

    /** Path to write inverted index JSON file */
    static String indexPath;

    /** Flag indicating to write word counts or not */
    static boolean counts = false;

    /** Flag indicating to write inverted index or not */
    static boolean index = false;

    /**
     * Main method
     *
     * @param args Command line arguments
     * @throws IOException If an I/O error occurs
     */
	public static void main(String[] args) throws IOException {
		counts = false;
		index = false;
		fileWordCounts.clear();
		invertedIndex.clear();

		ArgumentParser parser = new ArgumentParser(args);

		inputPath = parser.getString("-text");

		for (String arg : args) {
			if (arg.contains("-counts")) {
				countsPath = parser.getString("-counts", "counts.json");
				counts = true;
			}
			if (arg.contains("-index")) {
				indexPath = parser.getString("-index", "index.json");
				index = true;
			}
		}

		if (inputPath != null) {
			Path path = Path.of(inputPath);
			processPath(path);
		} else if (inputPath == null && counts == true && index == false) {
			fileWordCounts.put("No input provided", 0);
			JsonWriter.writeObject(fileWordCounts, Path.of(countsPath));
		} else if (inputPath == null && index == true && counts == false) {
			fileWordCounts.put("No input provided", 0);
			JsonWriter.writeObject(fileWordCounts, Path.of(indexPath));
		} else {
			System.out.println("No input text files provided");
		}
		
		System.out.println("Input Path: " + inputPath);
		System.out.println("Counts Flag: " + counts);
		System.out.println("Index Path: " + indexPath);
		System.out.println("Index Flag: " + index);
	}

	/**
	 * Processes the given path as file or directory
	 *
	 * @param path The path to process
	 * @throws IOException If an I/O error occurs
	 */
	private static void processPath(Path path) throws IOException {
		if (Files.isDirectory(path) && index && !counts) {
			processIndexDirectory(path);
		} else if (Files.isDirectory(path) && counts && !index) {
			processCountsDirectory(path);
		} else if (Files.isDirectory(path) && counts && index) {
			processCountsDirectory(path);
			processIndexDirectory(path);
		} else if (Files.exists(path) && index && !counts){
			processFileIndex(path, counts);
		} else if (Files.exists(path) && counts && !index){
			processFileCounts(path, counts);
		}
	}

	/**
	 * Recursively processes the directory to build and write the inverted index.
	 *
	 * @param directory The directory to process
	 * @throws IOException If an I/O error occurs
	 */
	private static void processIndexDirectory(Path directory) throws IOException {
		try (DirectoryStream<Path> listing = Files.newDirectoryStream(directory)) {
			for (Path path : listing) {
				if (Files.isDirectory(path)) {
					processIndexDirectory(path);
				} else {
					// @CITE StackOverflow 
					String relativePath = directory.resolve(path.getFileName()).toString();

					if (relativePath.toLowerCase().endsWith(".txt") || relativePath.toLowerCase().endsWith(".text")) {
						HashMap<String, Integer> wordCounts = processDirIndex(path);

						// @CITE StackOverflow
						int totalWords = wordCounts.values().stream().mapToInt(Integer::intValue).sum();
						if (totalWords > 0) {
							fileWordCounts.put(relativePath, totalWords);
						}
					}
				}
			}
		}

        writeInvertedIndex();
		System.out.println("Word counts have been written to: " + countsPath);
	}

	/**
	 * Processes file to generate word counts and build the inverted index
	 * 
	 * @param filePath The path of file to be processed
	 * @return A HashMap containing word counts for the file
	 * @throws IOException If an I/O error occurs
	 */
	private static HashMap<String, Integer> processDirIndex(Path filePath) throws IOException {
		System.out.println("Processing file: " + filePath);

		List<String> lines = Files.readAllLines(filePath);
		HashMap<String, Integer> wordCounts = new HashMap<>();
		int position = 0;

		for (String line : lines) {

			List<String> wordStems = FileStemmer.listStems(line);

			for (String stemmedWord : wordStems) {
				position += 1;
				if (wordCounts.containsKey(stemmedWord)) {
					wordCounts.put(stemmedWord, wordCounts.get(stemmedWord) + 1);
				} else {
					wordCounts.put(stemmedWord, 1);
				}

				if (!invertedIndex.containsKey(stemmedWord)) {
					invertedIndex.put(stemmedWord, new TreeMap<>());
				}

				TreeMap<String, List<Integer>> fileMap = invertedIndex.get(stemmedWord);

				if (!fileMap.containsKey(filePath.toString())) {
					fileMap.put(filePath.toString(), new ArrayList<>());

				}

				List<Integer> wordPosition = fileMap.get(filePath.toString());
				wordPosition.add(position);

			}
		}
		return wordCounts;
	}

	/**
	 * Processes file to generate word counts and build and write the inverted index

	 * @param filePath The path of the file to be processed
	 * @param counts   A boolean indicating to generate word counts or not
	 * @throws IOException If an I/O error occurs
	 */
	private static void processFileIndex(Path filePath, boolean counts) throws IOException {
		System.out.println("Processing file: " + filePath);

		List<String> lines = Files.readAllLines(filePath);
		HashMap<String, Integer> wordCounts = new HashMap<>();
		int position = 0;

		for (String line : lines) {

			List<String> wordStems = FileStemmer.listStems(line);

			for (String stemmedWord : wordStems) {
				position += 1;
				if (wordCounts.containsKey(stemmedWord)) {
					wordCounts.put(stemmedWord, wordCounts.get(stemmedWord) + 1);
				} else {
					wordCounts.put(stemmedWord, 1);
				}

				if (!invertedIndex.containsKey(stemmedWord)) {
					invertedIndex.put(stemmedWord, new TreeMap<>());
				}

				TreeMap<String, List<Integer>> fileMap = invertedIndex.get(stemmedWord);

				if (!fileMap.containsKey(filePath.toString())) {
					fileMap.put(filePath.toString(), new ArrayList<>());

				}

				List<Integer> wordPosition = fileMap.get(filePath.toString());
				wordPosition.add(position);

			}
		}
        writeInvertedIndex();
	}
	
	/**
	 * Recursively processes directory to generate word counts for files
	 * 
	 * @param directory The directory to process
	 * @throws IOException If an I/O error occurs
	 */
	private static void processCountsDirectory(Path directory) throws IOException {
		try (DirectoryStream<Path> listing = Files.newDirectoryStream(directory)) {
			for (Path path : listing) {
				if (Files.isDirectory(path)) {
					processCountsDirectory(path);
				} else {
					// @CITE StackOverflow 
					String relativePath = directory.resolve(path.getFileName()).toString();

					if (relativePath.toLowerCase().endsWith(".txt") || relativePath.toLowerCase().endsWith(".text")) {
						HashMap<String, Integer> wordCounts = processDirCounts(path);

						// @CITE StackOverflow
						int totalWords = wordCounts.values().stream().mapToInt(Integer::intValue).sum();
						if (totalWords > 0) {
							fileWordCounts.put(relativePath, totalWords);
						}
					}
				}
			}
		}
		JsonWriter.writeObject(fileWordCounts, Path.of(countsPath));
		System.out.println("Word counts have been written to: " + countsPath);
	}

	/**
	 * Processes file to generate word counts

	 * @param filePath The path of the file to be processed
	 * @return A HashMap containing the word counts for the file
	 * @throws IOException If an I/O error occurs
	 */
	private static HashMap<String, Integer> processDirCounts(Path filePath) throws IOException {
		List<String> lines = Files.readAllLines(filePath);
		HashMap<String, Integer> wordCounts = new HashMap<>();

		for (String line : lines) {
			List<String> wordStems = FileStemmer.listStems(line);

			for (String stemmedWord : wordStems) {
				if (wordCounts.containsKey(stemmedWord)) {
					int currentCount = wordCounts.get(stemmedWord);
					wordCounts.put(stemmedWord, currentCount + 1);
				} else {
					wordCounts.put(stemmedWord, 1);
				}
			}
		}
		return wordCounts;
	}

	/**
	 * Processes the file to generate and write word count
	 * 
	 * @param filePath  The path of the file to be processed
	 * @param counts    A boolean indicating to output word counts or not
	 * @throws IOException If an I/O error occurs
	 */
	private static void processFileCounts(Path filePath, boolean counts) throws IOException {
		System.out.println("Processing file: " + filePath);

		List<String> lines = Files.readAllLines(filePath);
		HashMap<String, Integer> wordCounts = new HashMap<>();

		for (String line : lines) {
			List<String> wordStems = FileStemmer.listStems(line);

			for (String stemmedWord : wordStems) {
				if (wordCounts.containsKey(stemmedWord)) {
					wordCounts.put(stemmedWord, wordCounts.get(stemmedWord) + 1);
				} else {
					wordCounts.put(stemmedWord, 1);
				}
			}
		}
		outputWordCounts(wordCounts, inputPath, countsPath);
	}

	/**
	 * Outputs word counts to JSON file

	 * @param wordCounts The word counts map to be written to file
	 * @param inputPath  The input path of the text file
	 * @param outputPath The output path of the JSON file
	 */
	private static void outputWordCounts(HashMap<String, Integer> wordCounts, String inputPath, String outputPath) {
		try {
			if (wordCounts.isEmpty()) {
				System.out.println("Word counts map is empty"); 
				HashMap<String, Integer> pathWordCount = new HashMap<>();

				JsonWriter.writeObject(pathWordCount, Path.of(outputPath));
				System.out.println("Empty word counts have been written to: " + outputPath);
			} else {
				HashMap<String, Integer> pathWordCount = new HashMap<>();
				int totalWords = 0;

				for (int count : wordCounts.values()) {
					totalWords += count;
				}
				System.out.println("Total words: " + totalWords);
				pathWordCount.put(inputPath, totalWords);

				JsonWriter.writeObject(pathWordCount, Path.of(outputPath));
				System.out.println("Word counts have been written to: " + outputPath);
			}
		} catch (Exception e) {
			System.err.println("Error writing word counts to file: " + e.getMessage());
		}
	}
	
	/**
	 * Writes inverted index to JSON file
	 */
    private static void writeInvertedIndex() {
        try {
            TreeMap<String, TreeMap<String, List<Integer>>> convertedIndex = new TreeMap<>(invertedIndex);

            try (BufferedWriter writer = Files.newBufferedWriter(Path.of(indexPath), StandardCharsets.UTF_8)) {
                JsonWriter.writeIndex(convertedIndex, writer, 0);
            }
            System.out.println("Inverted index has been written to: " + indexPath);
        } catch (IOException e) {
            System.err.println("Error writing inverted index to file: " + e.getMessage());
        }
    }
}
