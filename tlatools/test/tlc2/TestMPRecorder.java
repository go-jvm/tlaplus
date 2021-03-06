/*******************************************************************************
 * Copyright (c) 2015 Microsoft Research. All rights reserved. 
 *
 * The MIT License (MIT)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *   Markus Alexander Kuppe - initial API and implementation
 ******************************************************************************/

package tlc2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import tlc2.output.EC;

public class TestMPRecorder extends tlc2.output.MPRecorder {
	private final Map<Integer, List<Object>> records = new HashMap<Integer, List<Object>>();
	
	public void record(int code, Object... objects) {
		if(!records.containsKey(code)) {
			records.put(code, new ArrayList<Object>());
		}
		records.get(code).add(objects);
	}

	public boolean recorded(int code) {
		return records.containsKey(code);
	}

	public List<Object> getRecords(int code) {
		return records.get(code);
	}
	
	private List<Object> getRecordsOrDefault(final int code, final List<Object> defaultValue) {
		return records.getOrDefault(code, defaultValue);
	}

	public int getRecordAsInt(int code) {
		return Integer.parseInt(((String[]) records.get(code).get(0))[0]);
	}
	
	// This is a best effort implementation that only checks the first
	// elements of the nested records and contained arrays
	public boolean recordedWithStringValue(int code, String str) {
		try {
			return recordedWithStringValueAt(code, str, 0);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean recordedWithSubStringValue(int code, String substring) {
		return recordedWithSubStringValue(code, substring, 0);
	}
	
	public boolean recordedWithSubStringValue(int code, String substring, int idx) {
		try {
			Object object = records.get(code).get(0);
			if (object instanceof String[]) {
				String[] strs = (String[]) object;
				for (String string : strs) {
					if (string.contains(substring)) {
						return true;
					}
				}
				return false;
			} else if (object instanceof String) {
				return ((String) object).contains(substring);
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean recordedWithStringValueAt(int code, String str, int idx) {
		try {
			Object object = records.get(code).get(0);
			if (object instanceof String[]) {
				String[] strs = (String[]) object;
				return strs[idx].equals(str);
			} else if (object instanceof String) {
				return object.equals(str);
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean recordedWithStringValues(int code, String... strings) {
		int i = 0;
		for (String string : strings) {
			if (!recordedWithStringValueAt(code, string, i++)) {
				return false;
			}
		}
		return true;
	}

	public String getCoverageRecords() {
		final List<Object> coverages = getRecords(EC.TLC_COVERAGE_VALUE);
		String out = "";
		if (coverages == null) {
			return out;
		}
		for (final Object o : coverages) {
			final String[] coverage = (String[]) o;
			out += coverage[0] + ": " + Integer.parseInt(coverage[1]) + "\n";
		}
		return out;
	}
	
	public List<Coverage> getZeroCoverage() {
		return getCoverage((Predicate<? super String[]>) o -> Long.valueOf(((String[]) o)[1]) == 0);
	}
	
	public List<Coverage> getNonZeroCoverage() {
		return getCoverage((Predicate<? super String[]>) o -> Long.valueOf(((String[]) o)[1]) > 0);
	}

	private List<Coverage> getCoverage(Predicate<? super String[]> p) {
		final List<Object> coverages = getRecordsOrDefault(EC.TLC_COVERAGE_VALUE, new ArrayList<>(0));
		return coverages.stream().map(o -> (String[]) o).filter(p)
				.map(a -> new Coverage(a)).collect(Collectors.toList());
	}
	
	public static class Coverage {
		private final String line;
		private final long count;
		
		public Coverage(String[] line) {
			this.line = line[0].trim();
			this.count = Long.valueOf(line[1].trim());
		}
		
		public String getLine() {
			return line;
		}

		public long getCount() {
			return count;
		}
		
		public boolean isZero() {
			return count == 0L;
		}

		@Override
		public String toString() {
			return "Coverage [line=" + line + ", count=" + count + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (count ^ (count >>> 32));
			result = prime * result + ((line == null) ? 0 : line.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Coverage other = (Coverage) obj;
			if (count != other.count)
				return false;
			if (line == null) {
				if (other.line != null)
					return false;
			} else if (!line.equals(other.line))
				return false;
			return true;
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuffer buf = new StringBuffer(records.size());
		for(Integer key : records.keySet()) {
			final List<Object> list = records.get(key);
			for (Object elem : list) {
				if (elem instanceof String[]) {
					String[] strs = (String[]) elem;
					for (String s : strs) {
						buf.append(key);
						buf.append(" -> ");
						buf.append(s);
						buf.append("\n");
					}
				} else if (elem instanceof String) {
					buf.append(key);
					buf.append(" -> ");
					buf.append(elem);
					buf.append("\n");
				}
			}
		}
		return buf.toString();
	}
}
