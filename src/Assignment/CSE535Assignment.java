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
	
	public CSE535Assignment(String fileName, String kVal, String outFile, String queryFile) {
		k = Integer.parseInt(kVal);
		// open/create output log
		try {
			File output = new File(outFile);
			if(!output.exists()) {
				output.createNewFile();
			}
			FileWriter fLog = new FileWriter(output.getAbsoluteFile());
			outLog = new BufferedWriter(fLog);
			m_termHash = new HashMap<String, termObject>();
			indexFile(fileName);
			
			processQueryFile(queryFile);
			outLog.close();
			
		} catch (IOException e) {
			System.out.println("Error creating file" + outFile);
			e.printStackTrace();
		}
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
			outLog.write("Function: getTopK "+k);
			outLog.newLine();
			String result = "Result: ";
			for(int i=0; i<k; i++) {
				termObject t = topK.poll();
				result += (t.m_sTerm + " ");
			}
			outLog.write(result);
			outLog.newLine();
			
			bufferedReader.close();
		}
		catch(IOException ex) {
			System.out.println("Error reading file" + fileName);
		}
	}
	
	public void processQueryFile(String qFile) {
		try {
			FileReader fileReader = new FileReader(qFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			String query = null;
			while((query = bufferedReader.readLine()) != null) {
				String[] terms = query.split(" ");
				for(int i=0; i<terms.length; i++) {
					String pList = "Function: getPostings " + terms[i], pList2 = "Ordered by TF: ";
					outLog.write(pList);
					outLog.newLine();
					LinkedList<postingNode> td = m_termHash.get(terms[i]).m_docIdPL;
					LinkedList<postingNode> tf = m_termHash.get(terms[i]).m_termFreqPL;
					pList = "Ordered by doc IDs: ";
					for(int j=0; j<td.size(); j++) {
						pList += td.get(j).m_docId + " ";
						pList2 += tf.get(j).m_docId + " ";
					}
					outLog.write(pList);
					outLog.newLine();
					outLog.write(pList2);
					outLog.newLine();
				}
				termAtATimeQueryAnd(terms);
				termAtATimeQueryOr(terms);
			}
			bufferedReader.close();
			
		} catch(IOException e) {
			System.out.println("Error opening query file");
		}
	}
	
	public void termAtATimeQueryAnd(String[] qTerms) {
		// get the first term and fill the linked list with them
		long timeS = System.currentTimeMillis();
		
		LinkedList<postingNode> termFreq = m_termHash.get(qTerms[0]).m_termFreqPL;
		LinkedList<postingNode> result = new LinkedList<postingNode>(termFreq);
		
		int compareCount = 0;
		for(int i=1; i<qTerms.length; i++) {
			// get the termFreqPL posting list for this term
			termFreq = m_termHash.get(qTerms[i]).m_termFreqPL;
			for(int j=0; j<result.size(); j++) {
				boolean isFound = false;
				//System.out.println("Searching for docID in result: " + result.get(j).m_docId);
				for(int k=0; k<termFreq.size(); k++) {
					if(result.get(j).m_docId == termFreq.get(k).m_docId) {
						//System.out.println("Matching docID: "+ result.get(j).m_docId);
						isFound = true;
					}
					compareCount++;
				}
				if(!isFound) {
					//System.out.println("removing doc ID: " + result.get(j).m_docId);
					result.remove(j);
					j--;
				}
			}
			//System.out.println("Result size: "+result.size());
		}
		result.sort(new Comparator<postingNode>() {
	        public int compare(postingNode p1, postingNode p2) {
	            return p1.m_docId < p2.m_docId ? -1 : 1;
	        }
	    });
		long timeE = System.currentTimeMillis();
		
		try {
			String resString = "Function: termAtATimeQueryAnd ";
			for(int i=0; i<qTerms.length; i++) {
				resString += qTerms[i] + " ";
			}
			outLog.write(resString);
			outLog.newLine();
			
			outLog.write(result.size() + " documents are found");
			outLog.newLine();
			
			outLog.write(compareCount + " comparisons are made");
			outLog.newLine();
			
			outLog.write((timeE-timeS)/1000 + " seconds are used");
			outLog.newLine();
			
			resString = "Result: ";
			for(int i=0; i<result.size(); i++) {
				resString += (result.get(i).m_docId + " ") ;
			}
			outLog.write(resString);
			outLog.newLine();
			
		} catch (IOException e) {
			System.out.println("Failed to log");
			e.printStackTrace();
		}	
	}
	
	public void termAtATimeQueryOr(String[] qTerms) {
		long timeS = System.currentTimeMillis();
		try {
			for(int i=0; i<qTerms.length; i++) {
				String pList = "Function: getPostings " + qTerms[i], pList2 = "Ordered by TF: ";
				outLog.write(pList);
				outLog.newLine();
				LinkedList<postingNode> td = m_termHash.get(qTerms[i]).m_docIdPL;
				LinkedList<postingNode> tf = m_termHash.get(qTerms[i]).m_termFreqPL;
				pList = "Ordered by doc IDs: ";
				for(int j=0; j<td.size(); j++) {
					pList += td.get(j).m_docId + " ";
					pList2 += tf.get(j).m_docId + " ";
				}
				outLog.write(pList);
				outLog.newLine();
				outLog.write(pList2);
				outLog.newLine();
			}
		} catch(IOException io) {
			System.out.println("Failed to log");
		}
		
		LinkedList<postingNode> termFreq = m_termHash.get(qTerms[0]).m_termFreqPL;
		LinkedList<postingNode> result = new LinkedList<postingNode>(termFreq);
		
		int compareCount = 0;
		for(int i=1; i<qTerms.length; i++) {
			// get the termFreqPL posting list for this term
			termFreq = m_termHash.get(qTerms[i]).m_termFreqPL;
			LinkedList<postingNode> temp = new LinkedList<postingNode>();
			
			for(int j=0; j<termFreq.size(); j++) {
				boolean isfound = false;
				for(int k=0; k<result.size(); k++) {
					if(result.get(k).m_docId == termFreq.get(j).m_docId) {
						isfound = true;
					}
					compareCount++;
				}
				if(!isfound) {
					temp.add(termFreq.get(j));
				}
			}
			result.addAll(temp);
			
			//System.out.println("Result size: "+result.size());
		}
		result.sort(new Comparator<postingNode>() {
	        public int compare(postingNode p1, postingNode p2) {
	            return p1.m_docId < p2.m_docId ? -1 : 1;
	        }
	    });
		long timeE = System.currentTimeMillis();
		
		try {
			String resString = "Function: termAtATimeQueryOr ";
			for(int i=0; i<qTerms.length; i++) {
				resString += qTerms[i] + " ";
			}
			outLog.write(resString);
			outLog.newLine();
			
			outLog.write(result.size() + " documents are found");
			outLog.newLine();
			
			outLog.write(compareCount + " comparisons are made");
			outLog.newLine();
			
			outLog.write((timeE-timeS)/1000 + " seconds are used");
			outLog.newLine();
			
			resString = "Result: ";
			for(int i=0; i<result.size(); i++) {
				resString += (result.get(i).m_docId + " ") ;
			}
			outLog.write(resString);
			outLog.newLine();
			
		} catch (IOException e) {
			System.out.println("Failed to log");
			e.printStackTrace();
		}
		
	}

	public void docAtATimeQueryAnd(String[] qTerms) {
		
	}
	
	public void docAtATimeQueryOr(String[] qTerms) {
		
	}
	
	public static void main(String[] args) {
		CSE535Assignment Assgn = new CSE535Assignment(args[0], args[2], args[1], args[3]);
	}
}
