package opr.example.logging;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;


public class SimpleFileAppenderTest {
	
	@Rule
	public Timeout timeout = new Timeout(1000, TimeUnit.MILLISECONDS);
	
	/** search string as defined in log4j2.xml */
	private static final String SEARCH_STRING = "abcdef";
	/** log file as defined in log4j2.xml */
	private static final Path LOGFILE = Paths.get("target", "test.log");

	private Logger logger;
	
	
	@Before
	public void init() throws Exception {
		logger = LogManager.getLogger(getClass());
	}
	
	
	@Test
	public void should_log_to_file() throws IOException {
		logger.info("test msg");
		assertLogFileContains("test msg");
	}
	
	@Test
	public void should_not_interleave_concurrent_messages_in_sequence() throws Exception {
		int THREAD_COUNT = 100;
		writeMsgConcurrently("ab", THREAD_COUNT);
		
		assertLogFileDoesNotContain("aa");
		assertLogFileDoesNotContain("bb");
	}
	
	@Test
	public void should_find_search_string() throws IOException {
		logger.info("prefix" + SEARCH_STRING + "suffix");
		assertLogFileContains(SEARCH_STRING);
		assertLogFileContains(SimpleFileAppender.SEARCH_STRING_FOUND_MESSAGE);
	}

	private void writeMsgConcurrently(String msg, int threadCount) throws InterruptedException {
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch stopLatch = new CountDownLatch(threadCount);
		for (int i=0; i < threadCount; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						startLatch.await();
						logger.info(msg);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} finally {
						stopLatch.countDown();
					}
				}
			}).start();
		}
		startLatch.countDown();
		stopLatch.await();
	}


	private void assertLogFileContains(final String expectedSubstring) throws IOException {
		int linesCount = countLinesContaining(expectedSubstring);
		assertThat(linesCount, greaterThan(0));
	}

	private void assertLogFileDoesNotContain(String substring) throws IOException {
		int linesCount = countLinesContaining(substring);
		assertThat(linesCount, is(0));
	}

	private int countLinesContaining(final String substring) throws IOException {
		return (int) Files.lines(LOGFILE).filter(line -> line.contains(substring)).count();
	}
}
