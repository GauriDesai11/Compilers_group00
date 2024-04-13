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

		// Maps to hold the current constant value and last assignment handle for each variable index
		Map<Integer, Number> variableValues = new HashMap<>();
		Map<Integer, InstructionHandle> variableAssignments = new HashMap<>();

		if (il != null) {
			for (InstructionHandle ih : il.getInstructionHandles()) {
				Instruction inst = ih.getInstruction();

				if (inst instanceof StoreInstruction) {
					StoreInstruction store = (StoreInstruction) inst;
					if (isConstantPush(ih.getPrev().getInstruction())) {
						Number value = getConstantValue(ih.getPrev(), cpgen);
						if (value != null) {
							// Record the constant value and the location of the assignment
							variableValues.put(store.getIndex(), value);
							variableAssignments.put(store.getIndex(), ih);
						}
					}
				} else if (inst instanceof LoadInstruction) {
					LoadInstruction load = (LoadInstruction) inst;
					int index = load.getIndex();
					if (variableValues.containsKey(index)) {
						// Check if there have been no reassignments to the variable since its last assignment
						if (!hasBeenReassigned(index, variableAssignments.get(index), ih)) {
							replaceLoadWithConstant(il, ih, variableValues.get(index), cpgen);
							il.setPositions(true);
						} else {
							// The variable has been reassigned, remove it from the maps
							variableValues.remove(index);
							variableAssignments.remove(index);
						}
					}
				} // No else needed here, as the above condition covers the case
			}

			// Apply changes to the method
			mg.setInstructionList(il);
			mg.setMaxStack();
			mg.setMaxLocals();
			this.gen.replaceMethod(method, mg.getMethod());
		}
	}



	public void optimize() {
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		// Check if the class is the specific target for dynamic variable folding
		if ((cgen.getClassName()).equals("comp0012.target.DynamicVariableFolding")) {
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