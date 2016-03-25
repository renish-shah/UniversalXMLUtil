package org.rns.util;
/**
 * This is the XML Utility which reads and modifies the XML based on the given parameters.
 */
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * @author Renish Shah
 * 
 */

public class XMLStaxUtil {

	private static final String OPERATION_DELETE = "DELETE";

	private static final String OPERATION_UPDATE = "UPDATE";

	private static final String OPERATION_READ = "READ";

	private static final String OPERATION_GET = "GET";

	private static final String OPERATION_ADD = "ADD";

	private static final Logger logger = Logger.getLogger(XMLStaxUtil.class
			.getName());

	private XMLEventReader eventReader;
	private XMLEventWriter writer;
	private XMLEventFactory eventFactory;
	private String sep = null; // Separator

	/**
	 * @return the sep
	 */
	public String getSep() {
		return sep;
	}

	/**
	 * @param sep
	 *            the sep to set
	 */
	public void setSep(String sep) {
		this.sep = sep;
	}

	public void validateParams(OutputStream os, InputStream is, String operation)
			throws Exception {

		switch (operation) {

		case OPERATION_GET:
			if (is == null) {
				throw new Exception("Source Input Stream can not be null");
			}
			break;

		case OPERATION_READ:
			if (is == null) {
				throw new Exception("Source Input Stream can not be null");
			}
			break;

		default:
			if (is == null || os == null || sep == null || sep.length() < 1) {
				throw new Exception(
						"Input Stream/Output Stream/Separator can not be null/empty");
			}
			break;
		}
	}

	/**
	 * @param xPath
	 *            Desc: Update Tag value of XML. ex: xPath = "abc:xyz"; In this
	 *            case, <xyz>ABC</xyz> will be updated New file will be written
	 *            with the Updated Tag Value
	 */
	public boolean updateTagValue(String xPath, String newValue,
			OutputStream os, InputStream is) throws XMLStreamException,
			IOException {

		initializeXMLEvents(true, true, os, is, OPERATION_UPDATE);

		boolean isFound = false;
		String[] xPathTags = null;

		xPathTags = xPath.split(sep);
		int countOfxPath = xPathTags.length;

		int i = 0;
		Stack<String> stackOfTags = new Stack<String>();

		try {

			while (eventReader.hasNext()) {

				XMLEvent event = eventReader.nextEvent();
				writer.add(event);

				if (!isFound) {

					if (event.getEventType() == XMLEvent.END_ELEMENT) {
						String qName = event.asEndElement().getName()
								.getLocalPart();
						if (!stackOfTags.empty()
								&& stackOfTags.peek().equals(qName)) {
							logger.info("=== Matching End element: " + qName
									+ " not found ===");
							isFound = false;
							break;
						}

					} else if (event.getEventType() == XMLEvent.START_ELEMENT) {

						StartElement startElement = event.asStartElement();
						String qName = startElement.getName().getLocalPart();

						if (qName.equals(xPathTags[i])) {
							logger.info("=== " + (i + 1)
									+ " xPath Matched ===> " + qName);

							stackOfTags.push(qName);
							// found xPath to modify value
							if (i == countOfxPath - 1) {
								writer.add(eventFactory
										.createCharacters(newValue));
								// Don't write after this event as this is the
								// old value
								event = eventReader.nextEvent();
								isFound = true;
							}
							i++;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		writer.close();
		return isFound;
	}

	/**
	 * Desc: Add a new Tag with Value New file will be written with the New Tag
	 * in the provided insertion order ex 1: Insertion at first place; set xpath
	 * = "config"; ex 2: Insertion at particular order; set xPath =
	 * "abc:xyz:pqr" > New tag will be added after 'pqr' tag
	 */
	public boolean addTagValue(String xPath, String newTagName,
			String newValue, boolean insertAtFirstPlace, OutputStream os,
			InputStream is) throws XMLStreamException, IOException {

		initializeXMLEvents(true, true, os, is, OPERATION_ADD);

		boolean isFound = false;
		String[] xPathTags = null;

		xPathTags = xPath.split(sep);
		int countOfxPath = xPathTags.length;

		if (!insertAtFirstPlace && countOfxPath < 2) {
			logger.info("=== Insert at First Place => False, Need at least two values in xPath. For ex: abc:xyz:pqr ===");
			return false;
		}

		int i = 0;
		Stack<String> stackOfTags = new Stack<String>();

		while (eventReader.hasNext()) {

			XMLEvent event = eventReader.nextEvent();
			writer.add(event);

			if (!isFound) {

				if (event.getEventType() == XMLEvent.END_ELEMENT) {
					String qName = event.asEndElement().getName()
							.getLocalPart();
					if (!stackOfTags.empty()
							&& stackOfTags.peek().equals(qName)) {
						logger.info("=== Matching End element: " + qName
								+ " not found ===");
						isFound = false;
						break;
					}

				} else if (event.getEventType() == XMLEvent.START_ELEMENT) {

					StartElement startElement = event.asStartElement();
					String qName = startElement.getName().getLocalPart();
					logger.info("QNAME :" + qName);

					if (qName.equals(xPathTags[i])) {
						logger.info("=== " + (i + 1) + " xPath Matched ===> "
								+ qName);

						stackOfTags.push(qName);
						// found xPath to modify value
						if (i == countOfxPath - 1) {

							if (!insertAtFirstPlace) {
								event = eventReader.nextEvent(); // Go to Data
																	// Tag
								writer.add(event); // Write Data Tag
								event = eventReader.nextEvent(); // Go to End
																	// Tag
								writer.add(event); // Write End Tag

							}
							writer.add(eventFactory.createStartElement("",
									null, newTagName));
							writer.add(eventFactory.createCharacters(newValue));
							writer.add(eventFactory.createEndElement("", null,
									newTagName));

							isFound = true;

						}
						i++;

					}
				}
			}
		}
		return isFound;
	}

	/**
	 * Desc: Retrieve the specified Tag Value
	 * 
	 * @return tag value ex: xPath = "abc:xyz:pqr" > Value of <pqr>ABC</pqr>
	 *         will be returned
	 */
	public String getTagValue(String xPath, InputStream is)
			throws XMLStreamException {

		initializeXMLEvents(false, false, null, is, OPERATION_GET);

		String value = null;
		boolean isFound = false;
		String[] xPathTags = null;

		xPathTags = xPath.split(sep);
		int countOfxPath = xPathTags.length;

		int i = 0;
		Stack<String> stackOfTags = new Stack<String>();

		while (eventReader.hasNext()) {

			XMLEvent event = eventReader.nextEvent();

			if (!isFound) {

				if (event.getEventType() == XMLEvent.END_ELEMENT) {
					String qName = event.asEndElement().getName()
							.getLocalPart();
					if (!stackOfTags.empty()
							&& stackOfTags.peek().equals(qName)) {
						logger.info("=== Matching End element: " + qName
								+ " not found ===");
						isFound = false;
						break;
					}

				} else if (event.getEventType() == XMLEvent.START_ELEMENT) {

					StartElement startElement = event.asStartElement();
					String qName = startElement.getName().getLocalPart();
					logger.info("QNAME :" + qName);

					if (qName.equals(xPathTags[i])) {
						logger.info("=== " + (i + 1) + " xPath Matched ===> "
								+ qName);

						stackOfTags.push(qName);
						// found xPath to modify value
						if (i == countOfxPath - 1) {
							event = eventReader.nextEvent();
							value = event.asCharacters().getData();
							isFound = true;
							break;

						}
						i++;
					}
				}
			}
		}
		if (value == null) {
			logger.info("=== Value not found. Check your xPath properly ===");
		}
		return value;

	}

	/**
	 * Desc: Read and print whole file
	 * 
	 * @throws Exception
	 */
	public void readFile(InputStream is) throws Exception {

		validateParams(null, is, OPERATION_READ);

		InputStreamReader reader = new InputStreamReader(is);

		try (BufferedReader br = new BufferedReader(reader)) {
			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				System.out.println(sCurrentLine);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Desc: Remove the specified tag along with the value
	 * 
	 * @return tag value ex: xPath = "abc:xyz:pqr"; Value of <pqr>ABC</pqr> will
	 *         be returned
	 */
	public boolean removeTag(String xPath, OutputStream os, InputStream is) {

		initializeXMLEvents(true, true, os, is, OPERATION_DELETE);

		boolean isRemoved = false;
		String[] xPathTags = null;

		xPathTags = xPath.split(sep);
		int countOfxPath = xPathTags.length;

		int i = 0;
		Stack<String> stackOfTags = new Stack<String>();

		try {
			while (eventReader.hasNext()) {

				XMLEvent event = eventReader.nextEvent();

				if (!isRemoved) {

					switch (event.getEventType()) {

					case XMLEvent.START_ELEMENT:

						StartElement startElement = event.asStartElement();
						String qName = startElement.getName().getLocalPart();
						logger.info("QNAME :" + qName);

						if (qName.equals(xPathTags[i])) {
							logger.info("=== " + (i + 1)
									+ " xPath Matched ===> " + qName);

							stackOfTags.push(qName);
							// found xPath to modify value
							if (i == countOfxPath - 1) {
								event = eventReader.nextEvent(); // skip to data
								event = eventReader.nextEvent(); // skip to end
								isRemoved = true;
							} else {
								writer.add(event);
							}
							i++;

						} else {
							writer.add(event);
						}
						break;

					case XMLEvent.END_ELEMENT:
						qName = event.asEndElement().getName().getLocalPart();
						if (!stackOfTags.empty()
								&& stackOfTags.peek().equals(qName)) {
							logger.info("=== Matching End element: " + qName
									+ " not found ===");
							isRemoved = false;
							return isRemoved;
						}
						writer.add(event);
						break;

					default:
						writer.add(event);
						break;
					}
				} else if (isRemoved) {
					writer.add(event);
				}
			}

			writer.close();
		} catch (XMLStreamException | NoSuchElementException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (XMLStreamException e) {
				e.printStackTrace();
			}
		}
		return isRemoved;
	}

	/**
	 * Get event reader to read the file
	 */
	public XMLEventReader getEventReader(String sourceFilePath)
			throws FileNotFoundException, XMLStreamException {
		return getEventReader(new FileInputStream(sourceFilePath));
	}

	/**
	 * Get event reader to read the file
	 * 
	 * @param InputStream
	 */
	public XMLEventReader getEventReader(InputStream is)
			throws FileNotFoundException, XMLStreamException {
		XMLInputFactory inFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = inFactory.createXMLEventReader(is);
		return eventReader;
	}

	/**
	 * Get event writer to write the file
	 */
	public XMLEventWriter getEventWriter(String destFilePath)
			throws XMLStreamException, IOException {
		OutputStream outputStream = new FileOutputStream(destFilePath);
		return getEventWriter(outputStream);

	}

	/**
	 * Get event writer to write the file
	 * 
	 * @param OutputStream
	 */
	public XMLEventWriter getEventWriter(OutputStream os)
			throws XMLStreamException, IOException {

		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLEventWriter writer = factory.createXMLEventWriter(os);
		return writer;
	}

	/**
	 * initialize event reader, writer as well as validate required parameters
	 */
	public void initializeXMLEvents(boolean initWriter,
			boolean initEventFactory, OutputStream os, InputStream is,
			String operationType) {
		try {
			validateParams(os, is, operationType);

			eventReader = getEventReader(is);

			if (initWriter) {
				writer = getEventWriter(os);
			}

			if (initEventFactory) {
				eventFactory = XMLEventFactory.newInstance();
			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, "=== Exception ===> " + e.getMessage());
			e.printStackTrace();
		}
	}

}
