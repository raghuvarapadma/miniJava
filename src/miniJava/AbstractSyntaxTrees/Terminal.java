/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

abstract public class Terminal extends AST {

  public Terminal (Token t) {
	super(t.getSourcePosition());
    spelling = t.getSpelling();
    kind = t.getTokenKind();
  }

  public TokenKind kind;
  public String spelling;
}
