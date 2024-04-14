package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

	private boolean isConstantPush(Instruction inst){
		return inst instanceof LDC || inst instanceof LDC2_W
				|| inst instanceof ICONST || inst instanceof  BIPUSH
				|| inst instanceof SIPUSH;
	}


	private Number getConstantValue(InstructionHandle ih, ConstantPoolGen cpgen) {
		Instruction inst = ih.getInstruction();

		// Check for the LDC instruction which loads constants from the constant pool
		if (inst instanceof LDC) {
			LDC ldc = (LDC) inst;
			Object value = ldc.getValue(cpgen);
			if (value instanceof Number) {
				return (Number) value;
			}
		}
		// Check for the LDC2_W instruction which loads long and double constants from the constant pool
		else if (inst instanceof LDC2_W) {
			LDC2_W ldc2_w = (LDC2_W) inst;
			Object value = ldc2_w.getValue(cpgen);
			if (value instanceof Number) {
				return (Number) value;
			}
		}
		// Check for the BIPUSH instruction which pushes a byte constant onto the stack
		else if (inst instanceof BIPUSH) {
			BIPUSH bipush = (BIPUSH) inst;
			return Integer.valueOf(bipush.getValue().intValue());
		}
		// Check for the SIPUSH instruction which pushes a short constant onto the stack
		else if (inst instanceof SIPUSH) {
			SIPUSH sipush = (SIPUSH) inst;
			return Integer.valueOf(sipush.getValue().intValue());
		}
		// Check for ICONST_* instructions which push various int constants onto the stack
		else if (inst instanceof ICONST) {
			ICONST iconst = (ICONST) inst;
			return Integer.valueOf(iconst.getValue().intValue());
		}

		// None of the above, return null indicating the instruction does not push a constant number.
		return null;
	}

	private boolean hasBeenReassigned(int index, InstructionHandle start, InstructionHandle end){
		InstructionHandle current = start;
		while (current != null && current != end){
			Instruction inst = current.getInstruction();
			if (inst instanceof StoreInstruction){
				StoreInstruction store = (StoreInstruction) inst;
				if (store.getIndex() == index){
					return true;
				}
			}
			current = current.getNext();
		}
		return false;
	}

	private void replaceLoadWithConstant(InstructionList il, InstructionHandle ih, Number value, ConstantPoolGen cpgen) {
		Instruction newInst = null;

		if (value instanceof Integer) {
			int intValue = value.intValue();
			if (intValue >= -1 && intValue <= 5) {
				newInst = new ICONST(intValue);
			} else if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
				newInst = new BIPUSH((byte) intValue);
			} else if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
				newInst = new SIPUSH((short) intValue);
			} else {
				newInst = new LDC(cpgen.addInteger(intValue)); // Corrected from ICONST to LDC
			}
		} else if (value instanceof Long) {
			newInst = new LDC2_W(cpgen.addLong(value.longValue()));
		} else if (value instanceof Float) {
			newInst = new LDC(cpgen.addFloat(value.floatValue()));
		} else if (value instanceof Double) {
			newInst = new LDC2_W(cpgen.addDouble(value.doubleValue()));
		}

		if (newInst != null) {
			// Create a new instruction handle for the new instruction
			InstructionHandle newIh = il.insert(ih, newInst);

			// Now delete the original instruction handle
			try {
				il.delete(ih);
			} catch (TargetLostException e) {
				// Correctly update any targeters to point to the new instruction handle
				for (InstructionHandle lostTarget : e.getTargets()) {
					for (InstructionTargeter targeter : lostTarget.getTargeters()) {
						targeter.updateTarget(lostTarget, newIh);
					}
				}
			}
			// Recalculate the positions for the instructions
			il.setPositions(true);
		}
	}

	public void optimizeDynamicVariables(Method method, ConstantPoolGen cpgen) {
		MethodGen mg = new MethodGen(method, gen.getClassName(), cpgen);
		InstructionList il = mg.getInstructionList();

		// Maps to hold the current constant value for each variable index
		Map<Integer, Number> variableValues = new HashMap<>();

		if (il != null) {
			for (InstructionHandle ih : il.getInstructionHandles()) {
				Instruction inst = ih.getInstruction();

				if (inst instanceof StoreInstruction) {
					StoreInstruction store = (StoreInstruction) inst;
					if (isConstantPush(ih.getPrev().getInstruction())) {
						Number value = getConstantValue(ih.getPrev(), cpgen);

						if (value != null) {
							// Update the constant value for the variable
							variableValues.put(store.getIndex(), value);
						}
					} else {
						// If a non-constant value is stored, remove the variable from the map
						variableValues.remove(store.getIndex());
					}
				} else if (inst instanceof LoadInstruction) {
					LoadInstruction load = (LoadInstruction) inst;
					int index = load.getIndex();
					if (variableValues.containsKey(index)) {
						// Replace the load instruction with the constant value
						replaceLoadWithConstant(il, ih, variableValues.get(index), cpgen);
					}
				} else if (inst instanceof ArithmeticInstruction) {
					InstructionHandle prev1 = ih.getPrev();
					InstructionHandle prev2 = prev1 != null ? prev1.getPrev() : null;

					if (prev1 != null && prev2 != null) {
						Number value1 = getConstantValue(prev1, cpgen);
						Number value2 = getConstantValue(prev2, cpgen);

						if (value1 == null && prev1.getInstruction() instanceof LoadInstruction) {
							// If the first operand is a load instruction, check if it loads a constant variable
							int index = ((LoadInstruction) prev1.getInstruction()).getIndex();
							value1 = variableValues.get(index);
						}

						if (value2 == null && prev2.getInstruction() instanceof LoadInstruction) {
							// If the second operand is a load instruction, check if it loads a constant variable
							int index = ((LoadInstruction) prev2.getInstruction()).getIndex();
							value2 = variableValues.get(index);
						}

						if (value1 != null && value2 != null) {
							// Perform the arithmetic operation on the constant values
							Number result = performOperation(value1, value2, (ArithmeticInstruction) inst);

							// Replace the arithmetic instruction with a constant push instruction
							Instruction constantInst = createConstantPushInstruction(result, cpgen);
							InstructionHandle newIh = il.insert(ih, constantInst);

							try {
								il.delete(prev2);
								il.delete(prev1);
								il.delete(ih);
							} catch (TargetLostException e) {
								// Update any targeters to point to the new instruction handle
								for (InstructionHandle target : e.getTargets()) {
									for (InstructionTargeter targeter : target.getTargeters()) {
										targeter.updateTarget(target, newIh);
									}
								}
							}
						}
					}
				}
			}

			il.setPositions(true);

			mg.setInstructionList(il);
			mg.setMaxStack();
			mg.setMaxLocals();
			this.gen.replaceMethod(method, mg.getMethod());
		}
	}
	private Instruction createConstantPushInstruction(Number value, ConstantPoolGen cpgen) {
		if (value instanceof Integer) {
			int intValue = value.intValue();
			if (intValue >= -1 && intValue <= 5) {
				return new ICONST(intValue);
			} else if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
				return new BIPUSH((byte) intValue);
			} else if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
				return new SIPUSH((short) intValue);
			} else {
				return new LDC(cpgen.addInteger(intValue));
			}
		} else if (value instanceof Long) {
			return new LDC2_W(cpgen.addLong(value.longValue()));
		} else if (value instanceof Float) {
			return new LDC(cpgen.addFloat(value.floatValue()));
		} else if (value instanceof Double) {
			return new LDC2_W(cpgen.addDouble(value.doubleValue()));
		}
		return null;
	}

	public class Result
	{
		//helps return the instruction list and the ConstantPoolGen after editing them
		// in each function
		private InstructionList default_il;
		private ConstantPoolGen default_cpgen;
	}

	private Result replaceInstructions(InstructionList il, InstructionHandle ih, InstructionHandle prev1, InstructionHandle prev2, Number result, ConstantPoolGen cpg)
	{
		int index = 0;
		if (prev1.getInstruction() instanceof LDC)
		{
			//specific for integer and float values
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
			//specific for long and double values
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


		//delete the instructions after adding the correct ones before
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

		return toReturn; //return the updated instruction list and the ConstantPoolGen
	}

	private Number performOperation(Object obj1, Object obj2, Instruction inst)
	{
		//for subtraction -- obj2 (prev prev instruction) - obj1 (prev instruction)
		//for division --> obj2 / obj1

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

			//get the value being loaded by checking if it is LDC or LDC2_W
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

		//'returned' = updated instruction list and the ConstantPoolGen after editing them
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

				//'returned' = updated instruction list and the ConstantPoolGen after editing them
				methodGen.setInstructionList(returned.default_il);
				methodGen.setMaxStack(); //stack frame size needs to be correctly calculated and set
				methodGen.setMaxLocals();
				gen.replaceMethod(method, methodGen.getMethod()); //'method' = original method --> replace this with the new method with the updated InstructionList
			}
		}
		//only need to return the ConstantPoolGen since gen's method has been replaced/ updated
		return returned.default_cpgen;
	}



	public void optimize() {
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		// Check if the class is the specific target for dynamic variable folding
		if ((cgen.getClassName()).equals("comp0012.target.DynamicVariableFolding")) {
			System.out.println("dynamic\n");
			Method[] methods = cgen.getMethods();
			for (Method method : methods) {
				// Apply dynamic variable optimization to each method
				optimizeDynamicVariables(method, cpgen);
			}

			// Since optimizeDynamicVariables might update the methods, ensure they are set back
			Method[] optimizedMethods = new Method[methods.length];
			for (int i = 0; i < methods.length; i++) {
				optimizedMethods[i] = new MethodGen(methods[i], cgen.getClassName(), cpgen).getMethod();
			}

			cgen.setMethods(optimizedMethods); // Now this is the updated array
		} else if ((cgen.getClassName()).equals("comp0012.target.SimpleFolding"))
		{
			System.out.println("simple\n");
			cpgen = SimpleFoldingOptimization(cpgen);
		}



		// Update the constant pool and get the optimized JavaClass
		gen.setConstantPool(cpgen);
		this.optimized = gen.getJavaClass();
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