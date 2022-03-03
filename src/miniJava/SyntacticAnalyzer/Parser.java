package miniJava.SyntacticAnalyzer;


import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Parser {
	private Token currentToken;
	private final Scanner scanner;

	public Parser(Scanner scanner) {
		this.scanner = scanner;
	}

	private Package parseProgram() throws SyntaxException {
		ClassDeclList classDeclList = new ClassDeclList();
		while (currentToken.getTokenKind() != TokenKind.EOT) {
			classDeclList.add(parseClassDeclaration());
		}
		accept(TokenKind.EOT);
		return new Package(classDeclList, null);
	}

	private ClassDecl parseClassDeclaration () throws SyntaxException {
		accept(TokenKind.CLASS);
		String className = currentToken.getSpelling();
		accept(TokenKind.IDENTIFIER);
		accept(TokenKind.L_CURLY_BRACKET);
		ClassDecl classDecl = parseFieldDeclaration(className);
		accept(TokenKind.R_CURLY_BRACKET);
		return classDecl;
	}

	private ClassDecl parseFieldDeclaration (String className) throws SyntaxException {
		FieldDeclList fieldDeclList = new FieldDeclList();
		MethodDeclList methodDeclList = new MethodDeclList();
		while (currentToken.getTokenKind() != TokenKind.R_CURLY_BRACKET) {
			boolean isPrivate = parseVisibility();
			boolean isStatic = parseAccess();
			if (currentToken.getTokenKind() == TokenKind.VOID) {
				accept(TokenKind.VOID);
				methodDeclList.add(parseMethodDeclaration(TokenKind.VOID, isPrivate, isStatic,
						new BaseType(TypeKind.VOID, null), null));
			} else {
				TypeDenoter typeDenoter = parseType();
				String name = currentToken.getSpelling();
				accept(TokenKind.IDENTIFIER);
				if (currentToken.getTokenKind() == TokenKind.SEMICOLON) {
					acceptIt();
					fieldDeclList.add(new FieldDecl(isPrivate, isStatic, typeDenoter, name, null));
				} else {
					methodDeclList.add(parseMethodDeclaration(TokenKind.SEMICOLON, isPrivate, isStatic, typeDenoter, name));
				}
			}
		}
		return new ClassDecl(className, fieldDeclList, methodDeclList, null);
	}

	private MethodDecl parseMethodDeclaration (TokenKind tokenKindBranch, boolean isPrivate, boolean isStatic,
																						 TypeDenoter typeDenoter, String name) throws SyntaxException {
		if (tokenKindBranch == TokenKind.VOID) {
			name = currentToken.getSpelling();
			accept(TokenKind.IDENTIFIER);
		}
		accept(TokenKind.L_PAREN);
		ParameterDeclList parameterDeclList = new ParameterDeclList();
		if (!(currentToken.getTokenKind() == TokenKind.R_PAREN)) {
			parameterDeclList = parseParameterList();
		}
		accept(TokenKind.R_PAREN);
		accept(TokenKind.L_CURLY_BRACKET);
		StatementList statementList = new StatementList();
		while (currentToken.getTokenKind() != TokenKind.R_CURLY_BRACKET) {
			statementList.add(parseStatement());
		}
		accept(TokenKind.R_CURLY_BRACKET);
		return new MethodDecl(new FieldDecl(isPrivate, isStatic, typeDenoter, name, null), parameterDeclList,
				statementList,null);
	}

	private ParameterDeclList parseParameterList () throws SyntaxException {
		ParameterDeclList parameterDeclList = new ParameterDeclList();
		TypeDenoter typeDenoter = parseType();
		String name = currentToken.getSpelling();
		parameterDeclList.add(new ParameterDecl(typeDenoter, name, null));
		accept(TokenKind.IDENTIFIER);
		while (currentToken.getTokenKind() != TokenKind.R_PAREN) {
			accept(TokenKind.COMMA);
			typeDenoter = parseType();
			name = currentToken.getSpelling();
			accept(TokenKind.IDENTIFIER);
			parameterDeclList.add(new ParameterDecl(typeDenoter, name, null));
		}
		return parameterDeclList;
	}

	private ExprList parseArgumentList() throws SyntaxException {
		ExprList exprList = new ExprList();
		exprList.add(parseExpression());
		while (currentToken.getTokenKind() != TokenKind.R_PAREN) {
			accept(TokenKind.COMMA);
			exprList.add(parseExpression());
		}
		return exprList;
	}

	private Statement parseStatement() throws SyntaxException {
		switch (currentToken.getTokenKind()) {
			case L_CURLY_BRACKET:
				acceptIt();
				StatementList statementList = new StatementList();
				while (currentToken.getTokenKind() != TokenKind.R_CURLY_BRACKET) {
					statementList.add(parseStatement());
				}
				accept(TokenKind.R_CURLY_BRACKET);
				return new BlockStmt(statementList, null);
			case INT: case BOOLEAN:
				TypeDenoter typeDenoter = parseType();
				String name = currentToken.getSpelling();
				accept(TokenKind.IDENTIFIER);
				accept(TokenKind.ASSIGNMENT_OPERATOR);
				Expression expressionVarDeclStmtIntBool = parseExpression();
				accept(TokenKind.SEMICOLON);
				return new VarDeclStmt(new VarDecl(typeDenoter, name, null), expressionVarDeclStmtIntBool, null);
			case IDENTIFIER:
				Token identifierInitial = currentToken;
				acceptIt();
				if (currentToken.getTokenKind() == TokenKind.IDENTIFIER) {
					Token identifierID = currentToken;
					acceptIt();
					accept(TokenKind.ASSIGNMENT_OPERATOR);
					Expression expressionVarDeclID = parseExpression();
					accept(TokenKind.SEMICOLON);
					return new VarDeclStmt(new VarDecl(new ClassType(new Identifier(identifierInitial), null),
							identifierID.getSpelling(),null), expressionVarDeclID, null);
				}
				else {
					Reference reference = new IdRef(new Identifier(identifierInitial), null);
					if (currentToken.getTokenKind() == TokenKind.PERIOD) {
						reference = parseReference(identifierInitial);
					}
					if (currentToken.getTokenKind() == TokenKind.ASSIGNMENT_OPERATOR) {
						acceptIt();
						Expression expressionAssignStmt = parseExpression();
						accept(TokenKind.SEMICOLON);
						return new AssignStmt(reference, expressionAssignStmt, null);
					} else if (currentToken.getTokenKind() == TokenKind.L_SQUARE_BRACKET) {
						acceptIt();
						if (currentToken.getTokenKind() == TokenKind.R_SQUARE_BRACKET) {
							acceptIt();
							Token varName = currentToken;
							accept(TokenKind.IDENTIFIER);
							accept(TokenKind.ASSIGNMENT_OPERATOR);
							Expression expressionIxAssignStmt =  parseExpression();
							accept(TokenKind.SEMICOLON);
							return new VarDeclStmt(new VarDecl(new ArrayType(new ClassType(new Identifier(identifierInitial),
									null), null), varName.getSpelling(), null), expressionIxAssignStmt,
									null);
						} else {
							Expression expressionIxAssignStmt1 =  parseExpression();
							accept(TokenKind.R_SQUARE_BRACKET);
							accept(TokenKind.ASSIGNMENT_OPERATOR);
							Expression expressionIxAssignStmt2 = parseExpression();
							accept(TokenKind.SEMICOLON);
							return new IxAssignStmt(reference, expressionIxAssignStmt1, expressionIxAssignStmt2, null);
						}
					} else if (currentToken.getTokenKind() == TokenKind.L_PAREN) {
						ExprList exprList = new ExprList();
						acceptIt();
						if (currentToken.getTokenKind() != TokenKind.R_PAREN) {
							exprList = parseArgumentList();
						}
						accept(TokenKind.R_PAREN);
						accept(TokenKind.SEMICOLON);
						return new CallStmt(reference, exprList, null);
					} else {
						throw new SyntaxException("There is an error parsing the statement. The current Token you provided is \""
								+ currentToken.getSpelling() + "\" (CASE IDENTIFIER).");
					}
				}
			case THIS:
				Reference reference = parseReference(null);
				if (currentToken.getTokenKind() == TokenKind.ASSIGNMENT_OPERATOR) {
					acceptIt();
					Expression expressionAssignStmt = parseExpression();
					accept(TokenKind.SEMICOLON);
					return new AssignStmt(reference, expressionAssignStmt, null);
				} else if (currentToken.getTokenKind() == TokenKind.L_SQUARE_BRACKET) {
					acceptIt();
					Expression expressionIxAssignStmt1 = parseExpression();
					accept(TokenKind.R_SQUARE_BRACKET);
					accept(TokenKind.ASSIGNMENT_OPERATOR);
					Expression expressionIxAssignStmt2 = parseExpression();
					accept(TokenKind.SEMICOLON);
					return new IxAssignStmt(reference, expressionIxAssignStmt1, expressionIxAssignStmt2, null);
				} else if (currentToken.getTokenKind() == TokenKind.L_PAREN) {
					acceptIt();
					ExprList exprList = new ExprList();
					if (currentToken.getTokenKind() != TokenKind.R_PAREN) {
						exprList = parseArgumentList();
					}
					accept(TokenKind.R_PAREN);
					accept(TokenKind.SEMICOLON);
					return new CallStmt(reference, exprList, null);
				} else {
					throw new SyntaxException("There is an error parsing the statement. The current Token you provided is \""
							+ currentToken.getSpelling() + "\" (CASE THIS).");
				}
			case RETURN:
				acceptIt();
				Expression expressionReturn = null;
				if (currentToken.getTokenKind() != TokenKind.SEMICOLON) {
					expressionReturn = parseExpression();
				}
				accept(TokenKind.SEMICOLON);
				return new ReturnStmt(expressionReturn, null);
			case IF:
				acceptIt();
				accept(TokenKind.L_PAREN);
				Expression expressionIf = parseExpression();
				accept(TokenKind.R_PAREN);
				Statement statementIf = parseStatement();
				if (currentToken.getTokenKind() == TokenKind.ELSE) {
					acceptIt();
					Statement statementElse = parseStatement();
					return new IfStmt(expressionIf, statementIf, statementElse, null);
				} else {
					return new IfStmt(expressionIf, statementIf, null);
				}
			case WHILE:
				acceptIt();
				accept(TokenKind.L_PAREN);
				Expression expressionWhile = parseExpression();
				accept(TokenKind.R_PAREN);
				Statement statementWhile = parseStatement();
				return new WhileStmt(expressionWhile, statementWhile, null);
			default:
				throw new SyntaxException("There is an error parsing the statement. The current Token you provided " +
					"is \"" + currentToken.getSpelling() + "\" (NO CASE FOUND).");
		}
	}

	private Expression parseExpression() throws SyntaxException {
		return parseDisjunctionExpr();
	}

	private Expression parseDisjunctionExpr() throws SyntaxException {
		Expression e1 = parseConjunctionExpr();
		while (currentToken.getTokenKind() == TokenKind.OR) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseConjunctionExpr();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}

	private Expression parseConjunctionExpr() throws SyntaxException {
		Expression e1 = parseEqualityExpr();
		while (currentToken.getTokenKind() == TokenKind.AND) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseEqualityExpr();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}

	private Expression parseEqualityExpr() throws SyntaxException {
		Expression e1 = parseRelationalExpr();
		while (currentToken.getTokenKind() == TokenKind.EQUIVALENCE_COMPARISON || currentToken.getTokenKind() ==
				TokenKind.NOT_EQUAL) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseRelationalExpr();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}

	private Expression parseRelationalExpr() throws SyntaxException {
		Expression e1 = parseAdditiveExpr();
		while (currentToken.getTokenKind() == TokenKind.LESS_THAN_EQUAL || currentToken.getTokenKind() ==
				TokenKind.LESS_THAN || currentToken.getTokenKind() == TokenKind.GREATER_THAN || currentToken.getTokenKind()
				== TokenKind.GREATER_THAN_EQUAL) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseAdditiveExpr();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}

	private Expression parseAdditiveExpr() throws SyntaxException {
		Expression e1 = parseMultiplicativeExpr();
		while (currentToken.getTokenKind() == TokenKind.ADD || currentToken.getTokenKind() == TokenKind.MINUS) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseMultiplicativeExpr();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}

	private Expression parseMultiplicativeExpr() throws SyntaxException {
		Expression e1 = parseUnaryExpr();
		while (currentToken.getTokenKind() == TokenKind.MULTIPLY || currentToken.getTokenKind() == TokenKind.DIVIDE) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseUnaryExpr();
			e1 = new BinaryExpr(op, e1, e2, null);
		}
		return e1;
	}

	private Expression parseUnaryExpr() throws SyntaxException {
		if (currentToken.getTokenKind() != TokenKind.NOT && currentToken.getTokenKind() != TokenKind.MINUS) {
			return parseExpressionFinal();
		} else {
			Operator op = new Operator(currentToken);
			acceptIt();
			return new UnaryExpr(op, parseUnaryExpr(), null);
		}
	}

	private Expression parseExpressionFinal() throws SyntaxException {
		switch (currentToken.getTokenKind()) {
			case IDENTIFIER: case THIS:
				Reference reference = parseReference(null);
				if (currentToken.getTokenKind() == TokenKind.L_PAREN) {
					acceptIt();
					ExprList exprList = new ExprList();
					if (currentToken.getTokenKind() != TokenKind.R_PAREN) {
						exprList = parseArgumentList();
					}
					accept(TokenKind.R_PAREN);
					return new CallExpr(reference, exprList, null);
				} else if (currentToken.getTokenKind() == TokenKind.L_SQUARE_BRACKET) {
					acceptIt();
					Expression expression = parseExpression();
					accept(TokenKind.R_SQUARE_BRACKET);
					return new IxExpr(reference, expression, null);
				} else {
					return new RefExpr(reference, null);
				}
			case L_PAREN:
				acceptIt();
				Expression expressionParen = parseExpression();
				accept(TokenKind.R_PAREN);
				return expressionParen;
			case NUM: case TRUE: case FALSE:
				Token tokenLiteral = currentToken;
				acceptIt();
				if (tokenLiteral.getTokenKind() == TokenKind.NUM) {
					return new LiteralExpr(new IntLiteral(tokenLiteral), null);
				} else if (tokenLiteral.getTokenKind() == TokenKind.TRUE || tokenLiteral.getTokenKind() == TokenKind.FALSE) {
					return new LiteralExpr(new BooleanLiteral(tokenLiteral), null);
				} else {
					throw new SyntaxException("Must pass in a TokenKind of NUM, TRUE, or FALSE!");
				}
			case NEW:
				acceptIt();
				if (currentToken.getTokenKind() == TokenKind.IDENTIFIER) {
					Token identifier = currentToken;
					acceptIt();
					if (currentToken.getTokenKind() == TokenKind.L_SQUARE_BRACKET) {
						acceptIt();
						Expression expression = parseExpression();
						accept(TokenKind.R_SQUARE_BRACKET);
						return new NewArrayExpr(new ClassType(new Identifier(identifier),null), expression, null);
					} else if (currentToken.getTokenKind() == TokenKind.L_PAREN) {
						acceptIt();
						accept(TokenKind.R_PAREN);
						return new NewObjectExpr(new ClassType(new Identifier(identifier), null), null);
					}
				} else if (currentToken.getTokenKind() == TokenKind.INT) {
					acceptIt();
					accept(TokenKind.L_SQUARE_BRACKET);
					Expression expression = parseExpression();
					accept(TokenKind.R_SQUARE_BRACKET);
					return new NewArrayExpr(new BaseType(TypeKind.INT, null), expression, null);
				}
				break;
			default:
				throw new SyntaxException("There is an error parsing the expression. The current Token you provided " +
					"is \"" + currentToken.getSpelling() + "\" (NO CASE FOUND).");
		}
		return null;
	}

	private boolean parseVisibility() throws SyntaxException {
		TokenKind privateOrPublic = currentToken.getTokenKind();
		if (privateOrPublic == TokenKind.PUBLIC || privateOrPublic == TokenKind.PRIVATE) {
			acceptIt();
		}
		return (privateOrPublic == TokenKind.PRIVATE);
	}

	private boolean parseAccess() throws SyntaxException {
		TokenKind isStatic = currentToken.getTokenKind();
		if (currentToken.getTokenKind() == TokenKind.STATIC) {
			acceptIt();
		}
		return (isStatic == TokenKind.STATIC);
	}

	private TypeDenoter parseType() throws SyntaxException {
		TokenKind tokenKind = currentToken.getTokenKind();
		switch (tokenKind) {
			case BOOLEAN:
				acceptIt();
				return new BaseType(TypeKind.BOOLEAN, null);
			case INT: case IDENTIFIER:
				TypeDenoter typeDenoter = null;
				if (currentToken.getTokenKind() == TokenKind.INT) {
					typeDenoter = new BaseType(TypeKind.INT, null);
				} else if (currentToken.getTokenKind() == TokenKind.IDENTIFIER) {
					typeDenoter = new ClassType(new Identifier(currentToken), null);
				}
				acceptIt();
				if (currentToken.getTokenKind() == TokenKind.L_SQUARE_BRACKET) {
					acceptIt();
					accept(TokenKind.R_SQUARE_BRACKET);
					return new ArrayType(typeDenoter, null);
				}
				return typeDenoter;
			default:
				throw new SyntaxException("\"There is an error parsing the type. The current Token you provided is \""
					+ currentToken.getSpelling() + "\" (EXPECTED TOKENS INT, BOOLEAN, IDENTIFIER).");
		}
	}

	private Reference parseReference(Token option) throws SyntaxException {
		Token currToken;
		if (option == null) {
			currToken = currentToken;
			if (currToken.getTokenKind() == TokenKind.IDENTIFIER || currToken.getTokenKind() == TokenKind.THIS) {
				acceptIt();
			} else {
				throw new SyntaxException("parseReference() needs a TokenKind of IDENTIFIER or THIS.");
			}
		} else {
			currToken = option;
		}
		if (currentToken.getTokenKind() == TokenKind.PERIOD) {
			Reference qualRef;
			if (currToken.getTokenKind() == TokenKind.IDENTIFIER) {
				qualRef = new IdRef(new Identifier(currToken), null);
			} else {
				qualRef = new ThisRef(null);
			}
			while (currentToken.getTokenKind() == TokenKind.PERIOD) {
				acceptIt();
				if (currentToken.getTokenKind() == TokenKind.IDENTIFIER) {
					qualRef = new QualRef(qualRef, new Identifier(currentToken), null);
					acceptIt();
				} else {
					throw new SyntaxException("\"There is an error parsing the reference. The current Token you provided is \"" +
							currentToken.getSpelling() + "\" (EXPECTED TOKEN IDENTIFIER).");
				}
			}
			return qualRef;
		} else {
			if (currToken.getTokenKind() == TokenKind.IDENTIFIER) {
				return new IdRef(new Identifier(currToken), null);
			} else if (currToken.getTokenKind() == TokenKind.THIS) {
				return new ThisRef(null);
			} else {
				throw new SyntaxException("parseReference() needs a TokenKind of IDENTIFIER or THIS.");
			}
		}
	}

	private void acceptIt() throws SyntaxException {
		accept(currentToken.getTokenKind());
	}

	private void accept(TokenKind expectedToken) throws SyntaxException {
		if (currentToken.getTokenKind() == expectedToken) {
			if (currentToken.getTokenKind() != TokenKind.EOT) {
				currentToken = scanner.scan();
			}
		} else {
			throw new SyntaxException("There is an error accepting the Token. The current Token you provided is \"" +
					currentToken.getSpelling() + "\".");
		}
	}

	public Package parse () throws SyntaxException {
		currentToken = scanner.scan();
		return parseProgram();
	}
}
