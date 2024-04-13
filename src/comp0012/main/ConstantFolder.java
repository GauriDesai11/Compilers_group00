package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.InstructionFinder;


public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	public class Result
	{
		private InstructionList default_il;
		private ConstantPoolGen default_cpgen;
	}

	private Result replaceInstructions(InstructionList il, InstructionHandle ih, InstructionHandle prev1, InstructionHandle prev2, Number result, ConstantPoolGen cpg)
	{
		int index = 0;
		if (prev1.getInstruction() instanceof LDC)
		{
			Object obj1 = ((LDC) prev1.getInstruction()).getValue(cpg);
			if (obj1 instanceof Integer)
			{
				index = cpg.addInteger(result.intValue());
			}else if (obj1 instanceof Float)
			{
				index = cpg.addFloat(result.floatValue());
			}

			il.insert(ih, new LDC(index));

		} else if (prev1.getInstruction() instanceof LDC2_W)
		{
			Object obj1 = ((LDC2_W) prev1.getInstruction()).getValue(cpg);
			if (obj1 instanceof Long)
			{
				//System.out.println("long value --> " + result.longValue() + "\n");
				index = cpg.addLong(result.longValue());
			}else if (obj1 instanceof Double)
			{
				index = cpg.addDouble(result.doubleValue());
			}

			il.insert(ih, new LDC2_W(index));
		}



		try {
			il.delete(ih);
			il.delete(prev1);
			il.delete(prev2);
		} catch (TargetLostException e) {
			throw new RuntimeException(e);
		}
		Result toReturn = new Result();
		toReturn.default_il = il;
		toReturn.default_cpgen = cpg;

		return toReturn;
	}

	private Number performOperation(Object obj1, Object obj2, Instruction inst)
	{

		//Integers
		if (inst instanceof IADD && obj1 instanceof Integer && obj2 instanceof Integer) {
			//System.out.println("1st: " + (Integer) obj1 + " and " + (Integer) obj2 + " = ");
			return (Integer) obj1 + (Integer) obj2;
		} else if ((inst instanceof ISUB && obj1 instanceof Integer && obj2 instanceof Integer))
		{
			//System.out.println("1st: " + (Integer) obj1 + " and " + (Integer) obj2 + " = ");
			return (Integer) obj2 - (Integer) obj1;
		} else if ((inst instanceof IMUL && obj1 instanceof Integer && obj2 instanceof Integer))
		{
			//System.out.println("1st: " + (Integer) obj1 + " and " + (Integer) obj2 + " = ");
			return (Integer) obj1 * (Integer) obj2;
		} else if ((inst instanceof IDIV && obj1 instanceof Integer && obj2 instanceof Integer))
		{
			//System.out.println("1st: " + (Integer) obj1 + " and " + (Integer) obj2 + " = ");
			return (Integer) obj2 / (Integer) obj1;
		}

		//long
		if (inst instanceof LADD && obj1 instanceof Long && obj2 instanceof Long) {
			//System.out.println("1st: " + (Long) obj1 + " and " + (Long) obj2 + " = ");
			return (Long) obj1 + (Long) obj2;
		} else if ((inst instanceof LSUB && obj1 instanceof Long && obj2 instanceof Long))
		{
			//System.out.println("1st: " + obj2 + " - " + obj1 + " = " + ((Long) obj2 - (Long) obj1) + "\n");
			return (Long) obj2 - (Long) obj1;
		} else if ((inst instanceof LMUL && obj1 instanceof Long && obj2 instanceof Long))
		{
			//System.out.println("1st: " + (Long) obj1 + " and " + (Long) obj2 + " = ");
			return (Long) obj1 * (Long) obj2;
		} else if ((inst instanceof LDIV && obj1 instanceof Long && obj2 instanceof Long))
		{
			//System.out.println("1st: " + (Long) obj1 + " and " + (Long) obj2 + " = ");
			return (Long) obj2 / (Long) obj1;
		}

		//float
		if (inst instanceof FADD && obj1 instanceof Float && obj2 instanceof Float) {
			//System.out.println("1st: " + (Float) obj1 + " and " + (Float) obj2 + " = ");
			return (Float) obj1 + (Float) obj2;
		} else if ((inst instanceof FSUB && obj1 instanceof Float && obj2 instanceof Float))
		{
			//System.out.println("1st: " + (Float) obj1 + " and " + (Float) obj2 + " = ");
			return (Float) obj2 - (Float) obj1;
		} else if ((inst instanceof FMUL && obj1 instanceof Float && obj2 instanceof Float))
		{
			//System.out.println("1st: " + (Float) obj1 + " and " + (Float) obj2 + " = ");
			return (Float) obj1 * (Float) obj2;
		} else if ((inst instanceof FDIV && obj1 instanceof Float && obj2 instanceof Float))
		{
			//System.out.println("1st: " + (Float) obj1 + " and " + (Float) obj2 + " = ");
			return (Float) obj2 / (Float) obj1;
		}

		//double
		if (inst instanceof DADD && obj1 instanceof Double && obj2 instanceof Double) {
			//System.out.println("1st: " + (Double) obj1 + " and " + (Double) obj2 + " = ");
			return (Double) obj1 + (Double) obj2;
		} else if ((inst instanceof DSUB && obj1 instanceof Double && obj2 instanceof Double))
		{
			//System.out.println("1st: " + (Double) obj1 + " and " + (Double) obj2 + " = ");
			return (Double) obj2 - (Double) obj1;
		} else if ((inst instanceof DMUL && obj1 instanceof Double && obj2 instanceof Double))
		{
			//System.out.println("1st: " + (Double) obj1 + " and " + (Double) obj2 + " = ");
			return (Double) obj1 * (Double) obj2;
		} else if ((inst instanceof DDIV && obj1 instanceof Double && obj2 instanceof Double))
		{
			//System.out.println("1st: " + (Double) obj1 + " and " + (Double) obj2 + " = ");
			return (Double) obj2 / (Double) obj1;
		}


		// Add more conditions for other types and operations (ISUB, IMUL, IDIV, etc.)
		return 0;
	}

	public Result handleArithmetic(InstructionHandle ih, InstructionList instructionList, ConstantPoolGen cpgen)
	{
		Result returned = new Result();
		returned.default_il = instructionList;
		returned.default_cpgen = cpgen;

		InstructionHandle prev1 = ih.getPrev();
		InstructionHandle prev2 = prev1 != null ? prev1.getPrev() : null;
		if (prev1 != null && prev2 != null) {
			Instruction prevInst1 = prev1.getInstruction();
			Instruction prevInst2 = prev2.getInstruction();

			if (prevInst1 instanceof LDC && prevInst2 instanceof LDC) {
				Object obj1 = ((LDC) prevInst1).getValue(cpgen);
				Object obj2 = ((LDC) prevInst2).getValue(cpgen);

				if (obj1.getClass().equals(obj2.getClass())) {
					Number result = performOperation(obj1, obj2, ih.getInstruction());
					//System.out.println("result = " + result + "\n");
					returned = replaceInstructions(instructionList, ih, prev1, prev2, result, cpgen);
				}
			} else if (prevInst1 instanceof LDC2_W && prevInst2 instanceof LDC2_W)
			{
				Object obj1 = ((LDC2_W) prevInst1).getValue(cpgen);
				Object obj2 = ((LDC2_W) prevInst2).getValue(cpgen);

				if (obj1.getClass().equals(obj2.getClass())) {
					Number result = performOperation(obj1, obj2, ih.getInstruction());
					//System.out.println("result = " + result + "\n");
					returned = replaceInstructions(instructionList, ih, prev1, prev2, result, cpgen);
				}
			}
		}
		return returned;
	}

	public ConstantPoolGen SimpleFoldingOptimization(ConstantPoolGen cpgen)
	{
		Method[] methods = gen.getMethods();
		Result returned = new Result();

		for (Method method : methods) {

			MethodGen methodGen = new MethodGen(method, gen.getClassName(), cpgen);
			InstructionList instructionList = methodGen.getInstructionList();

			returned.default_il = instructionList;
			returned.default_cpgen = cpgen;

			if (instructionList != null) {
				for (InstructionHandle ih : instructionList.getInstructionHandles()) {

					Instruction instruction = ih.getInstruction();

					if (instruction instanceof ArithmeticInstruction)
					{
						returned = handleArithmetic(ih, instructionList, cpgen);
					}
				}

				methodGen.setInstructionList(returned.default_il);
				methodGen.setMaxStack(); //stack frame size needs to be correctly calculated and set
				methodGen.setMaxLocals();
				gen.replaceMethod(method, methodGen.getMethod()); //'method' = original method --> replace this with the new method with the updated InstructionList
			}
		}
		return returned.default_cpgen;
	}

	// private Map<Integer, Number> findConstantVariables(Method method, ConstantPoolGen cpgen) {
	// 	Map<Integer, Number> constantVariables = new HashMap<>();
	// 	Set<Integer> modifiedVariables = new HashSet<>();

	// 	InstructionList il = new InstructionList(method.getCode().getCode());
	// 	MethodGen mgen = new MethodGen(
	// 		method.getAccessFlags(),
	// 		method.getReturnType(),
	// 		method.getArgumentTypes(),
	// 		null,
	// 		method.getName(),
	// 		gen.getClassName(),
	// 		il,
	// 		cpgen
	// 	);

	// 	for (InstructionHandle ih : il.getInstructionHandles()) {
	// 		Instruction inst = ih.getInstruction();
	// 		if (inst instanceof StoreInstruction) {
	// 			StoreInstruction store = (StoreInstruction) inst;
	// 			if (!modifiedVariables.contains(store.getIndex())) {
	// 				InstructionHandle prevIh = ih.getPrev();
	// 				System.out.println(prevIh);
	// 				if (prevIh != null && (prevIh.getInstruction() instanceof LDC || 
	// 									   prevIh.getInstruction() instanceof LDC2_W ||
	// 									   prevIh.getInstruction() instanceof SIPUSH ||
	// 									   prevIh.getInstruction() instanceof BIPUSH)) {
	// 									//    prevIh.getInstruction() instanceof BIPUSH ||
	// 									//    prevIh.getInstruction() instanceof SIPUSH ||
	// 									//    prevIh.getInstruction() instanceof ICONST ||
	// 									//    prevIh.getInstruction() instanceof FCONST ||
	// 									//    prevIh.getInstruction() instanceof DCONST ||
	// 									//    prevIh.getInstruction() instanceof LCONST)) {
	// 					Number value = getConstantValueFromInstruction(prevIh.getInstruction(), cpgen);
	// 					if (value != null) {
	// 						constantVariables.put(store.getIndex(), value);
	// 						System.out.println("Constant variable found: Index = " + store.getIndex() + ", Value = " + value);
	// 					}
	// 				}
	// 			}
	// 		} else if (inst instanceof LoadInstruction) {
	// 			LoadInstruction load = (LoadInstruction) inst;
	// 			modifiedVariables.add(load.getIndex());
	// 		}
	// 	}

	// 	// for (InstructionHandle ih : il.getInstructionHandles()) {
	// 	// 	Instruction inst = ih.getInstruction();
	// 	// 	if (inst instanceof StoreInstruction) {
	// 	// 		StoreInstruction store = (StoreInstruction) inst;
	// 	// 		// If it's already in constantVariables but now being modified, remove it
	// 	// 		if (constantVariables.containsKey(store.getIndex())) {
	// 	// 			constantVariables.remove(store.getIndex());
	// 	// 			modifiedVariables.add(store.getIndex());
	// 	// 		}
	// 	// 		// If it's not marked modified yet, check if it should be added
	// 	// 		else if (!modifiedVariables.contains(store.getIndex())) {
	// 	// 			InstructionHandle prevIh = ih.getPrev();
	// 	// 			if (prevIh != null && isConstantValueInstruction(prevIh.getInstruction(), cpgen)) {
	// 	// 				Number value = getConstantValueFromInstruction(prevIh.getInstruction(), cpgen);
	// 	// 				constantVariables.put(store.getIndex(), value);
	// 	// 			}
	// 	// 		}
	// 	// 	}
	// 	// }
	// 	return constantVariables;
	// }

	private Map<Integer, Number> findConstantVariables(Method method, ConstantPoolGen cpgen) {
		Map<Integer, Number> constantVariables = new HashMap<>();
		Set<Integer> modifiedVariables = new HashSet<>(); // Track all variable indices that are modified
		
		Code methodCode = method.getCode();

		if (methodCode.getCode() == null) {
			System.out.println("No code in method: " + method.getName());
			return constantVariables;
		}
		
		InstructionList il = new InstructionList(methodCode.getCode());
		MethodGen mgen = new MethodGen(method, gen.getClassName(), cpgen);

		for (InstructionHandle ih : il.getInstructionHandles()) {
			Instruction inst = ih.getInstruction();

			// if (inst instanceof StoreInstruction) {
            //     StoreInstruction store = (StoreInstruction) inst;
            //     if (ih.getPrev() != null && ih.getPrev().getInstruction() instanceof PushInstruction) {
            //         PushInstruction push = (PushInstruction) ih.getPrev().getInstruction();
            //         if (push instanceof ConstantPushInstruction) {
            //             ConstantPushInstruction constPush = (ConstantPushInstruction) push;
            //             constantVariables.put(store.getIndex(), constPush.getValue().intValue());
            //         }
            //     }
            // } else if (inst instanceof LoadInstruction) {
            //     LoadInstruction load = (LoadInstruction) inst;
            //     if (constantVariables.containsKey(load.getIndex())) {
            //         int constValue = constantVariables.get(load.getIndex());
            //         InstructionList newInstList = new InstructionList();
            //         newInstList.append(new LDC(cpgen.addInteger(constValue)));
            //         try {
            //             il.insert(ih, newInstList);
            //             il.delete(ih);
            //         } catch (TargetLostException e) {
            //             for (InstructionHandle target : e.getTargets()) {
            //                 target.updateTarget(ih, ih.getNext());
            //             }
            //         }
            //     }
            // }

			if (inst instanceof StoreInstruction) {
				StoreInstruction store = (StoreInstruction) inst;
				int index = store.getIndex();
				
				// Check if the variable has not been modified previously
				if (!modifiedVariables.contains(index)) {
					InstructionHandle prevIh = ih.getPrev();
					if (prevIh != null) {
						Instruction prevInst = prevIh.getInstruction();
						Number constantValue = getConstantValueFromInstruction(prevInst, cpgen);

						if (constantValue != null) {
							constantVariables.put(index, constantValue);
							System.out.println("Constant variable found: Index = " + index + ", Value = " + constantValue);
						}
					}
				}
			} else {
				// If any instruction that can modify a variable is encountered, track the variable index
				if (inst instanceof LoadInstruction || inst instanceof IINC || inst instanceof ArithmeticInstruction) {
					int index = getIndexFromInstruction(inst);
					if (index >= 0) {
						modifiedVariables.add(index);
					}
				}
			}
		}

		constantVariables.keySet().removeAll(modifiedVariables);
    	constantVariables.keySet().removeIf(modifiedVariables::contains);
		return constantVariables;
	}

	private Number getConstantValueFromInstruction(Instruction inst, ConstantPoolGen cpgen) {
		if (inst instanceof LDC) return (Number) ((LDC) inst).getValue(cpgen);
		if (inst instanceof LDC2_W) return (Number) ((LDC2_W) inst).getValue(cpgen);
		if (inst instanceof BIPUSH) return ((BIPUSH) inst).getValue();
		if (inst instanceof SIPUSH) return ((SIPUSH) inst).getValue();
		if (inst instanceof ICONST) return ((ICONST) inst).getValue();
		if (inst instanceof FCONST) return ((FCONST) inst).getValue();
		if (inst instanceof DCONST) return ((DCONST) inst).getValue();
		if (inst instanceof LCONST) return ((LCONST) inst).getValue();
		return null; // Return null if the instruction does not yield a constant
	}

	private int getIndexFromInstruction(Instruction inst) {
		if (inst instanceof LocalVariableInstruction) {
			return ((LocalVariableInstruction) inst).getIndex();
		} else if (inst instanceof IINC) {
			return ((IINC) inst).getIndex();
		}
		return -1; // Return -1 if no valid index is found
	}

	private void replaceConstantVariableLoads(Method method, Map<Integer, Number> constants, ConstantPoolGen cpgen) {
		MethodGen mgen = new MethodGen(method, gen.getClassName(), cpgen);
		InstructionList il = mgen.getInstructionList();
		if (il == null) return;

		for (InstructionHandle ih : il.getInstructionHandles()) {
			Instruction inst = ih.getInstruction();
			if (inst instanceof LoadInstruction) {
				LoadInstruction load = (LoadInstruction) inst;
				if (constants.containsKey(load.getIndex())) {
					Number value = constants.get(load.getIndex());
					Instruction newInst = null;
					if (value instanceof Integer) {
						newInst = new LDC(cpgen.addInteger(value.intValue()));
					} else if (value instanceof Float) {
						newInst = new LDC(cpgen.addFloat(value.floatValue()));
					} else if (value instanceof Long) {
						newInst = new LDC2_W(cpgen.addLong(value.longValue()));
					} else if (value instanceof Double) {
						newInst = new LDC2_W(cpgen.addDouble(value.doubleValue()));
					}
					System.out.println("Replacing variable load with constant: Variable Index = " + load.getIndex() + ", Value = " + value);
					try {
						il.insert(ih, newInst);
						il.delete(ih);
					} catch (TargetLostException e) {
						for (InstructionHandle target : e.getTargets()) {
							InstructionTargeter[] targeters = target.getTargeters();
							for (InstructionTargeter t : targeters) {
								t.updateTarget(target, null);
							}
						}
					}
				}
			}
		}
		mgen.setInstructionList(il);
		mgen.setMaxStack();
		mgen.setMaxLocals();
		gen.replaceMethod(method, mgen.getMethod());
	}

	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		// Implement your optimization here

		if ((cgen.getClassName()).equals("comp0012.target.SimpleFolding"))
		{
			cpgen = SimpleFoldingOptimization(cpgen);
		}

		for (Method method : cgen.getMethods()) {
			System.out.println("Processing method: " + method.getName());
			Map<Integer, Number> constantVariables = findConstantVariables(method, cpgen);
			replaceConstantVariableLoads(method, constantVariables, cpgen);
		}

		gen.setConstantPool(cpgen); //update the constant pool
		this.optimized = gen.getJavaClass(); //line from the original code
	}


	public void write(String optimisedFilePath)
	{
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}
