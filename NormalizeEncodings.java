/*
	NormalizeEncodings 1.0.0.0, Copyright (c) 2016 Ma_Sys.ma.
	For further info send an e-mail to Ma_Sys.ma@web.de.

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.file.*;
import java.nio.charset.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NormalizeEncodings {

	private static final boolean DEBUG = false;
	private static final int M = Integer.MAX_VALUE;

	/**
	 * This has been constructed to work well for German texts
	 *
	 * Be aware that this program has been created for learning purposes.
	 * The means of encoding-detection presented here is mainly to
	 * demonstrate a minimal means of recognizing many documents. It is
	 * by no means reliable to be used in international contexts and
	 * recognizes only very few types of encodings.
	 */
	private static final EncP[] COMMON_ENCODING_PATTERNS = {
		// UTF-32
		new EncP("UTF-32",       M, 0xff, 0xfe, 0x00, 0x00), // BOM
		new EncP("UTF-32",       M, 0x00, 0x00, 0xfe, 0xff), // BOM
		// UTF-32 w/ german texts means there will be ASCII (e.g. 0x20)
		// and thus three zero-bytes need to appear somewhere
		new EncP("UTF-32LE",     4, 0x20, 0x00, 0x00, 0x00),
		new EncP("UTF-32BE",     4, 0x00, 0x00, 0x00, 0x20),

		// UTF-16
		new EncP("UTF-16",       M, 0xfe, 0xff), // BE-BOM
		new EncP("UTF-16",       M, 0xff, 0xfe), // LE-BOM
		// 0x20 0x00 (space) is common for UTF-16 LE
		new EncP("UTF-16LE",     2, 0x20, 0x00),
		new EncP("UTF-16BE",     2, 0x00, 0x20),

		// UTF-8 / 0xc3 is common to appear before a German Umlaut..
		new EncP("UTF-8Y",       M, 0xef, 0xbb, 0xbf), // UTF-8 BOM
		new EncP("UTF-8",        1, 0xc3),
		new EncP("UTF-8",        1, 0xc2), // before PARA

		new EncP("ISO-8859-15",  1, 0xa4), // EURO
		new EncP("windows-1252", 1, 0x80), // EURO
		new EncP("windows-1252", 1, 0xe4), // ae
		new EncP("windows-1252", 1, 0xf6), // oe
		new EncP("windows-1252", 1, 0xfc), // ue
		new EncP("windows-1252", 1, 0xdf), // sz
		new EncP("windows-1252", 1, 0xc4), // AE
		new EncP("windows-1252", 1, 0xd6), // OE
		new EncP("windows-1252", 1, 0xdc), // UE
		new EncP("windows-1252", 1, 0xa7), // PARA

		new EncP("IBM00858",     1, 0xd5), // EURO (!= 850)
		new EncP("IBM00858",     1, 0x84), // ae
		new EncP("IBM00858",     1, 0x94), // oe
		new EncP("IBM00858",     1, 0x81), // ue
		new EncP("IBM00858",     1, 0xe1), // sz
		new EncP("IBM00858",     1, 0x8e), // AE
		new EncP("IBM00858",     1, 0x99), // OE
		new EncP("IBM00858",     1, 0x9a), // UE
		new EncP("IBM00858",     1, 0xf5), // PARA
	};

	private static class EncP { // Encoding Pattern

		private final String forEncoding;
		private final int div;
		private final byte[] pattern;

		/**
		 * @param div
		 *	In order to match patterns only on the correct
		 * 	boundaries, div configures valid offsets for this
		 * 	pattern. This pattern is only matched, if it is found
		 * 	at a byte position x for which x mod div = 0.
		 * 	A common hack to specify ``begin of file'' is threfore
		 *	to use div = Integer.MAX_INT to make sure, a
		 * 	position beyond 0 never matches.
		 */
		EncP(String forEncoding, int div, int... pattern) {
			this.forEncoding = forEncoding;
			this.div = div;
			byte[] pat = new byte[pattern.length];
			for(int i = 0; i < pattern.length; i++)
				pat[i] = (byte)(pattern[i] & 0xff);
			this.pattern = pat;
		}

		private boolean isMatching(byte[] data) {
			for(int i = 0; i < data.length - pattern.length;
								i += div) {
				boolean found = true;
				for(int j = 0; j < pattern.length; j++) {
					if(pattern[j] != data[i + j]) {
						found = false;
						break;
					}
				}
				if(found)
					return true;
			}
			return false;
		}

	}

	public static void main(String[] args) throws IOException {
		if(args.length != 2)
			usage();
		else
			process(Paths.get(args[0]), Paths.get(args[1]));
	}

	private static void usage() {
		System.out.println("NormalizeEncodings 1.0.0.0, Copyright " +
							"(c) 2016 Ma_Sys.ma.");
		System.out.println("For further info send an e-mail to " +
							"Ma_Sys.ma@web.de.");
		System.out.println();
		System.out.println("USAGE java NormalizeEncodings ZIP DST");
		System.out.println();
		System.out.println("Reads the zip file from the first " +
				"parameter and processes all contained ");
		System.out.println("files as suggested by the exercise from " +
				"TU-Darmstadt DKE 2016 number 8.6.");
		System.out.println("The resulting normalized data is written " +
				"to the ");
		System.out.println("file given as the second parameter.");
	}

	private static void process(Path zipP, Path dstP) throws IOException {
		try(ZipFile zip = new ZipFile(zipP.toFile(), UTF_8)) {
			BufferedWriter wr = Files.newBufferedWriter(dstP,
									UTF_8);
			try {
				TreeMap<String,ZipEntry> sortedEntries =
							getSortedEntries(zip);
				for(Map.Entry<String,ZipEntry> e: sortedEntries.
								entrySet()) {
					processElement(zip, wr, e);
				}
			} finally {
				wr.close();
			}
		}
	}

	private static TreeMap<String,ZipEntry> getSortedEntries(ZipFile zip) {
		TreeMap<String,ZipEntry> ret = new TreeMap<String,ZipEntry>();
		Enumeration<? extends ZipEntry> ze = zip.entries();
		while(ze.hasMoreElements()) {
			ZipEntry e = ze.nextElement();
			if(e.isDirectory()) // skip directories.
				continue;
			String name = e.getName().toLowerCase();
			int pos1 = name.lastIndexOf('/') + 1;
			int pos2 = name.lastIndexOf('.');
			if(pos2 == -1)
				pos2 = name.length();
			ret.put(name.substring(pos1, pos2), e);
		}
		return ret;
	}

	private static void processElement(ZipFile zip, BufferedWriter wr,
			Map.Entry<String,ZipEntry> entry) throws IOException {
		byte[] data = getData(zip, entry.getValue());
		String enc = determineEncoding(data);
		if(DEBUG)
			System.err.println("Recognized " + entry.getKey() +
								" as " + enc);
		// Special handling to process documents which are UTF-8 with
		// byte order mark.
		int off = 0;
		int len = data.length;
		if(enc.equals("UTF-8Y")) {
			off += 3;
			len -= 3;
			enc = "UTF-8";
		}
		// TODO z It might be nice to use the CharsetDecoder as
		// suggested by the Java API documentation, but it is so
		// much more complex that for now, this simple method has
		// to suffice.
		String[] lines = new String(data, off, len, makeCharset(enc)).
				// Q&D Newline-Handling
				replace("\r\n", "\n").replace('\r', '\n').
				split("\n");
		processLines(entry.getKey(), lines, wr);
	}

	private static byte[] getData(ZipFile zip, ZipEntry entry)
							throws IOException {
		long len = entry.getSize();
		if(len > M)
			throw new IOException(
				"Encountered program limitation. This " +
				"application can only process text files " +
				"which are below " + M +
				" Bytes in uncompressed size."
			);

		// If this fails, you might want to increase the JVM memory
		// limits via a JVM argument like -Xmx4G or such.
		byte[] data = new byte[(int)len];

		InputStream in = zip.getInputStream(entry);
		try {
			int real = in.read(data, 0, data.length);
			assert(real == data.length);
		} finally {
			in.close();
		}

		return data;
	}

	private static String determineEncoding(byte[] data) {
		// We assume to have UTF-8 if none else matches because that
		// makes sense for ASCII (which -- as being a subset of all the
		// others -- is hard to detect by itself).
		String encFound = "UTF-8";
		for(EncP pattern: COMMON_ENCODING_PATTERNS) {
			if(pattern.isMatching(data)) {
				encFound = pattern.forEncoding;
				break;
			}
		}
		return encFound;
	}

	private static Charset makeCharset(String charset) {
		try {
			return Charset.forName(charset);
		} catch(IllegalArgumentException ex) {
			System.err.println("WARNING: Your JVM does not " +
						"provide charset " + charset +
						". Trying UTF-8 instead.");
			return UTF_8;
		}
	}

	private static void processLines(String recordID, String[] lines,
					BufferedWriter out) throws IOException {
		String type = "PRE";
		StringBuilder currentResult = new StringBuilder();
		for(String l: lines) {
			// This hack allows parsing data of those people who
			// just copied the PDF and thus have the line numbers
			// in their submissions
			l = l.replaceAll("^[0-9]+\t(.*)$", "$1");
			
			String lT = l.trim();
			if(lT.length() == 0)
				continue;
			if(lT.charAt(0) == '>') {
				flush(out, recordID, type, currentResult);
				type = getType(l);
			} else if(l.charAt(0) == '\t' || l.charAt(0) == ' ') {
				currentResult.append(' ');
				currentResult.append(lT);
			} else {
				flush(out, recordID, type, currentResult);
				currentResult.append(lT);
			}
		}
		flush(out, recordID, type, currentResult);
	}

	private static void flush(BufferedWriter out, String recordID,
			String type, StringBuilder result) throws IOException {
		if(result.length() == 0) // never flush empty line
			return;
		if(type.equals("PRE") || type.equals("UNKNOWN"))
			System.err.println("WARNING: unwanted data type " +
							"detected: " + type);
		out.write(recordID + '\t' + type + '\t' + result.toString() +
									'\n');
		result.setLength(0);
	}

	private static String getType(String line) {
		// This is designed to work even if decoding partially failed.
		// Also, this covers a few differnt spellings and variants.
		line = line.toLowerCase();
		if(line.indexOf("dom") != -1 && line.indexOf("ne") != -1)
			return "DOMAIN";
		else if(line.indexOf("beteiligt") != -1)
			return "PARTICIPANT";
		else if(line.indexOf("teristi") != -1 &&
						line.indexOf("daten") != -1)
			return "DATA";
		else if(line.indexOf("gesch") != -1 &&
						line.indexOf("lle") != -1)
			return "CASE";
		else if(line.indexOf("abfrage") != -1)
			return "QUERY";
		else if(line.indexOf("notizen") != -1 ||
					line.indexOf("interessant") != -1)
			return "NOTES";
		else
			return "UNKNOWN";
	}

}
