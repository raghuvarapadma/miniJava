package miniJava.SyntacticAnalyzer;


import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;


public class Parser {
	private Token currentToken;
	private final Scanner scanner;
	private SourcePosition previousTokenPosition;

	public Parser(Scanner scanner) {
		this.scanner = scanner;
		previousTokenPosition = new SourcePosition();
	}

	private Package parseProgram() throws SyntaxException {
		previousTokenPosition.start = 0;
		previousTokenPosition.finish = 0;
		ClassDeclList classDeclList = new ClassDeclList();
		while (currentToken.getTokenKind() != TokenKind.EOT) {
			classDeclList.add(parseClassDeclaration());
		}
		accept(TokenKind.EOT);
		return new Package(classDeclList, previousTokenPosition);
	}

	private ClassDecl parseClassDeclaration () throws SyntaxException {
		SourcePosition classSourcePosition = new SourcePosition();
		start(classSourcePosition);
		accept(TokenKind.CLASS);
		Token classNameToken = currentToken;
		String className = currentToken.getSpelling();
		accept(TokenKind.IDENTIFIER);
		accept(TokenKind.L_CURLY_BRACKET);
		ClassDecl classDecl = parseFieldDeclaration(className);
		accept(TokenKind.R_CURLY_BRACKET);
		finish(classSourcePosition);
		classDecl.posn = classSourcePosition;
		classDecl.type = new ClassType(new Identifier(classNameToken), classNameToken.getSourcePosition());
		return classDecl;
	}

	private ClassDecl parseFieldDeclaration (String className) throws SyntaxException {
		FieldDeclList fieldDeclList = new FieldDeclList();
		MethodDeclList methodDeclList = new MethodDeclList();
		while (currentToken.getTokenKind() != TokenKind.R_CURLY_BRACKET) {
			SourcePosition fieldOrMethodSourcePosition = new SourcePosition();
			SourcePosition methodFieldModifiers = new SourcePosition();
			start(fieldOrMethodSourcePosition);
			start(methodFieldModifiers);
			boolean isPrivate = parseVisibility();
			boolean isStatic = parseAccess();
			if (currentToken.getTokenKind() == TokenKind.VOID) {
				SourcePosition voidSourcePosition = new SourcePosition();
				start(voidSourcePosition);
				accept(TokenKind.VOID);
				finish(voidSourcePosition);
				methodDeclList.add(parseMethodDeclaration(TokenKind.VOID, isPrivate, isStatic, new BaseType(TypeKind.VOID,
						voidSourcePosition), null, methodFieldModifiers));
			} else {
				TypeDenoter typeDenoter = parseType();
				String name = currentToken.getSpelling();
				accept(TokenKind.IDENTIFIER);
				if (currentToken.getTokenKind() == TokenKind.SEMICOLON) {
					acceptIt();
					finish(fieldOrMethodSourcePosition);
					fieldDeclList.add(new FieldDecl(isPrivate, isStatic, typeDenoter, name, fieldOrMethodSourcePosition));
				} else {
					MethodDecl methodDecl = parseMethodDeclaration(TokenKind.SEMICOLON, isPrivate, isStatic, typeDenoter, name,
							methodFieldModifiers);
					finish(fieldOrMethodSourcePosition);
					methodDecl.posn = fieldOrMethodSourcePosition;
					methodDeclList.add(methodDecl);
				}
			}
		}
		return new ClassDecl(className, fieldDeclList, methodDeclList, null);
	}

	private MethodDecl parseMethodDeclaration (TokenKind tokenKindBranch, boolean isPrivate, boolean isStatic,
																						 TypeDenoter typeDenoter, String name, SourcePosition methodFieldModifiers)
			throws SyntaxException {
		if (tokenKindBranch == TokenKind.VOID) {
			name = currentToken.getSpelling();
			accept(TokenKind.IDENTIFIER);
		}
		finish(methodFieldModifiers);
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
		return new MethodDecl(new FieldDecl(isPrivate, isStatic, typeDenoter, name, methodFieldModifiers),
				parameterDeclList, statementList,null);
	}

	private ParameterDeclList parseParameterList () throws SyntaxException {
		ParameterDeclList parameterDeclList = new ParameterDeclList();
		SourcePosition parameterSourcePosition = new SourcePosition();
		start(parameterSourcePosition);
		TypeDenoter typeDenoter = parseType();
		String name = currentToken.getSpelling();
		accept(TokenKind.IDENTIFIER);
		finish(parameterSourcePosition);
		parameterDeclList.add(new ParameterDecl(typeDenoter, name, parameterSourcePosition));
		while (currentToken.getTokenKind() != TokenKind.R_PAREN) {
			accept(TokenKind.COMMA);
			start(parameterSourcePosition);
			typeDenoter = parseType();
			name = currentToken.getSpelling();
			accept(TokenKind.IDENTIFIER);
			finish(parameterSourcePosition);
			parameterDeclList.add(new ParameterDecl(typeDenoter, name, parameterSourcePosition));
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
		SourcePosition statementSourcePosition = new SourcePosition();
		start(statementSourcePosition);
		switch (currentToken.getTokenKind()) {
			case L_CURLY_BRACKET:
				acceptIt();
				StatementList statementList = new StatementList();
				while (currentToken.getTokenKind() != TokenKind.R_CURLY_BRACKET) {
					statementList.add(parseStatement());
				}
				accept(TokenKind.R_CURLY_BRACKET);
				finish(statementSourcePosition);
				return new BlockStmt(statementList, statementSourcePosition);
			case INT: case BOOLEAN:
				SourcePosition varDeclSourcePosition = new SourcePosition();
				start(varDeclSourcePosition);
				TypeDenoter typeDenoter = parseType();
				String name = currentToken.getSpelling();
				finish(varDeclSourcePosition);
				accept(TokenKind.IDENTIFIER);
				accept(TokenKind.ASSIGNMENT_OPERATOR);
				Expression expressionVarDeclStmtIntBool = parseExpression();
				accept(TokenKind.SEMICOLON);
				finish(statementSourcePosition);
				return new VarDeclStmt(new VarDecl(typeDenoter, name, varDeclSourcePosition), expressionVarDeclStmtIntBool,
						statementSourcePosition);
			case IDENTIFIER:
				SourcePosition arrayTypeSourcePosition = new SourcePosition();
				SourcePosition varDeclIdentifierSourcePosition = new SourcePosition();
				start(arrayTypeSourcePosition);
				start(varDeclIdentifierSourcePosition);
				Token identifierInitial = currentToken;
				SourcePosition identifierInitialSourcePosition = currentToken.getSourcePosition();
				acceptIt();
				if (currentToken.getTokenKind() == TokenKind.IDENTIFIER) {
					Token identifierID = currentToken;
					acceptIt();
					accept(TokenKind.ASSIGNMENT_OPERATOR);
					Expression expressionVarDeclID = parseExpression();
					accept(TokenKind.SEMICOLON);
					finish(statementSourcePosition);
					return new VarDeclStmt(new VarDecl(new ClassType(new Identifier(identifierInitial),
							identifierInitialSourcePosition), identifierID.getSpelling(), identifierInitialSourcePosition),
							expressionVarDeclID, statementSourcePosition);
				}
				else {
					Reference reference = new IdRef(new Identifier(identifierInitial), identifierInitialSourcePosition);
					if (currentToken.getTokenKind() == TokenKind.PERIOD) {
						reference = parseReference(identifierInitial);
					}
					if (currentToken.getTokenKind() == TokenKind.ASSIGNMENT_OPERATOR) {
						acceptIt();
						Expression expressionAssignStmt = parseExpression();
						accept(TokenKind.SEMICOLON);
						finish(statementSourcePosition);
						return new AssignStmt(reference, expressionAssignStmt, statementSourcePosition);
					} else if (currentToken.getTokenKind() == TokenKind.L_SQUARE_BRACKET) {
						acceptIt();
						if (currentToken.getTokenKind() == TokenKind.R_SQUARE_BRACKET) {
							acceptIt();
							finish(arrayTypeSourcePosition);
							Token varName = currentToken;
							accept(TokenKind.IDENTIFIER);
							finish(varDeclIdentifierSourcePosition);
							accept(TokenKind.ASSIGNMENT_OPERATOR);
							Expression expressionIxAssignStmt =  parseExpression();
							accept(TokenKind.SEMICOLON);
							finish(statementSourcePosition);
							return new VarDeclStmt(new VarDecl(new ArrayType(new ClassType(new Identifier(identifierInitial),
									identifierInitialSourcePosition), arrayTypeSourcePosition), varName.getSpelling(),
									varDeclIdentifierSourcePosition), expressionIxAssignStmt, statementSourcePosition);
						} else {
							Expression expressionIxAssignStmt1 =  parseExpression();
							accept(TokenKind.R_SQUARE_BRACKET);
							accept(TokenKind.ASSIGNMENT_OPERATOR);
							Expression expressionIxAssignStmt2 = parseExpression();
							accept(TokenKind.SEMICOLON);
							finish(statementSourcePosition);
							return new IxAssignStmt(reference, expressionIxAssignStmt1, expressionIxAssignStmt2,
									statementSourcePosition);
						}
					} else if (currentToken.getTokenKind() == TokenKind.L_PAREN) {
						ExprList exprList = new ExprList();
						acceptIt();
						if (currentToken.getTokenKind() != TokenKind.R_PAREN) {
							exprList = parseArgumentList();
						}
						accept(TokenKind.R_PAREN);
						accept(TokenKind.SEMICOLON);
						finish(statementSourcePosition);
						return new CallStmt(reference, exprList, statementSourcePosition);
					} else {
						throw new SyntaxException("There is an error parsing the statement. The current Token you provided is \""
								+ currentToken.getSpelling() + "\" (CASE IDENTIFIER).");
					}
				}
			case THIS:
				SourcePosition thisSourcePosition = new SourcePosition();
				start(thisSourcePosition);
				Reference reference = parseReference(null);
				if (currentToken.getTokenKind() == TokenKind.ASSIGNMENT_OPERATOR) {
					acceptIt();
					Expression expressionAssignStmt = parseExpression();
					accept(TokenKind.SEMICOLON);
					finish(thisSourcePosition);
					return new AssignStmt(reference, expressionAssignStmt, thisSourcePosition);
				} else if (currentToken.getTokenKind() == TokenKind.L_SQUARE_BRACKET) {
					acceptIt();
					Expression expressionIxAssignStmt1 = parseExpression();
					accept(TokenKind.R_SQUARE_BRACKET);
					accept(TokenKind.ASSIGNMENT_OPERATOR);
					Expression expressionIxAssignStmt2 = parseExpression();
					accept(TokenKind.SEMICOLON);
					finish(thisSourcePosition);
					return new IxAssignStmt(reference, expressionIxAssignStmt1, expressionIxAssignStmt2, thisSourcePosition);
				} else if (currentToken.getTokenKind() == TokenKind.L_PAREN) {
					acceptIt();
					ExprList exprList = new ExprList();
					if (currentToken.getTokenKind() != TokenKind.R_PAREN) {
						exprList = parseArgumentList();
					}
					accept(TokenKind.R_PAREN);
					accept(TokenKind.SEMICOLON);
					finish(thisSourcePosition);
					return new CallStmt(reference, exprList, thisSourcePosition);
				} else {
					throw new SyntaxException("There is an error parsing the statement. The current Token you provided is \""
							+ currentToken.getSpelling() + "\" (CASE THIS).");
				}
			case RETURN:
				SourcePosition returnSourcePosition = new SourcePosition();
				start(returnSourcePosition);
				acceptIt();
				Expression expressionReturn = null;
				if (currentToken.getTokenKind() != TokenKind.SEMICOLON) {
					expressionReturn = parseExpression();
				}
				accept(TokenKind.SEMICOLON);
				finish(returnSourcePosition);
				return new ReturnStmt(expressionReturn, returnSourcePosition);
			case IF:
				SourcePosition ifSourcePosition = new SourcePosition();
				start(ifSourcePosition);
				acceptIt();
				accept(TokenKind.L_PAREN);
				Expression expressionIf = parseExpression();
				accept(TokenKind.R_PAREN);
				Statement statementIf = parseStatement();
				if (currentToken.getTokenKind() == TokenKind.ELSE) {
					acceptIt();
					Statement statementElse = parseStatement();
					finish(ifSourcePosition);
					return new IfStmt(expressionIf, statementIf, statementElse, ifSourcePosition);
				} else {
					finish(ifSourcePosition);
					return new IfStmt(expressionIf, statementIf, ifSourcePosition);
				}
			case WHILE:
				SourcePosition whileSourcePosition = new SourcePosition();
				start(whileSourcePosition);
				acceptIt();
				accept(TokenKind.L_PAREN);
				Expression expressionWhile = parseExpression();
				accept(TokenKind.R_PAREN);
				Statement statementWhile = parseStatement();
				finish(whileSourcePosition);
				return new WhileStmt(expressionWhile, statementWhile, whileSourcePosition);
			default:
				throw new SyntaxException("There is an error parsing the statement. The current Token you provided " +
					"is \"" + currentToken.getSpelling() + "\" (NO CASE FOUND).");
		}
	}

	private Expression parseExpression() throws SyntaxException {
		return parseDisjunctionExpr();
	}

	private Expression parseDisjunctionExpr() throws SyntaxException {
		SourcePosition disjunctionExprSourcePosition = new SourcePosition();
		start(disjunctionExprSourcePosition);
		Expression e1 = parseConjunctionExpr();
		while (currentToken.getTokenKind() == TokenKind.OR) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseConjunctionExpr();
			finish(disjunctionExprSourcePosition);
			e1 = new BinaryExpr(op, e1, e2, disjunctionExprSourcePosition);
		}
		return e1;
	}

	private Expression parseConjunctionExpr() throws SyntaxException {
		SourcePosition conjunctionExprSourcePosition = new SourcePosition();
		start(conjunctionExprSourcePosition);
		Expression e1 = parseEqualityExpr();
		while (currentToken.getTokenKind() == TokenKind.AND) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseEqualityExpr();
			finish(conjunctionExprSourcePosition);
			e1 = new BinaryExpr(op, e1, e2, conjunctionExprSourcePosition);
		}
		return e1;
	}

	private Expression parseEqualityExpr() throws SyntaxException {
		SourcePosition equalityExprSourcePosition = new SourcePosition();
		start(equalityExprSourcePosition);
		Expression e1 = parseRelationalExpr();
		while (currentToken.getTokenKind() == TokenKind.EQUIVALENCE_COMPARISON || currentToken.getTokenKind() ==
				TokenKind.NOT_EQUAL) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseRelationalExpr();
			finish(equalityExprSourcePosition);
			e1 = new BinaryExpr(op, e1, e2, equalityExprSourcePosition);
		}
		return e1;
	}

	private Expression parseRelationalExpr() throws SyntaxException {
		SourcePosition relationalExprSourcePosition = new SourcePosition();
		start(relationalExprSourcePosition);
		Expression e1 = parseAdditiveExpr();
		while (currentToken.getTokenKind() == TokenKind.LESS_THAN_EQUAL || currentToken.getTokenKind() ==
				TokenKind.LESS_THAN || currentToken.getTokenKind() == TokenKind.GREATER_THAN || currentToken.getTokenKind()
				== TokenKind.GREATER_THAN_EQUAL) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseAdditiveExpr();
			finish(relationalExprSourcePosition);
			e1 = new BinaryExpr(op, e1, e2, relationalExprSourcePosition);
		}
		return e1;
	}

	private Expression parseAdditiveExpr() throws SyntaxException {
		SourcePosition additiveExprSourcePosition = new SourcePosition();
		start(additiveExprSourcePosition);
		Expression e1 = parseMultiplicativeExpr();
		while (currentToken.getTokenKind() == TokenKind.ADD || currentToken.getTokenKind() == TokenKind.MINUS) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseMultiplicativeExpr();
			finish(additiveExprSourcePosition);
			e1 = new BinaryExpr(op, e1, e2, additiveExprSourcePosition);
		}
		return e1;
	}

	private Expression parseMultiplicativeExpr() throws SyntaxException {
		SourcePosition multiplicativeExprSourcePosition = new SourcePosition();
		start(multiplicativeExprSourcePosition);
		Expression e1 = parseUnaryExpr();
		while (currentToken.getTokenKind() == TokenKind.MULTIPLY || currentToken.getTokenKind() == TokenKind.DIVIDE) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseUnaryExpr();
			finish(multiplicativeExprSourcePosition);
			e1 = new BinaryExpr(op, e1, e2, multiplicativeExprSourcePosition);
		}
		return e1;
	}

	private Expression parseUnaryExpr() throws SyntaxException {
		SourcePosition unaryExprSourcePosition = new SourcePosition();
		start(unaryExprSourcePosition);
		if (currentToken.getTokenKind() != TokenKind.NOT && currentToken.getTokenKind() != TokenKind.MINUS) {
			return parseExpressionFinal();
		} else {
			Operator op = new Operator(currentToken);
			acceptIt();
			finish(unaryExprSourcePosition);
			return new UnaryExpr(op, parseUnaryExpr(), unaryExprSourcePosition);
		}
	}

	private Expression parseExpressionFinal() throws SyntaxException {
		SourcePosition expressionSourcePosition = new SourcePosition();
		start(expressionSourcePosition);
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
					finish(expressionSourcePosition);
					return new CallExpr(reference, exprList, expressionSourcePosition);
				} else if (currentToken.getTokenKind() == TokenKind.L_SQUARE_BRACKET) {
					acceptIt();
					Expression expression = parseExpression();
					accept(TokenKind.R_SQUARE_BRACKET);
					finish(expressionSourcePosition);
					return new IxExpr(reference, expression, expressionSourcePosition);
				} else {
					finish(expressionSourcePosition);
					return new RefExpr(reference, expressionSourcePosition);
				}
			case L_PAREN:
				acceptIt();
				Expression expressionParen = parseExpression();
				accept(TokenKind.R_PAREN);
				return expressionParen;
			case NUM: case TRUE: case FALSE: case NULL:
				Token tokenLiteral = currentToken;
				acceptIt();
				finish(expressionSourcePosition);
				if (tokenLiteral.getTokenKind() == TokenKind.NUM) {
					return new LiteralExpr(new IntLiteral(tokenLiteral), expressionSourcePosition);
				} else if (tokenLiteral.getTokenKind() == TokenKind.TRUE || tokenLiteral.getTokenKind() == TokenKind.FALSE) {
					return new LiteralExpr(new BooleanLiteral(tokenLiteral), expressionSourcePosition);
				} else if (tokenLiteral.getTokenKind() == TokenKind.NULL) {
					return new LiteralExpr(new NullLiteral(tokenLiteral), expressionSourcePosition);
				} else {
					throw new SyntaxException("Must pass in a TokenKind of NUM, TRUE, NULL, or FALSE!");
				}
			case NEW:
				acceptIt();
				if (currentToken.getTokenKind() == TokenKind.IDENTIFIER) {
					Token identifier = currentToken;
					SourcePosition identifierSourcePosition = identifier.getSourcePosition();
					acceptIt();
					if (currentToken.getTokenKind() == TokenKind.L_SQUARE_BRACKET) {
						acceptIt();
						Expression expression = parseExpression();
						accept(TokenKind.R_SQUARE_BRACKET);
						finish(expressionSourcePosition);
						return new NewArrayExpr(new ClassType(new Identifier(identifier),identifierSourcePosition), expression,
								expressionSourcePosition);
					} else if (currentToken.getTokenKind() == TokenKind.L_PAREN) {
						acceptIt();
						accept(TokenKind.R_PAREN);
						finish(expressionSourcePosition);
						return new NewObjectExpr(new ClassType(new Identifier(identifier), identifierSourcePosition),
								expressionSourcePosition);
					} else {
						throw new SyntaxException("Must pass in a TokenKind of L_SQUARE_BRACKET or L_PAREN!");
					}
				} else if (currentToken.getTokenKind() == TokenKind.INT) {
					SourcePosition intSourcePosition = currentToken.getSourcePosition();
					acceptIt();
					accept(TokenKind.L_SQUARE_BRACKET);
					Expression expression = parseExpression();
					accept(TokenKind.R_SQUARE_BRACKET);
					finish(expressionSourcePosition);
					return new NewArrayExpr(new BaseType(TypeKind.INT, intSourcePosition), expression, expressionSourcePosition);
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
		SourcePosition typeSourcePosition = new SourcePosition();
		start(typeSourcePosition);
		TokenKind tokenKind = currentToken.getTokenKind();
		switch (tokenKind) {
			case BOOLEAN:
				acceptIt();
				finish(typeSourcePosition);
				return new BaseType(TypeKind.BOOLEAN, typeSourcePosition);
			case INT: case IDENTIFIER:
				TypeDenoter typeDenoter = null;
				if (currentToken.getTokenKind() == TokenKind.INT) {
					typeDenoter = new BaseType(TypeKind.INT, currentToken.getSourcePosition());
				} else if (currentToken.getTokenKind() == TokenKind.IDENTIFIER) {
					typeDenoter = new ClassType(new Identifier(currentToken), currentToken.getSourcePosition());
				} else {
					throw new SyntaxException("Token needs to be of type INT or IDENTIFIER");
				}
				acceptIt();
				if (currentToken.getTokenKind() == TokenKind.L_SQUARE_BRACKET) {
					acceptIt();
					accept(TokenKind.R_SQUARE_BRACKET);
					finish(typeSourcePosition);
					return new ArrayType(typeDenoter, typeSourcePosition);
				} else {
					finish(typeSourcePosition);
					typeDenoter.posn = typeSourcePosition;
				}
				return typeDenoter;
			default:
				throw new SyntaxException("\"There is an error parsing the type. The current Token you provided is \""
					+ currentToken.getSpelling() + "\" (EXPECTED TOKENS INT, BOOLEAN, IDENTIFIER).");
		}
	}

	private Reference parseReference(Token option) throws SyntaxException {
		SourcePosition referenceSourcePosition = new SourcePosition();
		start(referenceSourcePosition);
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
			finish(referenceSourcePosition);
			if (currToken.getTokenKind() == TokenKind.IDENTIFIER) {
				qualRef = new IdRef(new Identifier(currToken), referenceSourcePosition);
			} else {
				qualRef = new ThisRef(referenceSourcePosition);
			}
			while (currentToken.getTokenKind() == TokenKind.PERIOD) {
				acceptIt();
				SourcePosition nestedReferenceSourcePosition = new SourcePosition();
				start(nestedReferenceSourcePosition);
				if (currentToken.getTokenKind() == TokenKind.IDENTIFIER) {
					qualRef = new QualRef(qualRef, new Identifier(currentToken), null);
					acceptIt();
					finish(nestedReferenceSourcePosition);
					qualRef.posn = nestedReferenceSourcePosition;
				} else {
					throw new SyntaxException("\"There is an error parsing the reference. The current Token you provided is \"" +
							currentToken.getSpelling() + "\" (EXPECTED TOKEN IDENTIFIER).");
				}
			}
			return qualRef;
		} else {
			finish(referenceSourcePosition);
			if (currToken.getTokenKind() == TokenKind.IDENTIFIER) {
				return new IdRef(new Identifier(currToken), referenceSourcePosition);
			} else if (currToken.getTokenKind() == TokenKind.THIS) {
				return new ThisRef(referenceSourcePosition);
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
				previousTokenPosition = currentToken.getSourcePosition();
				currentToken = scanner.scan();
			}
		} else {
			throw new SyntaxException("There is an error accepting the Token. The current Token you provided is \"" +
					currentToken.getSpelling() + "\".");
		}
	}

	private void start(SourcePosition sourcePosition) {
		sourcePosition.start = currentToken.getSourcePosition().start;
	}

	private void finish(SourcePosition sourcePosition) {
		sourcePosition.finish = previousTokenPosition.finish;
	}

	public Package parse () throws SyntaxException {
		currentToken = scanner.scan();
		return parseProgram();
	}
}
