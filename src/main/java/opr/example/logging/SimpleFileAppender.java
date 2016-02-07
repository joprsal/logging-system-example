package opr.example.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;


@Plugin(name="SimpleFileAppender", category="Core", elementType="appender", printObject=true)
@SuppressWarnings("serial")
public class SimpleFileAppender extends AbstractAppender implements Appender {

	static final String SEARCH_STRING_FOUND_MESSAGE = "Search string found";

	private final File logFile;
	private FileOutputStream out;
    
	private final String searchString;
	private final Pattern searchStringPattern;


	@PluginFactory
	public static SimpleFileAppender createAppenderViaPluginFactory(
			@PluginAttribute("name") String name,
			@PluginAttribute("logFile") String logFile,
			@PluginAttribute("searchString") String searchString,
			@PluginElement("Filter") Filter filter,
			@PluginElement("Layout") Layout<? extends Serializable> layout) {

		if (name == null) {
            LOGGER.error("No name provided for SimpleFileAppender");
            return null;
        }
		if (logFile == null) {
            logFile = SimpleFileAppender.class.getSimpleName() + ".log";
        }
		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}
		return new SimpleFileAppender(name, logFile, searchString, filter, layout, true);
	}

	private SimpleFileAppender(
			String name,
			String logFile,
			String searchString,
			Filter filter,
			Layout<? extends Serializable> layout,
			boolean ignoreExceptions) {
		
		super(name, filter, layout, ignoreExceptions);
		this.searchString = searchString;
		this.searchStringPattern = compilePattern(searchString);
		this.logFile = new File(logFile);
		openWriter();
	}

	private Pattern compilePattern(String searchString) {
		if (searchString == null) {
			return null;
		}
		String searchStringQuoted = String.format(".*%s.*", Pattern.quote(searchString));
		return Pattern.compile(searchStringQuoted);
	}

	
	@Override
	public void stop() {
		super.stop();
		closeWriter();
	}
	
	private void openWriter() {
		try {
			out = new FileOutputStream(logFile, true);
		} catch (IOException e) {
			throw new RuntimeException("Failed to open log file writer", e);
		}
	}

	private void closeWriter() {
		try {
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			out = null;
		}
	}

	
	@Override
	synchronized
	public void append(LogEvent event) {
		try {
			byte[] bytes = getLayout().toByteArray(event);
			out.write(bytes);
			if (containsSearchString(event)) {
				writeSearchStringFoundMsg();
			}
		} catch (Exception ex) {
			if (!ignoreExceptions()) {
				throw new AppenderLoggingException(ex);
			}
		}
	}
	
	private boolean containsSearchString(LogEvent event) {
		if (searchStringPattern == null) {
			return false;
		}
		String msg = event.getMessage().getFormattedMessage();
		Matcher matcher = searchStringPattern.matcher(msg);
		return matcher.matches();
	}

	private void writeSearchStringFoundMsg() throws IOException {
		out.write(">>> ".getBytes());
		out.write(SEARCH_STRING_FOUND_MESSAGE.getBytes());
		out.write(": ".getBytes());
		out.write(searchString.getBytes());
		out.write("\n".getBytes());
	}

}