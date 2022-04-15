package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class TypeChecking implements Visitor<Object, TypeDenoter> {

	private final String typeError = "Type Error";
	public static boolean throwError = false;

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
			if (!isVoid) {
				if (statement instanceof ReturnStmt) {
					returnStatement = true;
				}
			}
			statement.visit(this, md);
		}
		if (!isVoid && !(statementList.get(statementList.size()-1) instanceof ReturnStmt)) {
			throwError(md.posn.start, "Methods which have a non-void return type must have a return " +
					"statement as the last statement in the method!");
		}
		if (!isVoid && !returnStatement) {
			throwError(md.posn.start, "Method that has a non-void return type should return something!");
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
			statement.visit(this, arg);
		}
		return null;
	}

	@Override
	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		TypeDenoter varDecl = stmt.varDecl.visit(this, null);
		TypeDenoter initExp = stmt.initExp.visit(this, stmt.varDecl.type);
		if (varDecl.typeKind.equals(TypeKind.CLASS)) {
			if (initExp.typeKind.equals(TypeKind.CLASS)) {
				catchClassReference(stmt.initExp);
				if (((ClassType)varDecl).className.spelling.equals(((ClassType)initExp).className.spelling)) {
					return null;
				} else {
					throwError(stmt.posn.start, "Mismatch of class types on both sides of the variable" +
							" declaration statement!");
					return null;
				}
			} else if (initExp.typeKind.equals(TypeKind.NULL)) {
				return null;
			} else {
				throwError(stmt.posn.start, "If the left side of a variable declaration is of a class " +
						"type, the right must be too!");
				return null;
			}
		} else if (varDecl.typeKind.equals(TypeKind.ARRAY)) {
			if (initExp.typeKind.equals(TypeKind.NULL)) {
				return null;
			} else if (!(initExp instanceof ArrayType)) {
				throwError(stmt.posn.start, "If the left side of a variable declaration is of an array " +
						"type, the right must be too!");
				return null;
			} else {
				if (((ArrayType) varDecl).eltType.typeKind.equals(TypeKind.INT)) {
					if (!(((ArrayType) initExp).eltType.typeKind.equals(TypeKind.INT))) {
						throwError(stmt.posn.start, "Mismatch of element types for the arrays on both " +
								"sides of the statement!");
					}
					return null;
				} else if (((ArrayType) varDecl).eltType instanceof ClassType) {
					if (((ArrayType) initExp).eltType instanceof ClassType) {
						if (!((ClassType) ((ArrayType) varDecl).eltType).className.spelling.equals(((ClassType) ((ArrayType)
								initExp).eltType).className.spelling)) {
							throwError(stmt.posn.start, "Mismatch of element types for the arrays on both " +
									"sides of the statement!");
						}
					} else {
						throwError(stmt.posn.start, "If the left side of a variable declaration is of an " +
								"array with an element type of class, the right side must be too!");
					}
					return null;
				} else {
					throwError(stmt.posn.start, "Array types do not have valid element types!");
					return null;
				}
			}
		} else if (varDecl.typeKind.equals(TypeKind.INT) || varDecl.typeKind.equals(TypeKind.BOOLEAN)) {
			if (varDecl.typeKind != initExp.typeKind) {
				throwError(stmt.posn.start, "Mismatch of base types on both sides of the statements!");
			}
			return null;
		} else {
			throwError(stmt.posn.start, "Declaring type is not a valid type!");
			return null;
		}
	}

	@Override
	public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
		TypeDenoter ref = stmt.ref.visit(this, null);
		TypeDenoter val = stmt.val.visit(this, stmt.ref.declaration.type);
		if ((stmt.ref.declaration instanceof ClassDecl) || (stmt.ref.declaration instanceof MethodDecl)) {
			throwError(stmt.posn.start, "The reference cannot directly point to a Class or " +
					"Method identifier!");
			return null;
		} else {
			if (ref.typeKind.equals(TypeKind.CLASS)) {
				if (val.typeKind.equals(TypeKind.CLASS)) {
					catchClassReference(stmt.val);
					if (((ClassType)ref).className.spelling.equals(((ClassType)val).className.spelling)) {
						return null;
					}  else {
						throwError(stmt.posn.start, "Mismatch of class types on both sides of the assignment " +
								"statement!");
						return null;
					}
				} else if (val.typeKind.equals(TypeKind.NULL)) {
					return null;
				} else {
					throwError(stmt.posn.start, "If the left side of an assigment statement is of a class " +
							"type, the right must be too!");
					return null;
				}
			} else if (ref instanceof ArrayType) {
				if (val.typeKind.equals(TypeKind.NULL)) {
					return null;
				} else if (!(val instanceof ArrayType)) {
					throwError(stmt.posn.start, "Right hand side of VarDeclStmt must be of type array if " +
							"left, the right must be too!");
					return null;
				} else {
					if (((ArrayType) ref).eltType.typeKind.equals(TypeKind.INT)) {
						if (!(((ArrayType) val).eltType.typeKind.equals(TypeKind.INT))) {
							throwError(stmt.posn.start, "Mismatch of element types for array types on both " +
									"sides of the statement!");
						}
						return null;
					} else if (((ArrayType) ref).eltType instanceof ClassType) {
						if (((ArrayType) val).eltType instanceof ClassType) {
							if (!((ClassType) ((ArrayType) ref).eltType).className.spelling.equals(((ClassType) ((ArrayType) val).
									eltType).className.spelling)) {
								throwError(stmt.posn.start, "Mismatch of element types for the arrays on both " +
										"sides of the statement!");
							}
						} else {
							throwError(stmt.posn.start, "If the left side of a variable declaration is of " +
									"an array with an element type of class, the right side must be too!");
						}
						return null;
					} else {
						throwError(stmt.posn.start, "Array types do not have valid element types!");
						return null;
					}
				}
			} else if (ref.typeKind.equals(TypeKind.INT) || ref.typeKind.equals(TypeKind.BOOLEAN)) {
				if (ref.typeKind != val.typeKind) {
					throwError(stmt.posn.start, "Mismatch of base types on both sides of the statements!");
				}
				return null;
			} else {
				throwError(ref.posn.start, "Reference type is not a valid type!");
				return null;
			}
		}
	}

	@Override
	public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		TypeDenoter ref = stmt.ref.visit(this, null);
		TypeDenoter ix = stmt.ix.visit(this, stmt.ref.declaration.type);
		TypeDenoter exp = stmt.exp.visit(this, stmt.ref.declaration.type);
		if (ix.typeKind.equals(TypeKind.INT)) {
			if (ref instanceof ArrayType) {
				if ((((ArrayType) ref).eltType).typeKind.equals(TypeKind.CLASS)) {
					if (exp.typeKind.equals(TypeKind.CLASS)) {
						catchClassReference(stmt.exp);
						if (!((ClassType) ((ArrayType) ref).eltType).className.spelling.equals(
								((ClassType)exp).className.spelling)) {
							throwError(stmt.posn.start, "Mismatch of element types - Class types do not match " +
									"up!");
						}
						return null;
					} else if (exp.typeKind.equals(TypeKind.NULL)) {
						return null;
					} else {
						throwError(stmt.posn.start, "Element types of statement do not match - both sides" +
								" must have element types of Class!");
						return null;
					}
				} else {
					if (!(((ArrayType) ref).eltType).typeKind.equals(exp.typeKind)) {
						throwError(stmt.posn.start, "Element types of statement do not match - both sides!");
					}
					return null;
				}
			} else {
				throwError(stmt.posn.start, "Cannot index a non-array variable!");
				return null;
			}
		} else {
			throwError(stmt.posn.start, "An integer is required indexing an array!");
			return null;
		}
	}

	@Override
	public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
		if (stmt.methodRef.declaration instanceof MethodDecl) {
			if (((MethodDecl) stmt.methodRef.declaration).parameterDeclList.size() != stmt.argList.size()) {
				throwError(stmt.posn.start, "Method call does not have correct size of arguments!");
				return null;
			}
			int i = 0;
			ParameterDeclList parameterDeclList = ((MethodDecl) stmt.methodRef.declaration).parameterDeclList;
			for (Expression expression: stmt.argList) {
				TypeDenoter parameterTypeDenoter = parameterDeclList.get(i).type;
				TypeDenoter expressionTypeDenoter = expression.visit(this, parameterTypeDenoter);
				if (parameterTypeDenoter instanceof ClassType) {
					if (expressionTypeDenoter instanceof ClassType) {
						catchClassReference(stmt.argList.get(i));
						if (!(((ClassType) parameterTypeDenoter).className.spelling.equals(((ClassType) expressionTypeDenoter).
								className.spelling))) {
							throwError(stmt.posn.start, "Mismatch of types in passing parameter!");
						}
					} else {
						throwError(stmt.posn.start, "Mismatch of types in passing parameter - both " +
								"sides must be of Class type!");
					}
				} else if (parameterTypeDenoter instanceof ArrayType) {
					if (!(expressionTypeDenoter instanceof ArrayType)) {
						throwError(stmt.posn.start, "Mismatch of types in passing parameter!");
					} else {
						if (((ArrayType) parameterTypeDenoter).eltType.typeKind.equals(TypeKind.INT)) {
							if (!(((ArrayType) expressionTypeDenoter).eltType.typeKind.equals(TypeKind.INT))) {
								throwError(stmt.posn.start, "Mismatch of element types for array types on both " +
										"sides of the statement!");
							}
						} else if (((ArrayType) parameterTypeDenoter).eltType instanceof ClassType) {
							if (((ArrayType) expressionTypeDenoter).eltType instanceof ClassType) {
								if (!((ClassType) ((ArrayType) parameterTypeDenoter).eltType).className.spelling.equals(((ClassType)
										((ArrayType) expressionTypeDenoter).eltType).className.spelling)) {
									throwError(stmt.posn.start, "Mismatch of element types in passing parameter!");
								}
							} else {
								throwError(stmt.posn.start, "Mismatch of types!");
							}
						} else {
							throwError(stmt.posn.start, "Parameter with invalid type passed in!");
						}
					}
				} else if (parameterTypeDenoter instanceof BaseType) {
					if (parameterTypeDenoter.typeKind.equals(TypeKind.INT) || parameterTypeDenoter.typeKind.equals(
							TypeKind.BOOLEAN)) {
						if (parameterTypeDenoter.typeKind != expressionTypeDenoter.typeKind) {
							throwError(stmt.posn.start, "Mismatch of types!");
						}
					} else {
						throwError(stmt.posn.start, "Parameter with invalid type passed in!");
					}
				}
				i++;
			}
			return null;
		} else {
			throwError(stmt.posn.start, "Fields cannot be called as methods!");
			return null;
		}
	}

	@Override
	public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
		MethodDecl methodDecl = ((MethodDecl)arg);
		if (methodDecl.type.typeKind.equals(TypeKind.VOID)) {
			if (stmt.returnExpr == null) {
				return new BaseType(TypeKind.NULL, null);
			} else {
				throwError(stmt.posn.start, "Methods of return type VOID must not return anything " +
						"even NULL!");
				return methodDecl.type;
			}
		}
		if (stmt.returnExpr == null) {
			throwError(stmt.posn.start, "Methods that have non-void return types must return something!");
			return methodDecl.type;
		}
		TypeDenoter returnStmt = stmt.returnExpr.visit(this, ((MethodDecl)arg).type);
		if (methodDecl.type.typeKind.equals(returnStmt.typeKind)) {
			if (methodDecl.type instanceof ClassType) {
				catchClassReference(stmt.returnExpr);
				if (((ClassType) methodDecl.type).className.spelling.equals(((ClassType)returnStmt).className.spelling)) {
					return returnStmt;
				} else {
					throwError(stmt.posn.start, "Return statement does not match the return type!");
					return methodDecl.type;
				}
			} else if (methodDecl.type instanceof ArrayType) {
				if (((ArrayType) methodDecl.type).eltType.typeKind.equals(TypeKind.INT)) {
					if (!(((ArrayType) returnStmt).eltType.typeKind.equals(TypeKind.INT))) {
						throwError(stmt.posn.start, "Return statement does not match the return type!");
						return methodDecl.type;
					} else {
						return returnStmt;
					}
				} else if (((ArrayType) methodDecl.type).eltType instanceof ClassType) {
					if (((ArrayType) returnStmt).eltType instanceof ClassType) {
						if (!((ClassType) ((ArrayType) methodDecl.type).eltType).className.spelling.equals(((ClassType)
								((ArrayType) returnStmt).eltType).className.spelling)) {
							throwError(stmt.posn.start, "Return statement does not match the return type!");
							return methodDecl.type;
						} else {
							return returnStmt;
						}
					} else {
						throwError(stmt.posn.start, "Return statement does not match the return type!");
						return methodDecl.type;
					}
				} else {
					throwError(stmt.posn.start, "Return statement does not match the return type!");
					return methodDecl.type;
				}
			} else {
				return methodDecl.type;
			}
		} else {
			throwError(stmt.posn.start, "Return statement does not match the return type!");
			return methodDecl.type;
		}
	}

	@Override
	public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
		TypeDenoter condition = stmt.cond.visit(this, new BaseType(TypeKind.BOOLEAN, null));
		stmt.thenStmt.visit(this, arg);
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, arg);
		}
		if (!condition.typeKind.equals(TypeKind.BOOLEAN)) {
			throwError(stmt.posn.start, "Condition in if statement should be of type boolean!");
		}
		return null;
	}

	@Override
	public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
		TypeDenoter condition = stmt.cond.visit(this, new BaseType(TypeKind.BOOLEAN, null));
		stmt.body.visit(this, arg);
		if (!condition.typeKind.equals(TypeKind.BOOLEAN)) {
			throwError(stmt.posn.start, "Condition in while statement should be of type boolean!");
		}
		return null;
	}

	@Override
	public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
		TypeDenoter expressionTypeDenoter = expr.expr.visit(this, arg);
		expr.operator.visit(this, null);
		if (expr.operator.spelling.equals("!")) {
			if (expressionTypeDenoter.typeKind.equals(TypeKind.BOOLEAN)) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else {
				throwError(expr.posn.start, "Unary expression using ! operator must be used with " +
						"boolean expression!");
				return new BaseType(TypeKind.BOOLEAN, null);
			}
		} else if (expr.operator.spelling.equals("-")) {
			if (expressionTypeDenoter.typeKind.equals(TypeKind.INT)) {
				return new BaseType(TypeKind.INT, null);
			} else {
				throwError(expr.posn.start, "Unary expression using - operator must be used with " +
						"integer expression!");
				return new BaseType(TypeKind.INT, null);
			}
		} else {
			return ((TypeDenoter)arg);
		}
	}

	@Override
	public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
		TypeDenoter expressionLeftTypeDenoter = expr.left.visit(this, arg);
		TypeDenoter expressionRightTypeDenoter = expr.right.visit(this, arg);
		expr.operator.visit(this, null);
		if (expr.operator.spelling.equals("+") || expr.operator.spelling.equals("-") || expr.operator.spelling.equals("*")
				|| expr.operator.spelling.equals("/")) {
			if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.INT) && expressionRightTypeDenoter.typeKind.equals(
					TypeKind.INT)) {
				return new BaseType(TypeKind.INT, null);
			} else {
				String operatorSpelling = expr.operator.spelling;
				throwError(expr.posn.start, operatorSpelling + " requires two expressions to evaluate" +
						" to INT!");
				return new BaseType(TypeKind.INT, null);
			}
		} else if (expr.operator.spelling.equals("&&") || expr.operator.spelling.equals("||")) {
			if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.BOOLEAN) && expressionRightTypeDenoter.typeKind.equals(
					TypeKind.BOOLEAN)) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else {
				String operatorSpelling = expr.operator.spelling;
				throwError(expr.posn.start, operatorSpelling + " requires two expressions to evaluate" +
						" to BOOLEAN!");
				return new BaseType(TypeKind.BOOLEAN, null);
			}
		} else if (expr.operator.spelling.equals("<") || expr.operator.spelling.equals("<=") || expr.operator.spelling.
				equals(">") || expr.operator.spelling.equals(">=")) {
			if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.INT) && expressionRightTypeDenoter.typeKind.equals(
					TypeKind.INT)) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else {
				String operatorSpelling = expr.operator.spelling;
				throwError(expr.posn.start, operatorSpelling + " requires two expressions to evaluate" +
						" to INT!");
				return new BaseType(TypeKind.BOOLEAN, null);
			}
		} else if (expr.operator.spelling.equals("==") || expr.operator.spelling.equals("!=")) {
			if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.INT) && expressionRightTypeDenoter.typeKind.equals(
					TypeKind.INT)) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.BOOLEAN) && expressionRightTypeDenoter.typeKind.
					equals(TypeKind.BOOLEAN)) {
				return new BaseType(TypeKind.BOOLEAN, null);
			} else if ((expressionLeftTypeDenoter.typeKind.equals(TypeKind.CLASS) && expressionRightTypeDenoter.typeKind.
					equals(TypeKind.CLASS))) {
				if (!(((ClassType)expressionLeftTypeDenoter).className.spelling.equals(((ClassType)expressionRightTypeDenoter).
						className.spelling))) {
					throwError(expr.posn.start, "Cannot compare two classes which are not of the same type!");
				}
				return new BaseType(TypeKind.BOOLEAN, null);
			} else if ((expressionLeftTypeDenoter.typeKind.equals(TypeKind.ARRAY) && expressionRightTypeDenoter.typeKind.
					equals(TypeKind.ARRAY))) {
				if (((ArrayType)expressionLeftTypeDenoter).eltType.typeKind.equals(((ArrayType)expressionRightTypeDenoter).
						eltType.typeKind)) {
					if (((ArrayType)expressionLeftTypeDenoter).eltType.typeKind.equals(TypeKind.CLASS)) {
						if (!(((ClassType)((ArrayType)expressionLeftTypeDenoter).eltType).className.spelling.equals(((ClassType)
								((ArrayType)expressionRightTypeDenoter).eltType).className.spelling))) {
							throwError(expr.posn.start, "Mismatch of types in comparison!");
						}
					} else {
						if (!(((ArrayType)expressionLeftTypeDenoter).eltType.typeKind.equals(((ArrayType)
								expressionRightTypeDenoter).eltType.typeKind))) {
							throwError(expr.posn.start, "Mismatch of types in comparison!");
						}
					}
				} else {
					throwError(expr.posn.start, "Mismatch of types in comparison!");
				}
				return new BaseType(TypeKind.BOOLEAN, null);
			} else {
				if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.NULL) && expressionRightTypeDenoter.typeKind.equals(TypeKind.CLASS)) {
					return new BaseType(TypeKind.BOOLEAN, null);
				} else if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.CLASS) && expressionRightTypeDenoter.typeKind.equals(TypeKind.NULL)) {
					return new BaseType(TypeKind.BOOLEAN, null);
				} else if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.NULL) && expressionRightTypeDenoter.typeKind.equals(TypeKind.ARRAY)) {
					return new BaseType(TypeKind.BOOLEAN, null);
				} else if (expressionLeftTypeDenoter.typeKind.equals(TypeKind.ARRAY) && expressionRightTypeDenoter.typeKind.equals(TypeKind.CLASS)) {
					return new BaseType(TypeKind.BOOLEAN, null);
				} else {
					String operatorSpelling = expr.operator.spelling;
					throwError(expr.posn.start, operatorSpelling + " requires two expressions of the " +
							"same type to evaluate!");
					return new BaseType(TypeKind.BOOLEAN, null);
				}
			}
		} else {
			throwError(expr.posn.start, "Provide a valid operator!");
			return ((TypeDenoter)arg);
		}
	}

	@Override
	public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
		return expr.ref.visit(this, null);
	}

	@Override
	public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
		TypeDenoter ref = expr.ref.visit(this, null);
		TypeDenoter ixExpr = expr.ixExpr.visit(this, arg);
		if (ixExpr.typeKind.equals(TypeKind.INT)) {
			if (ref instanceof ArrayType) {
				return ((ArrayType) ref).eltType;
			} else {
				throwError(expr.posn.start, "Cannot index a non-array!");
				return ((TypeDenoter) arg);
			}
		} else {
			throwError(expr.posn.start, "Need to index with an integer value!");
			return ((TypeDenoter)arg);
		}
	}

	@Override
	public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
		if (expr.functionRef.declaration instanceof MethodDecl) {
			if (((MethodDecl) expr.functionRef.declaration).parameterDeclList.size() != expr.argList.size()) {
				throwError(expr.posn.start, "Method call does not have correct size of arguments!");
				return ((TypeDenoter)arg);
			}
			int i = 0;
			ParameterDeclList parameterDeclList = ((MethodDecl) expr.functionRef.declaration).parameterDeclList;
			for (Expression expression: expr.argList) {
				TypeDenoter parameterTypeDenoter = parameterDeclList.get(i).type;
				TypeDenoter expressionTypeDenoter = expression.visit(this, parameterTypeDenoter);
				if (parameterTypeDenoter instanceof ClassType) {
					if (expressionTypeDenoter instanceof ClassType) {
						if (!(((ClassType) parameterTypeDenoter).className.spelling.equals(((ClassType) expressionTypeDenoter).
								className.spelling))) {
							throwError(expr.posn.start, "Mismatch of types in passing parameter!");
						}
					} else {
						throwError(expr.posn.start, "Mismatch of types in passing parameter - both " +
								"sides must be of Class type!");
					}
				} else if (parameterTypeDenoter instanceof ArrayType) {
					if (!(expressionTypeDenoter instanceof ArrayType)) {
						throwError(expr.posn.start, "Mismatch of types in passing parameter!");
					} else {
						if (((ArrayType) parameterTypeDenoter).eltType.typeKind.equals(TypeKind.INT)) {
							if (!(((ArrayType) expressionTypeDenoter).eltType.typeKind.equals(TypeKind.INT))) {
								throwError(expr.posn.start, "Mismatch of element types for array types on both " +
										"sides of the statement!");
							}
						} else if (((ArrayType) parameterTypeDenoter).eltType instanceof ClassType) {
							if (((ArrayType) expressionTypeDenoter).eltType instanceof ClassType) {
								if (!((ClassType) ((ArrayType) parameterTypeDenoter).eltType).className.spelling.equals(((ClassType)
										((ArrayType) expressionTypeDenoter).eltType).className.spelling)) {
									throwError(expr.posn.start, "Mismatch of element types in passing parameter!");
								}
							} else {
								throwError(expr.posn.start, "Mismatch of types!");
							}
						} else {
							throwError(expr.posn.start, "Parameter with invalid type passed in!");
						}
					}
				} else if (parameterTypeDenoter instanceof BaseType) {
					if (parameterTypeDenoter.typeKind.equals(TypeKind.INT) || parameterTypeDenoter.typeKind.equals(
							TypeKind.BOOLEAN)) {
						if (parameterTypeDenoter.typeKind != expressionTypeDenoter.typeKind) {
							throwError(expr.posn.start, "Mismatch of types!");
						}
					} else {
						throwError(expr.posn.start, "Parameter with invalid type passed in!");
					}
				}
				i++;
			}
			return expr.functionRef.declaration.type;
		} else {
			throwError(expr.posn.start, "Fields cannot be called as methods!");
			return expr.functionRef.declaration.type;
		}
	}

	@Override
	public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
		return expr.lit.visit(this, null);
	}

	@Override
	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		if (expr.classtype.className.spelling.equals("String")) {
			throwError(expr.posn.start, "Cannot declare new String objects!");
		}
		return expr.classtype.visit(this, null);
	}

	@Override
	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		TypeDenoter sizeExpr = expr.sizeExpr.visit(this, arg);
		TypeDenoter elementType = expr.eltType.visit(this, null);
		if (!sizeExpr.typeKind.equals(TypeKind.INT)) {
			throwError(expr.posn.start, "Index of new array expression needs to be an integer!");
		}
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
		if (ref.declaration == null) {
			if (ref.ref.declaration.type.typeKind.equals(TypeKind.ARRAY) && ref.id.spelling.equals("length")) {
				return new BaseType(TypeKind.INT, null);
			} else {
				throwError(ref.posn.start, "Declaration should only be null when retrieving length of " +
						"array!");
				return new BaseType(TypeKind.INT, null);
			}
		} else {
			return ref.declaration.type.visit(this, null);
		}
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

	private void throwError(int lineNumber, String message) {
		System.out.println("*** line " + lineNumber + ": " + "Type Error" + " - " + message);
		throwError = true;
	}

	private void catchClassReference(Expression expression) {
		if (expression instanceof RefExpr) {
			if (((RefExpr) expression).ref instanceof IdRef) {
				if (((RefExpr) expression).ref.declaration instanceof ClassDecl) {
					throwError(expression.posn.start, "Cannot reference a Class on its own!");
				}
			}
		}
	}
}
