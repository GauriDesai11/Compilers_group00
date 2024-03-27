package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
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

	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		// Implement your optimization here

		Method[] methods = gen.getMethods();

		for (Method method : methods) {
			MethodGen methodGen = new MethodGen(method, gen.getClassName(), cpgen);
			InstructionList instructionList = methodGen.getInstructionList();

			if (instructionList != null) {
				for (InstructionHandle ih : instructionList.getInstructionHandles()) {
					Instruction instruction = ih.getInstruction();

					// Check for arithmetic instructions
					if (instruction instanceof ArithmeticInstruction) {
						handleArithmeticInstruction(ih, instruction, instructionList, cpgen);
					}
				}
				methodGen.setMaxStack(); //stack frame size needs to be correctly calculated and set
				gen.replaceMethod(method, methodGen.getMethod()); //'method' = original method --> replace this with the new method with the updated InstructionList
			}
		}

		this.optimized = gen.getJavaClass(); //line from the original code
	}

	private void handleArithmeticInstruction(InstructionHandle ih, Instruction instruction, InstructionList il, ConstantPoolGen cpgen) {

		ArithmeticInstruction ai = (ArithmeticInstruction) instruction;

		// Previous instructions are likely to be the constants if they are constant loading instructions
		InstructionHandle prev1 = ih.getPrev();
		InstructionHandle prev2 = prev1 != null ? prev1.getPrev() : null;

		if (prev1 != null && prev2 != null) {
			Instruction prevInst1 = prev1.getInstruction();
			Instruction prevInst2 = prev2.getInstruction();

			// Check if these instructions are loading constants
			if (isConstantLoadingInstruction(prevInst1) && isConstantLoadingInstruction(prevInst2)) {
				Number constantValue = computeConstantValue(prevInst1, prevInst2, ai, cpgen);

				try {
					il.delete(ih, prev2);
				} catch (TargetLostException e) {
					// Handle cases where the instruction was a target of a branch instruction.
					/*
					for (InstructionHandle target : e.getTargets()) {
						for (InstructionTargeter targeter : target.getTargeters()) {
							targeter.updateTarget(target, new_target);
						}
					}
					*/

				}

				il.insert(ih, new PUSH(cpgen, constantValue)); //replacing the last 3 instructions
			}
		}
	}

	private boolean isConstantLoadingInstruction(Instruction inst) {
		// Check if the instruction is one of the constant loading instructions
		return (inst instanceof LDC);
				//|| (inst instanceof LDC2_W) || (inst instanceof BIPUSH) || (inst instanceof SIPUSH) || (inst instanceof ICONST) || (inst instanceof FCONST) || (inst instanceof LCONST) || (inst instanceof DCONST);
	}

	private Number computeConstantValue(Instruction inst1, Instruction inst2, ArithmeticInstruction ai, ConstantPoolGen cpgen) {
		Number value1 = getConstantValue(inst1, cpgen);
		Number value2 = getConstantValue(inst2, cpgen);

		if (ai instanceof IADD) {
			System.out.println("number: " + value1.intValue() + value2.intValue());
			return value1.intValue() + value2.intValue();
		}

		return 0;
	}

	private Number getConstantValue(Instruction inst, ConstantPoolGen cpgen) {
		LDC ldc = (LDC) inst;
		Object value = ldc.getValue(cpgen);

		if (value instanceof Number) {
			return (Number) value;
		}

		return 0;
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
