package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.TokenKind;

import java.lang.reflect.Method;

public class TypeChecking implements Visitor<Object, TypeDenoter> {

	private final String typeError = "Identification Error";

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
		md.type.visit(this, null);
		ParameterDeclList parameterDeclList = md.parameterDeclList;
		for (ParameterDecl parameterDecl: parameterDeclList) {
			parameterDecl.visit(this, null);
		}
		StatementList statementList = md.statementList;
		for (Statement statement: statementList) {
			statement.visit(this, md);
		}
		return null;
	}

	@Override
	public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
		pd.type.visit(this, null);
		return null;
	}

	@Override
	public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
		decl.type.visit(this, null);
		return null;
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
			if (((ClassType)varDecl).className.spelling.equals(((ClassType)initExp).className.spelling)) {
				return null;
			} else if (initExp.typeKind.equals(TypeKind.NULL)) {
				return null;
			} else {
				throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because class types don't match!");
				return null;
			}
		} else if (varDecl.typeKind.equals(TypeKind.ARRAY)) {
			if (((ArrayType)varDecl).eltType instanceof ClassType) {
				if (((ClassType)((ArrayType)varDecl).eltType).className.spelling.equals(((ClassType)(((ArrayType)initExp).eltType)).
						className.spelling)) {
					return null;
				} else if (initExp.typeKind.equals(TypeKind.NULL)) {
					return null;
				} else {
					throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because array types don't match " +
							"due to class differences!");
					return null;
				}
			} else if (((ArrayType)varDecl).eltType instanceof BaseType) {
					if (((ArrayType) varDecl).eltType.typeKind.equals(TypeKind.INT) || ((ArrayType) varDecl).eltType.typeKind.
							equals(TypeKind.BOOLEAN)) {
						if (((ArrayType) varDecl).eltType.typeKind.equals(((ArrayType)initExp).eltType.typeKind)) {
							return null;
						} else {
							throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because array types don't match!");
							return null;
						}
					} else {
						throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because array types don't match!");
						return null;
					}
			} else {
				throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because array types don't match!");
				return null;
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
		if (ref.typeKind.equals(TypeKind.CLASS)) {
			if (((ClassType)ref).className.equals(((ClassType)val).className)) {
				return null;
			} else if (val.typeKind.equals(TypeKind.NULL)) {
				return null;
			} else {
				throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because class types don't match!");
				return null;
			}
		} else if (ref.typeKind.equals(TypeKind.ARRAY)) {
			if (((ArrayType)ref).eltType instanceof ClassType) {
				if (((ClassType)((ArrayType)ref).eltType).className.equals(((ClassType)(((ArrayType)val).eltType)).
						className)) {
					return null;
				} else if (val.typeKind.equals(TypeKind.NULL)) {
					return null;
				} else {
					throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because array types don't match " +
							"due to class differences!");
					return null;
				}
			} else if (((ArrayType)ref).eltType instanceof BaseType) {
				if (((ArrayType) ref).eltType.typeKind.equals(TypeKind.INT) || ((ArrayType) ref).eltType.typeKind.
						equals(TypeKind.BOOLEAN)) {
					if (((ArrayType) ref).eltType.typeKind.equals(((ArrayType)val).eltType.typeKind)) {
						return null;
					} else {
						throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because array types don't match!");
						return null;
					}
				} else {
					throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because array types don't match!");
					return null;
				}
			} else {
				throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because array types don't match!");
				return null;
			}
		} else if (ref.typeKind.equals(TypeKind.INT) || ref.typeKind.equals(TypeKind.BOOLEAN)) {
			if (ref.typeKind != val.typeKind) {
				throwError(stmt.posn.start, typeError, "VarDeclStmt is not valid because types don't match!");
			}
		} else {
			throwError(ref.posn.start, typeError, "VarDecl in VarDeclStmt is not of a valid Type!");
			return null;
		}
		return null;
	}

	@Override
	public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		TypeDenoter ref = stmt.ref.visit(this, null);
		TypeDenoter ix = stmt.ix.visit(this, null);
		TypeDenoter exp = stmt.exp.visit(this, null);
		if (ix.typeKind.equals(TypeKind.INT)) {
			if (ref instanceof ArrayType) {
				if (((ArrayType) ref).eltType.typeKind.equals(ix.typeKind)) {
					return null;
				} else {
					throwError(ref.posn.start, typeError, "Type of expression does not match element type of reference!");
					return null;
				}
			} else {
				throwError(ref.posn.start, typeError, "ref is not a ArrayType!");
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
			for (Expression expression: stmt.argList) {
				TypeDenoter expressionTypeDenoter = expression.visit(this, null);
				TypeKind expressionTypeKind = expressionTypeDenoter.typeKind;
				if (!expressionTypeKind.equals(methodRefType)) {
					throwError(expression.posn.start, typeError, "Parameter does not match the require type kind!");
				}
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
		if (returnStmt.typeKind.equals(TypeKind.NULL)) {
			if (methodDecl.type.typeKind.equals(TypeKind.VOID)) {
				return null;
			} else {
				throwError(stmt.posn.start, typeError, "Return statement returns incorrect type for type VOID!");
				return null;
			}
		} else {
			if (returnStmt.typeKind.equals(methodDecl.type.typeKind)) {
				return null;
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
		 stmt.elseStmt.visit(this, arg);
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
				return new BaseType(TypeKind.INT, null);
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
				throwError(ixExpr.posn.start, typeError, "Reference needs to be !");
			}
		} else {
			throwError(ixExpr.posn.start, typeError, "Need to index with an integer value!");
		}
		return null;
	}

	@Override
	public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
		TypeDenoter functionRef = expr.functionRef.visit(this, null);
		if (expr.functionRef.declaration instanceof MethodDecl) {
			TypeKind functionRefType = functionRef.typeKind;
			for (Expression expression: expr.argList) {
				TypeDenoter expressionTypeDenoter = expression.visit(this, null);
				TypeKind expressionTypeKind = expressionTypeDenoter.typeKind;
				if (!expressionTypeKind.equals(functionRefType)) {
					throwError(expression.posn.start, typeError, "Parameter does not match the require type kind!");
				}
			}
		} else {
			throwError(expr.posn.start, typeError, "Fields cannot be called as methods!");
		}
		return null;
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
		expr.eltType.visit(this, null);
		return null;
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
		return ref.id.declaration.type.visit(this, null);
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
