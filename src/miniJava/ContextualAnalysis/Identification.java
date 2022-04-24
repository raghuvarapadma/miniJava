package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

import java.util.HashMap;

public class Identification implements Visitor<Object, Object> {

	public IdentificationTable table;
	public ClassDecl currentClassDecl;
	public String currentClass;
	public HashMap<String, Declaration> classesDecl;

	public boolean baseLevel = false;
	public boolean inStaticMethod = false;

	public Identification(Package ast) {
		table = new IdentificationTable();
		currentClassDecl = null;
		currentClass = null;
		classesDecl = new HashMap<>();
		ast.visit(this, null);
	}

	@Override
	public Object visitPackage(Package prog, Object arg)  {
		int countMainMethods = 0;
		for (ClassDecl classDecl: prog.classDeclList) {
			for (MethodDecl methodDecl: classDecl.methodDeclList) {
				if (!methodDecl.isPrivate && methodDecl.isStatic && methodDecl.name.equals("main") &&
						methodDecl.type.typeKind.equals(TypeKind.VOID) && (methodDecl.parameterDeclList.size() == 1 &&
						methodDecl.parameterDeclList.get(0).type.typeKind.equals(TypeKind.ARRAY) &&
						((ArrayType)methodDecl.parameterDeclList.get(0).type).eltType.typeKind.equals(TypeKind.CLASS) &&
						((ClassType)((ArrayType)methodDecl.parameterDeclList.get(0).type).eltType).className.spelling.
								equals("String"))) {
					methodDecl.isMain = true;
					countMainMethods++;
				}
			}
		}
		if (countMainMethods != 1) {
			throwError(prog.posn.start, "miniJava program should only contain unique main method!");
		}
		table.openScope();
		for (ClassDecl cd: prog.classDeclList) {
			table.enter(cd.name, cd);
			classesDecl.put(cd.name, cd);
		}
		for (ClassDecl cd: prog.classDeclList) {
			currentClass = cd.name;
			currentClassDecl = cd;
			cd.visit(this, null);
		}
		table.closeScope();
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg)  {
		table.openScope();
		for(FieldDecl fd: cd.fieldDeclList) {
			table.enter(fd.name, fd);
		}
		for(MethodDecl md: cd.methodDeclList) {
			table.enter(md.name, md);
		}
		for(FieldDecl fd: cd.fieldDeclList)
			fd.visit(this, null);
		for(MethodDecl md: cd.methodDeclList) {
			if (md.isStatic) {
				inStaticMethod = true;
			}
			md.visit(this, null);
			inStaticMethod = false;
		}
		table.closeScope();
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		fd.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		table.openScope();
		for (ParameterDecl pd: md.parameterDeclList) {
			pd.visit(this, null);
		}
		table.openScope();
		for (Statement st: md.statementList) {
			st.visit(this, null);
		}
		table.closeScope();
		table.closeScope();
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		table.enter(pd.name, pd);
		pd.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg)  {
		table.enter(decl.name, decl);
		decl.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		if (classesDecl.containsKey(type.className.spelling)) {
			type.className.declaration = classesDecl.get(type.className.spelling);
			return null;
		} else {
			throwError(type.posn.start, "Type needs to be set to a valid Class!");
			throw new ContextualAnalysisException();
		}
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		type.eltType.visit(this, null);
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		table.openScope();
		for (Statement statement: stmt.sl) {
			statement.visit(this, null);
		}
		table.closeScope();
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, stmt.varDecl.name);
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		if (stmt.ref.declaration instanceof MethodDecl) {
			throwError(stmt.posn.start, "Field is being read as method!");
			throw new ContextualAnalysisException();
		}
		stmt.val.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		if (stmt.ref.declaration instanceof MethodDecl) {
			throwError(stmt.posn.start, "Field is being read as method!");
			throw new ContextualAnalysisException();
		}
		stmt.ix.visit(this, null);
		stmt.exp.visit(this, null);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		stmt.methodRef.visit(this, null);
		if (!(stmt.methodRef.declaration instanceof MethodDecl)) {
			throwError(stmt.posn.start, "Need to call a valid Method!");
			throw new ContextualAnalysisException();
		}
		for (Expression expression: stmt.argList) {
			expression.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (stmt.returnExpr == null) {
			return null;
		} else {
			stmt.returnExpr.visit(this, null);
			return null;
		}
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		table.openScope();
		if (stmt.thenStmt instanceof VarDeclStmt) {
			throwError(stmt.posn.start, "A variable declaration cannot be the solitary " +
					"statement in a branch of a conditional statement");
			throw new ContextualAnalysisException();
		}
		stmt.thenStmt.visit(this, null);
		table.closeScope();
		table.openScope();
		if (stmt.elseStmt != null) {
			if (stmt.elseStmt instanceof VarDeclStmt) {
				throwError(stmt.posn.start, "A variable declaration cannot be the solitary " +
						"statement in a branch of a conditional statement");
				throw new ContextualAnalysisException();
			}
			stmt.elseStmt.visit(this, null);
		}
		table.closeScope();
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		table.openScope();
		if (stmt.body instanceof VarDeclStmt) {
			throwError(stmt.posn.start, "A variable declaration cannot be the solitary " +
					"statement in a branch of a conditional statement");
			throw new ContextualAnalysisException();
		}
		stmt.body.visit(this, null);
		table.closeScope();
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.expr.visit(this, arg);
		expr.operator.visit(this, arg);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		expr.left.visit(this, arg);
		expr.operator.visit(this, arg);
		expr.right.visit(this, arg);
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, arg);
		if (expr.ref.declaration instanceof MethodDecl) {
			throwError(expr.posn.start, "Field is being read as method!");
			throw new ContextualAnalysisException();
		}
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		expr.ref.visit(this, arg);
		if (expr.ref.declaration instanceof MethodDecl) {
			throwError(expr.posn.start, "Field is being read as method!");
			throw new ContextualAnalysisException();
		}
		expr.ixExpr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		expr.functionRef.visit(this, arg);
		if (!(expr.functionRef.declaration instanceof MethodDecl)) {
			throwError(expr.posn.start, "Need to call a valid Method!");
			throw new ContextualAnalysisException();
		}
		for (Expression expression: expr.argList) {
			expression.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		expr.lit.visit(this, arg);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		expr.classtype.visit(this, arg);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.eltType.visit(this, arg);
		expr.sizeExpr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		ref.declaration = currentClassDecl;
		if (inStaticMethod) {
			throwError(ref.posn.start, "Cannot use THIS keyword inside of static method!");
			throw new ContextualAnalysisException();
		}
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		if (arg != null) {
			if (arg instanceof QualRef) {
				Declaration declaration = table.retrieve(ref.id.spelling);
				if (declaration == null) {
					throwError(ref.posn.start, "Cannot reference undeclared identifier!");
					throw new ContextualAnalysisException();
				}
				if (declaration instanceof MethodDecl) {
					throwError(ref.posn.start, "Can only reference a Method towards the end of" +
							" a reference chain!");
					throw new ContextualAnalysisException();
				} else if (declaration instanceof FieldDecl) {
					if (inStaticMethod) {
						if (!((MemberDecl) declaration).isStatic) {
							throwError(ref.posn.start, "Cannot call an class attribute inside of " +
									"static context!");
							throw new ContextualAnalysisException();
						} else {
							ref.declaration = declaration;
							ref.id.declaration = declaration;
						}
					} else {
						ref.declaration = declaration;
						ref.id.declaration = declaration;
					}
				} else {
					ref.declaration = declaration;
					ref.id.declaration = declaration;
				}
			} else if (arg instanceof String) {
				if (arg.equals(ref.id.spelling)) {
					throwError(ref.posn.start, "Cannot use declared variable in " +
							"initializing expression!");
					throw new ContextualAnalysisException();
				}
				Declaration declaration = table.retrieve(ref.id.spelling);
				if (declaration == null) {
					throwError(ref.posn.start, "Cannot reference undeclared identifier!");
					throw new ContextualAnalysisException();
				}
				if (declaration instanceof FieldDecl || declaration instanceof MethodDecl) {
					if (inStaticMethod) {
						if (!((MemberDecl) declaration).isStatic) {
							throwError(ref.posn.start, "Cannot call an class attribute inside of " +
									"static context!");
							throw new ContextualAnalysisException();
						} else {
							ref.declaration = declaration;
							ref.id.declaration = declaration;
						}
					} else {
						ref.declaration = declaration;
						ref.id.declaration = declaration;
					}
				}
				else {
					ref.declaration = declaration;
					ref.id.declaration = declaration;
				}
			}
		} else {
			Declaration declaration = table.retrieve(ref.id.spelling);
			if (declaration == null) {
				throwError(ref.posn.start, "Cannot reference undeclared identifier!");
				throw new ContextualAnalysisException();
			}
			if (declaration instanceof FieldDecl || declaration instanceof MethodDecl) {
				if (inStaticMethod) {
					if (!((MemberDecl) declaration).isStatic) {
						throwError(ref.posn.start, "Cannot reference a class attribute inside of " +
								"static context!");
						throw new ContextualAnalysisException();
					} else {
						ref.declaration = declaration;
						ref.id.declaration = declaration;
					}
				} else {
					ref.declaration = declaration;
					ref.id.declaration = declaration;
				}
			}
			else {
				ref.declaration = declaration;
				ref.id.declaration = declaration;
			}
		}
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		if (!(arg instanceof Reference)) {
			arg = null;
		}
		ref.ref.visit(this, ref);
		if (ref.ref.declaration.type != null && ref.ref.declaration.type.typeKind.equals(TypeKind.ARRAY)) {
			if (ref.id.spelling.equals("length") && arg == null) {
				return null;
			} else {
				throwError(ref.posn.start, "The only field available to arrays is length!");
				throw new ContextualAnalysisException();
			}
		}
		FieldDeclList fieldDeclList;
		if (ref.ref instanceof ThisRef || ref.ref instanceof IdRef) {
			if (ref.ref.declaration instanceof ClassDecl) {
				fieldDeclList = ((ClassDecl)ref.ref.declaration).fieldDeclList;
			} else {
				if (ref.ref.declaration.type.typeKind.equals(TypeKind.CLASS)) {
					fieldDeclList = ((ClassDecl)classesDecl.get(((ClassType)(ref.ref.declaration).type).className.
							spelling)).fieldDeclList;
				} else {
					throwError(ref.posn.start, "References can only be of TypeKind class!");
					throw new ContextualAnalysisException();
				}
			}
		} else {
			fieldDeclList = ((ClassDecl)classesDecl.get(((ClassType)(ref.ref.declaration).type).className.
					spelling)).fieldDeclList;
		}
		boolean found = false;
		for (FieldDecl fieldDecl: fieldDeclList) {
			if (fieldDecl.name.equals(ref.id.spelling)) {
				ref.declaration = fieldDecl;
				ref.id.declaration = fieldDecl;
				found = true;
			}
		}
		if (arg == null && !found) {
			MethodDeclList methodDeclList;
			if (ref.ref.declaration instanceof ClassDecl) {
				methodDeclList = ((ClassDecl)ref.ref.declaration).methodDeclList;
			} else {
				methodDeclList = ((ClassDecl)classesDecl.get(((ClassType)(ref.ref.declaration).type).className.
						spelling)).methodDeclList;
			}
			for (MethodDecl methodDecl: methodDeclList) {
				if (methodDecl.name.equals(ref.id.spelling)) {
					ref.declaration = methodDecl;
					ref.id.declaration = methodDecl;
				}
			}
		}
		if (ref.declaration == null) {
			throwError(ref.posn.start, "Field or Method reference could not be found, so " +
					"could not grab the correct declaration!");
			throw new ContextualAnalysisException();
		}
		if (arg == null) {
			baseLevel = true;
		}
		ref.id.visit(this, ref);
		baseLevel = false;
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		ClassDecl classDecl;
		boolean isThis = false;
		boolean isStatic = false;
		if (((QualRef)arg).ref instanceof ThisRef) {
			classDecl = currentClassDecl;
			isThis = true;
		} else if (((QualRef)arg).ref instanceof IdRef) {
			if (((QualRef)arg).ref.declaration instanceof ClassDecl) {
				classDecl = (ClassDecl)((QualRef)arg).ref.declaration;
				isStatic = true;
			} else {
				classDecl = ((ClassDecl)classesDecl.get(((ClassType)(((QualRef)arg).ref.declaration).type).className.
						spelling));
			}
		} else {
			classDecl = ((ClassDecl)classesDecl.get(((ClassType)(((QualRef)arg).ref.declaration).type).className.spelling));
		}
		if (classDecl.name.equals(currentClass)) {
			isThis = true;
		}
		FieldDeclList fieldDeclList = classDecl.fieldDeclList;
		boolean found = false;

		if (isStatic) {
			for (FieldDecl fieldDecl: fieldDeclList) {
				if (fieldDecl.isStatic && !fieldDecl.isPrivate) {
					if (id.spelling.equals(fieldDecl.name)) {
						found = true;
						break;
					}
				}
			}
			if (baseLevel && !found) {
				MethodDeclList methodDeclList = classDecl.methodDeclList;
				for (MethodDecl methodDecl : methodDeclList) {
					if (methodDecl.isStatic && !methodDecl.isPrivate) {
						if (id.spelling.equals(methodDecl.name)) {
							found = true;
							break;
						}
					}
				}
			}
			if (isThis) {
				for (FieldDecl fieldDecl: fieldDeclList) {
					if (fieldDecl.isStatic) {
						if (id.spelling.equals(fieldDecl.name)) {
							found = true;
							break;
						}
					}
				}
				if (baseLevel && !found) {
					MethodDeclList methodDeclList = classDecl.methodDeclList;
					for (MethodDecl methodDecl : methodDeclList) {
						if (methodDecl.isStatic) {
							if (id.spelling.equals(methodDecl.name)) {
								found = true;
								break;
							}
						}
					}
				}
			}
		} else {
			if (isThis) {
				for (FieldDecl fieldDecl: fieldDeclList) {
					if (id.spelling.equals(fieldDecl.name)) {
						found = true;
						break;
					}
				}
				if (baseLevel && !found) {
					MethodDeclList methodDeclList = classDecl.methodDeclList;
					for (MethodDecl methodDecl: methodDeclList) {
						if (id.spelling.equals(methodDecl.name)) {
							found = true;
							break;
						}
					}
				}
			} else {
				for (FieldDecl fieldDecl: fieldDeclList) {
					if (!fieldDecl.isPrivate) {
						if (id.spelling.equals(fieldDecl.name)) {
							found = true;
							break;
						}
					}
				}
				if (baseLevel && !found) {
					MethodDeclList methodDeclList = classDecl.methodDeclList;
					for (MethodDecl methodDecl: methodDeclList) {
						if (!methodDecl.isPrivate) {
							if (id.spelling.equals(methodDecl.name)) {
								found = true;
								break;
							}
						}
					}
				}
			}
		}

		if (!found) {
			throwError(((QualRef)arg).posn.start, "Field or Method reference could not be " +
					"found or there is an issue with access modifiers!");
			throw new ContextualAnalysisException();
		}
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLiteral, Object o) {
		return null;
	}

	private void throwError(int lineNumber, String message) {
		System.out.println("*** line " + lineNumber + ": " + "Identification Error" + " - " + message);
	}
}