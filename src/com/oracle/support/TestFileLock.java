package com.oracle.support;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Date;

/*
 * Code from https://issuetracker.springsource.com/browse/DMS-1230?page=com.atlassian.jira.plugin.system.issuetabpanels:worklog-tabpanel
 * Modified by phil.gleghorn@oracle.com
 */
public class TestFileLock {

	private void log(String msg) {
		Date now = new Date();
		System.out.println(now.toString() + ": " + msg);
	}

	public static void main(String[] args) {
		TestFileLock t = new TestFileLock();
		t.doWork(args);
	}

	private void doWork(String[] args) {
		File file = new File(args[0]);

		if (file.exists()) {
			log("About to execute");
			new NioFileTemplate(file).execute(new NioFileCallback<byte[]>() {

				public byte[] doOnFile(RandomAccessFile file) throws Exception {
					log("Entered doOnFile");
					byte[] bytes = new byte[(int) file.length()];
					file.readFully(bytes);
					log("Exiting doOnFile");
					return bytes;
				}

			});
			log("Executed ok");
		} else {
			log("File " + file + " not found");
		}

	}

	public static interface NioFileCallback<T> {

		/**
		 * This method should contain the actual work that is desired. Check the
		 * <code>NioFileTemplate</code> documentation for information on the
		 * guarantees provided by that.
		 * 
		 * @param File
		 *            to be operated on
		 * @return The type required as a result of the closure
		 * @throws Exception
		 */
		T doOnFile(RandomAccessFile file) throws Exception;

	}

	public static class NioFileTemplate {

		private final File file;

		private void log(String msg) {
			Date now = new Date();
			System.out.println(now.toString() + ": " + msg);
		}
		
		/**
		 * 
		 * @param file
		 * @throws IOException
		 */
		public NioFileTemplate(File file) {
			this.file = file;
		}

		/**
		 * 
		 * @param <T>
		 * @param nioFileCallback
		 * @return
		 */
		public <T> T execute(NioFileCallback<T> nioFileCallback) {
			FileLock fileLock = null;
			RandomAccessFile randomAccessFile = null;

			try {
				log("Getting random access file " + this.file);
				randomAccessFile = this.getRandomAccessFile(this.file);
				log("Locking file channel");
				// fileLock = randomAccessFile.getChannel().lock();
				fileLock = randomAccessFile.getChannel().tryLock();
				log("Calling doOnFile");
				return nioFileCallback.doOnFile(randomAccessFile);
			} catch (Exception e) {
				log("Caught exception " + e.getMessage());
				throw new RuntimeException(e);
			} finally {
				try {
					if (fileLock != null) {
						log("Releasing file lock");
						fileLock.release(); // See if we can release the file
											// lock
						log("Released file lock");
					} else {
						log("tryLock() returned null");
					}
				} catch (Exception e) {
					log("Caught exception " + e.getMessage());
					throw new RuntimeException(e);
				} finally {
					try {
						if (randomAccessFile != null) {
							log("Closing random access file");
							randomAccessFile.close(); // See if we can close the
														// file stream
							log("Closed random access file");
						}
					} catch (Exception e) {
						System.out
								.println("Caught exception " + e.getMessage());
						throw new RuntimeException(e);
					}
				}
			}
		}

		/**
		 * 
		 * @param file
		 * @return
		 * @throws IOException
		 */
		private RandomAccessFile getRandomAccessFile(File file)
				throws IOException {
			boolean fileOK = true;
			try {
				if (!file.exists()) {
					File canonicalFile = file.getCanonicalFile();
					File parentCanonicalFile = canonicalFile.getParentFile();
					if (!parentCanonicalFile.exists()) {
						fileOK &= parentCanonicalFile.mkdirs();
					}
					fileOK &= canonicalFile.createNewFile();
				}
			} catch (IOException e) {
				throw new RuntimeException(
						"Unable to create a new Repository file with path '"
								+ file.getPath() + "'", e);
			}
			if (!fileOK) {
				throw new RuntimeException(
						"Unable to create a new Repository file with path '"
								+ file.getPath() + "'");
			}

			return new RandomAccessFile(file, "rws");
		}

	}

}