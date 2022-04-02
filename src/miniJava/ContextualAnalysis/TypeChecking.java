package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class TypeChecking implements Visitor<Object, TypeDenoter> {

	private final String typeError = "Type Error";

	public TypeChecking(Package ast) {
		ast.visit(this, null);
	}

	@Override
	public TypeDenoter visitPackage(Package prog, Object arg) {
		ClassDeclList classDeclList = prog.classDeclList;
		for (ClassDecl classDecl: classDeclList) {
			classDecl.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
		FieldDeclList fieldDeclList= cd.fieldDeclList;
		MethodDeclList methodDeclList = cd.methodDeclList;
		for (FieldDecl fieldDecl: fieldDeclList) {
			fieldDecl.visit(this, null);
		}
		for (MethodDecl methodDecl: methodDeclList) {
			methodDecl.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
		fd.type.visit(this, null);
		return null;
	}

	@Override
	public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
		TypeDenoter returnType = md.type;
		boolean isVoid = returnType.typeKind.equals(TypeKind.VOID);
		boolean returnStatement = false;
		md.type.visit(this, null);
		ParameterDeclList parameterDeclList = md.parameterDeclList;
		for (ParameterDecl parameterDecl: parameterDeclList) {
			parameterDecl.visit(this, null);
		}
		StatementList statementList = md.statementList;
		for (Statement statement: statementList) {
			if (isVoid) {
				statement.visit(this, md);
			} else {
				if (statement instanceof ReturnStmt) {
					returnStatement = true;
					statement.visit(this, md);
					break;
				} else {
					statement.visit(this, md);
				}
			}
		}
		if (!isVoid && !returnStatement) {
			throwError(md.posn.start, typeError, "Method that has a return type of something other than void" +
					" should return something!");
		}
		return null;
	}

	@Override
	public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
		return pd.type.visit(this, null);
	}

	@Override
	public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
		return decl.type.visit(this, null);
	}

	@Override
	public TypeDenoter visitBaseType(BaseType type, Object arg) {
		return type;
	}

	@Override
	public TypeDenoter visitClassType(ClassType type, Object arg) {
		return type;
	}

	@Override
	public TypeDenoter visitArrayType(ArrayType type, Object arg) {
		return type;
	}

	@Override
	public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
		StatementList statementList = stmt.sl;
		for (Statement statement: statementList) {
			statement.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		TypeDenoter varDecl = stmt.varDecl.visit(this, null);
		TypeDenoter initExp = stmt.initExp.visit(this, null);
		if (varDecl.typeKind.equals(TypeKind.CLASS)) {
			if (initExp.typeKind.equals(TypeKind.CLASS)) {
				if (((ClassType)varDecl).className.spelling.equals(((ClassType)initExp).className.spelling)) {
					return null;
				} else if (initExp.typeKind.equals(TypeKind.NULL)) {
					return null;
				} else {
					throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because class types don't match!");
					return null;
				}
			} else {
				throwError(stmt.posn.start, typeError, "initExp is not a class type!");
				return null;
			}
		} else if (varDecl.typeKind.equals(TypeKind.ARRAY)) {
			if (initExp.typeKind.equals(TypeKind.NULL)) {
				return null;
			} else if (!(initExp instanceof ArrayType)) {
				throwError(stmt.posn.start, typeError, "Right hand side of VarDeclStmt must be of type array if " +
						"left hand side if of type array!");
				return null;
			} else {
				if (((ArrayType) varDecl).eltType.typeKind.equals(TypeKind.INT)) {
					if (!(((ArrayType) initExp).eltType.typeKind.equals(TypeKind.INT))) {
						throwError(stmt.posn.start, typeError, "Mismatch of element types for array types on both " +
								"sides of the statement!");
					}
					return null;
				} else if (((ArrayType) varDecl).eltType instanceof ClassType) {
					if (((ArrayType) initExp).eltType instanceof ClassType) {
						if (!((ClassType) ((ArrayType) varDecl).eltType).className.spelling.equals(((ClassType) ((ArrayType)
								varDecl).eltType).className.spelling)) {
							throwError(stmt.posn.start, typeError, "Class types of arrays do not match!");
						}
					} else {
						throwError(stmt.posn.start, typeError, "Array Types do not match!");
					}
					return null;
				} else {
					throwError(stmt.posn.start, typeError, "Array types do not have valid element types!");
					return null;
				}
			}
		} else if (varDecl.typeKind.equals(TypeKind.INT) || varDecl.typeKind.equals(TypeKind.BOOLEAN)) {
			if (varDecl.typeKind != initExp.typeKind) {
				throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because types don't match!");
			}
			return null;
		} else {
			throwError(stmt.posn.start, typeError, "VarDecl in VarDeclStmt is not of a valid Type!");
			return null;
		}
	}

	@Override
	public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
		TypeDenoter ref = stmt.ref.visit(this, null);
		TypeDenoter val = stmt.val.visit(this, null);
		if ((stmt.ref.declaration instanceof ClassDecl) || (stmt.ref.declaration instanceof MethodDecl)) {
			throwError(stmt.posn.start, typeError, "The reference must point to a field, class, or method!");
			return null;
		} else {
			if (ref instanceof ClassType) {
				if (val instanceof ClassType) {
					if (((ClassType)ref).className.spelling.equals(((ClassType)val).className.spelling)) {
						return null;
					} else if (val.typeKind.equals(TypeKind.NULL)) {
						return null;
					} else {
						throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because class types don't match!");
						return null;
					}
				} else {
					throwError(stmt.posn.start, typeError, "Right hand side of VarDeclStmt must be of type class if " +
							"left hand side if of type class!");
					return null;
				}
			} else if (ref instanceof ArrayType) {
				if (val.typeKind.equals(TypeKind.NULL)) {
					return null;
				} else if (!(val instanceof ArrayType)) {
					throwError(stmt.posn.start, typeError, "Right hand side of VarDeclStmt must be of type array if " +
							"left hand side if of type array!");
					return null;
				} else {
					if (((ArrayType) ref).eltType.typeKind.equals(TypeKind.INT)) {
						if (!(((ArrayType) val).eltType.typeKind.equals(TypeKind.INT))) {
							throwError(stmt.posn.start, typeError, "Mismatch of element types for array types on both " +
									"sides of the statement!");
						}
						return null;
					} else if (((ArrayType) ref).eltType instanceof ClassType) {
						if (((ArrayType) val).eltType instanceof ClassType) {
							if (!((ClassType) ((ArrayType) ref).eltType).className.spelling.equals(((ClassType) ((ArrayType) val).
									eltType).className.spelling)) {
								throwError(stmt.posn.start, typeError, "Class types of arrays do not match!");
							}
						} else {
							throwError(stmt.posn.start, typeError, "Array Types do not match!");
						}
						return null;
					} else {
						throwError(stmt.posn.start, typeError, "Array types do not have valid element types!");
						return null;
					}
				}
			} else if (ref instanceof BaseType) {
				if (ref.typeKind.equals(TypeKind.INT) || ref.typeKind.equals(TypeKind.BOOLEAN)) {
					if (ref.typeKind != val.typeKind) {
						throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because types don't match!");
					}
				} else {
					throwError(stmt.posn.start, typeError, "Left side of VarDecl is not of type INT or BOOLEAN!");
				}
				return null;
			} else {
				throwError(ref.posn.start, typeError, "VarDecl in VarDeclStmt is not of a valid Type!");
				return null;
			}
		}
	}

	@Override
	public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		TypeDenoter ref = stmt.ref.visit(this, null);
		TypeDenoter ix = stmt.ix.visit(this, null);
		TypeDenoter exp = stmt.exp.visit(this, null);
		if (ix.typeKind.equals(TypeKind.INT)) {
			if (ref instanceof ArrayType) {
				if (exp instanceof ArrayType) {
					if (!((ArrayType) ref).eltType.typeKind.equals(((ArrayType) exp).eltType.typeKind)) {
						throwError(ref.posn.start, typeError, "Type of expression does not match element type of" +
								" reference!");
					}
				} else {
					throwError(ref.posn.start, typeError, "exp is not an ArrayType!");
				}
				return null;
			} else {
				throwError(ref.posn.start, typeError, "ref is not an ArrayType!");
				return null;
			}
		} else {
			throwError(ix.posn.start, typeError, "Index value needs to be an integer!");
			return null;
		}
	}

	@Override
	public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
		TypeDenoter methodRef = stmt.methodRef.visit(this, null);
		if (stmt.methodRef.declaration instanceof MethodDecl) {
			TypeKind methodRefType = methodRef.typeKind;
			if (((MethodDecl) stmt.methodRef.declaration).parameterDeclList.size() != stmt.argList.size()) {
				throwError(stmt.posn.start, typeError, "Method call does not have correct size of arguments!");
				return null;
			}
			int i = 0;
			ParameterDeclList parameterDeclList = ((MethodDecl) stmt.methodRef.declaration).parameterDeclList;
			for (Expression expression: stmt.argList) {
				TypeDenoter expressionTypeDenoter = expression.visit(this, null);
				TypeKind expressionTypeKind = expressionTypeDenoter.typeKind;
				if (!expressionTypeKind.equals(parameterDeclList.get(i).type.typeKind)) {
					throwError(expression.posn.start, typeError, "Parameter does not match the require type kind!");
				}
				i++;
			}
		} else {
			throwError(stmt.posn.start, typeError, "Fields cannot be called as methods!");
		}
		return null;
	}

	@Override
	public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
		TypeDenoter returnStmt = stmt.returnExpr.visit(this, null);
		MethodDecl methodDecl = ((MethodDecl)arg);
		if (methodDecl.type.typeKind.equals(TypeKind.VOID)) {
			if (returnStmt.typeKind.equals(TypeKind.NULL)) {
				return new BaseType(TypeKind.NULL, null);
			} else {
				throwError(stmt.posn.start, typeError, "Methods of return type VOID must not return anything!");
				return null;
			}
		} else {
			if (methodDecl.type.typeKind.equals(returnStmt.typeKind)) {
				if (methodDecl.type instanceof ClassType) {
					if (((ClassType) methodDecl.type).className.spelling.equals(((ClassType)returnStmt).className.spelling)) {
						return methodDecl.type;
					} else {
						throwError(stmt.posn.start, typeError, "Return statement does not match the class of the return " +
								"type!");
						return null;
					}
				} else if (methodDecl.type instanceof ArrayType) {
					if (((ArrayType) methodDecl.type).eltType.typeKind.equals(TypeKind.INT)) {
						if (!(((ArrayType) returnStmt).eltType.typeKind.equals(TypeKind.INT))) {
							throwError(stmt.posn.start, typeError, "Mismatch of element types for array types on both " +
									"sides of the statement!");
							return null;
						} else {
							return returnStmt;
						}
					} else if (((ArrayType) methodDecl.type).eltType instanceof ClassType) {
						if (((ArrayType) returnStmt).eltType instanceof ClassType) {
							if (!((ClassType) ((ArrayType) methodDecl.type).eltType).className.spelling.equals(((ClassType)
									((ArrayType) returnStmt).eltType).className.spelling)) {
								throwError(stmt.posn.start, typeError, "Class types of arrays do not match!");
								return null;
							}
						} else {
							throwError(stmt.posn.start, typeError, "Array Types do not match!");
							return null;
						}
						return null;
					} else {
						throwError(stmt.posn.start, typeError, "Array types do not have valid element types!");
						return null;
					}
				} else {
					return methodDecl.type;
				}
			} else {
				throwError(stmt.posn.start, typeError, "Return statement returns incorrect type!");
				return null;
			}
		}
	}

	@Override
	public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
		TypeDenoter condition = stmt.cond.visit(this, null);
		stmt.thenStmt.visit(this, arg);
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, arg);
		}
		if (!condition.typeKind.equals(TypeKind.BOOLEAN)) {
			throwError(condition.posn.start, typeError, "Condition in if statement should be of type boolean!");
		}
		return null;
	}

	@Override
	public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
		TypeDenoter condition = stmt.cond.visit(this, null);
		stmt.body.visit(this, null);
		if (!condition.typeKind.equals(TypeKind.BOOLEAN)) {
			throwError(condition.posn.start, typeError, "Condition in if statement should be of type boolean!");
		}
		return null;
	}

	@Override
	public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
		TypeDenoter expressionTypeDenoter = expr.expr.visit(this, null);
		expr.operator.visit(this, null);
		if (expr.operator.spelling.equals("!")) {
			if (expressionTypeDenoter.typeKind.equals(TypeKind.BOOLEAN)) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else {
				throwError(expr.posn.start, typeError, "Unary expression using ! operator must be used with " +
						"boolean expression!");
				return null;
			}
		} else if (expr.operator.spelling.equals("-")) {
			if (expressionTypeDenoter.typeKind.equals(TypeKind.INT)) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else {
				throwError(expr.posn.start, typeError, "Unary expression using - operator must be used with " +
						"integer expression!");
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
		TypeDenoter expressionLeftTypeDenoter = expr.left.visit(this, null);
		TypeDenoter expressionRightTypeDenoter = expr.right.visit(this, null);
		expr.operator.visit(this, null);
		if (expr.operator.spelling.equals("+") || expr.operator.spelling.equals("-") || expr.operator.spelling.equals("*")
				|| expr.operator.spelling.equals("/")) {
			if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.INT) && expressionRightTypeDenoter.typeKind.equals(
					TypeKind.INT)) {
				return new BaseType(TypeKind.INT, null);
			} else {
				String operatorSpelling = expr.operator.spelling;
				throwError(expr.posn.start, typeError, operatorSpelling + " requires two expressions to evaluate" +
						" to INT!");
				return null;
			}
		} else if (expr.operator.spelling.equals("&&") || expr.operator.spelling.equals("||")) {
			if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.BOOLEAN) && expressionRightTypeDenoter.typeKind.equals(
					TypeKind.BOOLEAN)) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else {
				String operatorSpelling = expr.operator.spelling;
				throwError(expr.posn.start, typeError, operatorSpelling + " requires two expressions to evaluate" +
						" to BOOLEAN!");
				return null;
			}
		} else if (expr.operator.spelling.equals("<") || expr.operator.spelling.equals("<=") || expr.operator.spelling.
				equals(">") || expr.operator.spelling.equals(">=")) {
			if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.INT) && expressionRightTypeDenoter.typeKind.equals(
					TypeKind.INT)) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else {
				String operatorSpelling = expr.operator.spelling;
				throwError(expr.posn.start, typeError, operatorSpelling + " requires two expressions to evaluate" +
						" to INT!");
				return null;
			}
		} else if (expr.operator.spelling.equals("==") || expr.operator.spelling.equals("!=")) {
			if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.INT) && expressionRightTypeDenoter.typeKind.equals(
					TypeKind.INT)) {
				return new BaseType(TypeKind.INT, null);
			} else if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.BOOLEAN) && expressionRightTypeDenoter.typeKind.equals(
					TypeKind.BOOLEAN)) {
				return new BaseType(TypeKind.BOOLEAN, null);
			}
			else {
				String operatorSpelling = expr.operator.spelling;
				throwError(expr.posn.start, typeError, operatorSpelling + " requires two expressions to evaluate" +
						" to INT or BOOLEAN!");
				return null;
			}
		} else {
			throwError(expr.posn.start, typeError, "Provide a valid operator");
			return null;
		}
	}

	@Override
	public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
		Declaration declaration = expr.ref.declaration;
		if (!(declaration instanceof ClassDecl)) {
			return declaration.type;
		} else {
			throwError(expr.posn.start, typeError, "The reference cannot refer to a ClassDecl!");
			return null;
		}
	}

	@Override
	public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
		TypeDenoter ref = expr.ref.visit(this, null);
		TypeDenoter ixExpr = expr.ixExpr.visit(this, null);
		if (ixExpr.typeKind.equals(TypeKind.INT)) {
			if (ref instanceof ArrayType) {
				return ((ArrayType) ref).eltType;
			} else {
				throwError(expr.posn.start, typeError, "Reference needs to be of Array Type!");
			}
		} else {
			throwError(expr.posn.start, typeError, "Need to index with an integer value!");
		}
		return null;
	}

	@Override
	public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
		TypeDenoter functionRef = expr.functionRef.visit(this, null);
		if (expr.functionRef.declaration instanceof MethodDecl) {
			TypeKind functionRefType = functionRef.typeKind;
			if (((MethodDecl) expr.functionRef.declaration).parameterDeclList.size() != expr.argList.size()) {
				throwError(expr.posn.start, typeError, "Method call does not have correct size of arguments!");
				return null;
			}
			int i = 0;
			ParameterDeclList parameterDeclList = ((MethodDecl) expr.functionRef.declaration).parameterDeclList;
			for (Expression expression: expr.argList) {
				TypeDenoter expressionTypeDenoter = expression.visit(this, null);
				TypeKind expressionTypeKind = expressionTypeDenoter.typeKind;
				if (!expressionTypeKind.equals(parameterDeclList.get(i).type.typeKind)) {
					throwError(expression.posn.start, typeError, "Parameter does not match the require type kind!");
				}
				i++;
			}
		} else {
			throwError(expr.posn.start, typeError, "Fields cannot be called as methods!");
		}
		return expr.functionRef.declaration.type;
	}

	@Override
	public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
		return expr.lit.visit(this, null);
	}

	@Override
	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		return expr.classtype.visit(this, null);
	}

	@Override
	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		// implement
		expr.sizeExpr.visit(this, null);
		TypeDenoter elementType = expr.eltType.visit(this, null);
		return new ArrayType(elementType, null);
	}

	@Override
	public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
		return ref.declaration.type.visit(this, null);
	}

	@Override
	public TypeDenoter visitIdRef(IdRef ref, Object arg) {
		return ref.declaration.type.visit(this, null);
	}

	@Override
	public TypeDenoter visitQRef(QualRef ref, Object arg) {
		// method case
		return ref.declaration.type.visit(this, null);
	}

	@Override
	public TypeDenoter visitIdentifier(Identifier id, Object arg) {
		return new ClassType(id, null);
	}

	@Override
	public TypeDenoter visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
		return new BaseType(TypeKind.INT, null);
	}

	@Override
	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return new BaseType(TypeKind.BOOLEAN, null);
	}

	@Override
	public TypeDenoter visitNullLiteral(NullLiteral nullLiteral, Object o) {
		return new BaseType(TypeKind.NULL, null);
	}

	private void throwError(int lineNumber, String errorKind, String message) {
		System.out.println("*** line " + lineNumber + ": " + errorKind + " - " + message);
	}
}
