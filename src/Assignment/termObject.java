package Assignment;

import java.util.LinkedList;

class postingNode {
	int m_docId;
	int m_termFreq;
	
	public postingNode(int id, int freq) {
		m_docId = id;
		m_termFreq = freq;
	}
}

public class termObject {
	String m_sTerm;
	int m_nDocCount;
	LinkedList<postingNode> m_termFreqPL;
	LinkedList<postingNode> m_docIdPL;

	public termObject(String name, int count) {
		// TODO Auto-generated constructor stub
		m_sTerm = name;
		m_nDocCount = count;
		m_termFreqPL = new LinkedList<postingNode>();
		m_docIdPL = new LinkedList<postingNode>();
	}
}
