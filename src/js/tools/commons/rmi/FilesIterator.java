package js.tools.commons.rmi;

import java.io.File;
import java.util.Iterator;
import java.util.Stack;

import js.tools.commons.BugError;
import js.tools.commons.util.Strings;

/**
 * Traverse all files in a directory hierarchy, no mater how deep it is. This class implements both {@link Iterator} and
 * {@link Iterable} interfaces; as a consequence files can be inspected using both <code>iterator</code> and
 * <code>for...each</code> loops, see sample code below. All files from all sub-directories of given base directory are
 * guaranteed to be exposed exactly once but there is no guarantee for a particular order.
 * <p>
 * Files iterator provides static helpers for creating couple kind of iterators, absolute or relative, by file or by file name,
 * see {@link #getAbsoluteIterator(File)} and related.
 * 
 * <pre>
 * FilesIterator&lt;File&gt; it = FilesIterator.getAbsoluteIterator(&quot;/var/www/&quot;);
 * while (it.hasNext()) {
 * 	File file = it.next();
 * 	// do something with the file
 * }
 * 
 * for (File file : FilesIterator.getAbsoluteIterator(&quot;/var/www/&quot;)) {
 * 	// do something with the file
 * }
 * </pre>
 * 
 * <p>
 * This files iterator is able to returns both absolute and relative file paths and both {@link File} paths and {@link String}
 * file names; there are factory methods for relevant use cases. Note that absolute/relative in this class context means
 * including or not including <code>base directory</code> at the beginning of returned file path. Base directory is that
 * supplied when iterator instance is created.
 * 
 * <pre>
 * for (File file : FilesIterator.getAbsoluteIterator(&quot;/var/www/&quot;)) {
 * 	// file is something like '/var/www/index.htm'
 * 	// base directory is present on file start
 * }
 * 
 * for (File file : FilesIterator.getRelativeIterator(&quot;/var/www/&quot;)) {
 * 	// file is something like 'index.htm'
 * 	// base directory is not present on file start
 * }
 * </pre>
 * 
 * <p>
 * Finally, this class is not intended to be reused: its state is changing while iterating over files. Also is not thread safe.
 * 
 * @author Iulian Rotaru
 * @since 1.7
 * @version final
 */
public class FilesIterator<T> implements Iterator<T>, Iterable<T> {
	// -------------------------------------------------------
	// Convenient factory methods

	/**
	 * Create absolute path files iterator.
	 * 
	 * @param baseDir base directory to scan for files.
	 * @return files iterator.
	 */
	public static FilesIterator<File> getAbsoluteIterator(File baseDir) {
		return new FilesIterator<File>(baseDir, Strategy.FILES);
	}

	/**
	 * Create relative path files iterator.
	 * 
	 * @param baseDir base directory to scan for files.
	 * @return files iterator.
	 */
	public static FilesIterator<File> getRelativeIterator(File baseDir) {
		return new FilesIterator<File>(baseDir, Strategy.RELATIVE_FILES);
	}

	/**
	 * Create absolute file names iterator.
	 * 
	 * @param baseDir base directory to scan for files.
	 * @return files iterator.
	 */
	public static FilesIterator<String> getAbsoluteNamesIterator(File baseDir) {
		return new FilesIterator<String>(baseDir, Strategy.NAMES);
	}

	/**
	 * Create relative file names iterator.
	 * 
	 * @param baseDir base directory to scan for files.
	 * @return files iterator.
	 */
	public static FilesIterator<String> getRelativeNamesIterator(File baseDir) {
		return new FilesIterator<String>(baseDir, Strategy.RELATIVE_NAMES);
	}

	/**
	 * Create absolute path files iterator.
	 * 
	 * @param baseDir base directory path to scan for files.
	 * @return files iterator.
	 */
	public static FilesIterator<File> getAbsoluteIterator(String baseDir) {
		return new FilesIterator<File>(new File(baseDir), Strategy.FILES);
	}

	/**
	 * Create relative path files iterator.
	 * 
	 * @param baseDir base directory path to scan for files.
	 * @return files iterator.
	 */
	public static FilesIterator<File> getRelativeIterator(String baseDir) {
		return new FilesIterator<File>(new File(baseDir), Strategy.RELATIVE_FILES);
	}

	/**
	 * Create absolute file names iterator.
	 * 
	 * @param baseDir base directory name to scan for files.
	 * @return files iterator.
	 */
	public static FilesIterator<String> getAbsoluteNamesIterator(String baseDir) {
		return new FilesIterator<String>(new File(baseDir), Strategy.NAMES);
	}

	/**
	 * Create relative file names iterator.
	 * 
	 * @param baseDir base directory path to scan for files.
	 * @return files iterator.
	 */
	public static FilesIterator<String> getRelativeNamesIterator(String baseDir) {
		return new FilesIterator<String>(new File(baseDir), Strategy.RELATIVE_NAMES);
	}

	// -------------------------------------------------------
	// Private implementation

	/** Keep track of directory hierarchy. */
	private Stack<Directory> stack;

	/** Store current working directory at every given moment. */
	private Directory workingDirectory;

	/** Processed files count for audit purposes. */
	private int processedFilesCount;

	/**
	 * Path components used to create file returned by {@link #next()} operation when <code>relativePath</code> is true. It is
	 * updated by {@link #push(File)} and {@link #pop()} methods, while iteration is in progress with all directory names from
	 * current path.
	 */
	private Stack<String> pathComponents;

	/** The actual {@link #next()} processing strategy. */
	private Handler handler;

	/**
	 * Flag, if true, returned files are relative to base directory, that is, does not include base directory at file path
	 * start. If false returned files include base directory. Please note that in this class context not relative path does not
	 * mean absolute path; it is still relative to base directory but include it.
	 */
	private boolean relativePath;

	/**
	 * Construct a files iterator instance for given base directory.
	 * 
	 * @param baseDir base directory to scan for files,
	 * @param strategy strategy used for iterator next operation processing.
	 */
	private FilesIterator(File baseDir, Strategy strategy) {
		if (!baseDir.exists()) {
			throw new IllegalArgumentException(String.format("File iterator base directory |%s| is missing.", baseDir));
		}
		if (!baseDir.isDirectory()) {
			throw new IllegalArgumentException(String.format("File iterator cannot iterate |%s| over an ordinary file.", baseDir));
		}

		switch (strategy) {
		case FILES:
			this.handler = new FilesHandler();
			break;
		case RELATIVE_FILES:
			this.handler = new RelativeFilesHandler();
			this.relativePath = true;
			break;
		case NAMES:
			this.handler = new NamesHandler();
			break;
		case RELATIVE_NAMES:
			this.handler = new RelativeNamesHandler();
			this.relativePath = true;
			break;
		default:
			throw new BugError("Unsupported files iterator handler type |%s|.", strategy);
		}

		this.stack = new Stack<Directory>();
		this.workingDirectory = new Directory(baseDir);
		if (relativePath) {
			this.pathComponents = new Stack<String>();
		}
	}

	/** Implements {@link Iterable} interface. */
	@Override
	public Iterator<T> iterator() {
		return this;
	}

	/**
	 * Return true if files iterator instance has at least one file not yet exposed. If this predicate returns true, next call
	 * of {@link #next()} method is guaranteed to return a valid file.
	 * 
	 * @return true if there is at least one file usable by {@link #next()}.
	 */
	@Override
	public boolean hasNext() {
		if (isLoopExitCondition()) {
			return false;
		}
		for (;;) {
			if (workingDirectory.files.length == 0) {
				pop();
			}
			if (isLoopExitCondition()) {
				return false;
			}
			workingDirectory.currentFile = workingDirectory.files[workingDirectory.index++];
			if (!workingDirectory.currentFile.isDirectory()) {
				break;
			}
			push(workingDirectory.currentFile);
		}
		return true;
	}

	/**
	 * Test for iteration loop exit condition. Iteration loop is ended if current working directory index reaches its files
	 * count and there is no more directories on stack.
	 * 
	 * @return true if iterator loop reach its end.
	 */
	private boolean isLoopExitCondition() {
		while (workingDirectory.index == workingDirectory.files.length) {
			if (stack.isEmpty()) {
				return true;
			}
			pop();
		}
		return false;
	}

	/**
	 * Returns current file or its name, relative or absolute, based on selected strategy. This method should be invoked only if
	 * {@link #hasNext()} companion returns true; otherwise behavior is not specified.
	 * <p>
	 * This method just return the value of selected strategy for next operation processing, that is, delegates {@link #handler}
	 * . Also takes care to increment processed files count.
	 * 
	 * @return next operation processing result of selected strategy.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T next() {
		++processedFilesCount;
		return (T) handler.next();
	}

	/**
	 * Get processed files count but does not include directories.
	 * 
	 * @return processed files count.
	 */
	public int getProcessedFilesCount() {
		return processedFilesCount;
	}

	/**
	 * Push current directory on directories stack when enter a sub-directory. Given directory is restored when child finishes,
	 * using {@link #pop()} counterpart.
	 * 
	 * @param directory current directory.
	 */
	private void push(File directory) {
		// at this point file is guaranteed to be a directory
		stack.push(workingDirectory);
		workingDirectory = new Directory(directory);
		if (relativePath) {
			pathComponents.push(directory.getName());
		}
	}

	/** Restore current working directory after a sub-directory completes scanning. */
	private void pop() {
		// at this point stack is guranteed to not be empty
		workingDirectory = stack.pop();
		if (relativePath) {
			pathComponents.pop();
		}
	}

	/** This operation is not supported. */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Working directory context data saved on directory stack.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.8
	 * @version final
	 */
	private class Directory {
		/** Directory files list. */
		File[] files;
		/** Files index. */
		int index;
		/** Current processing file. */
		File currentFile;

		/**
		 * Construct directory context data for given directory.
		 * 
		 * @param directory working directory.
		 */
		Directory(File directory) {
			this.files = directory.listFiles();
		}
	}

	// -------------------------------------------------------
	// Next processing strategy

	/**
	 * Strategy for processing files iterator next operation.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.8
	 * @version final
	 */
	private enum Strategy {
		/** Neutral value. */
		NONE,
		/** Iterate over files with absolute path. */
		FILES,
		/** Iterate over files with relative path. */
		RELATIVE_FILES,
		/** Iterate over file names with absolute path. */
		NAMES,
		/** Iterate over file names with relative path. */
		RELATIVE_NAMES
	}

	/**
	 * Strategy handler for iterator next operation processing.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.8
	 * @version final
	 */
	private interface Handler {
		/**
		 * Return iterator next item.
		 * 
		 * @return return next item.
		 */
		Object next();
	}

	/**
	 * Handler for absolute files iterator.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.8
	 * @version final
	 */
	private class FilesHandler implements Handler {
		/**
		 * Get current file from working directory.
		 * 
		 * @return current file from working directory.
		 */
		@Override
		public Object next() {
			return workingDirectory.currentFile;
		}
	}

	/**
	 * Handler for relative files iterator.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.8
	 * @version final
	 */
	private class RelativeFilesHandler implements Handler {
		/**
		 * Get relative path of current file from working directory.
		 * 
		 * @return current file from working directory.
		 */
		@Override
		public Object next() {
			pathComponents.push(workingDirectory.currentFile.getName());
			File file = new File(Strings.join(pathComponents, File.separatorChar));
			pathComponents.pop();
			return file;
		}
	}

	/**
	 * Handler for absolute file names iterator.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.8
	 * @version final
	 */
	private class NamesHandler extends FilesHandler {
		/**
		 * Get file name for current file from working directory.
		 * 
		 * @return file name for current file from working directory.
		 */
		@Override
		public Object next() {
			return ((File) super.next()).getPath();
		}
	}

	/**
	 * Handler for relative file names iterator.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.8
	 * @version final
	 */
	private class RelativeNamesHandler extends RelativeFilesHandler {
		/**
		 * Get relative file name for current file from working directory.
		 * 
		 * @return relative file name for current file from working directory.
		 */
		@Override
		public Object next() {
			return ((File) super.next()).getPath();
		}
	}
}
