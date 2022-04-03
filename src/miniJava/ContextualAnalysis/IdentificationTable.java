package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Declaration;

import java.util.*;

public class IdentificationTable {

	private Stack<HashMap<String,Declaration>> stack;
	private int level;

	public IdentificationTable() {
		this.stack = new Stack<>();
		level = 0;
	}

	public void openScope() {
		this.stack.push(new HashMap<>());
		this.level++;
	}

	public void closeScope() {
		stack.pop();
		this.level--;
	}

	public void enter (String string, Declaration declaration){
		HashMap<String, Declaration> levelFourHashMap = this.stack.pop();
		if (level == 4) {
			HashMap<String, Declaration> levelThreeHashMap = this.stack.pop();
			if (levelFourHashMap.containsKey(string) || levelThreeHashMap.containsKey(string)) {
				System.out.println("*** line " + declaration.posn.start + ": Identification Error + Each scope level can " +
						"only have at most one declaration for an identifier!");
				throw new ContextualAnalysisException();
			} else {
				this.stack.push(levelThreeHashMap);
				levelFourHashMap.put(string, declaration);
				this.stack.push(levelFourHashMap);
			}
		}
		else {
			if (levelFourHashMap.containsKey(string)) {
				System.out.println("*** line " + declaration.posn.start + ": Identification Error + Declarations at level" +
						" 4 or higher may not hide declarations at levels 3 or higher!");
				throw new ContextualAnalysisException();
			} else {
				levelFourHashMap.put(string, declaration);
				this.stack.push(levelFourHashMap);
			}
		}
	}

	public Declaration retrieve(String string) {
		ArrayList<HashMap<String, Declaration>> hashMapArrayList = new ArrayList<>();
		Iterator<HashMap<String, Declaration>> hashMapIterator = stack.iterator();
		HashMap<String, Declaration> hashMap;
		Declaration returnDeclaration = null;

		while (hashMapIterator.hasNext()) {
			hashMap = stack.pop();
			hashMapArrayList.add(hashMap);
			if (hashMap.containsKey(string)) {
				returnDeclaration = hashMap.get(string);
				break;
			}
		}

		Collections.reverse(hashMapArrayList);

		for (HashMap<String, Declaration> hashmapReplace: hashMapArrayList) {
			stack.push(hashmapReplace);
		}

		return returnDeclaration;
	}
}
