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
		//System.out.println("optimising general\n");
		//System.out.println("class name: " + cgen.getClassName() + "\n");

		if ((cgen.getClassName()).equals("comp0012.target.SimpleFolding"))
		{
			System.out.println("optimising simple folding\n");

			Method[] methods = gen.getMethods();

			for (Method method : methods) {
				//System.out.println("method\n");

				MethodGen methodGen = new MethodGen(method, gen.getClassName(), cpgen);
				InstructionList instructionList = methodGen.getInstructionList();

				if (instructionList != null) {
					for (InstructionHandle ih : instructionList.getInstructionHandles()) {
						//System.out.println("instruction\n");

						Instruction instruction = ih.getInstruction();

						// Check for arithmetic instructions
						/*
						if (instruction instanceof ArithmeticInstruction) {
							handleArithmeticInstruction(ih, instruction, instructionList, cpgen);
						}
						*/

						if (instruction instanceof IADD)
						{
							InstructionHandle prev1 = ih.getPrev();
							InstructionHandle prev2 = prev1 != null ? prev1.getPrev() : null;
							if (prev1 != null && prev2 != null) {
								Instruction prevInst1 = prev1.getInstruction();
								Instruction prevInst2 = prev2.getInstruction();

								if ((prevInst1 instanceof LDC) && (prevInst2 instanceof LDC))
								{
									System.out.println("found 2 loading instructions \n");

									Object obj1 = ((LDC) prevInst1).getValue(cpgen);
									Object obj2 = ((LDC) prevInst2).getValue(cpgen);

									if ((obj1 instanceof Integer) && (obj2 instanceof Integer))
									{

										System.out.println("adding 2 integers\n");

										Integer cnstValue = (Integer) obj1 + (Integer) obj2;
										System.out.println("number: " + cnstValue + "\n");

										instructionList.insert(ih, new PUSH(cpgen, cnstValue)); //adding the instruction at the correct handle
										System.out.println("added new instruction\n");

										try{
											instructionList.delete(ih);
											System.out.println("deleted add\n");
										} catch (TargetLostException e) {
											throw new RuntimeException(e);
										}
										try {
											instructionList.delete(prev1);
											System.out.println("deleted 1st load\n");
										} catch (TargetLostException e) {
											throw new RuntimeException(e);
										}
										try {
											instructionList.delete(prev2);
											System.out.println("deleted 2nd load\n");
										}  catch (TargetLostException e) {
											throw new RuntimeException(e);
										}

										//Instruction newInst = new PUSH(cpgen, cnstValue).getInstruction();
										//instructionList.append(newInst);

									}
								}
							}
						}
					}
					methodGen.setInstructionList(instructionList);
					System.out.println("updated the instructionList\n");
					methodGen.setMaxStack(); //stack frame size needs to be correctly calculated and set
					System.out.println("set the stack\n");
					gen.replaceMethod(method, methodGen.getMethod()); //'method' = original method --> replace this with the new method with the updated InstructionList
					System.out.println("replaced the method\n");
				}
			}
		}

		this.optimized = gen.getJavaClass(); //line from the original code
	}

	private void handleArithmeticInstruction(InstructionHandle ih, Instruction instruction, InstructionList il, ConstantPoolGen cpgen)
	{
		System.out.println("found arithmetic\n");

		ArithmeticInstruction ai = (ArithmeticInstruction) instruction;

		// Previous instructions are likely to be the constants if they are constant loading instructions
		InstructionHandle prev1 = ih.getPrev();
		InstructionHandle prev2 = prev1 != null ? prev1.getPrev() : null;
		int cnstValue = 0;

		if (prev1 != null && prev2 != null)
		{
			Instruction prevInst1 = prev1.getInstruction();
			Instruction prevInst2 = prev2.getInstruction();

			// Check if these instructions are loading constants
			if ((prevInst1 instanceof LDC) && (prevInst2 instanceof LDC))
			{
				System.out.println("found 2 loading instructions \n");

				Object obj1 = ((LDC) prevInst1).getValue(cpgen);
				Object obj2 = ((LDC) prevInst2).getValue(cpgen);

				if ((obj1 instanceof Integer) && (obj2 instanceof Integer))
				{
					if (ai instanceof IADD)
					{
						System.out.println("adding 2 integers\n");

						cnstValue = (Integer) obj1 + (Integer) obj2;
						System.out.println("number: " + cnstValue + "\n");
					}
				}

				try{
					il.delete(ih);
					System.out.println("deleted add\n");
				} catch (TargetLostException e) {
					throw new RuntimeException(e);
				}
				try {
					il.delete(prev1);
					System.out.println("deleted 1st load\n");
				} catch (TargetLostException e) {
					throw new RuntimeException(e);
				}
				try {
					il.delete(prev2);
					System.out.println("deleted 2nd load\n");
				}  catch (TargetLostException e) {
					throw new RuntimeException(e);
				}

				il.insert(ih, new PUSH(cpgen, cnstValue)); //replacing the last 3 instructions
				//Instruction newInst = new PUSH(cpgen, cnstValue).getInstruction();
				//il.append(newInst);
				System.out.println("added new instruction\n");

			}
		}
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
