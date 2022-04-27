package miniJava.CodeGenerator;

import mJAM.Machine;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.TokenKind;

import java.util.ArrayList;
import java.util.HashMap;

public class CodeGenerator implements Visitor<Object, Object> {

	public int patchAddrCallMain;
	public HashMap<Integer, Declaration> patching;
	public int framePointer;
	boolean baseLevel;
	boolean assignStatementFlag;
	public int instanceFieldCalls;

	public CodeGenerator(Package ast) {
		patching = new HashMap<>();
		framePointer = 0;
		baseLevel = false;
		assignStatementFlag = false;
		instanceFieldCalls = 0;
		Machine.initCodeGen();
		ast.visit(this, null);
	}

	@Override
	public Object visitPackage(Package prog, Object arg) {
		int displacement = 0;
		for (ClassDecl classDecl: prog.classDeclList) {
			int sizeClass = 0;
			for (FieldDecl fieldDecl: classDecl.fieldDeclList) {
				if (fieldDecl.isStatic) {
					fieldDecl.runtimeEntity = new KnownAddress(1, displacement, Machine.Reg.SB);
					displacement++;
				} else {
					sizeClass++;
				}
			}
			classDecl.runtimeEntity = new KnownAddress(sizeClass, displacement, Machine.Reg.SB);
		}
		Machine.emit(Machine.Op.PUSH, displacement);
		Machine.emit(Machine.Op.LOADL,0);
		Machine.emit(Machine.Prim.newarr);
		patchAddrCallMain = Machine.nextInstrAddr();
		Machine.emit(Machine.Op.CALL, Machine.Reg.CB,-1);
		Machine.emit(Machine.Op.HALT,0,0,0);
		for (ClassDecl classDecl: prog.classDeclList) {
			int index = 0;
			for (FieldDecl fieldDecl: classDecl.fieldDeclList) {
				if (!fieldDecl.isStatic) {
					fieldDecl.visit(this, index);
					index++;
				}
			}
		}
		for (ClassDecl classDecl: prog.classDeclList) {
			classDecl.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		for (MethodDecl methodDecl: cd.methodDeclList) {
			methodDecl.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		fd.runtimeEntity = new Field(1, (Integer)arg);
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		int methodAddress = Machine.nextInstrAddr();
		md.runtimeEntity = new KnownAddress(0, methodAddress, Machine.Reg.CB);
		if (md.isMain) {
			Machine.patch(patchAddrCallMain, ((KnownAddress)md.runtimeEntity).displacement);
		}
		checkPatching(md, methodAddress);
		int i = 1;
		for (ParameterDecl parameterDecl: md.parameterDeclList) {
			parameterDecl.visit(this, i);
			i++;
		}
		framePointer = 3;
		for (Statement statement:md.statementList) {
			statement.visit(this, md.type);
		}
		if (md.type.typeKind.equals(TypeKind.VOID)) {
			Machine.emit(Machine.Op.RETURN, 0, 0, md.parameterDeclList.size());
		} else {
			Machine.emit(Machine.Op.RETURN, 1, 0, md.parameterDeclList.size());
		}
		return null;
	}

	private void checkPatching(MethodDecl md, int methodAddress) {
		ArrayList<Integer> removeKeys = new ArrayList<>();
		for (Integer i: patching.keySet()) {
			if (patching.get(i).equals(md)) {
				Machine.patch(i, methodAddress);
				removeKeys.add(i);
			}
		}
		for (Integer i: removeKeys) {
			patching.remove(i);
		}
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		pd.runtimeEntity = new KnownAddress(1, -1*(Integer)arg, Machine.Reg.LB);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.runtimeEntity = new KnownAddress(Machine.characterSize, framePointer, Machine.Reg.LB);
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		int beforeVal = framePointer;
		for (Statement statement: stmt.sl) {
			statement.visit(this, null);
		}
		int popVal = framePointer - beforeVal;
		framePointer = framePointer - popVal;
		Machine.emit(Machine.Op.POP, popVal);
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null);
		framePointer++;
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		if (stmt.ref.declaration instanceof FieldDecl) {
			if (((FieldDecl) stmt.ref.declaration).isStatic) {
				stmt.val.visit(this, null);
				assignStatementFlag = true;
				stmt.ref.visit(this, null);
				assignStatementFlag = false;
				Machine.emit(Machine.Op.STOREI);
			} else {
				assignStatementFlag = true;
				stmt.ref.visit(this, null);
				assignStatementFlag = false;
				stmt.val.visit(this, null);
				Machine.emit(Machine.Prim.fieldupd);
			}
		} else if (stmt.ref.declaration instanceof LocalDecl) {
			stmt.val.visit(this, null);
			assignStatementFlag = true;
			stmt.ref.visit(this, null);
			assignStatementFlag = false;
			Machine.emit(Machine.Op.STOREI);
		}
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		stmt.ix.visit(this, null);
		stmt.exp.visit(this, null);
		Machine.emit(Machine.Prim.arrayupd);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		for(int i = stmt.argList.size()-1; i >= 0; i--) {
			stmt.argList.get(i).visit(this, null);
		}
		if (stmt.methodRef instanceof QualRef) {
			if (stmt.methodRef.declaration.name.equals("println")) {
				if (((ClassType)((QualRef) stmt.methodRef).ref.declaration.type).className.spelling.equals("_PrintStream")) {
					if (((QualRef)((QualRef) stmt.methodRef).ref).ref instanceof IdRef &&
							((QualRef)((QualRef) stmt.methodRef).ref).ref.declaration.name.equals("System")) {
						Machine.emit(Machine.Prim.putintnl);
						return null;
					}
				}
			}
		}
		stmt.methodRef.visit(this, null);
		int jump;
		if (stmt.methodRef.declaration.runtimeEntity == null) {
			jump = Machine.nextInstrAddr();
			patching.put(jump, stmt.methodRef.declaration);
		} else {
			jump = ((KnownAddress)stmt.methodRef.declaration.runtimeEntity).displacement;
		}
		if (((MethodDecl)stmt.methodRef.declaration).isStatic) {
			Machine.emit(Machine.Op.CALL, Machine.Reg.CB, jump);
		} else {
			Machine.emit(Machine.Op.CALLI, Machine.Reg.CB, jump);
		}
		if (!stmt.methodRef.declaration.type.typeKind.equals(TypeKind.VOID)) {
			Machine.emit(Machine.Op.POP, 1);
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		int patchCondAddr = Machine.nextInstrAddr();
		Machine.emit(Machine.Op.JUMPIF, 0, Machine.Reg.CB, -1);
		stmt.thenStmt.visit(this, null);
		int patchEndThen = Machine.nextInstrAddr();
		Machine.emit(Machine.Op.JUMP, Machine.Reg.CB, -1);
		int patchElseAddr = Machine.nextInstrAddr();
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, null);
		}
		int patchEndStmt = Machine.nextInstrAddr();
		Machine.patch(patchCondAddr, patchElseAddr);
		Machine.patch(patchEndThen, patchEndStmt);
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		int jumpToBePatched = Machine.nextInstrAddr();
		Machine.emit(Machine.Op.JUMP, Machine.Reg.CB, -1);
		int jumpBody = Machine.nextInstrAddr();
		stmt.body.visit(this, null);
		int jumpConditional = Machine.nextInstrAddr();
		Machine.patch(jumpToBePatched, jumpConditional);
		stmt.cond.visit(this, null);
		Machine.emit(Machine.Op.JUMPIF, 1, Machine.Reg.CB, jumpBody);
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.expr.visit(this, null);
		if (expr.operator.spelling.equals("-")) {
			Machine.emit(Machine.Prim.neg);
		} else if (expr.operator.spelling.equals("!")) {
			Machine.emit(Machine.Prim.not);
		}
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		if (expr.operator.spelling.equals("+")) {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			Machine.emit(Machine.Prim.add);
		} else if (expr.operator.spelling.equals("-")) {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			Machine.emit(Machine.Prim.sub);
		} else if (expr.operator.spelling.equals("*")) {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			Machine.emit(Machine.Prim.mult);
		} else if (expr.operator.spelling.equals("/")) {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			Machine.emit(Machine.Prim.div);
		} else if (expr.operator.spelling.equals("&&")) {
			expr.left.visit(this, null);
			int patchAndAddr = Machine.nextInstrAddr();
			Machine.emit(Machine.Op.JUMPIF, 0, Machine.Reg.CB, -1);
			Machine.emit(Machine.Op.LOADL, 1);
			expr.right.visit(this, null);
			Machine.emit(Machine.Prim.and);
			int patchAndAddrComplete = Machine.nextInstrAddr();
			Machine.emit(Machine.Op.JUMP, Machine.Reg.CB, -1);
			int patchAndAddrShortCircuit = Machine.nextInstrAddr();
			Machine.emit(Machine.Op.LOADL, 0);
			Machine.patch(patchAndAddr, patchAndAddrShortCircuit);
			int patchAndComplete = Machine.nextInstrAddr();
			Machine.patch(patchAndAddrComplete, patchAndComplete);
		} else if (expr.operator.spelling.equals("||")) {
			expr.left.visit(this, null);
			int patchOrAddr = Machine.nextInstrAddr();
			Machine.emit(Machine.Op.JUMPIF, 1, Machine.Reg.CB, -1);
			Machine.emit(Machine.Op.LOADL, 0);
			expr.right.visit(this, null);
			Machine.emit(Machine.Prim.or);
			int patchOrAddrComplete = Machine.nextInstrAddr();
			Machine.emit(Machine.Op.JUMP, Machine.Reg.CB, -1);
			int patchOrAddrShortCircuit = Machine.nextInstrAddr();
			Machine.emit(Machine.Op.LOADL, 1);
			Machine.patch(patchOrAddr, patchOrAddrShortCircuit);
			int patchOrComplete = Machine.nextInstrAddr();
			Machine.patch(patchOrAddrComplete, patchOrComplete);
		} else if (expr.operator.spelling.equals(">")) {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			Machine.emit(Machine.Prim.gt);
		} else if (expr.operator.spelling.equals("<")) {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			Machine.emit(Machine.Prim.lt);
		} else if (expr.operator.spelling.equals("==")) {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			Machine.emit(Machine.Prim.eq);
		} else if (expr.operator.spelling.equals("<=")) {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			Machine.emit(Machine.Prim.le);
		} else if (expr.operator.spelling.equals(">=")) {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			Machine.emit(Machine.Prim.ge);
		} else if (expr.operator.spelling.equals("!=")) {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			Machine.emit(Machine.Prim.ne);
		}
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
		Machine.emit(Machine.Prim.arrayref);
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		for(int i = expr.argList.size()-1; i >= 0; i--) {
			expr.argList.get(i).visit(this, null);
		}
		expr.functionRef.visit(this, null);
		int jump;
		if (expr.functionRef.declaration.runtimeEntity == null) {
			jump = Machine.nextInstrAddr();
			patching.put(jump, expr.functionRef.declaration);
		} else {
			jump = ((KnownAddress)expr.functionRef.declaration.runtimeEntity).displacement;
		}
		if (((MethodDecl)expr.functionRef.declaration).isStatic) {
			Machine.emit(Machine.Op.CALL, Machine.Reg.CB, jump);
		} else {
			Machine.emit(Machine.Op.CALLI, Machine.Reg.CB, jump);
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
		Machine.emit(Machine.Op.LOADL, -1);
		Machine.emit(Machine.Op.LOADL, expr.classtype.className.declaration.runtimeEntity.size);
		Machine.emit(Machine.Prim.newobj);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.sizeExpr.visit(this, null);
		Machine.emit( Machine.Prim.newarr);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		Machine.emit(Machine.Op.LOADA, Machine.Reg.OB, 0);
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		if (arg != null) {
			if (ref.declaration instanceof ClassDecl) {
				return null;
			} else if (ref.declaration instanceof FieldDecl) {
				if (!((FieldDecl) ref.declaration).isStatic) {
					instanceFieldCalls++;
					Machine.emit(Machine.Op.LOADA, Machine.Reg.OB, 0);
					Machine.emit(Machine.Op.LOADL, ((Field)ref.declaration.runtimeEntity).index);
					Machine.emit(Machine.Prim.fieldref);
				} else {
					Machine.emit(Machine.Op.LOAD, ((KnownAddress)ref.declaration.runtimeEntity).registerAddress,
							((KnownAddress)ref.declaration.runtimeEntity).displacement);
				}
			} else {
				instanceFieldCalls++;
				Machine.emit(Machine.Op.LOAD, ((KnownAddress)ref.declaration.runtimeEntity).registerAddress,
						((KnownAddress)ref.declaration.runtimeEntity).displacement);
			}
		} else {
			if (ref.declaration instanceof MethodDecl) {
				if (!((MethodDecl) ref.declaration).isStatic) {
					Machine.emit(Machine.Op.LOADA, Machine.Reg.OB, 0);
				}
			} else if (ref.declaration instanceof FieldDecl) {
				if (!((FieldDecl) ref.declaration).isStatic) {
					Machine.emit(Machine.Op.LOADA, Machine.Reg.OB, 0);
					Machine.emit(Machine.Op.LOADL, ((Field)ref.declaration.runtimeEntity).index);
					if (assignStatementFlag) {
						return null;
					}
					Machine.emit(Machine.Prim.fieldref);
				} else {
					if (assignStatementFlag) {
						Machine.emit(Machine.Op.LOADA, ((KnownAddress)ref.declaration.runtimeEntity).registerAddress,
								((KnownAddress)ref.declaration.runtimeEntity).displacement);
					} else {
						Machine.emit(Machine.Op.LOAD, ((KnownAddress)ref.declaration.runtimeEntity).registerAddress,
								((KnownAddress)ref.declaration.runtimeEntity).displacement);
					}
				}
			} else {
				if (assignStatementFlag) {
					Machine.emit(Machine.Op.LOADA, ((KnownAddress)ref.declaration.runtimeEntity).registerAddress,
							((KnownAddress)ref.declaration.runtimeEntity).displacement);
				} else {
					Machine.emit(Machine.Op.LOAD, ((KnownAddress)ref.declaration.runtimeEntity).registerAddress,
							((KnownAddress)ref.declaration.runtimeEntity).displacement);
				}
			}
		}
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		if (ref.ref.declaration.type != null && ref.ref.declaration.type.typeKind.equals(TypeKind.ARRAY)) {
			if (ref.id.spelling.equals("length")) {
				ref.ref.visit(this, null);
				Machine.emit(Machine.Prim.arraylen);
				return null;
			}
		}
		if (ref.id.declaration instanceof FieldDecl && arg == null && ((FieldDecl) ref.id.declaration).isStatic) {
			if (assignStatementFlag) {
				Machine.emit(Machine.Op.LOADA, ((KnownAddress)ref.id.declaration.runtimeEntity).registerAddress,
						((KnownAddress)ref.id.declaration.runtimeEntity).displacement);
			} else {
				Machine.emit(Machine.Op.LOAD, ((KnownAddress)ref.id.declaration.runtimeEntity).registerAddress,
						((KnownAddress)ref.id.declaration.runtimeEntity).displacement);
			}
			return null;
		}
		ref.ref.visit(this, ref);
		if (arg == null) {
			baseLevel = true;
		}
		ref.id.visit(this, ref);
		if (baseLevel) {
			instanceFieldCalls = 0;
		}
		baseLevel = false;
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		if (id.declaration instanceof FieldDecl) {
			if (!((FieldDecl) id.declaration).isStatic) {
				instanceFieldCalls++;
				Machine.emit(Machine.Op.LOADL, ((Field)id.declaration.runtimeEntity).index);
				if (baseLevel && assignStatementFlag) {
					return null;
				}
				Machine.emit(Machine.Prim.fieldref);
			} else {
				Machine.emit(Machine.Op.POP, instanceFieldCalls);
				instanceFieldCalls = 0;
				if (baseLevel && assignStatementFlag) {
					Machine.emit(Machine.Op.LOADA, ((KnownAddress)id.declaration.runtimeEntity).registerAddress,
							((KnownAddress)id.declaration.runtimeEntity).displacement);
				} else {
					Machine.emit(Machine.Op.LOAD, ((KnownAddress)id.declaration.runtimeEntity).registerAddress,
							((KnownAddress)id.declaration.runtimeEntity).displacement);
				}
			}
		} else if (id.declaration instanceof MethodDecl) {
			if (!((MethodDecl) id.declaration).isStatic) {
				return null;
			} else {
				Machine.emit(Machine.Op.POP, instanceFieldCalls);
			}
		}
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		Machine.emit(Machine.Op.LOADL, Integer.parseInt(num.spelling));
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		if (bool.kind.equals(TokenKind.TRUE)) {
			Machine.emit(Machine.Op.LOADL, Machine.trueRep);
		} else if (bool.kind.equals(TokenKind.FALSE)) {
			Machine.emit(Machine.Op.LOADL, Machine.falseRep);
		}
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLiteral, Object o) {
		Machine.emit(Machine.Op.LOADL, Machine.nullRep);
		return null;
	}
}
