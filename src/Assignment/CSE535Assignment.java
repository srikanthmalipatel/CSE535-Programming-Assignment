/**
 * 
 */
package Assignment;

import java.io.*;
import java.util.*;

/**
 * @author SrikanthMalipatel
 *
 */
public class CSE535Assignment {
	int k;
	HashMap<String, termObject> m_termHash;
	BufferedWriter outLog;
	
	public CSE535Assignment(String fileName, String kVal, String outFile) {
		k = Integer.parseInt(kVal);
		// open/create output log
		try {
			File output = new File(outFile);
			if(!output.exists()) {
				output.createNewFile();
			}
			FileWriter fLog = new FileWriter(output.getAbsoluteFile());
			outLog = new BufferedWriter(fLog);
		} catch (IOException e) {
			System.out.println("Error creating file" + outFile);
			e.printStackTrace();
		}
		m_termHash = new HashMap<String, termObject>();
		indexFile(fileName);
	}
	
	// This function parses and indexes data from .idx file
	public void indexFile(String fileName) {
		PriorityQueue<termObject> topK = new PriorityQueue<termObject>(k, new Comparator<termObject>() {
			@Override
			public int compare(termObject t1, termObject t2) {
				if(t1.m_nDocCount < t2.m_nDocCount) {
					return 1;
				} else if(t1.m_nDocCount > t2.m_nDocCount) {
					return -1;
				} else {
					return 0;
				}
			}
		});

		String line = null;
		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			// process one line at a time
			while((line = bufferedReader.readLine()) != null) {
				// parsing lines into tokens
				String fmLine = line.replace("\\", "\\\\");
				String[] tokens = fmLine.split("\\\\");
				
				// create and build the term object
				termObject term = new termObject(tokens[0], Integer.parseInt(tokens[2].replace("c", "")));

				// parse the posting list
				String[] fmList = (tokens[4].replace("m[", "").replace("]", "")).split(",");
				for(int i=0; i<fmList.length; i++) {
					String[] docIdAndFreq = fmList[i].replace(" ","").split("/");
					postingNode node = new postingNode(Integer.parseInt(docIdAndFreq[0]), Integer.parseInt(docIdAndFreq[1]));
					
					// add this node into Linked list
					term.m_docIdPL.addFirst(node);
					term.m_termFreqPL.addFirst(node);
				}
				// sort posting list in increasing order of docID
				Collections.sort(term.m_docIdPL, new Comparator<postingNode>() {
			        public int compare(postingNode p1, postingNode p2) {
			            return p1.m_docId < p2.m_docId ? -1 : 1;
			        }
			    });
				// sort posting list in decreasing order of term frequency
				Collections.sort(term.m_termFreqPL, new Comparator<postingNode>() {
			        public int compare(postingNode p1, postingNode p2) {
			            return p1.m_termFreq < p2.m_termFreq ? 1 : p1.m_termFreq == p2.m_termFreq ? 0 : -1;
			        }
			    });
				// add the node into hashmap
				m_termHash.put(term.m_sTerm, term);
				// add term to the priority queue
				topK.add(term);
			}
			
			// Printing top K terms
			outLog.write("Function getTopK "+k);
			outLog.newLine();
			String result = "Result: ";
			for(int i=0; i<k; i++) {
				termObject t = topK.poll();
				result += (t.m_sTerm + " ");
			}
			outLog.write(result);
			outLog.newLine();
			outLog.close();
			
			bufferedReader.close();
		}
		catch(IOException ex) {
			System.out.println("Error reading file" + fileName);
		}
	}

	public static void main(String[] args) {
		CSE535Assignment Assgn = new CSE535Assignment(args[0], args[2], args[1]);
	}
}
