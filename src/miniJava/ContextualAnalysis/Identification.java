package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

import java.util.HashMap;

public class Identification implements Visitor<Object, Object> {

	public IdentificationTable table;
	public ClassDecl currentClassDecl;
	public String currentClass;
	public HashMap<String, Declaration> classesDecl;

	public boolean baseLevel = false;
	public boolean inStaticMethod = false;

	private final String identificationError = "Identification Error";

	public Identification(Package ast) {
		table = new IdentificationTable();
		currentClassDecl = null;
		currentClass = null;
		classesDecl = new HashMap<>();
		ast.visit(this, null);
	}

	@Override
	public Object visitPackage(Package prog, Object arg)  {
		table.openScope();
		FieldDeclList fieldDeclListSystem = new FieldDeclList();
		fieldDeclListSystem.add(new FieldDecl(false, true, new ClassType(new Identifier(new Token
				(TokenKind.IDENTIFIER, "_PrintStream", null)), null), "out", null));
		ClassDecl system = new ClassDecl("System", fieldDeclListSystem, null, null);
		MethodDeclList methodDeclListPrintSystem = new MethodDeclList();
		ParameterDeclList parameterDeclList = new ParameterDeclList();
		parameterDeclList.add(new ParameterDecl(new BaseType(TypeKind.VOID, null), "n", null));
		methodDeclListPrintSystem.add(new MethodDecl(new FieldDecl(false, false, new BaseType
				(TypeKind.VOID, null), "println", null), parameterDeclList, new StatementList(), null));
		ClassDecl printStream = new ClassDecl("_PrintStream", null, methodDeclListPrintSystem, null);
		ClassDecl string = new ClassDecl("String", null, null, null);
		table.enter(system.name, system);
		table.enter(printStream.name, printStream);
		table.enter(string.name, string);
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
			throwError(type.posn.start, identificationError, "Need to declare object with valid ClassType");
			//throw new ContextualAnalysisException();
			return null;
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
		stmt.initExp.visit(this, null);
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		stmt.val.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		stmt.ix.visit(this, null);
		stmt.exp.visit(this, null);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		stmt.methodRef.visit(this, null);
		if (!(stmt.methodRef.declaration instanceof MethodDecl)) {
			throwError(stmt.posn.start, identificationError, "Need to call method with valid identifier!");
		}
		for (Expression expression: stmt.argList) {
			expression.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		stmt.returnExpr.visit(this, null);
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		table.openScope();
		stmt.thenStmt.visit(this, null);
		table.closeScope();
		table.openScope();
		stmt.elseStmt.visit(this, null);
		table.closeScope();
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		table.openScope();
		stmt.body.visit(this, null);
		table.closeScope();
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.expr.visit(this, null);
		expr.operator.visit(this, null);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		expr.left.visit(this, null);
		expr.operator.visit(this, null);
		expr.right.visit(this, null);
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		expr.ref.visit(this, null);
		expr.ixExpr.visit(this, null);
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		expr.functionRef.visit(this, null);
		if (!(expr.functionRef.declaration instanceof MethodDecl)) {
			throwError(expr.posn.start, identificationError, "Need to call method with valid identifier!");
		}
		for (Expression expression: expr.argList) {
			expression.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		expr.lit.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		expr.classtype.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.eltType.visit(this, null);
		expr.sizeExpr.visit(this, null);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		ref.declaration = currentClassDecl;
		return null;
	}

	// do I visit visitIdentification at all?
	// can declarations for both things be the same?

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		if (arg != null) {
			if (classesDecl.containsKey(ref.id.spelling)) {
				ref.id.declaration = classesDecl.get(ref.id.spelling);
				ref.declaration = classesDecl.get(ref.id.spelling);
			} else {
				ref.id.declaration = table.retrieve(ref.id.spelling);
				ref.declaration = table.retrieve(ref.id.spelling);
			}
		} else {
			Declaration declaration = table.retrieve(ref.id.spelling);
			if (declaration instanceof FieldDecl || declaration instanceof MethodDecl) {
				if (inStaticMethod) {
					if (!((MemberDecl) declaration).isStatic) {
						throwError(ref.posn.start, identificationError, "Cannot call an class attribute inside of static " +
								"context");
					} else {
						ref.declaration = declaration;
						ref.id.declaration = declaration;
					}
				} else {
					ref.declaration = declaration;
					ref.id.declaration = declaration;
				}
			} else if (declaration instanceof ClassDecl) {
				throwError(ref.posn.start, identificationError, "Cannot reference Class on it's own");
			} else {
				ref.declaration = declaration;
				ref.id.declaration = declaration;
			}
		}
		return null;
	}

	// correct logic?
	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		ref.ref.visit(this, true);
		FieldDeclList fieldDeclList;
		if (ref.ref instanceof ThisRef || ref.ref instanceof IdRef) {
			if (ref.ref.declaration instanceof ClassDecl) {
				fieldDeclList = ((ClassDecl)ref.ref.declaration).fieldDeclList;
			} else {
				fieldDeclList = ((ClassDecl)classesDecl.get(((ClassType)(ref.ref.declaration).type).className.
						spelling)).fieldDeclList;
			}
		} else {
			fieldDeclList = ((ClassDecl)classesDecl.get(((ClassType)(ref.ref.declaration).type).className.
					spelling)).fieldDeclList;
		}
		boolean found = false;
		for (FieldDecl fieldDecl: fieldDeclList) {
			if (fieldDecl.name.equals(ref.id.spelling)) {
				ref.declaration = fieldDecl;
				found = true;
			}
		}
		if (arg == null && !found) {
			MethodDeclList methodDeclList = ((ClassDecl)classesDecl.get(((ClassType)(ref.ref.declaration).type).className.
					spelling)).methodDeclList;;
			for (MethodDecl methodDecl: methodDeclList) {
				if (methodDecl.name.equals(ref.id.spelling)) {
					ref.declaration = methodDecl;
				}
			}
		}
		if (ref.declaration == null) {
			throwError(ref.posn.start, identificationError, "Could not grab the correct declaration");
			return null;
		}
		if (arg == null) {
			baseLevel = true;
		}
		ref.id.visit(this, ref);
		baseLevel = false;
		return null;
	}

	// correct logic?
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
				classDecl = ((ClassDecl)classesDecl.get(((ClassType)(((QualRef)arg).ref.declaration).type).className.spelling));
			}
		} else {
			classDecl = ((ClassDecl)classesDecl.get(((ClassType)(((QualRef)arg).ref.declaration).type).className.spelling));
		}
		FieldDeclList fieldDeclList = classDecl.fieldDeclList;
		boolean found = false;
		if (!isStatic && !isThis) {
			for (FieldDecl fieldDecl: fieldDeclList) {
				if (!fieldDecl.isPrivate && !fieldDecl.isStatic) {
					if (id.spelling.equals(fieldDecl.name)) {
						found = true;
						break;
					}
				}
			}
			if (baseLevel && !found) {
				MethodDeclList methodDeclList = classDecl.methodDeclList;
				for (MethodDecl methodDecl: methodDeclList) {
					if (!methodDecl.isPrivate && !methodDecl.isStatic) {
						if (id.spelling.equals(methodDecl.name)) {
							found = true;
							break;
						}
					}
				}
			}
		} else if (!isThis) {
			for (FieldDecl fieldDecl: fieldDeclList) {
				if (!fieldDecl.isPrivate && fieldDecl.isStatic) {
					if (id.spelling.equals(fieldDecl.name)) {
						found = true;
						break;
					}
				}
			}
			if (baseLevel && !found) {
				MethodDeclList methodDeclList = classDecl.methodDeclList;
				for (MethodDecl methodDecl: methodDeclList) {
					if (!methodDecl.isPrivate && methodDecl.isStatic) {
						if (id.spelling.equals(methodDecl.name)) {
							found = true;
							break;
						}
					}
				}
			}
		} else {
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
		}
		if (!found) {
			throwError(((QualRef)arg).posn.start, identificationError, "yo mama ugly");
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

	private void throwError(int lineNumber, String errorKind, String message) {
		System.out.println("*** line " + lineNumber + ": " + errorKind + " - " + message);
	}
}