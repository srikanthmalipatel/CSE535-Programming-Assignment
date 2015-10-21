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
	long timeTaken;
	
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
				result += (t.m_sTerm + ", ");
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
					
					pList = "Ordered by doc IDs: ";
					if(!m_termHash.containsKey(terms[i])) {
						pList += "Term not found";
						pList2 += "Term not found";
					}
					else {
						LinkedList<postingNode> td = m_termHash.get(terms[i]).m_docIdPL;
						LinkedList<postingNode> tf = m_termHash.get(terms[i]).m_termFreqPL;
						for(int j=0; j<td.size(); j++) {
							pList += td.get(j).m_docId + " ";
							pList2 += tf.get(j).m_docId + " ";
						}
					}
					outLog.write(pList);
					outLog.newLine();
					outLog.write(pList2);
					outLog.newLine();
				}
				String[] orderedTerms;
				orderedTerms = Arrays.copyOf(terms, terms.length);
				Arrays.sort(orderedTerms, new Comparator<String>() {
					public int compare(String p1, String p2) {
						int tf1 = (m_termHash.containsKey(p1)) ? m_termHash.get(p1).m_nDocCount : 0;
						int tf2 =  (m_termHash.containsKey(p1)) ? m_termHash.get(p2).m_nDocCount: 0;
						return tf1 < tf2 ? -1 : 0;
					}
				});
				
				long timeS = System.currentTimeMillis();
				termAtATimeQueryAnd(terms,false);
				termAtATimeQueryAnd(orderedTerms,true);
				termAtATimeQueryOr(terms,false);
				termAtATimeQueryOr(orderedTerms,true);
				docAtATimeQueryAnd(terms);
				docAtATimeQueryOr(terms);
				long timeE = System.currentTimeMillis();
				//System.out.println("Time Taken: " + (timeE-timeS)/1000.0);
			}
			bufferedReader.close();
			
		} catch(IOException e) {
			System.out.println("Error opening query file");
		}
	}
	
	public String termAtATimeQueryAnd(String[] qTerms, boolean isOptimized) {
		// get the first term and fill the linked list with them
		long timeS = System.currentTimeMillis();
		
		boolean andFailed = false;
		LinkedList<postingNode> termFreq = null;
		if(m_termHash.containsKey(qTerms[0]))
			termFreq = m_termHash.get(qTerms[0]).m_termFreqPL;
		else 
			andFailed = true;
		LinkedList<postingNode> result = new LinkedList<postingNode>(termFreq);
		
		int compareCount = 0;
		for(int i=1; i<qTerms.length; i++) {
			if(!m_termHash.containsKey(qTerms[i])) {
				//System.out.println("Terms not found " + qTerms[i] );
				andFailed = true;
				break;
			}
			
			// get the termFreqPL posting list for this term
			termFreq = m_termHash.get(qTerms[i]).m_termFreqPL;
			for(int j=0; j<result.size(); j++) {
				boolean isFound = false;
				//System.out.println("Searching for docID in result: " + result.get(j).m_docId);
				for(int k=0; k<termFreq.size(); k++) {
					compareCount++;
					if(result.get(j).m_docId == termFreq.get(k).m_docId) {
						//System.out.println("Matching docID: "+ result.get(j).m_docId);
						isFound = true;
						break;
					}
				}
				if(!isFound) {
					//System.out.println("removing doc ID: " + result.get(j).m_docId);
					result.remove(j);
					j--;
				}
			}
			//System.out.println("Result size: "+result.size());
		}
		if(!andFailed) {
			result.sort(new Comparator<postingNode>() {
				public int compare(postingNode p1, postingNode p2) {
					return p1.m_docId < p2.m_docId ? -1 : 1;
				}
			});
		}
		long timeE = System.currentTimeMillis();
		timeTaken = (timeE-timeS)/1000;
		
		try {
			String resString = null;
			if(!isOptimized) {	
				//System.out.println("Printing to stdout");
				resString = "Function: termAtATimeQueryAnd ";
				for(int i=0; i<qTerms.length; i++) {
					resString += qTerms[i] + ", ";
				}
				outLog.write(resString);
				outLog.newLine();
				
				int size;
				if(andFailed) size = 0;
				else size = result.size();
				outLog.write(size + " documents are found");
				outLog.newLine();
				
				outLog.write(compareCount + " comparisons are made");
				outLog.newLine();
				
				outLog.write((timeE-timeS)/1000.0 + " seconds are used");
				outLog.newLine();
			}
			else {
				outLog.write(compareCount + " comparisons are made after optimization");
				outLog.newLine();
			
				if(!andFailed) {
					resString = "Result: ";
					for(int i=0; i<result.size(); i++) {
						resString += (result.get(i).m_docId + ", ") ;
					}
				}
				else {
					resString = "Result: Terms not found";
				}
				outLog.write(resString);
				outLog.newLine();
			}
			
		} catch (IOException e) {
			System.out.println("Failed to log");
			e.printStackTrace();
		}
		return "";
	}
	
	public void termAtATimeQueryOr(String[] qTerms, boolean isOptimized) {
		long timeS = System.currentTimeMillis();
		
		LinkedList<postingNode> termFreq = m_termHash.get(qTerms[0]).m_termFreqPL;
		LinkedList<postingNode> result = new LinkedList<postingNode>(termFreq);
		
		int compareCount = 0;
		for(int i=1; i<qTerms.length; i++) {
			if(!m_termHash.containsKey(qTerms[i])) {
				continue;
			}
			// get the termFreqPL posting list for this term
			termFreq = m_termHash.get(qTerms[i]).m_termFreqPL;
			LinkedList<postingNode> temp = new LinkedList<postingNode>();
			
			for(int j=0; j<termFreq.size(); j++) {
				boolean isfound = false;
				for(int k=0; k<result.size(); k++) {
					compareCount++;
					if(result.get(k).m_docId == termFreq.get(j).m_docId) {
						isfound = true;
						break;
					}
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
		timeTaken = (timeE-timeS)/1000;
		
		try {
			String resString = null;
			if(!isOptimized) {
				resString = "Function: termAtATimeQueryOr ";
				for(int i=0; i<qTerms.length; i++) {
					resString += qTerms[i] + ", ";
				}
				outLog.write(resString);
				outLog.newLine();
				
				outLog.write(result.size() + " documents are found");
				outLog.newLine();
				
				outLog.write(compareCount + " comparisons are made");
				outLog.newLine();
				
				outLog.write((timeE-timeS)/1000.0 + " seconds are used");
				outLog.newLine();
			}
			else {
				outLog.write(compareCount + " comparisons are made after optimization");
				outLog.newLine();
				
				resString = "Result: ";
				if(result.size() == 0) {
					resString += " Terms not found";
				}
				else {
					for(int i=0; i<result.size(); i++) {
						resString += (result.get(i).m_docId + ", ") ;
					}
				}
				outLog.write(resString);
				outLog.newLine();
			}
			
		} catch (IOException e) {
			System.out.println("Failed to log");
			e.printStackTrace();
		} 
	}

	public void docAtATimeQueryAnd(String[] qTerms) {
		long timeS = System.currentTimeMillis();
		int[] listIx = new int[qTerms.length];
		LinkedList<String> result = new LinkedList<String>();
		
		int[] docIdCount = new int[qTerms.length];
		boolean andFailed = false;
		for(int i=0; i<qTerms.length; i++) {
			if(!m_termHash.containsKey(qTerms[i])) {
				andFailed = true;
				break;
			}
			docIdCount[i] = m_termHash.get(qTerms[i]).m_nDocCount;
		}
		
		int compareCount = 0;
		boolean allListsMerged = false;
		boolean foundMax = false;
		
		while(true && !andFailed) {
			if(allListsMerged) {
				break;
			}
			int maxDocId = Integer.MIN_VALUE;
			int maxTerm = Integer.MIN_VALUE;
			// find the maximum docId among all query terms at current index
			if(!foundMax) {
				for(int i=0; i<listIx.length; i++) {
					// get the termObject of a query term
					LinkedList<postingNode> docId = m_termHash.get(qTerms[i]).m_docIdPL;
					if(docId.get(listIx[i]).m_docId > maxDocId) {
						maxDocId = docId.get(listIx[i]).m_docId;
						maxTerm = i;
					}
					compareCount++;
				}
				foundMax = true;	
			}
			// Check if all the lists at their current Index have same element which is maxDocId
			boolean isFound = true;
			for(int i=0; i<listIx.length; i++) {
				if(maxTerm != i) {
					LinkedList<postingNode> docId = m_termHash.get(qTerms[i]).m_docIdPL;
					for(int j=0; j<docId.size(); j++) {
						if(listIx[i] == docIdCount[i]) {
							allListsMerged = true;
							break;
						}
						compareCount++;
						if(docId.get(listIx[i]).m_docId < maxDocId) {
							listIx[i]++;
							if(listIx[i] == docIdCount[i]) {
								isFound = false;
								allListsMerged = true;
								break;
							}
						}
						else {
							if(docId.get(listIx[i]).m_docId > maxDocId) {
								isFound = false;
								foundMax = false;
							}
							break;
						}
					}
				}
			}
			if(isFound) {
				result.add(Integer.toString(maxDocId));
				for(int i=0; i<listIx.length; i++) {
					listIx[i]++;
					if(listIx[i] == docIdCount[i]) {
						allListsMerged = true;
					}
				}
				foundMax = false;
			}
		}
		
		long timeE = System.currentTimeMillis();
		try {
			String resString = "Function: docAtATimeQueryAnd ";
			for(int i=0; i<qTerms.length; i++) {
				resString += qTerms[i] + ", ";
			}
			outLog.write(resString);
			outLog.newLine();
			
			outLog.write(result.size() + " documents are found");
			outLog.newLine();
			
			outLog.write(compareCount + " comparisons are made");
			outLog.newLine();
			
			outLog.write((timeE-timeS)/1000 + " seconds are used");
			outLog.newLine();
			
			if(!andFailed) {
				resString = "Result: ";
				for(int i=0; i<result.size(); i++) {
					resString += (result.get(i) + ", ") ;
				}
			}
			else {
				resString = "Result: Terms not found";
			}
			outLog.write(resString);
			outLog.newLine();
			
		} catch (IOException e) {
			System.out.println("Failed to log");
			e.printStackTrace();
		}
	}
	
	public void docAtATimeQueryOr(String[] qTermsOrg) {
		long timeS = System.currentTimeMillis();
		ArrayList<String> qTerms = new ArrayList<String>();
		int j=0;
		for(int i=0; i<qTermsOrg.length ; i++) {
			if(m_termHash.containsKey(qTermsOrg[i])) {
				qTerms.add(qTermsOrg[i]);
				j++;
			}
		}
		
		int[] listIx = new int[qTerms.size()];
		LinkedList<String> result = new LinkedList<String>();
		
		int[] docIdCount = new int[qTerms.size()];
		for(int i=0; i<qTerms.size(); i++) {
			docIdCount[i] = m_termHash.get(qTerms.get(i)).m_nDocCount;
		}
		
		int compareCount = 0;
		int listsToMergeCount = qTerms.size();
		
		while(true) {
			if(listsToMergeCount == 0) {
				break;
			}
			int minDocId = Integer.MAX_VALUE;
			// check if find the minimum docId among all query terms at current index
			for(int i=0; i<listIx.length; i++) {
				if(listIx[i] == -1) {
					continue;
				}
				// get the termObject of a query term
				LinkedList<postingNode> docId = m_termHash.get(qTerms.get(i)).m_docIdPL;
				if(docId.get(listIx[i]).m_docId < minDocId) {
					minDocId = docId.get(listIx[i]).m_docId;
				}
				compareCount++;
			}
			result.add(Integer.toString(minDocId));
			
			// add min docId to the result and if there are such in other lists then forward the Indexes by 1
			for(int i=0; i<listIx.length; i++) {
				if(listIx[i] == -1)
					continue;
				LinkedList<postingNode> docId = m_termHash.get(qTerms.get(i)).m_docIdPL;
				if(docId.get(listIx[i]).m_docId == minDocId) {
					listIx[i]++;
				}
				compareCount++;
				if(listIx[i] == docIdCount[i]) {
					listIx[i] = -1;
					listsToMergeCount--;
				}
			}
		}
		
		long timeE = System.currentTimeMillis();
		try {
			String resString = "Function: docAtATimeQueryOr ";
			for(int i=0; i<qTermsOrg.length; i++) {
				resString += qTermsOrg[i] + ", ";
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
			if(result.size() == 0) {
				resString += "Terms not found";
			}
			else {
				for(int i=0; i<result.size(); i++) {
					resString += (result.get(i) + ", ") ;
				}
			}
			outLog.write(resString);
			outLog.newLine();
			
		} catch (IOException e) {
			System.out.println("Failed to log");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		CSE535Assignment Assgn = new CSE535Assignment(args[0], args[2], args[1], args[3]);
	}
}
