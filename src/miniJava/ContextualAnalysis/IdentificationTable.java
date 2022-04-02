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
		HashMap<String, Declaration> hashMap = this.stack.pop();
		if (level == 4) {
			HashMap<String, Declaration> hashMapParameters = this.stack.pop();
			if (hashMapParameters.containsKey(string)) {
				System.out.println("idk");
			} else {
				this.stack.push(hashMapParameters);
				hashMap.put(string, declaration);
				this.stack.push(hashMap);
			}
		}
		else {
			if (hashMap.containsKey(string)) {
				System.out.println("idk");
				// throw new ContextualAnalysisException("Cannot have an identifier with the same name in the same scope!");
			} else {
				hashMap.put(string, declaration);
				this.stack.push(hashMap);
			}
		}
	}

	public Declaration retrieve(String string) {
		ArrayList<HashMap<String, Declaration>> arrayList = new ArrayList<>();
		Iterator<HashMap<String, Declaration>> iterator = stack.iterator();
		HashMap<String, Declaration> hashMap;
		Declaration returnDeclaration = null;

		while (iterator.hasNext()) {
			hashMap = stack.pop();
			arrayList.add(hashMap);
			if (hashMap.containsKey(string)) {
				returnDeclaration = hashMap.get(string);
				break;
			}
		}

		Collections.reverse(arrayList);

		for (HashMap<String, Declaration> hashmapReplace: arrayList) {
			stack.push(hashmapReplace);
		}

		return returnDeclaration;
	}
}
