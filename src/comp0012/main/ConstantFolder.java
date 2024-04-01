package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

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
		// Assuming result is always an integer for simplicity; adjust for other types
		int index = cpg.addInteger(result.intValue());
		il.insert(ih, new LDC(index));

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
		if (inst instanceof IADD && obj1 instanceof Integer && obj2 instanceof Integer) {
			return (Integer) obj1 + (Integer) obj2;
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

	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		// Implement your optimization here

		if ((cgen.getClassName()).equals("comp0012.target.SimpleFolding"))
		{
			cpgen = SimpleFoldingOptimization(cpgen);
		}

		gen.setConstantPool(cpgen);
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
